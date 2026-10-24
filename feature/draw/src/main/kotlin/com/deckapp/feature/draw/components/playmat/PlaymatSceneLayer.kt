package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.model.state.PlaymatActions
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun PlaymatSceneLayer(
    state: PlaymatSceneState,
    events: PlaymatCanvasEvents,
    graphicsLayer: GraphicsLayer
) {
    val actions = state.actions
    val activeSurface = state.activeSurface
    val cameraX = state.cameraX
    val cameraY = state.cameraY
    val cameraZoom = state.cameraZoom
    val onMoveCard = actions.onMoveCard
    val onSelectCard = actions.onSelectCard
    val onMoveStickyNote = actions.onMoveStickyNote
    val onDeleteStickyNote = actions.onDeleteStickyNote
    val onCardDetailClick = actions.onCardDetailClick
    val onMoveDeck = actions.onMoveDeck
    val onMoveDiscardPile = actions.onMoveDiscardPile
    val onDrawFromDeck = actions.onDrawFromDeck
    val onReshuffleDeck = actions.onReshuffleDeck
    val onCreateConnection = actions.onCreateConnection
    val onMoveToken = actions.onMoveToken
    val onUpdateTokenType = actions.onUpdateTokenType
    val onRemoveToken = actions.onRemoveToken
    val onUpdateClockProgress = actions.onUpdateClockProgress
    val onMoveClock = actions.onMoveClock
    val onRemoveClock = actions.onRemoveClock
    val selectedTokenId = state.selectedTokenId
    val selectedClockId = state.selectedClockId
    val hoveredSlotTag = state.hoveredSlotTag
    val connectingSourceEndpointId = state.connectingSourceEndpointId
    val cardsById = state.cardsById
    val endpointPositions = state.endpointPositions
    val piledCardsByDeck = state.piledCardsByDeck
    val currentTemplateSlots = state.currentTemplateSlots
    val spatialIndex = remember(activeSurface) { PlaymatSpatialIndex(activeSurface) }

    fun moveAsGroup(groupId: String?, originalX: Float, originalY: Float, newX: Float, newY: Float, moveSingle: () -> Unit) {
        val group = activeSurface.groups.firstOrNull { it.id == groupId }
        when {
            group == null -> moveSingle()
            group.isLocked -> Unit
            else -> actions.onMovePlaymatGroup(group.id, newX - originalX, newY - originalY)
        }
    }

// 1. Contenido Dibujable de la Mesa (Capturable por Snapshot)
BoxWithConstraints(
    modifier = Modifier
        .fillMaxSize()
        .drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(graphicsLayer)
        }
) {
    val viewportWidth = constraints.maxWidth.toFloat()
    val viewportHeight = constraints.maxHeight.toFloat()
    val marginX = viewportWidth / cameraZoom / 2f
    val marginY = viewportHeight / cameraZoom / 2f
    val visibleWorldBounds = Rect(
        left = -cameraX / cameraZoom - marginX,
        top = -cameraY / cameraZoom - marginY,
        right = (viewportWidth - cameraX) / cameraZoom + marginX,
        bottom = (viewportHeight - cameraY) / cameraZoom + marginY
    )
    val visibleEntries = remember(spatialIndex, visibleWorldBounds) { spatialIndex.query(visibleWorldBounds) }
    val visibleIdsByKind = remember(visibleEntries) { visibleEntries.groupBy { it.kind }.mapValues { (_, entries) -> entries.mapTo(mutableSetOf()) { it.id } } }
    val visibleCardIds = visibleIdsByKind[PlaymatSpatialIndex.Kind.CARD].orEmpty() + listOfNotNull(state.selectedPlaymatCardId?.let { "card:$it" }, connectingSourceEndpointId?.takeIf { it.startsWith("card_") }?.removePrefix("card_")?.let { "card:$it" })
    val visibleTokenIds = visibleIdsByKind[PlaymatSpatialIndex.Kind.TOKEN].orEmpty() + listOfNotNull(state.selectedTokenId?.let { "token:$it" }, connectingSourceEndpointId?.takeIf { it.startsWith("token_") }?.removePrefix("token_")?.let { "token:$it" })
    val visibleClockIds = visibleIdsByKind[PlaymatSpatialIndex.Kind.CLOCK].orEmpty() + listOfNotNull(state.selectedClockId?.let { "clock:$it" }, connectingSourceEndpointId?.takeIf { it.startsWith("clock_") }?.removePrefix("clock_")?.let { "clock:$it" })
    val visibleNotes = visibleIdsByKind[PlaymatSpatialIndex.Kind.NOTE].orEmpty()
    val visibleChips = visibleIdsByKind[PlaymatSpatialIndex.Kind.CHIP].orEmpty()
    val visibleDecks = visibleIdsByKind[PlaymatSpatialIndex.Kind.DECK].orEmpty()
    val visibleDiscards = visibleIdsByKind[PlaymatSpatialIndex.Kind.DISCARD].orEmpty()

    // Fondo Texturizado / Tapete
    PlaymatBackground(theme = activeSurface.backgroundTheme)

    // Capa de Battlemap y Preset Atmosférico
    PlaymatBattlemapLayer(
        customImageUri = activeSurface.customImageUri,
        preset = activeSurface.backgroundPreset,
        opacity = activeSurface.backgroundOpacity,
        scale = activeSurface.backgroundScale,
        offsetX = activeSurface.backgroundX,
        offsetY = activeSurface.backgroundY,
        zoom = cameraZoom
    )

    // Capa de Cuadrícula Táctica (Squares / Hexes / Dots)
    PlaymatGridLayer(
        gridType = activeSurface.gridType,
        gridStyle = activeSurface.gridStyle,
        cellSizeDp = activeSurface.cellSizeDp,
        cameraX = cameraX,
        cameraY = cameraY,
        zoom = cameraZoom
    )

    // Contenido Espacial 2D Transformado (Plantillas, Cartas, Notas)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(cameraX.roundToInt(), cameraY.roundToInt()) }
    ) {

        // Renderizado de Moldes y Plantillas Temáticas
        PlaymatTemplateSilhouettes(
            template = activeSurface.template,
            zoom = cameraZoom,
            highlightedSlotTag = hoveredSlotTag
        )

        // Fichas de Tablas
        activeSurface.tableChips.filter { "chip:${it.id}" in visibleChips }.forEach { chip ->
            PlaymatTableChipItem(
                chip = chip,
                zoom = cameraZoom,
                isMovementEnabled = activeSurface.groups.none { it.id == chip.groupId && it.isLocked },
                onMove = { newX, newY ->
                    moveAsGroup(chip.groupId, chip.x, chip.y, newX, newY) {
                        actions.onMoveTableChip(chip.id, newX, newY)
                    }
                }
            )
        }

        // Notas Adhesivas
        activeSurface.stickyNotes.filter { "note:${it.id}" in visibleNotes }.forEach { note ->
            PlaymatStickyNoteItem(
                note = note,
                zoom = cameraZoom,
                isMovementEnabled = activeSurface.groups.none { it.id == note.groupId && it.isLocked },
                onDelete = { onDeleteStickyNote(note.id) },
                onMove = { newX, newY -> moveAsGroup(note.groupId, note.x, note.y, newX, newY) { onMoveStickyNote(note.id, newX, newY) } }
            )
        }

        // Mazos Físicos 3D y Pilas de Descarte en la Mesa
        activeSurface.decks.filter { "deck:${it.stackId}" in visibleDecks || "discard:${it.stackId}" in visibleDiscards }.forEach { deckPlacement ->
            val deckName = state.deckNames[deckPlacement.stackId] ?: "Mazo"
            val availableCount = state.deckCardCounts[deckPlacement.stackId] ?: 0
            val piledCards = piledCardsByDeck[deckPlacement.stackId] ?: emptyList()
            val totalCount = availableCount + piledCards.size
            val backImage = state.deckBackImages[deckPlacement.stackId]

            key("deck_${deckPlacement.stackId}") {
                if ("deck:${deckPlacement.stackId}" in visibleDecks) PlaymatDeckItem(
                    placement = deckPlacement,
                    deckName = deckName,
                    availableCount = availableCount,
                    totalCount = totalCount,
                    backImage = backImage,
                    zoom = cameraZoom,
                    templateSlots = currentTemplateSlots,
                    isGroupLocked = activeSurface.groups.any { it.id == deckPlacement.groupId && it.isLocked },
                    onTapDraw = {
                        onDrawFromDeck(
                            deckPlacement.stackId,
                            deckPlacement.x + 150f,
                            deckPlacement.y,
                            null
                        )
                    },
                    onDragDrawCardToPlaymat = { targetX, targetY, slotTag ->
                        onDrawFromDeck(deckPlacement.stackId, targetX, targetY, slotTag)
                    },
                    onMoveDeck = { newX, newY -> moveAsGroup(deckPlacement.groupId, deckPlacement.x, deckPlacement.y, newX, newY) { onMoveDeck(deckPlacement.stackId, newX, newY) } },
                    onHoverSlotChanged = { tag ->
                        events.onHoveredSlotTagChanged(tag)
                    },
                    onShuffleDeck = {
                        onReshuffleDeck(deckPlacement.stackId)
                    }
                )

                if ("discard:${deckPlacement.stackId}" in visibleDiscards) PlaymatDiscardPileItem(
                    stackId = deckPlacement.stackId,
                    x = deckPlacement.discardX ?: (deckPlacement.x + 150f),
                    y = deckPlacement.discardY ?: deckPlacement.y,
                    zoom = cameraZoom,
                    isMovementEnabled = activeSurface.groups.none { it.id == deckPlacement.discardGroupId && it.isLocked },
                    piledCards = piledCards,
                    onReshuffleToDeck = {
                        onReshuffleDeck(deckPlacement.stackId)
                    },
                    onMoveDiscardPile = { newX, newY ->
                        val oldX = deckPlacement.discardX ?: (deckPlacement.x + 150f)
                        val oldY = deckPlacement.discardY ?: deckPlacement.y
                        moveAsGroup(deckPlacement.discardGroupId, oldX, oldY, newX, newY) { onMoveDiscardPile(deckPlacement.stackId, newX, newY) }
                    },
                    onInspectCard = onCardDetailClick
                )
            }
        }

        // Hilos Narrativos y Conexiones de Investigación
        activeSurface.connections.filter { conn ->
            val from = endpointPositions[conn.effectiveFromId] ?: return@filter false
            val to = endpointPositions[conn.effectiveToId] ?: return@filter false
            val middleX = (from.x + to.x) / 2f
            val middleY = (from.y + to.y) / 2f + (kotlin.math.hypot(to.x - from.x, to.y - from.y) * 0.05f).coerceAtMost(35f)
            Rect(
                left = minOf(from.x, to.x, middleX) - 60f,
                top = minOf(from.y, to.y, middleY) - 40f,
                right = maxOf(from.x, to.x, middleX) + 60f,
                bottom = maxOf(from.y, to.y, middleY) + 40f
            ).overlaps(visibleWorldBounds)
        }.forEach { conn ->
            val fromPos = endpointPositions[conn.effectiveFromId]
            val toPos = endpointPositions[conn.effectiveToId]
            if (fromPos != null && toPos != null) {
                key("conn_${conn.id}") {
                    PlaymatConnectionLine(
                        connection = conn,
                        fromPos = fromPos,
                        toPos = toPos,
                        onEdit = { events.onEditingConnectionChanged(conn) }
                    )
                }
            }
        }

        // Cartas colocadas sobre el tapete con arrastre desacoplado y ultra fluido
        activeSurface.cards.filter { "card:${it.cardId}" in visibleCardIds }.sortedBy { it.zIndex }.forEach { placement ->
            val card = cardsById[placement.cardId]
            if (card != null) {
                val isSelected = state.selectedPlaymatCardId == placement.cardId
                val isConnectingSource = connectingSourceEndpointId == "card_${placement.cardId}"
                val stackId = card.originDeckId ?: card.stackId
                val backImage = state.deckBackImages[stackId]

                key(placement.cardId) {
                    PlaymatCardItem(
                        placement = placement,
                        card = card,
                        backImage = backImage,
                        isSelected = isSelected,
                        isConnectingSource = isConnectingSource,
                        isMovementEnabled = activeSurface.groups.none { it.id == placement.clusterId && it.isLocked },
                        zoom = cameraZoom,
                        templateSlots = currentTemplateSlots,
                        onCardClick = {
                            if (connectingSourceEndpointId != null) {
                                val targetEndpoint = "card_${placement.cardId}"
                                if (connectingSourceEndpointId != targetEndpoint) {
                                    onCreateConnection(connectingSourceEndpointId!!, targetEndpoint)
                                }
                                events.onConnectingSourceChanged(null)
                            } else {
                                events.onSelectedTokenChanged(null)
                                events.onSelectedClockChanged(null)
                                // A tap selects the card. Selection is cleared by tapping empty canvas
                                // or by dismissing its action bar, so tapping again remains predictable.
                                onSelectCard(placement.cardId)
                            }
                        },
                        onMove = { cardId, finalX, finalY, slotTag ->
                            val finalXWithSnap = if (activeSurface.snapToGrid && slotTag == null && activeSurface.gridType != PlaymatGridType.NONE) {
                                (kotlin.math.round(finalX / activeSurface.cellSizeDp)) * activeSurface.cellSizeDp
                            } else finalX
                            val finalYWithSnap = if (activeSurface.snapToGrid && slotTag == null && activeSurface.gridType != PlaymatGridType.NONE) {
                                (kotlin.math.round(finalY / activeSurface.cellSizeDp)) * activeSurface.cellSizeDp
                            } else finalY
                            moveAsGroup(placement.clusterId, placement.x, placement.y, finalXWithSnap, finalYWithSnap) {
                                onMoveCard(cardId, finalXWithSnap, finalYWithSnap, slotTag)
                            }
                        },
                        onHoverSlotChanged = { tag ->
                            events.onHoveredSlotTagChanged(tag)
                        }
                    )
                }
            }
        }

        // Tokens de Personajes (PJ / PNJ)
        activeSurface.tokens.filter { "token:${it.id}" in visibleTokenIds }.forEach { token ->
            val isTokenSelected = selectedTokenId == token.id
            val isTokenConnectingSource = connectingSourceEndpointId == "token_${token.id}"
            key("token_${token.id}") {
                PlaymatTokenItem(
                    token = token,
                    isSelected = isTokenSelected,
                    isConnectingSource = isTokenConnectingSource,
                    zoom = cameraZoom,
                    isMovementEnabled = activeSurface.groups.none { it.id == token.groupId && it.isLocked },
                    onTokenClick = {
                        if (connectingSourceEndpointId != null) {
                            val targetEndpoint = "token_${token.id}"
                            if (connectingSourceEndpointId != targetEndpoint) {
                                onCreateConnection(connectingSourceEndpointId!!, targetEndpoint)
                            }
                            events.onConnectingSourceChanged(null)
                        } else {
                            events.onSelectedTokenChanged(if (isTokenSelected) null else token.id)
                            events.onSelectedClockChanged(null)
                            onSelectCard(null)
                        }
                    },
                    onDragProgress = { start, current ->
                        events.onTokenDragPositionsChanged(start, current)
                    },
                    onMove = { tokenId, finalX, finalY ->
                        val (snappedX, snappedY) = if (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE) {
                            val cell = activeSurface.cellSizeDp
                            Pair((kotlin.math.round(finalX / cell)) * cell, (kotlin.math.round(finalY / cell)) * cell)
                        } else Pair(finalX, finalY)
                        moveAsGroup(token.groupId, token.x, token.y, snappedX, snappedY) { onMoveToken(tokenId, snappedX, snappedY) }
                    },
                    onStartConnection = {
                        events.onConnectingSourceChanged("token_${token.id}")
                        events.onSelectedTokenChanged(null)
                    },
                    onEditHp = {
                        events.onEditingHpTokenChanged(token)
                        events.onSelectedTokenChanged(null)
                    },
                    onToggleFaction = {
                        val nextType = when (token.tokenType) {
                            PlaymatTokenType.ALLY -> PlaymatTokenType.NEUTRAL
                            PlaymatTokenType.NEUTRAL -> PlaymatTokenType.ENEMY
                            PlaymatTokenType.ENEMY -> PlaymatTokenType.ALLY
                        }
                        onUpdateTokenType(token.id, nextType)
                    },
                    onRemove = {
                        onRemoveToken(token.id)
                        events.onSelectedTokenChanged(null)
                    },
                    onDismissMenu = {
                        events.onSelectedTokenChanged(null)
                    }
                )
            }
        }

        // Relojes de Progreso (Progress Clocks estilo Blades in the Dark)
        activeSurface.clocks.filter { "clock:${it.id}" in visibleClockIds }.forEach { clock ->
            val isClockSelected = selectedClockId == clock.id
            val isClockConnectingSource = connectingSourceEndpointId == "clock_${clock.id}"
            key("clock_${clock.id}") {
                PlaymatClockItem(
                    clock = clock,
                    isSelected = isClockSelected,
                    isConnectingSource = isClockConnectingSource,
                    zoom = cameraZoom,
                    isMovementEnabled = activeSurface.groups.none { it.id == clock.groupId && it.isLocked },
                    onClockClick = {
                        if (connectingSourceEndpointId != null) {
                            val targetEndpoint = "clock_${clock.id}"
                            if (connectingSourceEndpointId != targetEndpoint) {
                                onCreateConnection(connectingSourceEndpointId!!, targetEndpoint)
                            }
                            events.onConnectingSourceChanged(null)
                        } else {
                            events.onSelectedClockChanged(if (isClockSelected) null else clock.id)
                            events.onSelectedTokenChanged(null)
                            onSelectCard(null)
                        }
                    },
                    onMove = { clockId, finalX, finalY ->
                        val (snappedX, snappedY) = if (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE) {
                            val cell = activeSurface.cellSizeDp
                            Pair((kotlin.math.round(finalX / cell)) * cell, (kotlin.math.round(finalY / cell)) * cell)
                        } else Pair(finalX, finalY)
                        moveAsGroup(clock.groupId, clock.x, clock.y, snappedX, snappedY) { onMoveClock(clockId, snappedX, snappedY) }
                    },
                    onAdvanceProgress = {
                        val nextProgress = (clock.filledSegments + 1).coerceAtMost(clock.totalSegments)
                        onUpdateClockProgress(clock.id, nextProgress)
                    },
                    onReduceProgress = {
                        val prevProgress = (clock.filledSegments - 1).coerceAtLeast(0)
                        onUpdateClockProgress(clock.id, prevProgress)
                    },
                    onResetProgress = {
                        onUpdateClockProgress(clock.id, 0)
                    },
                    onStartConnection = {
                        events.onConnectingSourceChanged("clock_${clock.id}")
                        events.onSelectedClockChanged(null)
                    },
                    onRemove = {
                        onRemoveClock(clock.id)
                        events.onSelectedClockChanged(null)
                    },
                    onDismissMenu = {
                        events.onSelectedClockChanged(null)
                    }
                )
            }
        }
    }
}
}
