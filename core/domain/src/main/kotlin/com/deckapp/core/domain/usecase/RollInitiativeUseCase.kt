package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Tira iniciativa para todas las criaturas de un encuentro que aún no tengan un valor asignado.
 * Utiliza DiceEvaluator para mantener la consistencia del motor de dados.
 */
class RollInitiativeUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    /**
     * @param encounterId ID del encuentro.
     * @param forceAll Si es true, vuelve a tirar iniciativa incluso para los que ya tienen valor.
     */
    suspend operator fun invoke(encounterId: Long, forceAll: Boolean = false) {
        val encounter = encounterRepository.getEncounterById(encounterId).first() ?: return
        
        encounter.creatures.forEach { creature ->
            if (forceAll || creature.initiativeRoll == null) {
                // Usamos el motor centralizado de dados
                val roll = DiceEvaluator.evaluate("1d20")
                val updated = creature.copy(initiativeRoll = roll)
                encounterRepository.updateCreature(updated)
                
                encounterRepository.recordLog(
                    CombatLogEntry(
                        encounterId = encounterId,
                        message = "${creature.name} saca un $roll en iniciativa (Total: ${updated.initiativeTotal})",
                        type = CombatLogType.META
                    )
                )
            }
        }
    }
}
