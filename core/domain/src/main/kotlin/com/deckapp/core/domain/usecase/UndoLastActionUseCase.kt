package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.DrawAction
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Deshace la última acción de la sesión (nivel 1).
 * Elimina el último DrawEvent del log y revierte el estado de la carta.
 */
class UndoLastActionUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository,
    private val updateCardStateUseCase: UpdateCardStateUseCase
) {
    suspend operator fun invoke(sessionId: Long): Boolean {
        val lastEvent = sessionRepository.getLastEventForSession(sessionId) ?: return false

        val cardId = lastEvent.cardId
        if (cardId != null) {
            when (lastEvent.action) {
                DrawAction.DRAW -> updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isDrawn = false))
                DrawAction.FLIP -> {
                    val card = cardRepository.getCardById(cardId).first()
                    if (card != null) {
                        val prevIndex = if (card.currentFaceIndex == 0) card.faces.size - 1
                                       else card.currentFaceIndex - 1
                        updateCardStateUseCase(card.id, UpdateCardStateUseCase.CardStateUpdate(faceIndex = prevIndex))
                    }
                }
                DrawAction.REVERSE ->
                    updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isReversed = false))
                DrawAction.ROTATE -> {
                    val card = cardRepository.getCardById(cardId).first()
                    if (card != null) {
                        val prevRotation = (card.currentRotation + 270) % 360
                        updateCardStateUseCase(card.id, UpdateCardStateUseCase.CardStateUpdate(rotation = prevRotation))
                    }
                }
                DrawAction.DISCARD ->
                    // Devuelve la carta a la mano (isDrawn = true)
                    updateCardStateUseCase(cardId, UpdateCardStateUseCase.CardStateUpdate(isDrawn = true))
                else -> { /* acciones no revertibles o sin cambio de estado físico */ }
            }
        }

        sessionRepository.deleteLastEvent(sessionId)
        return true
    }
}
