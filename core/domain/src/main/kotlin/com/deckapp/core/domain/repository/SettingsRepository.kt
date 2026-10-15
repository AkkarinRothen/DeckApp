package com.deckapp.core.domain.repository

import com.deckapp.core.model.AppTheme
import com.deckapp.core.model.TableAppearance
import com.deckapp.core.model.AppearancePreset
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getGeminiApiKey(): String
    fun setGeminiApiKey(key: String)
    fun getJpegQuality(): Int
    fun setJpegQuality(quality: Int)
    fun getAutoVisionEnabled(): Boolean
    fun setAutoVisionEnabled(enabled: Boolean)
    fun getSimplifiedModeEnabled(): Boolean
    fun setSimplifiedModeEnabled(enabled: Boolean)
    fun getAppTheme(): AppTheme
    fun setAppTheme(theme: AppTheme)
    fun observeAppTheme(): Flow<AppTheme>
    fun getTableAppearance(): TableAppearance
    fun setTableAppearance(appearance: TableAppearance)
    fun observeTableAppearance(): Flow<TableAppearance>
    fun getAppearancePresets(): List<AppearancePreset>
    fun saveAppearancePreset(name: String)
    fun applyAppearancePreset(id: String)
    fun deleteAppearancePreset(id: String)
}
