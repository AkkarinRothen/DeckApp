package com.deckapp.core.model

/** Sesión de juego activa */
data class Session(
    val id: Long = 0,
    val name: String,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val scheduledDate: Long? = null,
    val summary: String? = null,
    val showCardTitles: Boolean = true,
    val dmNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

enum class SessionStatus {
    PLANNED, ACTIVE, COMPLETED
}

/** Referencia de un mazo asignado a una sesión */
data class SessionDeckRef(
    val sessionId: Long,
    val stackId: Long,
    val drawModeOverride: DrawMode? = null,   // override del DrawMode del mazo
    val sortOrder: Int = 0                    // orden de las tabs en SessionScreen
)

/**
 * Evento del log de sesión (append-only).
 * El estado actual de la sesión se reconstruye reproduciendo estos eventos.
 * Esto habilita: undo, historial de sesión y crash recovery.
 */
data class DrawEvent(
    val id: Long = 0,
    val sessionId: Long,
    val cardId: Long?,
    val action: DrawAction,
    val metadata: String = "",  // JSON opcional (ej: nombre del jugador al hacer PASS)
    val timestamp: Long = System.currentTimeMillis()
)

/** Referencia de una tabla asignada a una sesión */
data class SessionTableRef(
    val sessionId: Long,
    val tableId: Long,
    val sortOrder: Int = 0
)
