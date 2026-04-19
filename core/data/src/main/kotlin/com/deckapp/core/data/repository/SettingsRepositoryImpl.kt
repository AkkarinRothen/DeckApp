package com.deckapp.core.data.repository

import android.content.Context
import com.deckapp.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("deckapp_settings", Context.MODE_PRIVATE)

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
