package com.deckapp.core.domain.repository

import android.graphics.Bitmap
import com.deckapp.core.model.OcrBlock

/**
 * Interfaz para el reconocimiento de texto (OCR) sobre imágenes.
 */
interface OcrRepository {
    /**
     * Procesa un [Bitmap] y devuelve [Result.success] con los bloques detectados,
     * o [Result.failure] con una [OcrException] si ML Kit falla.
     */
    suspend fun recognizeText(bitmap: Bitmap): Result<List<OcrBlock>>
}

/** Error específico del pipeline OCR, distinguible de otros fallos en el ViewModel. */
class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)
