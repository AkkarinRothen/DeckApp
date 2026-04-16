package com.deckapp.core.model

/**
 * Representa un encuentro de combate.
 * Puede ser un encuentro "preparado" (linkedSessionId = null) 
 * o un encuentro activo en una sesión.
 */
data class Encounter(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val creatures: List<EncounterCreature> = emptyList(),
    val linkedSessionId: Long? = null,
    val isActive: Boolean = false,
    val currentRound: Int = 1,
    val currentTurnIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Representa una criatura o PJ dentro de un encuentro.
 */
data class EncounterCreature(
    val id: Long = 0,
    val encounterId: Long,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val armorClass: Int = 10,
    val initiativeBonus: Int = 0,
    val initiativeRoll: Int? = null,     // Valor del dado (1d20)
    val conditions: Set<Condition> = emptySet(),
    val notes: String = "",
    val sortOrder: Int = 0
) {
    /** Iniciativa total calculada (Roll + Bonus) */
    val initiativeTotal: Int? get() = initiativeRoll?.let { it + initiativeBonus }
}

/**
 * Condiciones de estado (basado en SRD 5e).
 */
enum class Condition {
    BLINDED, CHARMED, DEAFENED, EXHAUSTED, FRIGHTENED,
    GRAPPLED, INCAPACITATED, INVISIBLE, PARALYZED,
    PETRIFIED, POISONED, PRONE, RESTRAINED, STUNNED, UNCONSCIOUS
}
