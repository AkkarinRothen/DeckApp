package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import javax.inject.Inject

/**
 * Caso de uso unificado para actualizar cualquier estado físico de una carta.
 * Evita la fragmentación de llamadas en los ViewModels y facilita la implementación futura de Undo.
 */
class UpdateCardStateUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Define los cambios de estado a aplicar. Los valores null no se modificarán.
     */
    data class CardStateUpdate(
        val isDrawn: Boolean? = null,
        val isRevealed: Boolean? = null,
        val isReversed: Boolean? = null,
        val rotation: Int? = null,
        val faceIndex: Int? = null,
        val notes: String? = null,
        val lastDrawnAt: Long? = null
    )

    suspend operator fun invoke(cardId: Long, update: CardStateUpdate) {
        update.isDrawn?.let { 
            cardRepository.updateCardDrawnState(cardId, it, update.lastDrawnAt) 
        }
        update.isRevealed?.let { 
            cardRepository.updateCardRevealed(cardId, it) 
        }
        update.isReversed?.let { 
            cardRepository.updateCardReversed(cardId, it) 
        }
        update.rotation?.let { 
            cardRepository.updateCardRotation(cardId, it) 
        }
        update.faceIndex?.let { 
            cardRepository.updateCardFaceIndex(cardId, it) 
        }
        update.notes?.let { 
            cardRepository.updateCardNotes(cardId, it) 
        }
    }
}
