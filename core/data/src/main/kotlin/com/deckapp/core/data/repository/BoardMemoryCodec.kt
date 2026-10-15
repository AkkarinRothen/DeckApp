package com.deckapp.core.data.repository

import com.deckapp.core.model.BoardSessionMemory
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object BoardMemoryCodec {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    fun encode(memory: BoardSessionMemory): String = json.encodeToString(memory)
    fun decode(value: String?): BoardSessionMemory = value?.let {
        runCatching { json.decodeFromString<BoardSessionMemory>(it) }.getOrNull()
    } ?: BoardSessionMemory()
}
