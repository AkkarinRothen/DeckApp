package com.deckapp.core.model

import kotlinx.serialization.Serializable

/** Sesión de Mythic GM Emulator */
@Serializable
data class MythicSession(
    val id: Long = 0,
    val name: String,
    val chaosFactor: Int = 5,          // 1–9
    val sceneNumber: Int = 1,
    val actionTableId: Long? = null,   // tabla Acción para Eventos
    val subjectTableId: Long? = null,  // tabla Sujeto para Eventos
    val createdAt: Long = System.currentTimeMillis()
)

/** Personaje en la lista de Mythic */
@Serializable
data class MythicCharacter(
    val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val notes: String = "",
    val sortOrder: Int = 0
)

/** Hilo (trama) activo o resuelto */
@Serializable
data class MythicThread(
    val id: Long = 0,
    val sessionId: Long,
    val description: String,
    val isResolved: Boolean = false,
    val sortOrder: Int = 0
)

/** Registro de una tirada al Oráculo o Evento */
@Serializable
data class MythicRoll(
    val id: Long = 0,
    val sessionId: Long,
    val question: String = "",
    val probability: ProbabilityLevel,
    val chaosFactor: Int,
    val roll: Int,                     // 1–100
    val result: FateResult,
    val isRandomEvent: Boolean = false,
    val eventFocus: String = "",
    val eventAction: String = "",
    val eventSubject: String = "",
    val sceneNumber: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ProbabilityLevel {
    CERTAIN, NEARLY_CERTAIN, VERY_LIKELY, LIKELY,
    FIFTY_FIFTY, UNLIKELY, VERY_UNLIKELY, NEARLY_IMPOSSIBLE, IMPOSSIBLE,
    NARRATIVE
}

enum class FateResult { EXCEPTIONAL_YES, YES, NO, EXCEPTIONAL_NO, NONE }
