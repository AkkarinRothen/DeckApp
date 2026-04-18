package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import com.deckapp.core.model.Condition
import javax.inject.Inject

/**
 * Añade o quita una condición de estado a una criatura.
 * Registra automáticamente el cambio en el log de combate para enriquecer la narrativa.
 */
class ToggleConditionUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(creatureId: Long, condition: Condition) {
        val creature = encounterRepository.getCreatureById(creatureId) ?: return
        
        val isAdding = !creature.conditions.contains(condition)
        val newConditions = if (isAdding) {
            creature.conditions + condition
        } else {
            creature.conditions - condition
        }

        encounterRepository.updateCreature(creature.copy(conditions = newConditions))

        // Registro narrativo en el log
        val conditionName = formatConditionName(condition)
        val message = if (isAdding) {
            "${creature.name} ahora está $conditionName."
        } else {
            "${creature.name} ya no está $conditionName."
        }

        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = creature.encounterId,
                message = message,
                type = CombatLogType.META
            )
        )
    }

    private fun formatConditionName(condition: Condition): String = when (condition) {
        Condition.BLINDED -> "Cegado"
        Condition.CHARMED -> "Encantado"
        Condition.DEAFENED -> "Sordo"
        Condition.EXHAUSTED -> "Agotado"
        Condition.FRIGHTENED -> "Asustado"
        Condition.GRAPPLED -> "Agarrado"
        Condition.INCAPACITATED -> "Incapacitado"
        Condition.INVISIBLE -> "Invisible"
        Condition.PARALYZED -> "Paralizado"
        Condition.PETRIFIED -> "Petrificado"
        Condition.POISONED -> "Envenenado"
        Condition.PRONE -> "Derribado"
        Condition.RESTRAINED -> "Apresado"
        Condition.STUNNED -> "Aturdido"
        Condition.UNCONSCIOUS -> "Inconsciente"
    }
}
