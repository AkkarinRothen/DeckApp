package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.AiTableSuggestions
import com.deckapp.core.domain.repository.SettingsRepository
import javax.inject.Inject

class RecognizeTableFromImageUseCase @Inject constructor(
    private val aiRepository: AiTableRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): AiTableSuggestions {
        val apiKey = settingsRepository.getGeminiApiKey()
        return aiRepository.recognizeTableFromImage(bitmap, apiKey)
    }
}
