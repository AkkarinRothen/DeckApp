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
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long): Boolean {
        val lastEvent = sessionRepository.getLastEventForSession(sessionId) ?: return false

        val cardId = lastEvent.cardId
        when (lastEvent.action) {
            DrawAction.DRAW -> if (cardId != null) cardRepository.updateCardDrawnState(cardId, isDrawn = false)
            DrawAction.FLIP -> {
                if (cardId != null) {
                    val card = cardRepository.getCardById(cardId).first()
                    if (card != null) {
                        val prevIndex = if (card.currentFaceIndex == 0) card.faces.size - 1
                                       else card.currentFaceIndex - 1
                        cardRepository.updateCardFaceIndex(card.id, prevIndex)
                    }
                }
            }
            DrawAction.REVERSE ->
                if (cardId != null) cardRepository.updateCardReversed(cardId, isReversed = false)
            DrawAction.ROTATE -> {
                if (cardId != null) {
                    val card = cardRepository.getCardById(cardId).first()
                    if (card != null) {
                        val prevRotation = (card.currentRotation + 270) % 360
                        cardRepository.updateCardRotation(card.id, prevRotation)
                    }
                }
            }
            DrawAction.DISCARD ->
                // Devuelve la carta a la mano (isDrawn = true)
                if (cardId != null) cardRepository.updateCardDrawnState(cardId, isDrawn = true)
            DrawAction.PASS, DrawAction.RESET, DrawAction.PEEK, DrawAction.SHUFFLE_BACK -> { /* no revertibles or no state change */ }
        }

        sessionRepository.deleteLastEvent(sessionId)
        return true
    }
}
