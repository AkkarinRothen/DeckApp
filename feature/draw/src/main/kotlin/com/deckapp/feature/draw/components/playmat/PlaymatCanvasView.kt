package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.toSize
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Representación geométrica de una ranura dentro de una plantilla temática.
 */
data class TemplateSlotDef(
    val x: Float,
    val y: Float,
    val label: String,
    val tag: String
)

/**
 * Obtiene las posiciones de ranuras de cartas para cada molde temático.
 */
fun getTemplateSlots(template: PlaymatTemplateType): List<TemplateSlotDef> {
    return when (template) {
        PlaymatTemplateType.DECK_OF_WORLDS_MICROSETTING -> listOf(
            TemplateSlotDef(300f, 300f, "Región (Núcleo)", "REGION"),
            TemplateSlotDef(300f, 100f, "Hito / Sitio", "LANDMARK"),
            TemplateSlotDef(160f, 300f, "Nombres", "NAMESAKE"),
            TemplateSlotDef(160f, 100f, "Orígenes", "ORIGINS"),
            TemplateSlotDef(440f, 300f, "Atributos", "ATTRIBUTES"),
            TemplateSlotDef(440f, 500f, "Crisis", "ADVENT")
        )
        PlaymatTemplateType.STORY_ENGINE_PREMISE -> listOf(
            TemplateSlotDef(100f, 200f, "Potenciador", "BOOSTER"),
            TemplateSlotDef(240f, 200f, "Trasfondo", "BACKSTORY"),
            TemplateSlotDef(380f, 200f, "Misterio", "MYSTERY"),
            TemplateSlotDef(520f, 200f, "Objeto Único", "UNIQUE_ITEM")
        )
        PlaymatTemplateType.TAROT_THREE_CARDS -> listOf(
            TemplateSlotDef(160f, 240f, "Pasado", "PAST"),
            TemplateSlotDef(300f, 240f, "Presente", "PRESENT"),
            TemplateSlotDef(440f, 240f, "Futuro", "FUTURE")
        )
        PlaymatTemplateType.ORACLE_CROSS -> listOf(
            TemplateSlotDef(300f, 240f, "Centro", "CENTER"),
            TemplateSlotDef(300f, 50f, "Arriba", "TOP"),
            TemplateSlotDef(300f, 430f, "Abajo", "BOTTOM"),
            TemplateSlotDef(160f, 240f, "Izquierda", "LEFT"),
            TemplateSlotDef(440f, 240f, "Derecha", "RIGHT")
        )
        PlaymatTemplateType.FREEFORM -> emptyList()
    }
}

/**
 * Vista interactiva principal del Tapete de Cartas espacial 2D (Playmat).
 */
@Composable
fun PlaymatCanvasView(
    uiState: SessionUiState,
    actions: com.deckapp.feature.draw.model.state.PlaymatActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeSurface = uiState.activePlaymatSurface
    val allSurfaces = uiState.boardMemory.playmatSurfaces.ifEmpty { listOf(activeSurface) }

    var cameraX by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraX) }
    var cameraY by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraY) }
    var cameraZoom by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraZoom.coerceIn(0.4f, 2.5f)) }

    LaunchedEffect(activeSurface.id, cameraX, cameraY, cameraZoom) {
        if (cameraX != activeSurface.cameraX || cameraY != activeSurface.cameraY || cameraZoom != activeSurface.cameraZoom) {
            delay(300)
            actions.onUpdateCamera(cameraX, cameraY, cameraZoom)
        }
    }

    var showQuickDrawDialog by remember { mutableStateOf(false) }
    var isGroupSelectionMode by remember(activeSurface.id) { mutableStateOf(false) }
    var selectedGroupMemberIds by remember(activeSurface.id) { mutableStateOf(emptySet<String>()) }
    var showStickyNoteDialog by remember { mutableStateOf(false) }
    var showSendToTacticalDialog by remember { mutableStateOf(false) }
    var showDiceOverlay by remember { mutableStateOf(false) }
    var showTokenPickerDialog by remember { mutableStateOf(false) }
    var showClockCreationDialog by remember { mutableStateOf(false) }
    var showTacticalSettingsDialog by remember { mutableStateOf(false) }
    var isRulerActive by remember { mutableStateOf(false) }
    var rulerStartPos by remember { mutableStateOf<Offset?>(null) }
    var rulerCurrentPos by remember { mutableStateOf<Offset?>(null) }
    var tokenDragStartPos by remember { mutableStateOf<Offset?>(null) }
    var tokenDragCurrentPos by remember { mutableStateOf<Offset?>(null) }
    var editingHpToken by remember { mutableStateOf<PlaymatTokenPlacement?>(null) }
    var selectedTokenId by remember { mutableStateOf<String?>(null) }
    var selectedClockId by remember { mutableStateOf<String?>(null) }
    var stickyNoteText by remember { mutableStateOf("") }
    var hoveredSlotTag by remember { mutableStateOf<String?>(null) }
    var connectingSourceEndpointId by remember { mutableStateOf<String?>(null) }
    var editingConnection by remember { mutableStateOf<PlaymatConnection?>(null) }
    var canvasBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    var handTrayBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    var handCardDrag by remember { mutableStateOf<PlaymatHandCardDrag?>(null) }

    val cardsById = remember(uiState.hand, uiState.pile) {
        (uiState.hand + uiState.pile).associateBy { it.id }
    }

    val renderSurface = remember(activeSurface) {
        val hidden = activeSurface.groups.filter { it.isHidden }.map { it.id }.toSet()
        activeSurface.copy(
            cards = activeSurface.cards.filterNot { it.clusterId in hidden },
            stickyNotes = activeSurface.stickyNotes.filterNot { it.groupId in hidden },
            tableChips = activeSurface.tableChips.filterNot { it.groupId in hidden },
            tokens = activeSurface.tokens.filterNot { it.groupId in hidden },
            clocks = activeSurface.clocks.filterNot { it.groupId in hidden },
            decks = activeSurface.decks.filterNot { it.groupId in hidden || it.discardGroupId in hidden }
                .filter { it.isVisibleOnPlaymat }
        )
    }
    val playmatSpatialIndex = remember(renderSurface) { PlaymatSpatialIndex(renderSurface) }
    val visibleMemberIds = remember(renderSurface) {
        buildSet {
            renderSurface.cards.forEach { add("card:${it.cardId}") }
            renderSurface.tokens.forEach { add("token:${it.id}") }
            renderSurface.clocks.forEach { add("clock:${it.id}") }
            renderSurface.stickyNotes.forEach { add("note:${it.id}") }
            renderSurface.tableChips.forEach { add("chip:${it.id}") }
            renderSurface.decks.forEach {
                add("deck:${it.stackId}")
                if (it.discardX != null || it.discardY != null) add("discard:${it.stackId}")
            }
        }
    }
    val visibleSelectedGroupMemberIds = selectedGroupMemberIds intersect visibleMemberIds

    val endpointPositions = remember(renderSurface.cards, renderSurface.tokens, renderSurface.clocks) {
        val map = mutableMapOf<String, Offset>()
        renderSurface.cards.forEach { card ->
            map["card_${card.cardId}"] = Offset(card.x + 60f, card.y + 18f)
        }
        renderSurface.tokens.forEach { token ->
            map["token_${token.id}"] = Offset(token.x + 32f, token.y + 32f)
        }
        renderSurface.clocks.forEach { clock ->
            map["clock_${clock.id}"] = Offset(clock.x + 38f, clock.y + 38f)
        }
        map
    }

    val piledCardsByDeck = remember(uiState.pile) {
        uiState.pile.groupBy { it.stackId }
    }

    val placedCardIds = remember(activeSurface.cards) {
        activeSurface.cards.map { it.cardId }.toSet()
    }

    val currentTemplateSlots = remember(activeSurface.template) {
        getTemplateSlots(activeSurface.template)
    }

    val selectedCardPlacement = remember(renderSurface.cards, uiState.selectedPlaymatCardId) {
        renderSurface.cards.find { it.cardId == uiState.selectedPlaymatCardId }
    }
    val selectedCard = remember(selectedCardPlacement, cardsById) {
        selectedCardPlacement?.let { cardsById[it.cardId] }
    }

    val graphicsLayer = rememberGraphicsLayer()
    val latestSurfaceCamera = rememberUpdatedState(Triple(activeSurface.cameraX, activeSurface.cameraY, activeSurface.cameraZoom))
    val latestCameraUpdater = rememberUpdatedState(actions.onUpdateCamera)
    val centerAtWorld: (Float, Float) -> Unit = { x, y ->
        cameraX = canvasBoundsInWindow.width / 2f - x * cameraZoom
        cameraY = canvasBoundsInWindow.height / 2f - y * cameraZoom
    }
    val centerGroup: (String) -> Unit = { groupId ->
        findPlaymatGroupCenter(activeSurface, groupId)?.let { centerAtWorld(it.first, it.second) }
    }

    val canvasEvents = remember(activeSurface, canvasBoundsInWindow, actions, context, graphicsLayer, coroutineScope) {
        PlaymatCanvasEvents(
            onHoveredSlotTagChanged = { hoveredSlotTag = it },
            onConnectingSourceChanged = { connectingSourceEndpointId = it },
            onSelectedTokenChanged = { selectedTokenId = it },
            onSelectedClockChanged = { selectedClockId = it },
            onTokenDragPositionsChanged = { start, current ->
                tokenDragStartPos = start
                tokenDragCurrentPos = current
            },
            onEditingHpTokenChanged = { editingHpToken = it },
            onEditingConnectionChanged = { editingConnection = it },
            onShowQuickDrawDialogChanged = { showQuickDrawDialog = it },
            onShowStickyNoteDialogChanged = { showStickyNoteDialog = it },
            onShowSendToTacticalDialogChanged = { showSendToTacticalDialog = it },
            onShowDiceOverlayChanged = { showDiceOverlay = it },
            onShowTokenPickerDialogChanged = { showTokenPickerDialog = it },
            onShowClockCreationDialogChanged = { showClockCreationDialog = it },
            onShowTacticalSettingsDialogChanged = { showTacticalSettingsDialog = it },
            onIsRulerActiveChanged = { isRulerActive = it },
            onRulerPositionsChanged = { start, current ->
                rulerStartPos = start
                rulerCurrentPos = current
            },
            onStickyNoteTextChanged = { stickyNoteText = it },
            onResetCamera = {
                cameraX = 0f
                cameraY = 0f
                cameraZoom = 1f
            },
            onExportSnapshot = {
                coroutineScope.launch {
                    try {
                        val imageBitmap = graphicsLayer.toImageBitmap()
                        val bitmap = imageBitmap.asAndroidBitmap()
                        PlaymatSnapshotHelper.shareBitmap(context, bitmap)
                    } catch (e: Exception) {
                        android.util.Log.e("PlaymatCanvasView", "Fallo al capturar snapshot de tapete", e)
                    }
                }
            },
            onHandCardDragChanged = { handCardDrag = it },
            onHandTrayBoundsChanged = { handTrayBoundsInWindow = it },
            onGroupSelectionModeChanged = { enabled ->
                isGroupSelectionMode = enabled
                if (!enabled) selectedGroupMemberIds = emptySet()
            },
            onGroupSelectionFinished = {
                isGroupSelectionMode = false
                selectedGroupMemberIds = emptySet()
            }
        )
    }

    // Reuse each slice while its layer-specific inputs stay equal so unrelated UI changes can skip it.
    val sceneState = remember(
        renderSurface, cameraX, cameraY, cameraZoom,
        uiState.deckNames, uiState.deckCardCounts, uiState.deckBackImages,
        uiState.selectedPlaymatCardId, selectedTokenId, selectedClockId,
        hoveredSlotTag, connectingSourceEndpointId, cardsById, endpointPositions,
        piledCardsByDeck, currentTemplateSlots, actions
    ) {
        PlaymatSceneState(
            activeSurface = renderSurface,
            cameraX = cameraX,
            cameraY = cameraY,
            cameraZoom = cameraZoom,
            deckNames = uiState.deckNames,
            deckCardCounts = uiState.deckCardCounts,
            deckBackImages = uiState.deckBackImages,
            selectedPlaymatCardId = uiState.selectedPlaymatCardId,
            selectedTokenId = selectedTokenId,
            selectedClockId = selectedClockId,
            hoveredSlotTag = hoveredSlotTag,
            connectingSourceEndpointId = connectingSourceEndpointId,
            cardsById = cardsById,
            endpointPositions = endpointPositions,
            piledCardsByDeck = piledCardsByDeck,
            currentTemplateSlots = currentTemplateSlots,
            actions = actions
        )
    }

    val currentSceneTitle = remember(uiState.scenes, uiState.focusedSceneId) {
        uiState.scenes.find { it.id == uiState.focusedSceneId }?.title
    }
    val overlayState = remember(
        activeSurface, allSurfaces, cameraX, cameraY, cameraZoom,
        uiState.isZenMode, showDiceOverlay, uiState.isSessionDrawerOpen,
        uiState.sessionDrawerTab, uiState.hand, uiState.showPlaymatHandTray,
        uiState.deckRefs, uiState.deckNames, uiState.deckCardCounts, uiState.deckAspectRatios, uiState.deckBackImages,
        uiState.sessionId, uiState.sessionLog.logs, uiState.sessionLog.noteDraftMarkdown,
        uiState.sessionLog.isFilterCurrentScene, currentSceneTitle,
        isRulerActive, rulerStartPos, rulerCurrentPos, tokenDragStartPos,
        tokenDragCurrentPos, selectedTokenId, selectedClockId,
        connectingSourceEndpointId, cardsById, placedCardIds, currentTemplateSlots,
        selectedCardPlacement, selectedCard, canvasBoundsInWindow,
        handTrayBoundsInWindow, handCardDrag, isGroupSelectionMode, visibleSelectedGroupMemberIds, actions,
        centerGroup, centerAtWorld
    ) {
        PlaymatOverlayState(
            activeSurface = activeSurface,
            allSurfaces = allSurfaces,
            cameraX = cameraX,
            cameraY = cameraY,
            cameraZoom = cameraZoom,
            canvasBoundsInWindow = canvasBoundsInWindow,
            handTrayBoundsInWindow = handTrayBoundsInWindow,
            handCardDrag = handCardDrag,
            isZenMode = uiState.isZenMode,
            showDiceOverlay = showDiceOverlay,
            isSessionDrawerOpen = uiState.isSessionDrawerOpen,
            sessionDrawerTab = uiState.sessionDrawerTab,
            hand = uiState.hand,
            showPlaymatHandTray = uiState.showPlaymatHandTray,
            deckRefs = uiState.deckRefs,
            deckNames = uiState.deckNames,
            deckCardCounts = uiState.deckCardCounts,
            deckAspectRatios = uiState.deckAspectRatios,
            deckBackImages = uiState.deckBackImages,
            sessionId = uiState.sessionId,
            sessionLogs = uiState.sessionLog.logs,
            notesMarkdown = uiState.sessionLog.noteDraftMarkdown,
            isFilterCurrentScene = uiState.sessionLog.isFilterCurrentScene,
            currentSceneTitle = currentSceneTitle,
            isRulerActive = isRulerActive,
            rulerStartPos = rulerStartPos,
            rulerCurrentPos = rulerCurrentPos,
            tokenDragStartPos = tokenDragStartPos,
            tokenDragCurrentPos = tokenDragCurrentPos,
            selectedTokenId = selectedTokenId,
            selectedClockId = selectedClockId,
            connectingSourceEndpointId = connectingSourceEndpointId,
            cardsById = cardsById,
            placedCardIds = placedCardIds,
            currentTemplateSlots = currentTemplateSlots,
            selectedCardPlacement = selectedCardPlacement,
            selectedCard = selectedCard,
            isGroupSelectionMode = isGroupSelectionMode,
            selectedGroupMemberIds = visibleSelectedGroupMemberIds,
            selectedGroupMemberCount = visibleSelectedGroupMemberIds.size,
            onCenterGroup = centerGroup,
            onCenterAtWorld = centerAtWorld,
            actions = actions
        )
    }

    val dialogState = remember(
        activeSurface, cameraX, cameraY, cameraZoom,
        uiState.deckRefs, uiState.deckNames, uiState.deckCardCounts, uiState.npcs,
        showQuickDrawDialog, showStickyNoteDialog, showSendToTacticalDialog,
        showTokenPickerDialog, showClockCreationDialog, showTacticalSettingsDialog,
        editingHpToken, stickyNoteText, editingConnection, selectedCard, actions
    ) {
        PlaymatDialogState(
            activeSurface = activeSurface,
            cameraX = cameraX,
            cameraY = cameraY,
            cameraZoom = cameraZoom,
            deckRefs = uiState.deckRefs,
            deckNames = uiState.deckNames,
            deckCardCounts = uiState.deckCardCounts,
            npcs = uiState.npcs,
            showQuickDrawDialog = showQuickDrawDialog,
            showStickyNoteDialog = showStickyNoteDialog,
            showSendToTacticalDialog = showSendToTacticalDialog,
            showTokenPickerDialog = showTokenPickerDialog,
            showClockCreationDialog = showClockCreationDialog,
            showTacticalSettingsDialog = showTacticalSettingsDialog,
            editingHpToken = editingHpToken,
            stickyNoteText = stickyNoteText,
            editingConnection = editingConnection,
            selectedCard = selectedCard,
            actions = actions
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                canvasBoundsInWindow = Rect(coordinates.positionInWindow(), coordinates.size.toSize())
            }
            .playmatCanvasGestures(
                isRulerActive = isRulerActive,
                surfaceId = activeSurface.id,
                cameraX = cameraX,
                cameraY = cameraY,
                cameraZoom = cameraZoom,
                rulerStartPos = rulerStartPos,
                connectingSourceEndpointId = connectingSourceEndpointId,
                onRulerPositionsChanged = canvasEvents.onRulerPositionsChanged,
                onTransform = { pan, zoom ->
                    cameraZoom = (cameraZoom * zoom).coerceIn(0.4f, 2.5f)
                    cameraX += pan.x
                    cameraY += pan.y
                },
                onTransformEnd = {
                    val persisted = latestSurfaceCamera.value
                    if (cameraX != persisted.first || cameraY != persisted.second || cameraZoom != persisted.third) {
                        latestCameraUpdater.value(cameraX, cameraY, cameraZoom)
                    }
                },
                onCancelConnection = { connectingSourceEndpointId = null },
                onCanvasTap = { tapPosition ->
                    val canvasX = (tapPosition.x - cameraX) / cameraZoom
                    val canvasY = (tapPosition.y - cameraY) / cameraZoom
                    val tappedCard = playmatSpatialIndex.hitTest(canvasX, canvasY)?.kind == PlaymatSpatialIndex.Kind.CARD
                    if (!tappedCard) {
                        actions.onSelectCard(null)
                        selectedTokenId = null
                        selectedClockId = null
                    }
                }
            )
    ) {
        PlaymatSceneLayer(state = sceneState, events = canvasEvents, graphicsLayer = graphicsLayer)
        PlaymatSelectionLayer(
            surface = renderSurface,
            cameraX = cameraX,
            cameraY = cameraY,
            cameraZoom = cameraZoom,
            enabled = isGroupSelectionMode,
            selectedIds = visibleSelectedGroupMemberIds,
            onSelectionChanged = { selectedGroupMemberIds = it }
        )
        PlaymatCanvasOverlays(state = overlayState, events = canvasEvents)
        PlaymatDialogsHost(state = dialogState, events = canvasEvents)
    }
}

private fun findPlaymatGroupCenter(surface: PlaymatSurface, groupId: String): Pair<Float, Float>? {
    val memberIds = surface.groups.firstOrNull { it.id == groupId }?.memberIds ?: return null
    val points = buildList {
        surface.cards.filter { "card:${it.cardId}" in memberIds }.forEach { add(it.x + 60f to it.y + 90f) }
        surface.tokens.filter { "token:${it.id}" in memberIds }.forEach { add(it.x + 32f to it.y + 41f) }
        surface.clocks.filter { "clock:${it.id}" in memberIds }.forEach { add(it.x + 38f to it.y + 50f) }
        surface.stickyNotes.filter { "note:${it.id}" in memberIds }.forEach { add(it.x + 50f to it.y + 50f) }
        surface.tableChips.filter { "chip:${it.id}" in memberIds }.forEach { add(it.x + 90f to it.y + 24f) }
        surface.decks.forEach { deck ->
            if ("deck:${deck.stackId}" in memberIds) add(deck.x + 60f to deck.y + 90f)
            if ("discard:${deck.stackId}" in memberIds) add((deck.discardX ?: deck.x + 150f) + 60f to (deck.discardY ?: deck.y) + 90f)
        }
    }
    if (points.isEmpty()) return null
    return points.map { it.first }.average().toFloat() to points.map { it.second }.average().toFloat()
}

/** Compatibility adapter for callers that still pass the canvas callbacks individually. */
@Deprecated(
    message = "Usar la sobrecarga principal que recibe PlaymatActions",
    replaceWith = ReplaceWith("PlaymatCanvasView(uiState = uiState, actions = actions, modifier = modifier)")
)
@Composable
fun PlaymatCanvasView(
    uiState: SessionUiState,
    onPlaceCardOnPlaymat: (Long, Float, Float) -> Unit,
    onMoveCard: (Long, Float, Float, String?) -> Unit,
    onRotateCard: (Long, Int) -> Unit,
    onFlipCard: (Long) -> Unit,
    onReturnToHand: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onSelectCard: (Long?) -> Unit,
    onToggleHandTray: () -> Unit,
    onSelectSurface: (String) -> Unit,
    onCreateSurface: (String, PlaymatTheme, PlaymatTemplateType) -> Unit,
    onDeleteSurface: (String) -> Unit,
    onSetTheme: (PlaymatTheme) -> Unit,
    onSetTemplate: (PlaymatTemplateType) -> Unit,
    onUpdateCamera: (Float, Float, Float) -> Unit,
    onQuickDraw: (Long, Float, Float) -> Unit,
    onAddStickyNote: (String, Float, Float) -> Unit,
    onMoveStickyNote: (String, Float, Float) -> Unit = { _, _, _ -> },
    onDeleteStickyNote: (String) -> Unit,
    onCardDetailClick: (Long) -> Unit,
    onSendCardToTacticalMap: (Card, Boolean) -> Unit = { _, _ -> },
    onMoveDeck: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onMoveDiscardPile: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onDrawFromDeck: (Long, Float, Float, String?) -> Unit = { _, _, _, _ -> },
    onReshuffleDeck: (Long) -> Unit = {},
    onAddTableChip: (Long, String, String, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onCreateConnection: (String, String) -> Unit = { _, _ -> },
    onUpdateConnection: (String, String, String) -> Unit = { _, _, _ -> },
    onRemoveConnection: (String) -> Unit = {},
    onAddToken: (name: String, imagePath: String?, npcId: Long?, x: Float, y: Float, maxHp: Int, currentHp: Int, tokenType: PlaymatTokenType, ringColorHex: String) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onMoveToken: (String, Float, Float) -> Unit = { _, _, _ -> },
    onUpdateTokenHp: (String, Int) -> Unit = { _, _ -> },
    onUpdateTokenType: (String, PlaymatTokenType) -> Unit = { _, _ -> },
    onRemoveToken: (String) -> Unit = {},
    onAddClock: (title: String, segments: Int, colorHex: String, x: Float, y: Float) -> Unit = { _, _, _, _, _ -> },
    onUpdateClockProgress: (clockId: String, filledSegments: Int) -> Unit = { _, _ -> },
    onMoveClock: (clockId: String, x: Float, y: Float) -> Unit = { _, _, _ -> },
    onRemoveClock: (clockId: String) -> Unit = {},
    onSetBackgroundMap: (uri: String?, preset: PlaymatAtmosphericPreset) -> Unit = { _, _ -> },
    onSetBackgroundTransform: (opacity: Float, scale: Float) -> Unit = { _, _ -> },
    onToggleBackgroundLock: () -> Unit = {},
    onSetGridConfig: (gridType: PlaymatGridType, gridStyle: PlaymatGridStyle, cellSizeDp: Float, snapToGrid: Boolean) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) = PlaymatCanvasView(
    uiState = uiState,
    actions = com.deckapp.feature.draw.model.state.PlaymatActions(
        onPlaceCardOnPlaymat = onPlaceCardOnPlaymat,
        onMoveCard = onMoveCard,
        onRotateCard = onRotateCard,
        onFlipCard = onFlipCard,
        onReturnToHand = onReturnToHand,
        onDiscardCard = onDiscardCard,
        onSelectCard = onSelectCard,
        onToggleHandTray = onToggleHandTray,
        onSelectSurface = onSelectSurface,
        onCreateSurface = onCreateSurface,
        onDeleteSurface = onDeleteSurface,
        onSetTheme = onSetTheme,
        onSetTemplate = onSetTemplate,
        onUpdateCamera = onUpdateCamera,
        onQuickDraw = onQuickDraw,
        onAddStickyNote = onAddStickyNote,
        onMoveStickyNote = onMoveStickyNote,
        onDeleteStickyNote = onDeleteStickyNote,
        onCardDetailClick = onCardDetailClick,
        onSendCardToTacticalMap = onSendCardToTacticalMap,
        onMoveDeck = onMoveDeck,
        onMoveDiscardPile = onMoveDiscardPile,
        onDrawFromDeck = onDrawFromDeck,
        onReshuffleDeck = onReshuffleDeck,
        onAddTableChip = onAddTableChip,
        onCreateConnection = onCreateConnection,
        onUpdateConnection = onUpdateConnection,
        onRemoveConnection = onRemoveConnection,
        onAddToken = onAddToken,
        onMoveToken = onMoveToken,
        onUpdateTokenHp = onUpdateTokenHp,
        onUpdateTokenType = onUpdateTokenType,
        onRemoveToken = onRemoveToken,
        onAddClock = onAddClock,
        onUpdateClockProgress = onUpdateClockProgress,
        onMoveClock = onMoveClock,
        onRemoveClock = onRemoveClock,
        onSetBackgroundMap = onSetBackgroundMap,
        onSetBackgroundTransform = onSetBackgroundTransform,
        onToggleBackgroundLock = onToggleBackgroundLock,
        onSetGridConfig = onSetGridConfig
    ),
    modifier = modifier
)
