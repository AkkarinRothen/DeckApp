package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TensionClock(
    val id: Long = 0L,
    val sessionId: Long = 0L,
    val name: String = "",
    val segments: Int = 4,
    val progress: Int = 0,
    val colorHex: String? = null,
    val triggerDescription: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
