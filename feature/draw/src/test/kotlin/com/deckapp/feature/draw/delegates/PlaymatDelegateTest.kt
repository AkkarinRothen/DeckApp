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

    @Test
    fun physicalDecks_moveDeckAndDiscardPile() {
        delegate.ensureInitialSurface()
        delegate.addDeckToPlaymat(stackId = 1L, x = 100f, y = 150f)

        val active = uiState.value.activePlaymatSurface
        val deckPlacement = active.decks.find { it.stackId == 1L }
        assertNotNull(deckPlacement)
        assertEquals(100f, deckPlacement!!.x, 0.01f)
        assertEquals(150f, deckPlacement.y, 0.01f)

        // Mover mazo
        delegate.moveDeck(stackId = 1L, newX = 250f, newY = 300f)
        val movedDeck = uiState.value.activePlaymatSurface.decks.find { it.stackId == 1L }
        assertEquals(250f, movedDeck!!.x, 0.01f)
        assertEquals(300f, movedDeck.y, 0.01f)

        // Mover pila de descarte
        delegate.moveDiscardPile(stackId = 1L, newX = 400f, newY = 300f)
        val movedDiscard = uiState.value.activePlaymatSurface.decks.find { it.stackId == 1L }
        assertEquals(400f, movedDiscard!!.discardX ?: 0f, 0.01f)
        assertEquals(300f, movedDiscard.discardY ?: 0f, 0.01f)

        // Re-barajar
        delegate.reshuffleDeck(stackId = 1L)
        verify { deckDelegate.shufflePileBack(1L) }
    }

    @Test
    fun narrativeConnections_createUpdateAndAutoPruneOnDiscard() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1001, x = 100f, y = 100f)
        delegate.placeCardOnPlaymat(cardId = 1002, x = 300f, y = 200f)

        // Crear conexión
        delegate.createConnection(fromCardId = 1001, toCardId = 1002, label = "Sospechoso de", colorHex = "#E53935")
        var surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.connections.size)
        val conn = surface.connections.first()
        assertEquals(1001L, conn.fromCardId)
        assertEquals(1002L, conn.toCardId)
        assertEquals("Sospechoso de", conn.label)
        assertEquals("#E53935", conn.colorHex)

        // Evitar duplicados
        delegate.createConnection(fromCardId = 1001, toCardId = 1002)
        assertEquals(1, uiState.value.activePlaymatSurface.connections.size)
        delegate.createConnection(fromCardId = 1002, toCardId = 1001)
        assertEquals(1, uiState.value.activePlaymatSurface.connections.size)

        // Actualizar etiqueta y color
        delegate.updateConnection(connectionId = conn.id, label = "Aliado con", colorHex = "#FFB300")
        surface = uiState.value.activePlaymatSurface
        assertEquals("Aliado con", surface.connections.first().label)
        assertEquals("#FFB300", surface.connections.first().colorHex)

        // Al descartar o devolver una de las dos cartas a la mano, el hilo se poda automáticamente
        delegate.returnCardToHand(1001)
        surface = uiState.value.activePlaymatSurface
        assertTrue(surface.connections.isEmpty())
    }

    @Test
    fun characterTokens_crudAndUniversalConnections() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 1001, x = 100f, y = 100f)

        // 1. Añadir token de personaje
        delegate.addTokenToPlaymat(
            name = "Detective Vance",
            npcId = 77L,
            x = 250f,
            y = 150f,
            maxHp = 15,
            currentHp = 15,
            tokenType = PlaymatTokenType.ALLY,
            ringColorHex = "#4CAF50"
        )

        var surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.tokens.size)
        val token = surface.tokens.first()
        assertEquals("Detective Vance", token.name)
        assertEquals(15, token.maxHp)
        assertEquals(15, token.currentHp)
        assertEquals(PlaymatTokenType.ALLY, token.tokenType)

        // 2. Mover token
        delegate.moveToken(token.id, newX = 280f, newY = 180f)
        assertEquals(280f, uiState.value.activePlaymatSurface.tokens.first().x, 0.01f)

        // 3. Modificar PV y Facción
        delegate.updateTokenHp(token.id, newHp = 10)
        assertEquals(10, uiState.value.activePlaymatSurface.tokens.first().currentHp)

        delegate.updateTokenType(token.id, PlaymatTokenType.ENEMY)
        assertEquals(PlaymatTokenType.ENEMY, uiState.value.activePlaymatSurface.tokens.first().tokenType)
        assertEquals("#E53935", uiState.value.activePlaymatSurface.tokens.first().ringColorHex)

        // 4. Conexión Universal: Carta ↔ Token
        delegate.createConnection(
            fromEndpointId = "card_1001",
            toEndpointId = "token_${token.id}",
            label = "Sospechoso de",
            colorHex = "#E53935"
        )
        surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.connections.size)
        val conn = surface.connections.first()
        assertEquals("card_1001", conn.effectiveFromId)
        assertEquals("token_${token.id}", conn.effectiveToId)

        // 5. Añadir segundo token y conexión Token ↔ Token
        delegate.addTokenToPlaymat(
            name = "Guardia Nocturno",
            x = 400f,
            y = 300f,
            maxHp = 10,
            tokenType = PlaymatTokenType.NEUTRAL
        )
        val secondToken = uiState.value.activePlaymatSurface.tokens.find { it.name == "Guardia Nocturno" }!!
        delegate.createConnection(
            fromEndpointId = "token_${token.id}",
            toEndpointId = "token_${secondToken.id}",
            label = "Sobornado por",
            colorHex = "#FFB300"
        )
        assertEquals(2, uiState.value.activePlaymatSurface.connections.size)

        // 6. Eliminar token poda automáticamente sus hilos asociados
        delegate.removeToken(token.id)
        surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.tokens.size)
        assertTrue(surface.connections.isEmpty())
    }

    @Test
    fun cardFlip_toggleIsFaceUp() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 2001, x = 120f, y = 140f)

        var cardPlacement = uiState.value.activePlaymatSurface.cards.find { it.cardId == 2001L }
        assertNotNull(cardPlacement)
        assertTrue(cardPlacement!!.isFaceUp)

        // Voltear boca abajo
        delegate.flipCard(2001L)
        cardPlacement = uiState.value.activePlaymatSurface.cards.find { it.cardId == 2001L }
        assertFalse(cardPlacement!!.isFaceUp)

        // Voltear boca arriba de nuevo
        delegate.flipCard(2001L)
        cardPlacement = uiState.value.activePlaymatSurface.cards.find { it.cardId == 2001L }
        assertTrue(cardPlacement!!.isFaceUp)
    }

    @Test
    fun progressClocks_crudAndUniversalConnections() {
        delegate.ensureInitialSurface()
        delegate.placeCardOnPlaymat(cardId = 3001, x = 100f, y = 100f)

        // 1. Añadir Reloj de Tensión
        delegate.addClockToPlaymat(
            title = "Alerta del Castillo",
            totalSegments = 6,
            x = 220f,
            y = 180f,
            colorHex = "#E53935"
        )

        var surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.clocks.size)
        val clock = surface.clocks.first()
        assertEquals("Alerta del Castillo", clock.title)
        assertEquals(6, clock.totalSegments)
        assertEquals(0, clock.filledSegments)
        assertEquals("#E53935", clock.colorHex)

        // 2. Mover Reloj
        delegate.moveClock(clock.id, newX = 260f, newY = 200f)
        assertEquals(260f, uiState.value.activePlaymatSurface.clocks.first().x, 0.01f)

        // 3. Modificar Progreso con clamping
        delegate.updateClockProgress(clock.id, filledSegments = 4)
        assertEquals(4, uiState.value.activePlaymatSurface.clocks.first().filledSegments)

        // Clamping superior
        delegate.updateClockProgress(clock.id, filledSegments = 10)
        assertEquals(6, uiState.value.activePlaymatSurface.clocks.first().filledSegments)

        // Clamping inferior
        delegate.updateClockProgress(clock.id, filledSegments = -3)
        assertEquals(0, uiState.value.activePlaymatSurface.clocks.first().filledSegments)

        // 4. Conexión Universal: Carta ↔ Reloj
        delegate.createConnection(
            fromEndpointId = "card_3001",
            toEndpointId = "clock_${clock.id}",
            label = "Aumenta tensión",
            colorHex = "#E53935"
        )
        surface = uiState.value.activePlaymatSurface
        assertEquals(1, surface.connections.size)
        val conn = surface.connections.first()
        assertEquals("card_3001", conn.effectiveFromId)
        assertEquals("clock_${clock.id}", conn.effectiveToId)

        // 5. Retirar Reloj poda automáticamente los hilos conectados a él
        delegate.removeClock(clock.id)
        surface = uiState.value.activePlaymatSurface
        assertTrue(surface.clocks.isEmpty())
        assertTrue(surface.connections.isEmpty())
    }

    @Test
    fun battlemap_setPresetAndCustomImageTransform() {
        delegate.ensureInitialSurface()

        // 1. Cargar imagen de battlemap
        delegate.setBackgroundMap(uri = "content://media/battlemap_dungeon.jpg", preset = PlaymatAtmosphericPreset.NONE)
        var surface = uiState.value.activePlaymatSurface
        assertEquals("content://media/battlemap_dungeon.jpg", surface.customImageUri)
        assertEquals(PlaymatAtmosphericPreset.NONE, surface.backgroundPreset)

        // 2. Modificar opacidad y escala
        delegate.setBackgroundTransform(opacity = 0.65f, scale = 1.8f, x = 100f, y = -50f)
        surface = uiState.value.activePlaymatSurface
        assertEquals(0.65f, surface.backgroundOpacity, 0.01f)
        assertEquals(1.8f, surface.backgroundScale, 0.01f)
        assertEquals(100f, surface.backgroundX, 0.01f)
        assertEquals(-50f, surface.backgroundY, 0.01f)

        // 3. Alternar bloqueo
        assertTrue(surface.isBackgroundLocked)
        delegate.toggleBackgroundLock()
        assertFalse(uiState.value.activePlaymatSurface.isBackgroundLocked)

        // 4. Cambiar a preset atmosférico
        delegate.setBackgroundMap(uri = null, preset = PlaymatAtmosphericPreset.DUNGEON_STONE)
        surface = uiState.value.activePlaymatSurface
        assertNull(surface.customImageUri)
        assertEquals(PlaymatAtmosphericPreset.DUNGEON_STONE, surface.backgroundPreset)
    }

    @Test
    fun tacticalGrid_configurationAndSnapping() {
        delegate.ensureInitialSurface()

        // 1. Configurar cuadrícula de cuadrados de 50dp con snap activado
        delegate.setGridConfig(
            gridType = PlaymatGridType.SQUARE,
            gridStyle = PlaymatGridStyle.DOTS,
            cellSizeDp = 50f,
            snapToGrid = true
        )
        var surface = uiState.value.activePlaymatSurface
        assertEquals(PlaymatGridType.SQUARE, surface.gridType)
        assertEquals(PlaymatGridStyle.DOTS, surface.gridStyle)
        assertEquals(50f, surface.cellSizeDp, 0.01f)
        assertTrue(surface.snapToGrid)

        // 2. Probar snapping matemático
        val snapped = delegate.calculateSnappedPosition(53f, 98f, PlaymatGridType.SQUARE, 50f)
        assertEquals(50f, snapped.first, 0.01f)
        assertEquals(100f, snapped.second, 0.01f)

        // 3. Sin cuadrícula (NONE) no debe alterar coordenadas
        val noSnap = delegate.calculateSnappedPosition(53f, 98f, PlaymatGridType.NONE, 50f)
        assertEquals(53f, noSnap.first, 0.01f)
        assertEquals(98f, noSnap.second, 0.01f)

        // 4. Toggle snap
        delegate.toggleSnapToGrid()
        assertFalse(uiState.value.activePlaymatSurface.snapToGrid)
    }
}


