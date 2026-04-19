package com.deckapp.core.domain.repository

import android.graphics.Bitmap

data class ReferenceTableAiResult(
    val headers: List<String>,
    val rows: List<List<String>>
)

interface AiReferenceRepository {
    suspend fun recognizeReferenceTableFromImage(bitmap: Bitmap, apiKey: String): ReferenceTableAiResult
}
