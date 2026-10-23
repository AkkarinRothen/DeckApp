package com.deckapp.core.data.repository

import com.deckapp.core.model.StartPage

import com.deckapp.core.model.GeneralPreferences

import android.content.Context
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.model.AppTheme
import com.deckapp.core.model.TableAppearance
import com.deckapp.core.model.TableDensity
import com.deckapp.core.model.TextureIntensity
import com.deckapp.core.model.ThemeAccent
import com.deckapp.core.model.AppearancePreset


import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("deckapp_settings", Context.MODE_PRIVATE)

    override fun getBoardSessionMemory(sessionId: Long) =
        BoardMemoryCodec.decode(prefs.getString("board_session_$sessionId", null))

    override fun setBoardSessionMemory(sessionId: Long, memory: com.deckapp.core.model.BoardSessionMemory) {
        prefs.edit().putString("board_session_$sessionId", BoardMemoryCodec.encode(memory)).apply()
    }

    override fun getSavedBoardTemplates(): List<com.deckapp.core.model.SavedBoardTemplate> =
        BoardMemoryCodec.decodeTemplates(prefs.getString("saved_board_templates", null))

    override fun setSavedBoardTemplates(templates: List<com.deckapp.core.model.SavedBoardTemplate>) {
        prefs.edit().putString("saved_board_templates", BoardMemoryCodec.encodeTemplates(templates)).apply()
    }

    private val generalPreferencesFlow = MutableStateFlow(
        GeneralPreferences(
            startPage = runCatching {
                StartPage.valueOf(prefs.getString("start_page", "LIBRARY")!!)
            }.getOrDefault(StartPage.LIBRARY),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            hapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            textScale = prefs.getFloat("text_scale", 1f).takeIf { it.isFinite() }?.coerceIn(1f, 1.3f) ?: 1f
        )
    )

    override fun getGeneralPreferences() = generalPreferencesFlow.value
    override fun observeGeneralPreferences() = generalPreferencesFlow.asStateFlow()
    override fun setGeneralPreferences(preferences: GeneralPreferences) {
        val safe = preferences.copy(textScale = preferences.textScale.takeIf { it.isFinite() }?.coerceIn(1f, 1.3f) ?: 1f)
        prefs.edit().putString("start_page", safe.startPage.name)
            .putBoolean("keep_screen_on", safe.keepScreenOn)
            .putBoolean("haptics_enabled", safe.hapticsEnabled)
            .putFloat("text_scale", safe.textScale).apply()
        generalPreferencesFlow.value = safe
    }

    private val _appThemeFlow = MutableStateFlow(loadInitialTheme())
    private val tableAppearanceFlow = MutableStateFlow(
        TableAppearance(
            texture = runCatching {
                TextureIntensity.valueOf(prefs.getString("table_texture", null).orEmpty())
            }.getOrDefault(TextureIntensity.SOFT),
            density = runCatching {
                TableDensity.valueOf(prefs.getString("table_density", null).orEmpty())
            }.getOrDefault(TableDensity.COMFORTABLE),
            accent = runCatching {
                ThemeAccent.valueOf(prefs.getString("theme_accent", null).orEmpty())
            }.getOrDefault(ThemeAccent.THEME)
        )
    )

    override fun getTableAppearance(): TableAppearance = tableAppearanceFlow.value

    override fun setTableAppearance(appearance: TableAppearance) {
        prefs.edit()
            .putString("table_texture", appearance.texture.name)
            .putString("table_density", appearance.density.name)
            .putString("theme_accent", appearance.accent.name)
            .apply()
        tableAppearanceFlow.value = appearance
    }

    override fun observeTableAppearance(): Flow<TableAppearance> = tableAppearanceFlow.asStateFlow()

    override fun getAppearancePresets(): List<AppearancePreset> =
        AppearancePresetCodec.decode(prefs.getString("appearance_presets", "[]") ?: "[]")

    private fun writePresets(presets: List<AppearancePreset>) {
        prefs.edit().putString("appearance_presets", AppearancePresetCodec.encode(presets)).apply()
    }
    override fun saveAppearancePreset(name: String) {
        val trimmed = name.trim().take(60)
        if (trimmed.isEmpty()) return
        val presets = getAppearancePresets()
        val existing = presets.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
        val preset = AppearancePreset(existing?.id ?: UUID.randomUUID().toString(), trimmed, getAppTheme(), getTableAppearance())
        writePresets(presets.filterNot { it.id == preset.id } + preset)
    }

    override fun applyAppearancePreset(id: String) {
        val preset = getAppearancePresets().firstOrNull { it.id == id } ?: return
        setAppTheme(preset.theme)
        setTableAppearance(preset.appearance)
    }

    override fun deleteAppearancePreset(id: String) = writePresets(getAppearancePresets().filterNot { it.id == id })

    private fun loadInitialTheme(): AppTheme {
        val themeName = prefs.getString("app_theme", AppTheme.DARK.name) ?: AppTheme.DARK.name
        return try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.DARK
        }
    }

    override fun getAppTheme(): AppTheme = _appThemeFlow.value

    override fun setAppTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
        _appThemeFlow.value = theme
    }

    override fun observeAppTheme(): Flow<AppTheme> = _appThemeFlow.asStateFlow()

    override fun getGeminiApiKey(): String {
        return prefs.getString("gemini_api_key", "") ?: ""
    }

    override fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    override fun getJpegQuality(): Int {
        return prefs.getInt("jpeg_quality", 90)
    }

    override fun setJpegQuality(quality: Int) {
        prefs.edit().putInt("jpeg_quality", quality).apply()
    }

    override fun getAutoVisionEnabled(): Boolean {
        return prefs.getBoolean("auto_vision_enabled", true)
    }

    override fun setAutoVisionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_vision_enabled", enabled).apply()
    }

    override fun getSimplifiedModeEnabled(): Boolean {
        return prefs.getBoolean("simplified_mode_enabled", false)
    }

    override fun setSimplifiedModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("simplified_mode_enabled", enabled).apply()
    }
}

