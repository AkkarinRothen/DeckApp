package com.deckapp.core.model

/**
 * Representa un evento en el log de combate.
 */
data class CombatLogEntry(
    val id: Long = 0,
    val encounterId: Long,
    val message: String,
    val type: CombatLogType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CombatLogType {
    DAMAGE,
    HEAL,
    TURN_START,
    ROUND_START,
    CONDITION_GAIN,
    CONDITION_LOSS,
    META
}
