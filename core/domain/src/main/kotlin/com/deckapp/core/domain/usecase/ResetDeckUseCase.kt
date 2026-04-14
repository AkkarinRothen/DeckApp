package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import javax.inject.Inject

/** Devuelve todas las cartas de un mazo a su estado original (isDrawn = false) */
class ResetDeckUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, deckId: Long) {
        cardRepository.resetDeck(deckId)
        sessionRepository.logEvent(
            DrawEvent(sessionId = sessionId, cardId = -1, action = DrawAction.RESET,
                metadata = """{"deckId": $deckId}""")
        )
    }
}
