package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import javax.inject.Inject

class DiscardCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, cardId: Long) {
        // El card ya tiene isDrawn=true; solo registramos el evento de descarte
        sessionRepository.logEvent(
            DrawEvent(sessionId = sessionId, cardId = cardId, action = DrawAction.DISCARD)
        )
    }
}
