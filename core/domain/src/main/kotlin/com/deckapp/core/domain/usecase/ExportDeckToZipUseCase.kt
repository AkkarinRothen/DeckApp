package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.CardManifest
import com.deckapp.core.model.DeckManifest
import com.deckapp.core.model.FaceManifest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

/**
 * Exporta las imágenes de un mazo a un archivo ZIP, incluyendo metadatos (manifiesto).
 */
class ExportDeckToZipUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend operator fun invoke(deckId: Long, outputUri: Uri): Result<Unit> {
        return try {
            val deck = cardRepository.getDeckById(deckId).first() ?: throw Exception("Deck not found")
            val cards = cardRepository.getCardsForStack(deckId).first()

            val manifest = DeckManifest(
                name = deck.name,
                description = deck.description,
                drawMode = deck.drawMode.name,
                aspectRatio = deck.aspectRatio.name,
                backImagePath = deck.backImagePath?.let { File(it).name },
                tags = deck.tags.map { it.name },
                cards = cards.map { card ->
                    CardManifest(
                        title = card.title,
                        suit = card.suit,
                        value = card.value?.toString(),
                        dmNotes = card.dmNotes,
                        faces = card.faces.map { face ->
                            FaceManifest(
                                name = face.name,
                                fileName = face.imagePath?.let { File(it).name },
                                contentMode = face.contentMode.name
                            )
                        }
                    )
                }
            )

            val manifestJson = json.encodeToString(manifest)
            fileRepository.zipDeckDirectory(deckId, outputUri, manifestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
