package com.deckapp.core.domain.repository

import android.graphics.Bitmap
import com.deckapp.core.model.TableEntry
import kotlinx.coroutines.flow.Flow

data class AiTableSuggestions(
    val entries: List<TableEntry>,
    val suggestedName: String = "",
    val suggestedFormula: String = "1d6",
    val suggestedCategory: String = ""
)

sealed class AiStreamEvent {
    data class Metadata(val name: String, val formula: String, val category: String) : AiStreamEvent()
    data class Entry(val entry: TableEntry) : AiStreamEvent()
}

interface AiTableRepository {
    suspend fun reconstructTable(rawText: String, apiKey: String): AiTableSuggestions
    fun streamTableFromImage(
        bitmap: Bitmap,
        apiKey: String,
        onRetryWait: (suspend (Long) -> Unit)? = null
    ): Flow<AiStreamEvent>
}

class AiApiException(message: String, val causeException: Throwable? = null) : Exception(message, causeException)
