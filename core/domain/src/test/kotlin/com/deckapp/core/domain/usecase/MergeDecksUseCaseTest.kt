package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.CardContentMode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class MergeDecksUseCaseTest {

    private val cardRepository = mockk<CardRepository>()
    private val fileRepository = mockk<FileRepository>()
    private val mergeDecksUseCase = MergeDecksUseCase(cardRepository, fileRepository)

    @Test
    fun `invoke should copy cards from source to target with remapped paths`() = runTest {
        // Given
        val sourceDeckId = 1L
        val targetDeckId = 2L
        
        val sourceCards = listOf(
            Card(
                id = 101L,
                stackId = sourceDeckId,
                originDeckId = sourceDeckId,
                title = "Card 1",
                faces = listOf(
                    CardFace(name = "Front", imagePath = "/old/path/1.jpg", contentMode = CardContentMode.IMAGE_ONLY)
                ),
                sortOrder = 0
            )
        )
        
        coEvery { cardRepository.getCardsForStack(sourceDeckId) } returns flowOf(sourceCards)
        coEvery { fileRepository.duplicateDeckImages(sourceDeckId, targetDeckId) } returns mapOf(
            "/old/path/1.jpg" to "/new/path/1.jpg"
        )
        coEvery { cardRepository.saveCard(any()) } returns 201L

        // When
        val result = mergeDecksUseCase(sourceDeckId, targetDeckId)

        // Then
        assertTrue(result.isSuccess)
        coVerify {
            cardRepository.saveCard(match {
                it.stackId == targetDeckId &&
                it.originDeckId == targetDeckId &&
                it.faces[0].imagePath == "/new/path/1.jpg" &&
                it.id == 0L
            })
        }
    }
}
