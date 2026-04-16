package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.DrawMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Roba una carta de un mazo según el [DrawMode] configurado.
 * Registra el evento en el log ANTES de retornar — garantiza persistencia
 * incluso si la app es terminada durante la animación.
 */
class DrawCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        sessionId: Long,
        deckId: Long,
        drawMode: DrawMode
    ): Card? {
        val availableCards = cardRepository.getCardsForStack(deckId).first()
            .filter { !it.isDrawn }
            .sortedBy { it.sortOrder }

        if (availableCards.isEmpty()) return null

        val card = when (drawMode) {
            DrawMode.TOP -> availableCards.first()
            DrawMode.BOTTOM -> availableCards.last()
            DrawMode.RANDOM -> availableCards.random()
        }

        // Persistir estado antes de animar
        val now = System.currentTimeMillis()
        cardRepository.updateCardDrawnState(card.id, isDrawn = true, lastDrawnAt = now)

        val deck = cardRepository.getDeckById(deckId).first()
        val faceDown = deck?.drawFaceDown == true
        if (faceDown) {
            cardRepository.updateCardRevealed(card.id, isRevealed = false)
        }

        sessionRepository.logEvent(
            DrawEvent(
                sessionId = sessionId,
                cardId = card.id,
                action = DrawAction.DRAW
            )
        )

        return card.copy(isDrawn = true, isRevealed = !faceDown)
    }
}
