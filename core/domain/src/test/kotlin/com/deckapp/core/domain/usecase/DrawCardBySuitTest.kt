package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.*
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DrawCardBySuitTest {
    private val cards = mockk<CardRepository>(relaxed = true)
    private val sessions = mockk<SessionRepository>(relaxed = true)
    private val draw = DrawCardUseCase(cards, sessions, UpdateCardStateUseCase(cards))
    private val face = CardFace(name = "Frente")

    @Test
    fun `draw by suit only selects an available matching card and logs the suit`() = runTest {
        val hearts = Card(id = 1, stackId = 7, originDeckId = null, title = "As", suit = "Copas", faces = listOf(face), sortOrder = 0)
        val spades = Card(id = 2, stackId = 7, originDeckId = null, title = "Dos", suit = "Espadas", faces = listOf(face), sortOrder = 1)
        every { cards.getCardsForStack(7) } returns flowOf(listOf(hearts, spades))
        every { cards.getDeckById(7) } returns flowOf(CardStack(id = 7, name = "Tarokka", type = StackType.DECK))

        val drawn = draw(sessionId = 3, deckId = 7, drawMode = DrawMode.TOP, suit = "Copas")

        assertEquals(hearts.id, drawn?.id)
        coVerify { cards.updateCardDrawnState(hearts.id, true, any()) }
        coVerify { sessions.logEvent(match { it.cardId == hearts.id && it.metadata == "suit:Copas" }) }
    }

    @Test
    fun `draw by suit returns null without changing a deck when that suit is exhausted`() = runTest {
        val exhausted = Card(id = 1, stackId = 7, originDeckId = null, title = "As", suit = "Copas", faces = listOf(face), isDrawn = true)
        every { cards.getCardsForStack(7) } returns flowOf(listOf(exhausted))

        val drawn = draw(sessionId = 3, deckId = 7, drawMode = DrawMode.RANDOM, suit = "Copas")

        assertNull(drawn)
        coVerify(exactly = 0) { cards.updateCardDrawnState(any(), any(), any()) }
        coVerify(exactly = 0) { sessions.logEvent(any()) }
    }
}
