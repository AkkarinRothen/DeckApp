package com.deckapp.feature.draw.delegates

import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Delegado encargado de la gestión espacial del Tapete de Cartas (Playmat).
 * Controla superficies 2D, colocación, arrastre, rotación, volteo,
 * notas adhesivas y moldes temáticos.
 */
class PlaymatDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val updateBoardMemory: ((BoardSessionMemory) -> BoardSessionMemory) -> Unit,
    private val deckDelegate: SessionDeckDelegate
) {

    /**
     * Asegura que exista al menos una superficie de tapete activa.
     */
    fun ensureInitialSurface(): PlaymatSurface {
        val memory = _uiState.value.boardMemory
        val surface = if (memory.playmatSurfaces.isNotEmpty()) {
            val activeId = memory.activePlaymatSurfaceId ?: memory.playmatSurfaces.first().id
            memory.playmatSurfaces.find { it.id == activeId } ?: memory.playmatSurfaces.first()
        } else {
            val defaultSurface = PlaymatSurface(
                id = "surface_default_$sessionId",
                name = "Mesa Principal",
                backgroundTheme = PlaymatTheme.FELT_GREEN,
                template = PlaymatTemplateType.FREEFORM,
                cameraX = 0f,
                cameraY = 0f,
                cameraZoom = 1f
            )
            updateBoardMemory {
                it.copy(
                    playmatSurfaces = listOf(defaultSurface),
                    activePlaymatSurfaceId = defaultSurface.id
                )
            }
            defaultSurface
        }
        ensureDecksPlaced()
        return surface
    }

    fun selectCard(cardId: Long?) {
        _uiState.update { it.copy(selectedPlaymatCardId = cardId) }
    }

    fun toggleHandTray() {
        _uiState.update { it.copy(showPlaymatHandTray = !it.showPlaymatHandTray) }
    }

    fun setHandTrayVisible(visible: Boolean) {
        _uiState.update { it.copy(showPlaymatHandTray = visible) }
    }

    fun switchSurface(surfaceId: String) {
        updateBoardMemory {
            it.copy(activePlaymatSurfaceId = surfaceId)
        }
        selectCard(null)
    }

    fun createSurface(
        name: String,
        theme: PlaymatTheme = PlaymatTheme.FELT_GREEN,
        template: PlaymatTemplateType = PlaymatTemplateType.FREEFORM
    ) {
        val newSurface = PlaymatSurface(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Mesa ${(_uiState.value.boardMemory.playmatSurfaces.size + 1)}" },
            backgroundTheme = theme,
            template = template,
            cameraX = 0f,
            cameraY = 0f,
            cameraZoom = 1f
        )
        updateBoardMemory {
            it.copy(
                playmatSurfaces = it.playmatSurfaces + newSurface,
                activePlaymatSurfaceId = newSurface.id
            )
        }
        selectCard(null)
    }

    fun deleteSurface(surfaceId: String) {
        val currentSurfaces = _uiState.value.boardMemory.playmatSurfaces
        if (currentSurfaces.size <= 1) return // Mantener al menos una mesa
        val remaining = currentSurfaces.filterNot { it.id == surfaceId }
        val newActiveId = if (_uiState.value.boardMemory.activePlaymatSurfaceId == surfaceId) {
            remaining.first().id
        } else {
            _uiState.value.boardMemory.activePlaymatSurfaceId
        }
        updateBoardMemory {
            it.copy(
                playmatSurfaces = remaining,
                activePlaymatSurfaceId = newActiveId
            )
        }
        selectCard(null)
    }

    fun setSurfaceTheme(surfaceId: String, theme: PlaymatTheme) {
        updateActiveSurface { surface ->
            if (surface.id == surfaceId) surface.copy(backgroundTheme = theme) else surface
        }
    }

    fun setSurfaceTemplate(surfaceId: String, template: PlaymatTemplateType) {
        updateActiveSurface { surface ->
            if (surface.id == surfaceId) surface.copy(template = template) else surface
        }
    }

    fun updateCamera(x: Float, y: Float, zoom: Float) {
        updateCurrentActiveSurface { it.copy(cameraX = x, cameraY = y, cameraZoom = zoom) }
    }

    /**
     * Coloca una carta de la mano en el tapete en las coordenadas dadas.
     */
    fun placeCardOnPlaymat(
        cardId: Long,
        x: Float,
        y: Float,
        rotationDegrees: Int = 0,
        isFaceUp: Boolean = true,
        slotTag: String? = null
    ) {
        updateCurrentActiveSurface { surface ->
            val highestZ = (surface.cards.maxOfOrNull { it.zIndex } ?: 0) + 1
            val filtered = surface.cards.filterNot { it.cardId == cardId }
            val newPlacement = PlaymatCardPlacement(
                cardId = cardId,
                x = x,
                y = y,
                rotationDegrees = rotationDegrees,
                zIndex = highestZ,
                isFaceUp = isFaceUp,
                slotTag = slotTag
            )
            surface.copy(cards = filtered + newPlacement)
        }
        selectCard(cardId)
    }

    /**
     * Coloca automáticamente una carta robada desde la guía de Story Engine en su ranura temática correspondiente.
     */
    fun placeStepCardOnPlaymat(
        tab: com.deckapp.feature.draw.model.StoryEngineGuideTab,
        stepIndex: Int,
        cardId: Long,
        rotationDegrees: Int = 0
    ) {
        ensureInitialSurface()
        val templateType = if (tab == com.deckapp.feature.draw.model.StoryEngineGuideTab.DECK_OF_WORLDS) {
            PlaymatTemplateType.DECK_OF_WORLDS_MICROSETTING
        } else {
            PlaymatTemplateType.STORY_ENGINE_PREMISE
        }

        val (slotX, slotY, slotTag) = if (tab == com.deckapp.feature.draw.model.StoryEngineGuideTab.DECK_OF_WORLDS) {
            when (stepIndex) {
                0 -> Triple(300f, 300f, "REGION")
                1 -> Triple(300f, 100f, "LANDMARK")
                2 -> Triple(160f, 300f, "NAMESAKE")
                3 -> Triple(160f, 100f, "ORIGINS")
                4 -> Triple(440f, 300f, "ATTRIBUTES")
                5 -> Triple(440f, 500f, "ADVENT")
                else -> Triple(300f, 300f, "SLOT_$stepIndex")
            }
        } else {
            when (stepIndex) {
                0 -> Triple(100f, 200f, "BOOSTER")
                1 -> Triple(240f, 200f, "BACKSTORY")
                2 -> Triple(380f, 200f, "MYSTERY")
                3 -> Triple(520f, 200f, "UNIQUE_ITEM")
                else -> Triple(100f + (stepIndex * 140f), 200f, "SLOT_$stepIndex")
            }
        }

        updateCurrentActiveSurface { surface ->
            val highestZ = (surface.cards.maxOfOrNull { it.zIndex } ?: 0) + 1
            val filtered = surface.cards.filterNot { it.cardId == cardId }
            val newPlacement = PlaymatCardPlacement(
                cardId = cardId,
                x = slotX,
                y = slotY,
                rotationDegrees = rotationDegrees,
                zIndex = highestZ,
                isFaceUp = true,
                slotTag = slotTag
            )
            surface.copy(
                template = templateType,
                cards = filtered + newPlacement
            )
        }
        selectCard(cardId)
    }

    /**
     * Mueve una carta ya presente en el tapete.
     */
    fun moveCard(cardId: Long, newX: Float, newY: Float, slotTag: String? = null) {
        updateCurrentActiveSurface { surface ->
            val updatedCards = surface.cards.map { placement ->
                if (placement.cardId == cardId) {
                    placement.copy(
                        x = newX,
                        y = newY,
                        slotTag = slotTag ?: placement.slotTag
                    )
                } else placement
            }
            surface.copy(cards = updatedCards)
        }
    }

    /**
     * Rota la carta especificada sobre el tapete (por defecto 90° en sentido horario).
     */
    fun rotateCard(cardId: Long, deltaDegrees: Int = 90) {
        updateCurrentActiveSurface { surface ->
            val updatedCards = surface.cards.map { placement ->
                if (placement.cardId == cardId) {
                    placement.copy(rotationDegrees = (placement.rotationDegrees + deltaDegrees) % 360)
                } else placement
            }
            surface.copy(cards = updatedCards)
        }
    }

    /**
     * Alterna la cara visible (frente / dorso).
     */
    fun flipCard(cardId: Long) {
        updateCurrentActiveSurface { surface ->
            val updatedCards = surface.cards.map { placement ->
                if (placement.cardId == cardId) {
                    placement.copy(isFaceUp = !placement.isFaceUp)
                } else placement
            }
            surface.copy(cards = updatedCards)
        }
    }

    /**
     * Devuelve una carta colocada en el tapete a la mano privada.
     */
    fun returnCardToHand(cardId: Long) {
        val cardKey = "card_$cardId"
        updateCurrentActiveSurface { surface ->
            surface.copy(
                cards = surface.cards.filterNot { it.cardId == cardId },
                connections = surface.connections.filterNot {
                    it.fromCardId == cardId || it.toCardId == cardId ||
                    it.effectiveFromId == cardKey || it.effectiveToId == cardKey
                }
            )
        }
        if (_uiState.value.selectedPlaymatCardId == cardId) {
            selectCard(null)
        }
        _uiState.update { it.copy(snackbarMessage = "Carta devuelta a la mano") }
    }

    /**
     * Descarta una carta directamente desde el tapete.
     */
    fun discardCard(cardId: Long) {
        returnCardToHand(cardId)
        deckDelegate.discardCard(cardId)
    }

    /**
     * Roba del mazo seleccionado y la ubica directamente en el tapete en las coordenadas dadas.
     */
    fun quickDrawToPlaymat(stackId: Long, x: Float, y: Float) {
        scope.launch {
            val handBefore = _uiState.value.hand.map { it.id }.toSet()
            deckDelegate.drawCardFromDeck(stackId)
            // Localizar la nueva carta en mano
            val newHand = _uiState.value.hand
            val drawnCard = newHand.firstOrNull { it.id !in handBefore }
            if (drawnCard != null) {
                placeCardOnPlaymat(
                    cardId = drawnCard.id,
                    x = x,
                    y = y,
                    rotationDegrees = drawnCard.currentRotation,
                    isFaceUp = drawnCard.isRevealed
                )
            }
        }
    }

    /**
     * Añade una nota adhesiva al tapete.
     */
    fun addStickyNote(text: String, x: Float, y: Float, colorHex: String = "#FFF59D") {
        if (text.isBlank()) return
        val note = PlaymatStickyNote(
            id = UUID.randomUUID().toString(),
            text = text.trim(),
            colorHex = colorHex,
            x = x,
            y = y
        )
        updateCurrentActiveSurface { surface ->
            surface.copy(stickyNotes = surface.stickyNotes + note)
        }
    }

    fun moveStickyNote(noteId: String, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.stickyNotes.map {
                if (it.id == noteId) it.copy(x = newX, y = newY) else it
            }
            surface.copy(stickyNotes = updated)
        }
    }

    fun deleteStickyNote(noteId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(stickyNotes = surface.stickyNotes.filterNot { it.id == noteId })
        }
    }

    /**
     * Añade una ficha de tirada de tabla al tapete.
     */
    fun addTableChip(tableId: Long, tableName: String, rollResultText: String, x: Float, y: Float) {
        val chip = PlaymatTableChip(
            id = UUID.randomUUID().toString(),
            tableId = tableId,
            tableName = tableName,
            rollResultText = rollResultText,
            x = x,
            y = y
        )
        updateCurrentActiveSurface { surface ->
            surface.copy(tableChips = surface.tableChips + chip)
        }
    }

    fun moveTableChip(chipId: String, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.tableChips.map {
                if (it.id == chipId) it.copy(x = newX, y = newY) else it
            }
            surface.copy(tableChips = updated)
        }
    }

    fun deleteTableChip(chipId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(tableChips = surface.tableChips.filterNot { it.id == chipId })
        }
    }

    /**
     * Asegura que los mazos activos en sesión tengan colocación inicial en la mesa si no existen aún.
     */
    fun ensureDecksPlaced() {
        val activeSurface = _uiState.value.activePlaymatSurface
        val deckRefs = _uiState.value.deckRefs
        if (deckRefs.isEmpty()) return

        val currentPlacements = activeSurface.decks.associateBy { it.stackId }
        var changed = false
        val newPlacements = deckRefs.mapIndexed { index, ref ->
            val existing = currentPlacements[ref.stackId]
            if (existing != null) {
                existing
            } else {
                changed = true
                PlaymatDeckPlacement(
                    stackId = ref.stackId,
                    x = -160f,
                    y = 80f + (index * 240f),
                    discardX = -10f,
                    discardY = 80f + (index * 240f)
                )
            }
        }

        if (changed) {
            updateCurrentActiveSurface { it.copy(decks = newPlacements) }
        }
    }

    fun moveDeck(stackId: Long, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.decks.map { deck ->
                if (deck.stackId == stackId) {
                    deck.copy(x = newX, y = newY)
                } else deck
            }
            surface.copy(decks = updated)
        }
    }

    fun moveDiscardPile(stackId: Long, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.decks.map { deck ->
                if (deck.stackId == stackId) {
                    deck.copy(discardX = newX, discardY = newY)
                } else deck
            }
            surface.copy(decks = updated)
        }
    }

    fun drawFromPhysicalDeck(
        stackId: Long,
        targetX: Float,
        targetY: Float,
        isFaceUp: Boolean = true,
        slotTag: String? = null
    ) {
        scope.launch {
            val handBefore = _uiState.value.hand.map { it.id }.toSet()
            deckDelegate.drawCardFromDeck(stackId)
            val newHand = _uiState.value.hand
            val drawnCard = newHand.firstOrNull { it.id !in handBefore }
            if (drawnCard != null) {
                placeCardOnPlaymat(
                    cardId = drawnCard.id,
                    x = targetX,
                    y = targetY,
                    rotationDegrees = drawnCard.currentRotation,
                    isFaceUp = isFaceUp,
                    slotTag = slotTag
                )
            }
        }
    }

    fun reshuffleDeck(stackId: Long) {
        deckDelegate.shufflePileBack(stackId)
        val deckName = _uiState.value.deckNames[stackId] ?: "Mazo"
        _uiState.update { it.copy(snackbarMessage = "Descartes de \"$deckName\" barajados de vuelta al mazo") }
    }

    fun addDeckToPlaymat(stackId: Long, x: Float, y: Float) {
        updateCurrentActiveSurface { surface ->
            val filtered = surface.decks.filterNot { it.stackId == stackId }
            val newPlacement = PlaymatDeckPlacement(
                stackId = stackId,
                x = x,
                y = y,
                discardX = x + 150f,
                discardY = y
            )
            surface.copy(decks = filtered + newPlacement)
        }
    }

    fun removeDeckFromPlaymat(stackId: Long) {
        updateCurrentActiveSurface { surface ->
            surface.copy(decks = surface.decks.filterNot { it.stackId == stackId })
        }
    }

    // --- Tokens de Personajes (PJ / PNJ) ---

    fun addTokenToPlaymat(
        name: String,
        imagePath: String? = null,
        npcId: Long? = null,
        x: Float,
        y: Float,
        maxHp: Int = 10,
        currentHp: Int = 10,
        tokenType: PlaymatTokenType = PlaymatTokenType.ALLY,
        ringColorHex: String = "#4CAF50"
    ) {
        val token = PlaymatTokenPlacement(
            id = UUID.randomUUID().toString(),
            npcId = npcId,
            name = name.trim().ifBlank { "Personaje" },
            imagePath = imagePath,
            x = x,
            y = y,
            maxHp = maxHp,
            currentHp = currentHp,
            tokenType = tokenType,
            ringColorHex = ringColorHex
        )
        updateCurrentActiveSurface { surface ->
            surface.copy(tokens = surface.tokens + token)
        }
        _uiState.update { it.copy(snackbarMessage = "Token '${token.name}' colocado en la mesa") }
    }

    fun moveToken(tokenId: String, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.tokens.map { token ->
                if (token.id == tokenId) token.copy(x = newX, y = newY) else token
            }
            surface.copy(tokens = updated)
        }
    }

    fun updateTokenHp(tokenId: String, newHp: Int) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.tokens.map { token ->
                if (token.id == tokenId) {
                    val clampedHp = newHp.coerceIn(0, token.maxHp)
                    token.copy(currentHp = clampedHp)
                } else token
            }
            surface.copy(tokens = updated)
        }
    }

    fun updateTokenType(tokenId: String, newType: PlaymatTokenType) {
        val newRingColor = when (newType) {
            PlaymatTokenType.ALLY -> "#4CAF50"
            PlaymatTokenType.NEUTRAL -> "#FFB300"
            PlaymatTokenType.ENEMY -> "#E53935"
        }
        updateCurrentActiveSurface { surface ->
            val updated = surface.tokens.map { token ->
                if (token.id == tokenId) {
                    token.copy(tokenType = newType, ringColorHex = newRingColor)
                } else token
            }
            surface.copy(tokens = updated)
        }
    }

    fun removeToken(tokenId: String) {
        updateCurrentActiveSurface { surface ->
            val tokenKey = "token_$tokenId"
            surface.copy(
                tokens = surface.tokens.filterNot { it.id == tokenId },
                connections = surface.connections.filterNot {
                    it.effectiveFromId == tokenKey || it.effectiveToId == tokenKey ||
                    it.fromEndpointId == tokenKey || it.toEndpointId == tokenKey
                }
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Token retirado de la mesa") }
    }

    // --- Hilos Narrativos / Conexiones de Investigación ---

    fun createConnection(
        fromEndpointId: String,
        toEndpointId: String,
        label: String = "",
        colorHex: String = "#E53935"
    ) {
        if (fromEndpointId == toEndpointId || fromEndpointId.isBlank() || toEndpointId.isBlank()) return
        updateCurrentActiveSurface { surface ->
            val alreadyExists = surface.connections.any {
                (it.effectiveFromId == fromEndpointId && it.effectiveToId == toEndpointId) ||
                (it.effectiveFromId == toEndpointId && it.effectiveToId == fromEndpointId)
            }
            if (alreadyExists) return@updateCurrentActiveSurface surface

            val fromCardId = if (fromEndpointId.startsWith("card_")) {
                fromEndpointId.removePrefix("card_").toLongOrNull() ?: 0L
            } else 0L
            val toCardId = if (toEndpointId.startsWith("card_")) {
                toEndpointId.removePrefix("card_").toLongOrNull() ?: 0L
            } else 0L

            val newConnection = PlaymatConnection(
                id = UUID.randomUUID().toString(),
                fromCardId = fromCardId,
                toCardId = toCardId,
                fromEndpointId = fromEndpointId,
                toEndpointId = toEndpointId,
                label = label.trim(),
                colorHex = colorHex
            )
            surface.copy(connections = surface.connections + newConnection)
        }
        _uiState.update { it.copy(snackbarMessage = "Hilos conectados") }
    }

    fun createConnection(
        fromCardId: Long,
        toCardId: Long,
        label: String = "",
        colorHex: String = "#E53935"
    ) {
        createConnection(
            fromEndpointId = "card_$fromCardId",
            toEndpointId = "card_$toCardId",
            label = label,
            colorHex = colorHex
        )
    }

    fun updateConnection(
        connectionId: String,
        label: String,
        colorHex: String
    ) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.connections.map { conn ->
                if (conn.id == connectionId) {
                    conn.copy(label = label.trim(), colorHex = colorHex)
                } else conn
            }
            surface.copy(connections = updated)
        }
    }

    fun removeConnection(connectionId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(connections = surface.connections.filterNot { it.id == connectionId })
        }
        _uiState.update { it.copy(snackbarMessage = "Conexión eliminada") }
    }

    // --- Helpers privados ---

    private fun updateCurrentActiveSurface(transform: (PlaymatSurface) -> PlaymatSurface) {
        ensureInitialSurface()
        val activeId = _uiState.value.boardMemory.activePlaymatSurfaceId
            ?: _uiState.value.boardMemory.playmatSurfaces.firstOrNull()?.id
            ?: return

        updateActiveSurface { surface ->
            if (surface.id == activeId) transform(surface) else surface
        }
    }

    private fun updateActiveSurface(transform: (PlaymatSurface) -> PlaymatSurface) {
        updateBoardMemory { memory ->
            val updatedSurfaces = memory.playmatSurfaces.map(transform)
            memory.copy(playmatSurfaces = updatedSurfaces)
        }
    }
}
