package com.deckapp.feature.draw.delegates

import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.model.state.PlaymatUiState
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
    private val deckDelegate: SessionDeckDelegate,
    private val onLogTokenHp: ((tokenName: String, delta: Int, newHp: Int, maxHp: Int) -> Unit)? = null,
    private val onLogClockProgress: ((title: String, current: Int, max: Int) -> Unit)? = null,
    private val onLogCardAction: ((cardTitle: String, action: String) -> Unit)? = null
) {

    /**
     * Helper para mutaciones atómicas del sub-estado [PlaymatUiState].
     */
    fun updatePlaymatState(transform: (PlaymatUiState) -> PlaymatUiState) {
        _uiState.update { it.copy(playmat = transform(it.playmat)) }
    }

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
        updatePlaymatState { it.copy(selectedCardId = cardId) }
    }

    fun toggleHandTray() {
        updatePlaymatState { it.copy(showHandTray = !it.showHandTray) }
    }

    fun setHandTrayVisible(visible: Boolean) {
        updatePlaymatState { it.copy(showHandTray = visible) }
    }

    fun createPlaymatGroup(name: String, memberIds: Set<String>) {
        val cleanName = name.trim().ifBlank { "Nuevo grupo" }
        if (memberIds.isEmpty()) return
        val group = PlaymatElementGroup(name = cleanName, memberIds = memberIds)
        updateCurrentActiveSurface { surface ->
            val existingIds = surface.groups.map { it.id }.toSet()
            val validMembers = memberIds.filterTo(mutableSetOf()) { member ->
                when {
                    member.startsWith("card:") -> surface.cards.any { it.cardId == member.removePrefix("card:").toLongOrNull() }
                    member.startsWith("note:") -> surface.stickyNotes.any { it.id == member.removePrefix("note:") }
                    member.startsWith("chip:") -> surface.tableChips.any { it.id == member.removePrefix("chip:") }
                    member.startsWith("deck:") || member.startsWith("discard:") -> surface.decks.any { it.stackId == member.substringAfter(':').toLongOrNull() }
                    member.startsWith("token:") -> surface.tokens.any { it.id == member.removePrefix("token:") }
                    member.startsWith("clock:") -> surface.clocks.any { it.id == member.removePrefix("clock:") }
                    else -> false
                }
            }
            if (validMembers.isEmpty()) return@updateCurrentActiveSurface surface
            val groupId = if (group.id in existingIds) UUID.randomUUID().toString() else group.id
            val nextGroup = group.copy(id = groupId, memberIds = validMembers)
            fun assign(id: String, current: String?) = if (id in validMembers) groupId else current?.takeIf { old ->
                surface.groups.none { it.id == old && id in it.memberIds }
            }
            surface.copy(
                groups = surface.groups.map { old -> old.copy(memberIds = old.memberIds - validMembers) }.filter { it.memberIds.isNotEmpty() } + nextGroup,
                cards = surface.cards.map { it.copy(clusterId = assign("card:${it.cardId}", it.clusterId)) },
                stickyNotes = surface.stickyNotes.map { it.copy(groupId = assign("note:${it.id}", it.groupId)) },
                tableChips = surface.tableChips.map { it.copy(groupId = assign("chip:${it.id}", it.groupId)) },
                decks = surface.decks.map { it.copy(groupId = assign("deck:${it.stackId}", it.groupId)) }
            ).let { updated ->
                updated.copy(
                    decks = updated.decks.map { placement ->
                        placement.copy(discardGroupId = assign("discard:${placement.stackId}", placement.discardGroupId))
                    },
                    tokens = updated.tokens.map { it.copy(groupId = assign("token:${it.id}", it.groupId)) },
                    clocks = updated.clocks.map { it.copy(groupId = assign("clock:${it.id}", it.groupId)) }
                )
            }
        }
    }

    fun renamePlaymatGroup(groupId: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        updateCurrentActiveSurface { surface ->
            surface.copy(groups = surface.groups.map { if (it.id == groupId) it.copy(name = cleanName) else it })
        }
    }

    fun setPlaymatGroupMembers(groupId: String, memberIds: Set<String>) {
        updateCurrentActiveSurface { surface ->
            val validMembers = memberIds.filterTo(mutableSetOf()) { member ->
                when {
                    member.startsWith("card:") -> surface.cards.any { it.cardId == member.removePrefix("card:").toLongOrNull() }
                    member.startsWith("note:") -> surface.stickyNotes.any { it.id == member.removePrefix("note:") }
                    member.startsWith("chip:") -> surface.tableChips.any { it.id == member.removePrefix("chip:") }
                    member.startsWith("deck:") || member.startsWith("discard:") -> surface.decks.any { it.stackId == member.substringAfter(':').toLongOrNull() }
                    member.startsWith("token:") -> surface.tokens.any { it.id == member.removePrefix("token:") }
                    member.startsWith("clock:") -> surface.clocks.any { it.id == member.removePrefix("clock:") }
                    else -> false
                }
            }
            fun groupFor(id: String, existing: String?) = when {
                id in validMembers -> groupId
                existing == groupId -> null
                else -> existing
            }
            surface.copy(
                groups = surface.groups.map { group ->
                    if (group.id == groupId) group.copy(memberIds = validMembers)
                    else group.copy(memberIds = group.memberIds - validMembers)
                }.filter { it.id == groupId || it.memberIds.isNotEmpty() },
                cards = surface.cards.map { it.copy(clusterId = groupFor("card:${it.cardId}", it.clusterId)) },
                stickyNotes = surface.stickyNotes.map { it.copy(groupId = groupFor("note:${it.id}", it.groupId)) },
                tableChips = surface.tableChips.map { it.copy(groupId = groupFor("chip:${it.id}", it.groupId)) },
                decks = surface.decks.map { placement ->
                    placement.copy(
                        groupId = groupFor("deck:${placement.stackId}", placement.groupId),
                        discardGroupId = groupFor("discard:${placement.stackId}", placement.discardGroupId)
                    )
                },
                tokens = surface.tokens.map { it.copy(groupId = groupFor("token:${it.id}", it.groupId)) },
                clocks = surface.clocks.map { it.copy(groupId = groupFor("clock:${it.id}", it.groupId)) }
            )
        }
    }

    fun togglePlaymatGroupLock(groupId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(groups = surface.groups.map { if (it.id == groupId) it.copy(isLocked = !it.isLocked) else it })
        }
    }

    fun togglePlaymatGroupVisibility(groupId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(groups = surface.groups.map {
                if (it.id == groupId) it.copy(isHidden = !it.isHidden) else it
            })
        }
    }

    fun deletePlaymatGroup(groupId: String) {
        updateCurrentActiveSurface { surface ->
            surface.copy(
                groups = surface.groups.filterNot { it.id == groupId },
                cards = surface.cards.map { if (it.clusterId == groupId) it.copy(clusterId = null) else it },
                stickyNotes = surface.stickyNotes.map { if (it.groupId == groupId) it.copy(groupId = null) else it },
                tableChips = surface.tableChips.map { if (it.groupId == groupId) it.copy(groupId = null) else it },
                decks = surface.decks.map { it.copy(
                    groupId = it.groupId?.takeUnless { id -> id == groupId },
                    discardGroupId = it.discardGroupId?.takeUnless { id -> id == groupId }
                ) },
                tokens = surface.tokens.map { if (it.groupId == groupId) it.copy(groupId = null) else it },
                clocks = surface.clocks.map { if (it.groupId == groupId) it.copy(groupId = null) else it }
            )
        }
    }

    fun movePlaymatGroup(groupId: String, dx: Float, dy: Float) {
        updateCurrentActiveSurface { surface ->
            val group = surface.groups.firstOrNull { it.id == groupId } ?: return@updateCurrentActiveSurface surface
            if (group.isLocked) return@updateCurrentActiveSurface surface
            fun member(type: String, id: Any) = "$type:$id" in group.memberIds
            surface.copy(
                cards = surface.cards.map { if (member("card", it.cardId)) it.copy(x = it.x + dx, y = it.y + dy) else it },
                stickyNotes = surface.stickyNotes.map { if (member("note", it.id)) it.copy(x = it.x + dx, y = it.y + dy) else it },
                tableChips = surface.tableChips.map { if (member("chip", it.id)) it.copy(x = it.x + dx, y = it.y + dy) else it },
                decks = surface.decks.map { placement ->
                    val moveDeck = member("deck", placement.stackId)
                    val moveDiscard = member("discard", placement.stackId)
                    placement.copy(
                        x = if (moveDeck) placement.x + dx else placement.x,
                        y = if (moveDeck) placement.y + dy else placement.y,
                        discardX = if (moveDiscard) (placement.discardX ?: placement.x + 150f) + dx else placement.discardX,
                        discardY = if (moveDiscard) (placement.discardY ?: placement.y) + dy else placement.discardY
                    )
                },
                tokens = surface.tokens.map { if (member("token", it.id)) it.copy(x = it.x + dx, y = it.y + dy) else it },
                clocks = surface.clocks.map { if (member("clock", it.id)) it.copy(x = it.x + dx, y = it.y + dy) else it }
            )
        }
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
        var isNowFaceUp = true
        updateCurrentActiveSurface { surface ->
            val updatedCards = surface.cards.map { placement ->
                if (placement.cardId == cardId) {
                    val next = !placement.isFaceUp
                    isNowFaceUp = next
                    placement.copy(isFaceUp = next)
                } else placement
            }
            surface.copy(cards = updatedCards)
        }
        val cardName = _uiState.value.hand.find { it.id == cardId }?.title ?: "Carta #$cardId"
        onLogCardAction?.invoke(cardName, if (isNowFaceUp) "Revelada boca arriba" else "Volteada boca abajo")
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
        val cardName = _uiState.value.hand.find { it.id == cardId }?.title ?: "Carta #$cardId"
        returnCardToHand(cardId)
        deckDelegate.discardCard(cardId)
        onLogCardAction?.invoke(cardName, "Descartada al cementerio")
    }

    /**
     * Devuelve una carta directamente al mazo desde el tapete.
     */
    fun returnCardToDeck(cardId: Long) {
        val cardName = _uiState.value.hand.find { it.id == cardId }?.title ?: "Carta #$cardId"
        returnCardToHand(cardId)
        deckDelegate.returnToDeck(cardId)
        onLogCardAction?.invoke(cardName, "Devuelta al mazo")
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
        val memory = _uiState.value.boardMemory
        val surfaces = memory.playmatSurfaces
        if (surfaces.isEmpty()) return
        val activeId = memory.activePlaymatSurfaceId ?: surfaces.first().id
        val activeSurface = surfaces.find { it.id == activeId } ?: surfaces.first()
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
            updateActiveSurface { surface ->
                if (surface.id == activeId) surface.copy(decks = newPlacements) else surface
            }
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

    fun setDeckVisibleOnPlaymat(stackId: Long, visible: Boolean) {
        updateCurrentActiveSurface { surface ->
            val existing = surface.decks.firstOrNull { it.stackId == stackId }
            if (existing != null) {
                surface.copy(decks = surface.decks.map {
                    if (it.stackId == stackId) it.copy(isVisibleOnPlaymat = visible) else it
                })
            } else {
                val index = surface.decks.size
                val x = -160f
                val y = 80f + index * 240f
                surface.copy(decks = surface.decks + PlaymatDeckPlacement(
                    stackId = stackId,
                    x = x,
                    y = y,
                    discardX = x + 150f,
                    discardY = y,
                    isVisibleOnPlaymat = visible
                ))
            }
        }
    }

    fun setDeckFavorite(stackId: Long, favorite: Boolean) {
        updateCurrentActiveSurface { surface ->
            val favorites = if (favorite) surface.favoriteDeckIds + stackId else surface.favoriteDeckIds - stackId
            surface.copy(favoriteDeckIds = favorites)
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
        var tokenFound: PlaymatTokenPlacement? = null
        var oldHp = 0
        updateCurrentActiveSurface { surface ->
            val updated = surface.tokens.map { token ->
                if (token.id == tokenId) {
                    tokenFound = token
                    oldHp = token.currentHp
                    val clampedHp = newHp.coerceIn(0, token.maxHp)
                    token.copy(currentHp = clampedHp)
                } else token
            }
            surface.copy(tokens = updated)
        }
        tokenFound?.let { token ->
            val clampedHp = newHp.coerceIn(0, token.maxHp)
            val delta = clampedHp - oldHp
            if (delta != 0) {
                onLogTokenHp?.invoke(token.name, delta, clampedHp, token.maxHp)
            }
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

    // --- Relojes de Progreso (Progress Clocks) ---

    fun addClockToPlaymat(
        title: String,
        totalSegments: Int,
        x: Float,
        y: Float,
        colorHex: String = "#E53935"
    ) {
        val clock = PlaymatClockPlacement(
            title = title.ifBlank { "Reloj de Tensión" },
            totalSegments = totalSegments.coerceIn(4, 8),
            filledSegments = 0,
            x = x,
            y = y,
            colorHex = colorHex
        )
        updateCurrentActiveSurface { surface ->
            surface.copy(clocks = surface.clocks + clock)
        }
        _uiState.update { it.copy(snackbarMessage = "Reloj '${clock.title}' añadido") }
    }

    fun updateClockProgress(clockId: String, filledSegments: Int) {
        var clockFound: PlaymatClockPlacement? = null
        updateCurrentActiveSurface { surface ->
            val updated = surface.clocks.map { clock ->
                if (clock.id == clockId) {
                    clockFound = clock
                    val clamped = filledSegments.coerceIn(0, clock.totalSegments)
                    clock.copy(filledSegments = clamped)
                } else clock
            }
            surface.copy(clocks = updated)
        }
        clockFound?.let { clock ->
            val clamped = filledSegments.coerceIn(0, clock.totalSegments)
            if (clamped != clock.filledSegments) {
                onLogClockProgress?.invoke(clock.title, clamped, clock.totalSegments)
            }
        }
    }

    fun moveClock(clockId: String, newX: Float, newY: Float) {
        updateCurrentActiveSurface { surface ->
            val updated = surface.clocks.map { clock ->
                if (clock.id == clockId) clock.copy(x = newX, y = newY) else clock
            }
            surface.copy(clocks = updated)
        }
    }

    fun removeClock(clockId: String) {
        updateCurrentActiveSurface { surface ->
            val clockKey = "clock_$clockId"
            surface.copy(
                clocks = surface.clocks.filterNot { it.id == clockId },
                connections = surface.connections.filterNot {
                    it.effectiveFromId == clockKey || it.effectiveToId == clockKey ||
                    it.fromEndpointId == clockKey || it.toEndpointId == clockKey
                }
            )
        }
        _uiState.update { it.copy(snackbarMessage = "Reloj retirado de la mesa") }
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

    // --- Fondos de Escena, Battlemaps y Cuadrícula Táctica (Fase 7) ---

    fun setBackgroundMap(
        uri: String?,
        preset: PlaymatAtmosphericPreset = PlaymatAtmosphericPreset.NONE
    ) {
        updateCurrentActiveSurface { surface ->
            surface.copy(
                customImageUri = uri,
                backgroundPreset = preset
            )
        }
        val msg = if (preset != PlaymatAtmosphericPreset.NONE) "Preset atmosférico aplicado" else if (uri != null) "Battlemap cargado" else "Fondo restablecido"
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun setBackgroundTransform(
        opacity: Float? = null,
        scale: Float? = null,
        x: Float? = null,
        y: Float? = null
    ) {
        updateCurrentActiveSurface { surface ->
            surface.copy(
                backgroundOpacity = opacity?.coerceIn(0.1f, 1.0f) ?: surface.backgroundOpacity,
                backgroundScale = scale?.coerceIn(0.2f, 5.0f) ?: surface.backgroundScale,
                backgroundX = x ?: surface.backgroundX,
                backgroundY = y ?: surface.backgroundY
            )
        }
    }

    fun toggleBackgroundLock() {
        updateCurrentActiveSurface { surface ->
            surface.copy(isBackgroundLocked = !surface.isBackgroundLocked)
        }
    }

    fun setGridConfig(
        gridType: PlaymatGridType,
        gridStyle: PlaymatGridStyle = PlaymatGridStyle.LINES,
        cellSizeDp: Float = 48f,
        snapToGrid: Boolean = false
    ) {
        updateCurrentActiveSurface { surface ->
            surface.copy(
                gridType = gridType,
                gridStyle = gridStyle,
                cellSizeDp = cellSizeDp.coerceIn(24f, 128f),
                snapToGrid = snapToGrid
            )
        }
    }

    fun toggleSnapToGrid() {
        updateCurrentActiveSurface { surface ->
            surface.copy(snapToGrid = !surface.snapToGrid)
        }
    }

    /**
     * Calcula la posición con snap magnético más cercana según el tipo de cuadrícula.
     */
    fun calculateSnappedPosition(x: Float, y: Float, gridType: PlaymatGridType, cellSize: Float): Pair<Float, Float> {
        if (gridType == PlaymatGridType.NONE || cellSize <= 0f) return Pair(x, y)
        return when (gridType) {
            PlaymatGridType.SQUARE -> {
                val snappedX = (kotlin.math.round(x / cellSize)) * cellSize
                val snappedY = (kotlin.math.round(y / cellSize)) * cellSize
                Pair(snappedX, snappedY)
            }
            PlaymatGridType.HEXAGONAL -> {
                val hexWidth = cellSize * 1.5f
                val hexHeight = cellSize * kotlin.math.sqrt(3.0).toFloat()
                val col = kotlin.math.round(x / hexWidth)
                val rowOffset = if ((col.toInt() % 2) != 0) hexHeight / 2f else 0f
                val row = kotlin.math.round((y - rowOffset) / hexHeight)
                Pair(col * hexWidth, (row * hexHeight) + rowOffset)
            }
            PlaymatGridType.NONE -> Pair(x, y)
        }
    }

    // --- Helpers privados ---

    private fun updateCurrentActiveSurface(transform: (PlaymatSurface) -> PlaymatSurface) {
        val memory = _uiState.value.boardMemory
        val activeId = memory.activePlaymatSurfaceId
            ?: memory.playmatSurfaces.firstOrNull()?.id

        if (activeId == null) {
            val defaultSurface = PlaymatSurface(
                id = "surface_default_$sessionId",
                name = "Mesa Principal",
                backgroundTheme = PlaymatTheme.FELT_GREEN,
                template = PlaymatTemplateType.FREEFORM,
                cameraX = 0f,
                cameraY = 0f,
                cameraZoom = 1f
            )
            val updated = transform(defaultSurface)
            updateBoardMemory {
                it.copy(
                    playmatSurfaces = listOf(updated),
                    activePlaymatSurfaceId = updated.id
                )
            }
            return
        }

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
