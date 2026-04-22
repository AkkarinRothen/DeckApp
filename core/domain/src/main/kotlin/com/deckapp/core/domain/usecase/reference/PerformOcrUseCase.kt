package com.deckapp.core.domain.usecase.reference

import android.graphics.Bitmap
import com.deckapp.core.model.OcrResult
import javax.inject.Inject

class PerformOcrUseCase @Inject constructor() {
    suspend operator fun invoke(bitmap: Bitmap): OcrResult {
        // TODO: Implementar usando AiReferenceRepository + Gemini API
        return OcrResult(
            text = "OCR no implementado en esta versión local. Requiere conexión con la API de Gemini.",
            confidence = 1.0f
        )
    }
}
