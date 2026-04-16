package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import javax.inject.Inject

/**
 * Actualiza las notas privadas del DM para una carta específica.
 */
class UpdateCardNotesUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(cardId: Long, notes: String?) {
        cardRepository.updateCardNotes(cardId, notes)
    }
}
