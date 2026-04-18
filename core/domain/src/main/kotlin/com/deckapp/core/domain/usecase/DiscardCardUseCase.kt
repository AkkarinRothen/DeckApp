package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import javax.inject.Inject

class DiscardCardUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val updateCardStateUseCase: UpdateCardStateUseCase
) {
    suspend operator fun invoke(sessionId: Long, cardId: Long) {
        // Marcamos como no robada (isDrawn=false) para que pase a la pila de descarte
        updateCardStateUseCase(
            cardId, 
            UpdateCardStateUseCase.CardStateUpdate(isDrawn = false, lastDrawnAt = null)
        )

        sessionRepository.logEvent(
            DrawEvent(sessionId = sessionId, cardId = cardId, action = DrawAction.DISCARD)
        )
    }
}
