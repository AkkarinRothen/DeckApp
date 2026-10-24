package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.deckapp.core.model.*
import com.deckapp.feature.draw.model.state.PlaymatActions
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
internal fun PlaymatCanvasOverlays(
    state: PlaymatOverlayState,
    events: PlaymatCanvasEvents
) {
    val actions = state.actions
    val activeSurface = state.activeSurface
    val cameraX = state.cameraX
    val cameraY = state.cameraY
    val cameraZoom = state.cameraZoom
    val density = androidx.compose.ui.platform.LocalDensity.current
    val onPlaceCardOnPlaymat = actions.onPlaceCardOnPlaymat
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
    val onCardDetailClick = actions.onCardDetailClick
    val onAddTableChip = actions.onAddTableChip
    val allSurfaces = state.allSurfaces
    val handCardDrag = state.handCardDrag
    val showDiceOverlay = state.showDiceOverlay
    val isRulerActive = state.isRulerActive
    val rulerStartPos = state.rulerStartPos
    val rulerCurrentPos = state.rulerCurrentPos
    val tokenDragStartPos = state.tokenDragStartPos
    val tokenDragCurrentPos = state.tokenDragCurrentPos
    val connectingSourceEndpointId = state.connectingSourceEndpointId
    val placedCardIds = state.placedCardIds
    val selectedCardPlacement = state.selectedCardPlacement
    var radialMenuSize by remember { mutableStateOf(IntSize.Zero) }
    var showGroupsDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showMinimap by remember { mutableStateOf(false) }
    var showSearchDialog by remember(activeSurface.id) { mutableStateOf(false) }
    var showDeckManager by remember(activeSurface.id) { mutableStateOf(false) }
    var favoritesDockExpanded by remember(activeSurface.id) { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    if (showSearchDialog) PlaymatSearchDialog(
        surface = activeSurface,
        cardsById = state.cardsById,
        deckNames = state.deckNames,
        onDismiss = { showSearchDialog = false },
        onFocus = { result ->
            result.hiddenGroupIds.forEach(actions.onTogglePlaymatGroupVisibility)
            if (result.focusGroupId != null) state.onCenterGroup(result.focusGroupId)
            else result.position?.let { state.onCenterAtWorld(it.x, it.y) }
            showSearchDialog = false
        }
    )
    if (showGroupsDialog) PlaymatGroupsDialog(
        surface = activeSurface,
        actions = actions,
        deckNames = state.deckNames,
        onJumpToGroup = state.onCenterGroup,
        onDismiss = { showGroupsDialog = false }
    )
    if (showCreateGroupDialog) AlertDialog(
        onDismissRequest = { showCreateGroupDialog = false },
        title = { Text("Crear grupo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${state.selectedGroupMemberCount} piezas seleccionadas")
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nombre del grupo") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newGroupName.isNotBlank() && state.selectedGroupMemberIds.isNotEmpty()) {
                    actions.onCreatePlaymatGroup(newGroupName, state.selectedGroupMemberIds)
                    events.onGroupSelectionFinished()
                    showCreateGroupDialog = false
                }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = { showCreateGroupDialog = false }) { Text("Cancelar") } }
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
    if (showDeckManager && !state.isZenMode) {
        PlaymatDeckManagerDialog(
            deckRefs = state.deckRefs,
            deckNames = state.deckNames,
            deckCardCounts = state.deckCardCounts,
            placements = activeSurface.decks,
            favoriteDeckIds = activeSurface.favoriteDeckIds,
            groups = activeSurface.groups,
            resources = remember(activeSurface, state.cardsById, state.deckNames) {
                buildPlaymatSearchResults(activeSurface, state.cardsById, state.deckNames)
            },
            windowClass = when {
                maxWidth >= 1000.dp -> DeckManagerWindowClass.EXPANDED
                maxWidth >= 720.dp -> DeckManagerWindowClass.MEDIUM
                else -> DeckManagerWindowClass.COMPACT
            },
            onSetVisible = actions.onSetDeckVisibleOnPlaymat,
            onSetFavorite = actions.onSetDeckFavorite,
            onReorderDecks = actions.onReorderSessionDecks,
            onFocusResource = { result ->
                result.hiddenGroupIds.forEach(actions.onTogglePlaymatGroupVisibility)
                if (result.focusGroupId != null) state.onCenterGroup(result.focusGroupId)
                else result.position?.let { state.onCenterAtWorld(it.x, it.y) }
                showDeckManager = false
            },
            onQuickAction = { result ->
                when (result.kind) {
                    PlaymatSearchKind.CARD -> result.id.removePrefix("card:").toLongOrNull()?.let { cardId ->
                        showDeckManager = false
                        actions.onCardDetailClick(cardId)
                    }
                    PlaymatSearchKind.DECK -> result.id.removePrefix("deck:").toLongOrNull()?.let { stackId ->
                        activeSurface.decks.firstOrNull { it.stackId == stackId }?.let { deck ->
                            actions.onQuickDraw(stackId, deck.x + 150f, deck.y)
                        }
                    }
                    PlaymatSearchKind.DISCARD -> result.id.removePrefix("discard:").toLongOrNull()?.let(actions.onReshuffleDeck)
                    else -> Unit
                }
            },
            onCreateGroupForDeck = { name, stackId ->
                actions.onCreatePlaymatGroup(name, setOf("deck:$stackId"))
            },
            onAssignDeckToGroup = { stackId, targetGroupId ->
                val memberId = "deck:$stackId"
                activeSurface.groups.forEach { group ->
                    val existingMembers = group.memberIds - memberId
                    val updatedMembers = if (group.id == targetGroupId) existingMembers + memberId else existingMembers
                    actions.onSetPlaymatGroupMembers(group.id, updatedMembers)
                }
            },
            onManageGroups = {
                showDeckManager = false
                showGroupsDialog = true
            },
            onDismiss = { showDeckManager = false }
        )
    }
    if (showMinimap && !state.isZenMode) {
        PlaymatMinimap(
            surface = activeSurface,
            cameraX = cameraX,
            cameraY = cameraY,
            cameraZoom = cameraZoom,
            viewportWidth = state.canvasBoundsInWindow.width,
            viewportHeight = state.canvasBoundsInWindow.height,
            onCenterAt = state.onCenterAtWorld,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp).width(190.dp)
        )
    }
    // 2. Menú Radial Contextual sobre la carta seleccionada
    if (selectedCardPlacement != null) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val horizontalMargin = with(density) { 8.dp.roundToPx() }
        val gap = with(density) { 8.dp.roundToPx() }
        val cardWidthPx = with(density) { 120.dp.toPx() } * cameraZoom
        val cardHeightPx = with(density) { 180.dp.toPx() } * cameraZoom
        val cardScreenX = (selectedCardPlacement.x * cameraZoom) + cameraX
        val cardScreenY = (selectedCardPlacement.y * cameraZoom) + cameraY
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
    
        Box(
            modifier = Modifier
                .onSizeChanged { radialMenuSize = it }
                .offset {
                    val menuWidth = radialMenuSize.width
                    val menuHeight = radialMenuSize.height
                    val maxX = (viewportWidth - menuWidth - horizontalMargin).coerceAtLeast(horizontalMargin)
                    val maxY = (viewportHeight - menuHeight - horizontalMargin).coerceAtLeast(horizontalMargin)
                    val centeredX = cardScreenX + cardWidthPx / 2f - menuWidth / 2f
                    val aboveCardY = cardScreenY - menuHeight - gap
                    val belowCardY = cardScreenY + cardHeightPx + gap
                    val preferredY = if (aboveCardY >= horizontalMargin) aboveCardY else belowCardY
                    IntOffset(
                        centeredX.roundToInt().coerceIn(horizontalMargin, maxX),
                        preferredY.roundToInt().coerceIn(horizontalMargin, maxY)
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
        visible = !state.isZenMode,
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
            onManageDecks = { showDeckManager = true },
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
            isSessionDrawerOpen = state.isSessionDrawerOpen,
            onManageGroups = { showGroupsDialog = true },
            onToggleMinimap = { showMinimap = !showMinimap },
            isMinimapOpen = showMinimap,
            onSearch = { showSearchDialog = true },
            isGroupSelectionMode = state.isGroupSelectionMode,
            selectedGroupMemberCount = state.selectedGroupMemberCount,
            onToggleGroupSelection = {
                if (state.isGroupSelectionMode) events.onGroupSelectionFinished()
                else events.onGroupSelectionModeChanged(true)
            },
            onCreateSelectedGroup = {
                newGroupName = ""
                showCreateGroupDialog = true
            }
        )
    }

    if (!state.isZenMode && !showDeckManager && activeSurface.favoriteDeckIds.isNotEmpty()) {
        val handTrayOpen = state.showPlaymatHandTray && state.hand.isNotEmpty()
        val dockBottomPadding = if (handTrayOpen) (maxHeight * 0.29f).coerceIn(112.dp, 260.dp) else 14.dp
        PlaymatFavoriteDeckDock(
            deckRefs = state.deckRefs,
            favoriteDeckIds = activeSurface.favoriteDeckIds,
            deckNames = state.deckNames,
            deckCardCounts = state.deckCardCounts,
            placements = activeSurface.decks,
            expanded = favoritesDockExpanded,
            onToggleExpanded = { favoritesDockExpanded = !favoritesDockExpanded },
            onShowDeck = { stackId, x, y ->
                val placement = activeSurface.decks.firstOrNull { it.stackId == stackId }
                activeSurface.groups.filter { it.isHidden && "deck:$stackId" in it.memberIds }
                    .forEach { actions.onTogglePlaymatGroupVisibility(it.id) }
                if (placement == null) {
                    actions.onSetDeckVisibleOnPlaymat(stackId, true)
                    actions.onMoveDeck(stackId, x, y)
                } else if (!placement.isVisibleOnPlaymat) {
                    actions.onSetDeckVisibleOnPlaymat(stackId, true)
                }
                state.onCenterAtWorld((placement?.x ?: x) + 75f, (placement?.y ?: y) + 90f)
            },
            onDrawCard = { stackId, x, y ->
                val placement = activeSurface.decks.firstOrNull { it.stackId == stackId }
                activeSurface.groups.filter { it.isHidden && "deck:$stackId" in it.memberIds }
                    .forEach { actions.onTogglePlaymatGroupVisibility(it.id) }
                if (placement == null) {
                    actions.onSetDeckVisibleOnPlaymat(stackId, true)
                    actions.onMoveDeck(stackId, x, y)
                } else if (!placement.isVisibleOnPlaymat) {
                    actions.onSetDeckVisibleOnPlaymat(stackId, true)
                }
                actions.onQuickDraw(stackId, (placement?.x ?: x) + 150f, placement?.y ?: y)
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = dockBottomPadding)
        )
    }
    
    // Botón Minimalista para Salir del Modo Zen (Solo visible en Zen Mode)
    if (state.isZenMode) {
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
        visible = !state.isZenMode && state.showPlaymatHandTray && !showDeckManager,
        hand = state.hand,
        placedCardIds = placedCardIds,
        deckNames = state.deckNames,
        deckAspectRatios = state.deckAspectRatios,
        deckBackImages = state.deckBackImages,
        sessionId = state.sessionId,
        onToggleVisible = onToggleHandTray,
        onOpenDeckManager = { showDeckManager = true },
        onPlaceOnPlaymat = { cardId ->
            val centerX = (-cameraX + 300) / cameraZoom
            val centerY = (-cameraY + 300) / cameraZoom
            onPlaceCardOnPlaymat(cardId, centerX, centerY)
        },
        onCardClick = onCardDetailClick,
        onDiscardCard = onDiscardCard,
        onRotateCard = { cardId -> onRotateCard(cardId, 90) },
        draggedCardId = handCardDrag?.cardId,
        onHandCardDragChanged = events.onHandCardDragChanged,
        onTrayBoundsChanged = events.onHandTrayBoundsChanged,
        onDropHandCard = { drag ->
            val pointer = drag.pointerInWindow
            val canvasBounds = state.canvasBoundsInWindow
            val releasedOnTray = state.handTrayBoundsInWindow.contains(pointer)
            if (canvasBounds.contains(pointer) && !releasedOnTray) {
                val cardWidth = with(density) { 120.dp.toPx() }
                val cardHeight = with(density) { 180.dp.toPx() }
                val canvasPoint = Offset(
                    pointer.x - canvasBounds.left,
                    pointer.y - canvasBounds.top
                )
                var cardX = (canvasPoint.x - cameraX) / cameraZoom - drag.grabFraction.x * cardWidth
                var cardY = (canvasPoint.y - cameraY) / cameraZoom - drag.grabFraction.y * cardHeight
                val nearestSlot = state.currentTemplateSlots
                    .map { slot ->
                        val dx = slot.x - cardX
                        val dy = slot.y - cardY
                        slot to kotlin.math.hypot(dx, dy)
                    }
                    .filter { (_, distance) -> distance <= 60f }
                    .minByOrNull { (_, distance) -> distance }
                    ?.first

                if (nearestSlot != null) {
                    cardX = nearestSlot.x
                    cardY = nearestSlot.y
                } else if (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE) {
                    cardX = kotlin.math.round(cardX / activeSurface.cellSizeDp) * activeSurface.cellSizeDp
                    cardY = kotlin.math.round(cardY / activeSurface.cellSizeDp) * activeSurface.cellSizeDp
                }

                onPlaceCardOnPlaymat(drag.cardId, cardX, cardY)
                if (nearestSlot != null || (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE)) {
                    actions.onMoveCard(drag.cardId, cardX, cardY, nearestSlot?.tag)
                }
            }
            events.onHandCardDragChanged(null)
        },
        modifier = Modifier.align(Alignment.BottomCenter)
    )

    // Preview follows the pointer while the original card remains dimmed in the hand.
    handCardDrag?.let { drag ->
        val card = state.hand.firstOrNull { it.id == drag.cardId }
        if (card != null) {
            val stackId = card.originDeckId ?: card.stackId
            val aspectRatio = state.deckAspectRatios[stackId] ?: CardAspectRatio.STANDARD
            val backImage = state.deckBackImages[stackId]
            val previewWidth = with(density) { 100.dp.toPx() }
            val previewHeight = previewWidth / aspectRatio.ratio
            val localX = drag.pointerInWindow.x - state.canvasBoundsInWindow.left
            val localY = drag.pointerInWindow.y - state.canvasBoundsInWindow.top

            // Ghost Preview proyectado en el tapete mientras la carta sobrevuela desde la mano
            val worldX = (localX - cameraX) / cameraZoom
            val worldY = (localY - cameraY) / cameraZoom
            val templateSlots = remember(activeSurface.template) { getTemplateSlots(activeSurface.template) }
            val nearestSlot = templateSlots.find { slot -> hypot(slot.x - worldX, slot.y - worldY) <= 60f }
            val ghostWorldX = nearestSlot?.x ?: if (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE && activeSurface.cellSizeDp > 0f) {
                round(worldX / activeSurface.cellSizeDp) * activeSurface.cellSizeDp
            } else null
            val ghostWorldY = nearestSlot?.y ?: if (activeSurface.snapToGrid && activeSurface.gridType != PlaymatGridType.NONE && activeSurface.cellSizeDp > 0f) {
                round(worldY / activeSurface.cellSizeDp) * activeSurface.cellSizeDp
            } else null

            if (ghostWorldX != null && ghostWorldY != null) {
                PlaymatGhostSilhouette(
                    x = ghostWorldX,
                    y = ghostWorldY,
                    width = 120.dp,
                    height = 180.dp,
                    zoom = cameraZoom,
                    shape = RoundedCornerShape(8.dp),
                    accentColor = if (nearestSlot != null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                    label = nearestSlot?.label
                )
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (localX - previewWidth * drag.grabFraction.x).roundToInt(),
                            (localY - previewHeight * drag.grabFraction.y).roundToInt()
                        )
                    }
                    .zIndex(20f)
                    .graphicsLayer {
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = 0.95f
                    }
                    .shadow(22.dp, RoundedCornerShape(8.dp))
            ) {
                com.deckapp.feature.draw.components.CardItem(
                    card = card,
                    aspectRatio = aspectRatio,
                    showTitle = true,
                    backImagePath = backImage,
                    onClick = {},
                    onDiscard = {},
                    onReveal = {},
                    onRollTable = {}
                )
            }
        }
    }
    
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
        isOpen = state.isSessionDrawerOpen,
        selectedTab = state.sessionDrawerTab,
        logs = state.sessionLogs,
        notesMarkdown = state.notesMarkdown,
        isFilterCurrentScene = state.isFilterCurrentScene,
        currentSceneTitle = state.currentSceneTitle,
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
