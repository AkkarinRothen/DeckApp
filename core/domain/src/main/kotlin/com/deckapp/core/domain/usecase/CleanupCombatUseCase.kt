package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import javax.inject.Inject

/**
 * Gestiona el mantenimiento del combate, como la eliminación segura de participantes.
 * Asegura que el orden de iniciativa se mantenga coherente tras cambios estructurales.
 */
class CleanupCombatUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository,
    private val calculateInitiativeOrderUseCase: CalculateInitiativeOrderUseCase
) {
    /**
     * Elimina una criatura del encuentro y recalcula el orden de iniciativa para evitar inconsistencias.
     */
    suspend fun removeCreature(encounterId: Long, creatureId: Long) {
        encounterRepository.deleteCreature(creatureId)
        
        // Recalculamos el orden explícito para evitar huecos en sortOrder 
        // que puedan afectar a NextTurnUseCase.
        calculateInitiativeOrderUseCase(encounterId)
    }
}
