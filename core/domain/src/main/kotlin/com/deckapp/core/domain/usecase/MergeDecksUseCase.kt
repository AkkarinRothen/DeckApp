package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardFace
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fusiona dos mazos: copia todas las cartas del mazo origen al mazo destino.
 * Las imágenes se copian al directorio del mazo destino y las rutas de las cartas
 * se actualizan para apuntar a las nuevas copias.
 *
 * El mazo origen se mantiene intacto.
 */
class MergeDecksUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(sourceDeckId: Long, targetDeckId: Long): Result<Unit> = try {
        // 1. Obtener las cartas del mazo origen
        val sourceCards = cardRepository.getCardsForStack(sourceDeckId).first()
        if (sourceCards.isEmpty()) {
            Result.success(Unit) // Nada que fusionar
        } else {
            // 2. Copiar imágenes y obtener mapa de rutas antiguas → nuevas
            val pathMap = fileRepository.duplicateDeckImages(sourceDeckId, targetDeckId)

            // 3. Crear copias de las cartas con rutas remapeadas
            sourceCards.forEach { card ->
                val newFaces = card.faces.map { face ->
                    face.copy(
                        imagePath = face.imagePath?.let { pathMap[it] ?: it }
                    )
                }

                val newCard = card.copy(
                    id = 0L,
                    stackId = targetDeckId,
                    originDeckId = targetDeckId, // Opcional: mantener originDeckId original o cambiarlo
                    faces = newFaces,
                    isDrawn = false
                )
                cardRepository.saveCard(newCard)
            }
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
