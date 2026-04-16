package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.SessionDeckRef
import javax.inject.Inject

/**
 * Caso de uso para gestionar dinámicamente qué recursos (mazos y tablas)
 * están disponibles en una sesión activa.
 */
class UpdateSessionResourcesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend fun addDeck(sessionId: Long, stackId: Long) {
        sessionRepository.addDeckToSession(
            SessionDeckRef(
                sessionId = sessionId,
                stackId = stackId,
                sortOrder = 0 // Podría incrementarse según necesidad
            )
        )
    }

    suspend fun removeDeck(sessionId: Long, stackId: Long) {
        sessionRepository.removeDeckFromSession(sessionId, stackId)
    }

    suspend fun addTable(sessionId: Long, tableId: Long) {
        sessionRepository.addTableToSession(sessionId, tableId)
    }

    suspend fun removeTable(sessionId: Long, tableId: Long) {
        sessionRepository.removeTableFromSession(sessionId, tableId)
    }
}
