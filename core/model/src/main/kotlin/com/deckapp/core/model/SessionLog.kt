package com.deckapp.core.model

/**
 * Evento registrado en la bitácora o historial de una sesión de juego.
 */
data class SessionLogEntry(
    val id: Long = 0,
    val sessionId: Long,
    val sceneId: Long? = null,
    val eventType: SessionLogEventType,
    val title: String,
    val details: String = "",
    val payloadJson: String = "{}",
    val timestamp: Long = System.currentTimeMillis()
)

enum class SessionLogEventType {
    DICE,
    CARD,
    TOKEN,
    ORACLE,
    CLOCK,
    MANUAL
}

/**
 * Bloque de notas rápidas en Markdown asociadas a una sesión (y opcionalmente a una escena).
 */
data class SessionNote(
    val id: Long = 0,
    val sessionId: Long,
    val sceneId: Long? = null,
    val contentMarkdown: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
