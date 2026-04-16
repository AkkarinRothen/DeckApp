package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.CombatLogEntry
import com.deckapp.core.model.CombatLogType
import com.deckapp.core.model.Encounter
import javax.inject.Inject

/**
 * Inicia un encuentro dentro de una sesión activa.
 * Clona el encuentro de la biblioteca a la sesión y marca la sesión como activa en combate.
 */
class StartEncounterUseCase @Inject constructor(
    private val encounterRepository: EncounterRepository
) {
    suspend operator fun invoke(encounterId: Long, sessionId: Long): Encounter {
        val result = encounterRepository.startEncounterInSession(encounterId, sessionId)
        
        // Log start
        encounterRepository.recordLog(
            CombatLogEntry(
                encounterId = result.id,
                message = "Combate iniciado: ${result.name}",
                type = CombatLogType.META
            )
        )
        
        return result
    }
}
