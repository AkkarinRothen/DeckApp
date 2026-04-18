package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import com.deckapp.core.model.EncounterCreature
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Calcula el orden de combate y lo persiste en el campo sortOrder de cada criatura.
 * Resuelve empates de forma determinista (Iniciativa Total > Bono de Iniciativa > Nombre).
 * Esto asegura que el tracker de turnos sea estable y no salte entre criaturas con igual iniciativa.
 */
class CalculateInitiativeOrderUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(encounterId: Long) {
        val encounter = encounterRepository.getEncounterById(encounterId).first() ?: return
        
        // Ordenar con lógica de desempate estándar TTRPG
        val sorted = encounter.creatures.sortedWith(
            compareByDescending<EncounterCreature> { it.initiativeTotal ?: 0 }
                .thenByDescending { it.initiativeBonus }
                .thenBy { it.name }
        )

        // Persistir el orden explícito en la base de datos
        sorted.forEachIndexed { index, creature ->
            if (creature.sortOrder != index) {
                encounterRepository.updateCreature(creature.copy(sortOrder = index))
            }
        }

        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = encounterId,
                message = "Orden de combate recalculado",
                type = CombatLogType.META
            )
        )
    }
}
