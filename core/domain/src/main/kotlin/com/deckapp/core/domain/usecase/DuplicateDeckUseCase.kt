package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.CardStack
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Duplica un mazo completo: copia el CardStack, todas sus cartas con sus caras,
 * y las imágenes en almacenamiento interno.
 *
 * Las rutas de imagen son remapeadas para apuntar al nuevo deckId.
 * El nuevo mazo aparece en Biblioteca con el nombre original + " (copia)".
 *
 * @return El id del nuevo mazo creado, o null si el mazo origen no existe.
 */
class DuplicateDeckUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) {

    suspend operator fun invoke(sourceDeckId: Long): Result<Long> = try {
        val sourceStack = cardRepository.getDeckById(sourceDeckId).first()
            ?: return Result.failure(IllegalArgumentException("Deck $sourceDeckId not found"))

        // 1. Crear nuevo stack con nombre " (copia)"
        val newStack = sourceStack.copy(
            id = 0L,
            name = "${sourceStack.name} (copia)",
            createdAt = System.currentTimeMillis()
        )
        val newDeckId = cardRepository.saveStack(newStack)

        // 2. Copiar imágenes y obtener mapa de rutas antiguas → nuevas
        val pathMap = fileRepository.duplicateDeckImages(sourceDeckId, newDeckId)

        // 3. Copiar cartas con rutas de imagen remapeadas
        val sourceCards = cardRepository.getCardsForStack(sourceDeckId).first()
        sourceCards.forEach { card ->
            val newFaces = card.faces.map { face ->
                face.copy(
                    imagePath = face.imagePath?.let { pathMap[it] ?: it }
                )
            }
            // Actualizar coverImagePath del stack si existe
            val newCard = card.copy(
                id = 0L,
                stackId = newDeckId,
                originDeckId = newDeckId,
                faces = newFaces,
                isDrawn = false
            )
            cardRepository.saveCard(newCard)
        }

        // 4. Actualizar portada del nuevo stack con ruta remapeada
        val newCoverPath = sourceStack.coverImagePath?.let { pathMap[it] ?: it }
        if (newCoverPath != null) {
            val savedStack = cardRepository.getDeckById(newDeckId).first()
            savedStack?.let { cardRepository.updateStack(it.copy(coverImagePath = newCoverPath)) }
        }

        Result.success(newDeckId)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
