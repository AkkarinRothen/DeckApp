package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

@Singleton
class UpdateChoirCombatStateUseCase @Inject constructor(private val repository: EncounterRepository) {
    private val mutex = Mutex()

    suspend operator fun invoke(creatureId: Long, command: ChoirCombatCommand) = mutex.withLock {
        val creature = repository.getCreatureById(creatureId) ?: return@withLock
        val profile = ChoirProfile.fromNotes(creature.notes) ?: return@withLock
        val previous = ChoirCombatState.fromNotes(creature.notes)
        val state = when (command) {
            ChoirCombatCommand.ADD_MAJOR -> previous.copy(major = previous.major + 1)
            ChoirCombatCommand.REMOVE_MAJOR -> previous.copy(major = (previous.major - 1).coerceAtLeast(0))
            ChoirCombatCommand.ADD_MINOR -> previous.copy(minor = previous.minor + 1)
            ChoirCombatCommand.REMOVE_MINOR -> previous.copy(minor = (previous.minor - 1).coerceAtLeast(0))
            ChoirCombatCommand.ROLL_ACTION -> previous.copy(actionRoll = Random.nextInt(1, 11))
            ChoirCombatCommand.ROLL_TARGET -> previous.copy(targetRoll = Random.nextInt(1, 7))
        }
        repository.updateCreature(creature.copy(notes = state.writeTo(creature.notes)))
        val detail = when {
            command == ChoirCombatCommand.ROLL_ACTION ->
                "Acción d10=${state.actionRoll}: ${ChoirCombatRules.action(profile, state.major, state.actionRoll)}"
            command == ChoirCombatCommand.ROLL_TARGET ->
                "Objetivo d6=${state.targetRoll}: ${ChoirCombatRules.target(state.targetRoll)}"
            else -> "Heridas: ${state.major} mayores, ${state.minor} menores"
        }
        repository.recordLog(CombatLogEntry(encounterId = creature.encounterId,
            message = "${creature.name}: $detail", type = CombatLogType.META))
    }
}
