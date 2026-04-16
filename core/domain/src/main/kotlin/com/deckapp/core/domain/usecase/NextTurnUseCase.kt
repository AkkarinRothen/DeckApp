package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Avanza el turno del combate.
 * Incrementa el índice de turno; si llega al final de la lista,
 * vuelve al inicio e incrementa el contador de ronda.
 */
class NextTurnUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(encounterId: Long) {
        val encounter = encounterRepository.getEncounterById(encounterId).first() ?: return
        
        // El orden de combate es descendente por iniciativa (total)
        val sortedCreatures = encounter.creatures.sortedByDescending { it.initiativeTotal ?: 0 }
        if (sortedCreatures.isEmpty()) return

        var nextIndex = encounter.currentTurnIndex + 1
        var nextRound = encounter.currentRound

        if (nextIndex >= sortedCreatures.size) {
            nextIndex = 0
            nextRound++
        }

        encounterRepository.saveEncounter(
            encounter.copy(
                currentTurnIndex = nextIndex,
                currentRound = nextRound
            )
        )

        // Loguear cambio de ronda si aplica
        if (nextRound > encounter.currentRound) {
            encounterRepository.recordLog(
                CombatLogEntry(
                    encounterId = encounterId,
                    message = "Inicio de la Ronda $nextRound",
                    type = CombatLogType.ROUND_START
                )
            )
        }

        // Loguear nuevo turno
        val nextCreature = sortedCreatures[nextIndex]
        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = encounterId,
                message = "Turno de ${nextCreature.name}",
                type = CombatLogType.TURN_START
            )
        )
    }
}
