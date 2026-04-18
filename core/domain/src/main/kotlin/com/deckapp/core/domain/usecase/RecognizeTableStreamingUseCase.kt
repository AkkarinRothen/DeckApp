package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.AiStreamEvent
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RecognizeTableStreamingUseCase @Inject constructor(
    private val aiRepository: AiTableRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(
        bitmap: Bitmap,
        onRetryWait: (suspend (Long) -> Unit)? = null
    ): Flow<AiStreamEvent> {
        val apiKey = settingsRepository.getGeminiApiKey()
        return aiRepository.streamTableFromImage(bitmap, apiKey, onRetryWait)
    }
}
