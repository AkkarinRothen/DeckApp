package com.deckapp.core.model

/**
 * Participante unificado en el flujo de iniciativa de combate (Creature o PJ).
 */
sealed class UnifiedParticipant {
    abstract val initiative: Int
    abstract val stableId: String
    abstract val name: String
    open val ac: Int get() = 10

    data class Creature(val data: EncounterCreature) : UnifiedParticipant() {
        override val initiative: Int = data.initiativeTotal ?: 0
        override val stableId: String = "creature_${data.id}"
        override val name: String = data.name
        override val ac: Int get() = data.armorClass
    }

    data class Player(val data: PlayerInitiativeParticipant) : UnifiedParticipant() {
        override val initiative: Int = data.initiativeTotal
        override val stableId: String = "player_${data.id}"
        override val name: String = data.name
        override val ac: Int get() = 10
    }
}
