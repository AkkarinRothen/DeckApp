package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.AiStreamEvent
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateTableWithAiUseCase @Inject constructor(
    private val aiRepository: AiTableRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(
        prompt: String,
        onRetryWait: (suspend (Long) -> Unit)? = null
    ): Flow<AiStreamEvent> {
        val apiKey = settingsRepository.getGeminiApiKey()
        return aiRepository.generateTable(prompt, apiKey, onRetryWait)
    }
}
