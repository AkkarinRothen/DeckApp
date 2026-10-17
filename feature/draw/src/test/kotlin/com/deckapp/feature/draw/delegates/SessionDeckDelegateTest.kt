package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDeckDelegateTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val cardRepository: CardRepository = mockk(relaxed = true)
    private val sessionRepository: SessionRepository = mockk(relaxed = true)
    private val drawCardUseCase: DrawCardUseCase = mockk(relaxed = true)
    private val discardCardUseCase: DiscardCardUseCase = mockk(relaxed = true)
    private val resetDeckUseCase: ResetDeckUseCase = mockk(relaxed = true)
    private val shuffleDeckUseCase: ShuffleDeckUseCase = mockk(relaxed = true)
    private val updateCardStateUseCase: UpdateCardStateUseCase = mockk(relaxed = true)
    private val undoLastActionUseCase: UndoLastActionUseCase = mockk(relaxed = true)

    private val sessionId = 100L
    private val deckId = 1L
    private var peekCardIdValue: Long? = null
    private val uiState = MutableStateFlow(
        SessionUiState(
            sessionId = sessionId,
            selectedDeckId = deckId,
            deckRefs = listOf(SessionDeckRef(sessionId, deckId)),
            deckNames = mapOf(deckId to "Mazo de Pruebas")
        )
    )

    private lateinit var delegate: SessionDeckDelegate

    @Before
    fun setUp() {
        peekCardIdValue = null
        uiState.value = SessionUiState(
            sessionId = sessionId,
            selectedDeckId = deckId,
            deckRefs = listOf(SessionDeckRef(sessionId, deckId)),
            deckNames = mapOf(deckId to "Mazo de Pruebas")
        )
        clearMocks(drawCardUseCase, discardCardUseCase, resetDeckUseCase, shuffleDeckUseCase, updateCardStateUseCase, undoLastActionUseCase)
        delegate = SessionDeckDelegate(
            sessionId = sessionId,
            _uiState = uiState,
            scope = testScope,
            cardRepository = cardRepository,
            sessionRepository = sessionRepository,
            drawCardUseCase = drawCardUseCase,
            discardCardUseCase = discardCardUseCase,
            resetDeckUseCase = resetDeckUseCase,
            shuffleDeckUseCase = shuffleDeckUseCase,
            updateCardStateUseCase = updateCardStateUseCase,
            undoLastActionUseCase = undoLastActionUseCase,
            getPeekCardId = { peekCardIdValue },
            setPeekCardId = { peekCardIdValue = it }
        )
    }

    @Test
    fun `drawCard invokes drawCardUseCase with active deck and session`() = runTest(testDispatcher) {
        val expectedCard = Card(
            id = 42L,
            stackId = deckId,
            originDeckId = deckId,
            title = "Carta de Prueba",
            faces = listOf(CardFace(name = "Frente")),
            isDrawn = true
        )
        coEvery { drawCardUseCase(sessionId = sessionId, deckId = deckId, drawMode = any()) } returns expectedCard

        delegate.drawCard()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { drawCardUseCase(sessionId = sessionId, deckId = deckId, drawMode = any()) }
        assertNull(uiState.value.snackbarMessage)
    }

    @Test
    fun `drawCard displays snackbar when no cards left in deck`() = runTest(testDispatcher) {
        coEvery { drawCardUseCase(sessionId = sessionId, deckId = deckId, drawMode = any()) } returns null

        delegate.drawCard()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("\"Mazo de Pruebas\" no tiene más cartas disponibles", uiState.value.snackbarMessage)
    }

    @Test
    fun `discardCard forwards sessionId and cardId to use case`() = runTest(testDispatcher) {
        delegate.discardCard(42L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { discardCardUseCase(sessionId, 42L) }
    }

    @Test
    fun `toggleNightMode flips the night mode state`() {
        assertFalse(uiState.value.nightMode)

        delegate.toggleNightMode()
        assertTrue(uiState.value.nightMode)

        delegate.toggleNightMode()
        assertFalse(uiState.value.nightMode)
    }

    @Test
    fun `toggleDeckCollapse updates collapsed deck ids set`() {
        assertTrue(uiState.value.collapsedDeckIds.isEmpty())

        delegate.toggleDeckCollapse(deckId)
        assertTrue(uiState.value.collapsedDeckIds.contains(deckId))

        delegate.toggleDeckCollapse(deckId)
        assertFalse(uiState.value.collapsedDeckIds.contains(deckId))
    }

    @Test
    fun `peekTopCard updates peekCard and invokes setPeekCardId`() = runTest(testDispatcher) {
        val topCard = Card(
            id = 88L,
            stackId = deckId,
            originDeckId = deckId,
            title = "Top Card",
            faces = listOf(CardFace(name = "Frente"))
        )
        coEvery { cardRepository.getTopCard(deckId) } returns topCard

        delegate.peekTopCard()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(topCard, uiState.value.peekCard)
        assertEquals(88L, peekCardIdValue)

        delegate.clearPeek()
        assertNull(uiState.value.peekCard)
        assertNull(peekCardIdValue)
    }

    @Test
    fun `resetSingleDeck calls use case and sets confirmation snackbar`() = runTest(testDispatcher) {
        delegate.resetSingleDeck(deckId)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { resetDeckUseCase(sessionId, deckId) }
        assertEquals("\"Mazo de Pruebas\" reseteado", uiState.value.snackbarMessage)
    }
}
