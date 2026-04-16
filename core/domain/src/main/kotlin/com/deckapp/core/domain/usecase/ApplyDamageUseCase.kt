package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import com.deckapp.core.model.Condition
import javax.inject.Inject

/**
 * Aplica daño o curación a una criatura.
 * Clampea el resultado entre 0 y maxHp.
 * Si HP llega a 0, añade automáticamente la condición UNCONSCIOUS.
 */
class ApplyDamageUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(creatureId: Long, delta: Int) {
        val creature = encounterRepository.getCreatureById(creatureId) ?: return
        
        val newHp = (creature.currentHp + delta).coerceIn(0, creature.maxHp)
        
        val newConditions = if (newHp == 0) {
            creature.conditions + Condition.UNCONSCIOUS
        } else if (creature.currentHp == 0 && newHp > 0) {
            // Si resucita/cura desde 0, quitamos inconsciente
            creature.conditions - Condition.UNCONSCIOUS
        } else {
            creature.conditions
        }

        encounterRepository.updateCreature(
            creature.copy(
                currentHp = newHp,
                conditions = newConditions
            )
        )

        // Registrar en el log
        val message = if (delta < 0) {
            "${creature.name} recibe ${-delta} de daño ($newHp/${creature.maxHp})"
        } else {
            "${creature.name} recupera $delta de HP ($newHp/${creature.maxHp})"
        }
        
        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = creature.encounterId,
                message = message,
                type = if (delta < 0) CombatLogType.DAMAGE else CombatLogType.HEAL
            )
        )
    }
}
