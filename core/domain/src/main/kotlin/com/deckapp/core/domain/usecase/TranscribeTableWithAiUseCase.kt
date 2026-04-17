package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.AiTableSuggestions
import com.deckapp.core.domain.repository.SettingsRepository
import javax.inject.Inject

class TranscribeTableWithAiUseCase @Inject constructor(
    private val aiRepository: AiTableRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(rawText: String): AiTableSuggestions {
        val apiKey = settingsRepository.getGeminiApiKey()
        return aiRepository.reconstructTable(rawText, apiKey)
    }
}
