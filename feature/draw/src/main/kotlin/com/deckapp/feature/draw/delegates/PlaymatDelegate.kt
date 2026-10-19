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
        if (memory.playmatSurfaces.isNotEmpty()) {
            val activeId = memory.activePlaymatSurfaceId ?: memory.playmatSurfaces.first().id
            return memory.playmatSurfaces.find { it.id == activeId } ?: memory.playmatSurfaces.first()
        }

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
        return defaultSurface
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
     * Mueve una carta ya presente en el tapete.
     */
    fun moveCard(cardId: Long, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updatedCards = surface.cards.map { placement ->
                if (placement.cardId == cardId) {
                    placement.copy(x = newX, y = newY)
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
        updateCurrentActiveSurface { surface ->
            surface.copy(cards = surface.cards.filterNot { it.cardId == cardId })
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
