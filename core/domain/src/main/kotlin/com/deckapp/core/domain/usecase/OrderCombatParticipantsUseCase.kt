package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*

/** Choir fixed initiatives share the same queue as player initiatives. */
object OrderCombatParticipantsUseCase {
    operator fun invoke(creatures: List<EncounterCreature>, players: List<PlayerInitiativeParticipant>): List<UnifiedParticipant> {
        val participants = creatures.map { UnifiedParticipant.Creature(it) } + players.map { UnifiedParticipant.Player(it) }
        return if (creatures.any { ChoirProfile.fromNotes(it.notes) != null }) {
            participants.sortedWith(compareByDescending<UnifiedParticipant> { it.initiative }.thenBy { it.stableId })
        } else {
            participants.sortedWith(compareBy<UnifiedParticipant> {
                if (it is UnifiedParticipant.Creature) it.data.sortOrder else -1
            }.thenByDescending { it.initiative })
        }
    }
}
