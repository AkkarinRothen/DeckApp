package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.AiReferenceRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.model.ImportPreviewData
import com.deckapp.core.model.ReferenceImportSource
import javax.inject.Inject

class RecognizeReferenceTableFromImageUseCase @Inject constructor(
    private val aiRepository: AiReferenceRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Result<ImportPreviewData> = runCatching {
        val apiKey = settingsRepository.getGeminiApiKey()
        val result = aiRepository.recognizeReferenceTableFromImage(bitmap, apiKey)
        ImportPreviewData(result.headers, result.rows, ReferenceImportSource.OCR_IMAGE)
    }
}
