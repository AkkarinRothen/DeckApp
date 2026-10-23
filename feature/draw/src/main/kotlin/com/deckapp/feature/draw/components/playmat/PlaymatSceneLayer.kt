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
    state: PlaymatCanvasState,
    events: PlaymatCanvasEvents,
    graphicsLayer: GraphicsLayer
) {
    val uiState = state.uiState
    val actions = state.actions
    val activeSurface = state.activeSurface
    val cameraX = state.cameraX
    val cameraY = state.cameraY
    val cameraZoom = state.cameraZoom
    val onPlaceCardOnPlaymat = actions.onPlaceCardOnPlaymat
    val onMoveCard = actions.onMoveCard
    val onRotateCard = actions.onRotateCard
    val onFlipCard = actions.onFlipCard
    val onReturnToHand = actions.onReturnToHand
    val onDiscardCard = actions.onDiscardCard
    val onSelectCard = actions.onSelectCard
    val onToggleHandTray = actions.onToggleHandTray
    val onSelectSurface = actions.onSelectSurface
    val onCreateSurface = actions.onCreateSurface
    val onDeleteSurface = actions.onDeleteSurface
    val onSetTheme = actions.onSetTheme
    val onSetTemplate = actions.onSetTemplate
    val onUpdateCamera = actions.onUpdateCamera
    val onQuickDraw = actions.onQuickDraw
    val onAddStickyNote = actions.onAddStickyNote
    val onMoveStickyNote = actions.onMoveStickyNote
    val onDeleteStickyNote = actions.onDeleteStickyNote
    val onCardDetailClick = actions.onCardDetailClick
    val onSendCardToTacticalMap = actions.onSendCardToTacticalMap
    val onMoveDeck = actions.onMoveDeck
    val onMoveDiscardPile = actions.onMoveDiscardPile
    val onDrawFromDeck = actions.onDrawFromDeck
    val onReshuffleDeck = actions.onReshuffleDeck
    val onAddTableChip = actions.onAddTableChip
    val onCreateConnection = actions.onCreateConnection
    val onUpdateConnection = actions.onUpdateConnection
    val onRemoveConnection = actions.onRemoveConnection
    val onAddToken = actions.onAddToken
    val onMoveToken = actions.onMoveToken
    val onUpdateTokenHp = actions.onUpdateTokenHp
    val onUpdateTokenType = actions.onUpdateTokenType
    val onRemoveToken = actions.onRemoveToken
    val onAddClock = actions.onAddClock
    val onUpdateClockProgress = actions.onUpdateClockProgress
    val onMoveClock = actions.onMoveClock
    val onRemoveClock = actions.onRemoveClock
    val onSetBackgroundMap = actions.onSetBackgroundMap
    val onSetBackgroundTransform = actions.onSetBackgroundTransform
    val onToggleBackgroundLock = actions.onToggleBackgroundLock
    val onSetGridConfig = actions.onSetGridConfig
    val allSurfaces = state.allSurfaces
    val showQuickDrawDialog = state.showQuickDrawDialog
    val showStickyNoteDialog = state.showStickyNoteDialog
    val showSendToTacticalDialog = state.showSendToTacticalDialog
    val showDiceOverlay = state.showDiceOverlay
    val showTokenPickerDialog = state.showTokenPickerDialog
    val showClockCreationDialog = state.showClockCreationDialog
    val showTacticalSettingsDialog = state.showTacticalSettingsDialog
    val isRulerActive = state.isRulerActive
    val rulerStartPos = state.rulerStartPos
    val rulerCurrentPos = state.rulerCurrentPos
    val tokenDragStartPos = state.tokenDragStartPos
    val tokenDragCurrentPos = state.tokenDragCurrentPos
    val editingHpToken = state.editingHpToken
    val selectedTokenId = state.selectedTokenId
    val selectedClockId = state.selectedClockId
    val stickyNoteText = state.stickyNoteText
    val hoveredSlotTag = state.hoveredSlotTag
    val connectingSourceEndpointId = state.connectingSourceEndpointId
    val editingConnection = state.editingConnection
    val cardsById = state.cardsById
    val endpointPositions = state.endpointPositions
    val piledCardsByDeck = state.piledCardsByDeck
    val placedCardIds = state.placedCardIds
    val currentTemplateSlots = state.currentTemplateSlots
    val selectedCardPlacement = state.selectedCardPlacement
    val selectedCard = state.selectedCard

// 1. Contenido Dibujable de la Mesa (Capturable por Snapshot)
Box(
    modifier = Modifier
        .fillMaxSize()
        .drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(graphicsLayer)
        }
) {
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
        activeSurface.tableChips.forEach { chip ->
            PlaymatTableChipItem(
                chip = chip,
                zoom = cameraZoom
            )
        }

        // Notas Adhesivas
        activeSurface.stickyNotes.forEach { note ->
            PlaymatStickyNoteItem(
                note = note,
                zoom = cameraZoom,
                onDelete = { onDeleteStickyNote(note.id) },
                onMove = { newX, newY -> onMoveStickyNote(note.id, newX, newY) }
            )
        }

        // Mazos Físicos 3D y Pilas de Descarte en la Mesa
        activeSurface.decks.forEach { deckPlacement ->
            val deckName = uiState.deckNames[deckPlacement.stackId] ?: "Mazo"
            val availableCount = uiState.deckCardCounts[deckPlacement.stackId] ?: 0
            val piledCards = piledCardsByDeck[deckPlacement.stackId] ?: emptyList()
            val totalCount = availableCount + piledCards.size
            val backImage = uiState.deckBackImages[deckPlacement.stackId]

            key("deck_${deckPlacement.stackId}") {
                PlaymatDeckItem(
                    placement = deckPlacement,
                    deckName = deckName,
                    availableCount = availableCount,
                    totalCount = totalCount,
                    backImage = backImage,
                    zoom = cameraZoom,
                    templateSlots = currentTemplateSlots,
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
                    onMoveDeck = { newX, newY ->
                        onMoveDeck(deckPlacement.stackId, newX, newY)
                    },
                    onHoverSlotChanged = { tag ->
                        events.onHoveredSlotTagChanged(tag)
                    },
                    onShuffleDeck = {
                        onReshuffleDeck(deckPlacement.stackId)
                    }
                )

                PlaymatDiscardPileItem(
                    stackId = deckPlacement.stackId,
                    x = deckPlacement.discardX ?: (deckPlacement.x + 150f),
                    y = deckPlacement.discardY ?: deckPlacement.y,
                    zoom = cameraZoom,
                    piledCards = piledCards,
                    onReshuffleToDeck = {
                        onReshuffleDeck(deckPlacement.stackId)
                    },
                    onMoveDiscardPile = { newX, newY ->
                        onMoveDiscardPile(deckPlacement.stackId, newX, newY)
                    },
                    onInspectCard = onCardDetailClick
                )
            }
        }

        // Hilos Narrativos y Conexiones de Investigación
        activeSurface.connections.forEach { conn ->
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
        activeSurface.cards.sortedBy { it.zIndex }.forEach { placement ->
            val card = cardsById[placement.cardId]
            if (card != null) {
                val isSelected = uiState.selectedPlaymatCardId == placement.cardId
                val isConnectingSource = connectingSourceEndpointId == "card_${placement.cardId}"
                val stackId = card.originDeckId ?: card.stackId
                val backImage = uiState.deckBackImages[stackId]

                key(placement.cardId) {
                    PlaymatCardItem(
                        placement = placement,
                        card = card,
                        backImage = backImage,
                        isSelected = isSelected,
                        isConnectingSource = isConnectingSource,
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
                                onSelectCard(if (isSelected) null else placement.cardId)
                            }
                        },
                        onMove = { cardId, finalX, finalY, slotTag ->
                            val finalXWithSnap = if (activeSurface.snapToGrid && slotTag == null && activeSurface.gridType != PlaymatGridType.NONE) {
                                (kotlin.math.round(finalX / activeSurface.cellSizeDp)) * activeSurface.cellSizeDp
                            } else finalX
                            val finalYWithSnap = if (activeSurface.snapToGrid && slotTag == null && activeSurface.gridType != PlaymatGridType.NONE) {
                                (kotlin.math.round(finalY / activeSurface.cellSizeDp)) * activeSurface.cellSizeDp
                            } else finalY
                            onMoveCard(cardId, finalXWithSnap, finalYWithSnap, slotTag)
                        },
                        onHoverSlotChanged = { tag ->
                            events.onHoveredSlotTagChanged(tag)
                        }
                    )
                }
            }
        }

        // Tokens de Personajes (PJ / PNJ)
        activeSurface.tokens.forEach { token ->
            val isTokenSelected = selectedTokenId == token.id
            val isTokenConnectingSource = connectingSourceEndpointId == "token_${token.id}"
            key("token_${token.id}") {
                PlaymatTokenItem(
                    token = token,
                    isSelected = isTokenSelected,
                    isConnectingSource = isTokenConnectingSource,
                    zoom = cameraZoom,
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
                        onMoveToken(tokenId, snappedX, snappedY)
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
        activeSurface.clocks.forEach { clock ->
            val isClockSelected = selectedClockId == clock.id
            val isClockConnectingSource = connectingSourceEndpointId == "clock_${clock.id}"
            key("clock_${clock.id}") {
                PlaymatClockItem(
                    clock = clock,
                    isSelected = isClockSelected,
                    isConnectingSource = isClockConnectingSource,
                    zoom = cameraZoom,
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
                        onMoveClock(clockId, snappedX, snappedY)
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
