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
internal fun PlaymatCanvasOverlays(
    state: PlaymatCanvasState,
    events: PlaymatCanvasEvents
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
    val graphicsActions = actions
    Box(Modifier.fillMaxSize()) {
    // 2. Menú Radial Contextual sobre la carta seleccionada
    if (selectedCardPlacement != null) {
        val cardScreenX = (selectedCardPlacement.x * cameraZoom) + cameraX
        val cardScreenY = (selectedCardPlacement.y * cameraZoom) + cameraY
    
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cardScreenX - 60).roundToInt(),
                        (cardScreenY - 80).roundToInt()
                    )
                }
        ) {
            PlaymatRadialMenu(
                visible = true,
                onRotate = { onRotateCard(selectedCardPlacement.cardId, 90) },
                onFlip = { onFlipCard(selectedCardPlacement.cardId) },
                onReturnToHand = { onReturnToHand(selectedCardPlacement.cardId) },
                onDiscard = { onDiscardCard(selectedCardPlacement.cardId) },
                onInspect = { onCardDetailClick(selectedCardPlacement.cardId) },
                onSendToTacticalMap = { events.onShowSendToTacticalDialogChanged(true) },
                onStartConnection = {
                    events.onConnectingSourceChanged("card_${selectedCardPlacement.cardId}")
                    onSelectCard(null)
                },
                onDismiss = { onSelectCard(null) }
            )
        }
    }
    
    // 3. Barra Superior Flotante del Tapete (Ocultable en Modo Zen)
    AnimatedVisibility(
        visible = !uiState.isZenMode,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier.align(Alignment.TopStart)
    ) {
        PlaymatToolbar(
            activeSurface = activeSurface,
            allSurfaces = allSurfaces,
            onSelectSurface = onSelectSurface,
            onCreateSurface = onCreateSurface,
            onDeleteSurface = onDeleteSurface,
            onSetTheme = onSetTheme,
            onSetTemplate = onSetTemplate,
            onResetCamera = events.onResetCamera,
            onAddStickyNote = {
                events.onStickyNoteTextChanged("")
                events.onShowStickyNoteDialogChanged(true)
            },
            onOpenQuickDraw = { events.onShowQuickDrawDialogChanged(true) },
            onOpenTokenPicker = { events.onShowTokenPickerDialogChanged(true) },
            onOpenClockCreator = { events.onShowClockCreationDialogChanged(true) },
            onOpenTacticalSettings = { events.onShowTacticalSettingsDialogChanged(true) },
            onToggleRuler = {
                        events.onIsRulerActiveChanged(!isRulerActive)
                        events.onRulerPositionsChanged(null, null)
                    },
            isRulerActive = isRulerActive,
            onToggleDiceRoller = { events.onShowDiceOverlayChanged(!showDiceOverlay) },
            onExportSnapshot = events.onExportSnapshot,
                    onToggleZenMode = actions.onToggleZenMode,
            onToggleSessionDrawer = actions.onToggleSessionDrawer,
            isSessionDrawerOpen = uiState.isSessionDrawerOpen
        )
    }
    
    // Botón Minimalista para Salir del Modo Zen (Solo visible en Zen Mode)
    if (uiState.isZenMode) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = CircleShape,
            color = Color(0xFF14151B).copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.6f)),
            shadowElevation = 6.dp
        ) {
            IconButton(
                onClick = actions.onToggleZenMode,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Salir de Modo Zen",
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // Lanzador de Dados Flotante en el Tapete
    PlaymatDiceOverlay(
        visible = showDiceOverlay,
        onDismiss = { events.onShowDiceOverlayChanged(false) },
        onPinResultToTable = { name, resultText ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            onAddTableChip(0L, name, resultText, centerX, centerY)
        },
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 64.dp, end = 12.dp)
    )
    
    // 4. Bandeja Inferior Acoplable de la Mano (Ocultable en Modo Zen)
    PlaymatHandTray(
        visible = !uiState.isZenMode && uiState.showPlaymatHandTray,
        hand = uiState.hand,
        placedCardIds = placedCardIds,
        deckNames = uiState.deckNames,
        deckAspectRatios = uiState.deckAspectRatios,
        deckBackImages = uiState.deckBackImages,
        sessionId = uiState.sessionId,
        onToggleVisible = onToggleHandTray,
        onPlaceOnPlaymat = { cardId ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            onPlaceCardOnPlaymat(cardId, centerX, centerY)
        },
        onCardClick = onCardDetailClick,
        onDiscardCard = onDiscardCard,
        onRotateCard = { cardId -> onRotateCard(cardId, 90) },
        modifier = Modifier.align(Alignment.BottomCenter)
    )
    
    // 5. Banner indicador de modo Conexión de Hilos
    AnimatedVisibility(
        visible = connectingSourceEndpointId != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 76.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Toca la segunda carta o personaje para tender el hilo",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(
                    onClick = { events.onConnectingSourceChanged(null) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
    
    // 6. Cinta métrica táctica y herramienta de Regla Libre
    PlaymatMeasurementRuler(
        startPos = tokenDragStartPos,
        currentPos = tokenDragCurrentPos,
        cellSizeDp = activeSurface.cellSizeDp,
        zoom = cameraZoom,
        cameraX = cameraX,
        cameraY = cameraY
    )
    
    PlaymatMeasurementRuler(
        startPos = rulerStartPos,
        currentPos = rulerCurrentPos,
        cellSizeDp = activeSurface.cellSizeDp,
        zoom = cameraZoom,
        cameraX = cameraX,
        cameraY = cameraY
    )
    
    // Banner indicador de modo Regla
    AnimatedVisibility(
        visible = isRulerActive,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 76.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Modo Regla activo: arrastra sobre la mesa para medir distancias",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                TextButton(
                    onClick = {
                        events.onIsRulerActiveChanged(false)
                        events.onRulerPositionsChanged(null, null)
                    }
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
    
    // 7. Panel Lateral / Bottom Sheet de Bitácora y Notas Rápidas
    PlaymatSessionDrawer(
        isOpen = uiState.isSessionDrawerOpen,
        selectedTab = uiState.sessionDrawerTab,
        logs = uiState.sessionLog.logs,
        notesMarkdown = uiState.sessionLog.noteDraftMarkdown,
        isFilterCurrentScene = uiState.sessionLog.isFilterCurrentScene,
        currentSceneTitle = uiState.scenes.find { it.id == uiState.focusedSceneId }?.title,
        onClose = actions.onToggleSessionDrawer,
        onTabSelected = actions.onSetSessionDrawerTab,
        onToggleFilterCurrentScene = actions.onToggleFilterCurrentScene,
        onAddManualLog = actions.onAddManualLog,
        onSaveNotes = actions.onSaveNotes,
        onClearLogs = actions.onClearLogs,
        modifier = Modifier.align(Alignment.CenterEnd)
    )
    }
}
