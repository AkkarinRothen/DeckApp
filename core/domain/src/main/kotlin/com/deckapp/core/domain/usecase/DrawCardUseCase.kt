package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.DrawMode
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Roba una carta de un mazo según el [DrawMode] configurado.
 * Si la carta tiene múltiples opciones (FOUR_EDGE_CUES, TOP_BOTTOM_SPLIT, REVERSIBLE),
 * asigna aleatoriamente una orientación inicial.
 * Registra el evento en el log ANTES de retornar — garantiza persistencia
 * incluso si la app es terminada durante la animación.
 */
class DrawCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository,
    private val updateCardStateUseCase: UpdateCardStateUseCase
) {
    suspend operator fun invoke(
        sessionId: Long,
        deckId: Long,
        drawMode: DrawMode,
        suit: String? = null
    ): Card? {
        val availableCards = cardRepository.getCardsForStack(deckId).first()
            .filter { !it.isDrawn }
            .filter { suit == null || it.suit == suit }
            .sortedBy { it.sortOrder }

        if (availableCards.isEmpty()) return null

        val card = when (drawMode) {
            DrawMode.TOP -> availableCards.first()
            DrawMode.BOTTOM -> availableCards.last()
            DrawMode.RANDOM -> availableCards.random()
        }

        val deck = cardRepository.getDeckById(deckId).first()
        val faceDown = deck?.drawFaceDown == true

        val mode = card.activeFace.contentMode
        val randomRotation = when (mode) {
            CardContentMode.FOUR_EDGE_CUES -> listOf(0, 90, 180, 270).random()
            CardContentMode.TOP_BOTTOM_SPLIT,
            CardContentMode.REVERSIBLE -> listOf(0, 180).random()
            else -> card.currentRotation
        }
        val isReversed = mode == CardContentMode.REVERSIBLE && randomRotation == 180

        // Actualizar estado de forma atómica a través del Use Case unificado
        updateCardStateUseCase(
            cardId = card.id,
            update = UpdateCardStateUseCase.CardStateUpdate(
                isDrawn = true,
                lastDrawnAt = System.currentTimeMillis(),
                isRevealed = !faceDown,
                rotation = randomRotation,
                isReversed = isReversed
            )
        )

        sessionRepository.logEvent(
            DrawEvent(
                sessionId = sessionId,
                cardId = card.id,
                action = DrawAction.DRAW,
                metadata = buildList {
                    suit?.let { add("suit:$it") }
                    if (randomRotation != 0) add("rotation:$randomRotation")
                }.joinToString(";")
            )
        )

        return card.copy(
            isDrawn = true,
            isRevealed = !faceDown,
            currentRotation = randomRotation,
            isReversed = isReversed
        )
    }
}
