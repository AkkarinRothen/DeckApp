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

        when (lastEvent.action) {
            DrawAction.DRAW -> cardRepository.updateCardDrawnState(lastEvent.cardId, isDrawn = false)
            DrawAction.FLIP -> {
                val card = cardRepository.getCardById(lastEvent.cardId).first()
                if (card != null) {
                    val prevIndex = if (card.currentFaceIndex == 0) card.faces.size - 1
                                   else card.currentFaceIndex - 1
                    cardRepository.updateCardFaceIndex(card.id, prevIndex)
                }
            }
            DrawAction.REVERSE ->
                cardRepository.updateCardReversed(lastEvent.cardId, isReversed = false)
            DrawAction.ROTATE -> {
                val card = cardRepository.getCardById(lastEvent.cardId).first()
                if (card != null) {
                    val prevRotation = (card.currentRotation + 270) % 360
                    cardRepository.updateCardRotation(card.id, prevRotation)
                }
            }
            DrawAction.DISCARD ->
                // Devuelve la carta a la mano (isDrawn = true)
                cardRepository.updateCardDrawnState(lastEvent.cardId, isDrawn = true)
            DrawAction.PASS, DrawAction.RESET, DrawAction.PEEK -> { /* no revertibles or no state change */ }
        }

        sessionRepository.deleteLastEvent(sessionId)
        return true
    }
}
