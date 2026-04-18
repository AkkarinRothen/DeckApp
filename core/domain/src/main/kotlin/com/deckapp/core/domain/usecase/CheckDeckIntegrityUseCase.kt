package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Verifica la integridad física de un mazo y sus cartas.
 * Comprueba que todas las imágenes referenciadas existan realmente en el disco.
 */
class CheckDeckIntegrityUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) {
    data class IntegrityReport(
        val deckId: Long,
        val isValid: Boolean,
        val missingImages: List<String>,
        val totalCardsChecked: Int
    )

    suspend operator fun invoke(deckId: Long): IntegrityReport {
        val deck = cardRepository.getDeckById(deckId).first() ?: return IntegrityReport(deckId, false, emptyList(), 0)
        val cards = cardRepository.getCardsForStack(deckId).first()
        
        val missing = mutableListOf<String>()
        
        // 1. Verificar imágenes del mazo
        deck.backImagePath?.let { if (!fileRepository.exists(it)) missing.add(it) }
        deck.coverImagePath?.let { if (!fileRepository.exists(it)) missing.add(it) }
        
        // 2. Verificar imágenes de cada carta
        cards.forEach { card ->
            card.faces.forEach { face ->
                face.imagePath?.let { if (!fileRepository.exists(it)) missing.add(it) }
            }
        }
        
        return IntegrityReport(
            deckId = deckId,
            isValid = missing.isEmpty(),
            missingImages = missing.distinct(),
            totalCardsChecked = cards.size
        )
    }
}
