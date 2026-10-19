package com.deckapp.feature.draw.delegates

import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaymatDelegateTest {

    private val deckDelegate = mockk<SessionDeckDelegate>(relaxed = true)
    private lateinit var uiState: MutableStateFlow<SessionUiState>
    private lateinit var delegate: PlaymatDelegate

    private var currentMemory = BoardSessionMemory()

    @Before
    fun setUp() {
        currentMemory = BoardSessionMemory()
        uiState = MutableStateFlow(
            SessionUiState(
                sessionId = 42,
                session = Session(id = 42, name = "Campaña Test"),
                hand = listOf(
                    Card(id = 1001, stackId = 1, originDeckId = 1, title = "Espada Sagrada", faces = listOf(CardFace(name = "Frente"))),
                    Card(id = 1002, stackId = 1, originDeckId = 1, title = "Escudo Antiguo", faces = listOf(CardFace(name = "Frente")))
                ),
                boardMemory = currentMemory
            )
        )

        delegate = PlaymatDelegate(
            sessionId = 42,
            _uiState = uiState,
            scope = mockk(relaxed = true),
            updateBoardMemory = { transform ->
                currentMemory = transform(currentMemory)
                uiState.value = uiState.value.copy(boardMemory = currentMemory)
            },
            deckDelegate = deckDelegate
        )
    }

    @Test
    fun ensureInitialSurface_createsDefaultSurfaceIfEmpty() {
        val surface = delegate.ensureInitialSurface()
        assertNotNull(surface)
        assertEquals("Mesa Principal", surface.name)
        assertEquals(PlaymatTheme.FELT_GREEN, surface.backgroundTheme)
        assertEquals(1, uiState.value.boardMemory.playmatSurfaces.size)
    }

    @Test
    fun placeCardOnPlaymat_andMoveCard_worksCorrectly() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1001, x = 150f, y = 200f, rotationDegrees = 0)

        val active = uiState.value.activePlaymatSurface
        assertEquals(1, active.cards.size)
        val placement = active.cards.first()
        assertEquals(1001L, placement.cardId)
        assertEquals(150f, placement.x, 0.01f)
        assertEquals(200f, placement.y, 0.01f)

        // Mover carta
        delegate.moveCard(cardId = 1001, newX = 350f, newY = 400f)
        val moved = uiState.value.activePlaymatSurface.cards.first()
        assertEquals(350f, moved.x, 0.01f)
        assertEquals(400f, moved.y, 0.01f)
    }

    @Test
    fun rotateAndFlipCard_updatesState() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1001, x = 100f, y = 100f, rotationDegrees = 0)

        delegate.rotateCard(1001, 90)
        assertEquals(90, uiState.value.activePlaymatSurface.cards.first().rotationDegrees)

        delegate.rotateCard(1001, 90)
        assertEquals(180, uiState.value.activePlaymatSurface.cards.first().rotationDegrees)

        // Voltear
        assertTrue(uiState.value.activePlaymatSurface.cards.first().isFaceUp)
        delegate.flipCard(1001)
        assertFalse(uiState.value.activePlaymatSurface.cards.first().isFaceUp)
    }

    @Test
    fun returnCardToHand_removesPlacementFromSurface() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1001, x = 100f, y = 100f)
        assertEquals(1, uiState.value.activePlaymatSurface.cards.size)

        delegate.returnCardToHand(1001)
        assertEquals(0, uiState.value.activePlaymatSurface.cards.size)
        assertNull(uiState.value.selectedPlaymatCardId)
    }

    @Test
    fun discardCard_callsDeckDelegateAndRemovesFromSurface() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1002, x = 100f, y = 100f)

        delegate.discardCard(1002)
        assertEquals(0, uiState.value.activePlaymatSurface.cards.size)
        verify { deckDelegate.discardCard(1002) }
    }

    @Test
    fun stickyNotes_addAndRemove() {
        delegate.ensureInitialSurface()
        delegate.addStickyNote("Entrada secreta en la cueva", 250f, 300f)

        val active = uiState.value.activePlaymatSurface
        assertEquals(1, active.stickyNotes.size)
        val note = active.stickyNotes.first()
        assertEquals("Entrada secreta en la cueva", note.text)

        delegate.deleteStickyNote(note.id)
        assertEquals(0, uiState.value.activePlaymatSurface.stickyNotes.size)
    }

    @Test
    fun surfaceManagement_createAndSwitch() {
        delegate.ensureInitialSurface()
        delegate.createSurface("Mazmorra Nivel 2", PlaymatTheme.RUSTIC_WOOD, PlaymatTemplateType.FREEFORM)

        assertEquals(2, uiState.value.boardMemory.playmatSurfaces.size)
        assertEquals("Mazmorra Nivel 2", uiState.value.activePlaymatSurface.name)
        assertEquals(PlaymatTheme.RUSTIC_WOOD, uiState.value.activePlaymatSurface.backgroundTheme)

        val firstSurfaceId = uiState.value.boardMemory.playmatSurfaces.first().id
        delegate.switchSurface(firstSurfaceId)
        assertEquals("Mesa Principal", uiState.value.activePlaymatSurface.name)
    }
}
