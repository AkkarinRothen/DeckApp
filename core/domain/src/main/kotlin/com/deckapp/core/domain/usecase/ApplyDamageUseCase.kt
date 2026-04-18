package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import com.deckapp.core.model.Condition
import javax.inject.Inject

/**
 * Aplica daño o curación a una criatura.
 * Clampea el resultado entre 0 y maxHp.
 * Automatiza condiciones de inconsciencia y narrativa del log.
 */
class ApplyDamageUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(creatureId: Long, delta: Int) {
        val creature = encounterRepository.getCreatureById(creatureId) ?: return
        
        val oldHp = creature.currentHp
        val newHp = (oldHp + delta).coerceIn(0, creature.maxHp)
        
        val addedConditions = mutableListOf<String>()
        val removedConditions = mutableListOf<String>()
        var newConditions = creature.conditions

        // Lógica automática de Inconsciencia
        if (newHp == 0 && oldHp > 0) {
            newConditions = newConditions + Condition.UNCONSCIOUS
            addedConditions.add("Inconsciente")
        } else if (newHp > 0 && oldHp == 0 && newConditions.contains(Condition.UNCONSCIOUS)) {
            newConditions = newConditions - Condition.UNCONSCIOUS
            removedConditions.add("Inconsciente")
        }

        encounterRepository.updateCreature(
            creature.copy(
                currentHp = newHp,
                conditions = newConditions
            )
        )

        // Registro enriquecido en el log
        val action = if (delta < 0) "recibe ${-delta} de daño" else "recupera $delta de vida"
        val hpStatus = "($newHp/${creature.maxHp})"
        
        var logMessage = "${creature.name} $action $hpStatus."
        if (addedConditions.isNotEmpty()) logMessage += " ¡Ha caído ${addedConditions.joinToString()}!"
        if (removedConditions.isNotEmpty()) logMessage += " Ya no está ${removedConditions.joinToString()}."
        
        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = creature.encounterId,
                message = logMessage,
                type = if (delta < 0) CombatLogType.DAMAGE else CombatLogType.HEAL
            )
        )
    }
}
