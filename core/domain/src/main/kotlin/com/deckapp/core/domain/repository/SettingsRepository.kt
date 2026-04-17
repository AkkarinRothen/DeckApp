package com.deckapp.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getGeminiApiKey(): String
    fun setGeminiApiKey(key: String)
    fun getJpegQuality(): Int
    fun setJpegQuality(quality: Int)
    fun getAutoVisionEnabled(): Boolean
    fun setAutoVisionEnabled(enabled: Boolean)
}
