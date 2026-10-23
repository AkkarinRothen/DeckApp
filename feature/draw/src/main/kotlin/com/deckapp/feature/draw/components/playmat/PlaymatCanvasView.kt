package com.deckapp.feature.draw.components.playmat

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.core.ui.util.toCoilModel
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeSurface = uiState.activePlaymatSurface
    val allSurfaces = uiState.boardMemory.playmatSurfaces.ifEmpty { listOf(activeSurface) }

    var cameraX by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraX) }
    var cameraY by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraY) }
    var cameraZoom by remember(activeSurface.id) { mutableFloatStateOf(activeSurface.cameraZoom.coerceIn(0.4f, 2.5f)) }

    var showQuickDrawDialog by remember { mutableStateOf(false) }
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

    val cardsById = remember(uiState.hand, uiState.pile) {
        (uiState.hand + uiState.pile).associateBy { it.id }
    }

    val cardPlacementsById = remember(activeSurface.cards) {
        activeSurface.cards.associateBy { it.cardId }
    }

    val endpointPositions = remember(activeSurface.cards, activeSurface.tokens, activeSurface.clocks) {
        val map = mutableMapOf<String, Offset>()
        activeSurface.cards.forEach { card ->
            map["card_${card.cardId}"] = Offset(card.x + 60f, card.y + 18f)
        }
        activeSurface.tokens.forEach { token ->
            map["token_${token.id}"] = Offset(token.x + 32f, token.y + 32f)
        }
        activeSurface.clocks.forEach { clock ->
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

    val selectedCardPlacement = remember(activeSurface.cards, uiState.selectedPlaymatCardId) {
        activeSurface.cards.find { it.cardId == uiState.selectedPlaymatCardId }
    }
    val selectedCard = remember(selectedCardPlacement, cardsById) {
        selectedCardPlacement?.let { cardsById[it.cardId] }
    }

    val graphicsLayer = rememberGraphicsLayer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRulerActive, activeSurface.id) {
                if (isRulerActive) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val canvasX = (offset.x - cameraX) / cameraZoom
                            val canvasY = (offset.y - cameraY) / cameraZoom
                            rulerStartPos = Offset(canvasX, canvasY)
                            rulerCurrentPos = Offset(canvasX, canvasY)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val canvasX = (change.position.x - cameraX) / cameraZoom
                            val canvasY = (change.position.y - cameraY) / cameraZoom
                            rulerCurrentPos = Offset(canvasX, canvasY)
                        },
                        onDragEnd = { },
                        onDragCancel = {
                            rulerStartPos = null
                            rulerCurrentPos = null
                        }
                    )
                }
            }
            .pointerInput(isRulerActive, activeSurface.id) {
                if (!isRulerActive) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        cameraZoom = (cameraZoom * zoom).coerceIn(0.4f, 2.5f)
                        cameraX += pan.x
                        cameraY += pan.y
                        onUpdateCamera(cameraX, cameraY, cameraZoom)
                    }
                }
            }
            .pointerInput(activeSurface.id) {
                detectTapGestures(
                    onTap = {
                        if (rulerStartPos != null) {
                            rulerStartPos = null
                            rulerCurrentPos = null
                        } else if (connectingSourceEndpointId != null) {
                            connectingSourceEndpointId = null
                        } else {
                            onSelectCard(null)
                            selectedTokenId = null
                            selectedClockId = null
                        }
                    }
                )
            }
    ) {
    val canvasState = PlaymatCanvasState(
        uiState = uiState,
        actions = actions,
        activeSurface = activeSurface,
        allSurfaces = allSurfaces,
        cameraX = cameraX,
        cameraY = cameraY,
        cameraZoom = cameraZoom,
        showQuickDrawDialog = showQuickDrawDialog,
        showStickyNoteDialog = showStickyNoteDialog,
        showSendToTacticalDialog = showSendToTacticalDialog,
        showDiceOverlay = showDiceOverlay,
        showTokenPickerDialog = showTokenPickerDialog,
        showClockCreationDialog = showClockCreationDialog,
        showTacticalSettingsDialog = showTacticalSettingsDialog,
        isRulerActive = isRulerActive,
        rulerStartPos = rulerStartPos,
        rulerCurrentPos = rulerCurrentPos,
        tokenDragStartPos = tokenDragStartPos,
        tokenDragCurrentPos = tokenDragCurrentPos,
        editingHpToken = editingHpToken,
        selectedTokenId = selectedTokenId,
        selectedClockId = selectedClockId,
        stickyNoteText = stickyNoteText,
        hoveredSlotTag = hoveredSlotTag,
        connectingSourceEndpointId = connectingSourceEndpointId,
        editingConnection = editingConnection,
        cardsById = cardsById,
        endpointPositions = endpointPositions,
        piledCardsByDeck = piledCardsByDeck,
        placedCardIds = placedCardIds,
        currentTemplateSlots = currentTemplateSlots,
        selectedCardPlacement = selectedCardPlacement,
        selectedCard = selectedCard
    )
    val canvasEvents = PlaymatCanvasEvents(
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
            onUpdateCamera(0f, 0f, 1f)
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
        }
    )
        PlaymatSceneLayer(
            state = canvasState,
            events = canvasEvents,
            graphicsLayer = graphicsLayer
        )
        PlaymatCanvasOverlays(
            state = canvasState,
            events = canvasEvents
        )
        PlaymatDialogsHost(
            state = canvasState,
            events = canvasEvents
        )
    }
}

/**
 * Renderizado de carta física colocada en el tapete 2D con arrastre fluido y desacoplado a 120 FPS.
 */
@Composable
internal fun PlaymatCardItem(
    placement: PlaymatCardPlacement,
    card: Card,
    backImage: String?,
    isSelected: Boolean,
    isConnectingSource: Boolean = false,
    zoom: Float,
    templateSlots: List<TemplateSlotDef>,
    onCardClick: () -> Unit,
    onMove: (cardId: Long, finalX: Float, finalY: Float, slotTag: String?) -> Unit,
    onHoverSlotChanged: (String?) -> Unit
) {
    val width = 120.dp * zoom
    val height = 180.dp * zoom

    var localX by remember(placement.cardId, placement.x) { mutableFloatStateOf(placement.x) }
    var localY by remember(placement.cardId, placement.y) { mutableFloatStateOf(placement.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(placement.x, placement.y) {
        if (!isDragging) {
            localX = placement.x
            localY = placement.y
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = placement.rotationDegrees.toFloat(),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardRotation"
    )

    val animatedFlipRotation by animateFloatAsState(
        targetValue = if (placement.isFaceUp) 0f else 180f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardFlipRotation"
    )

    val density = LocalDensity.current

    val currentNearestSlot = remember(localX, localY, isDragging, templateSlots) {
        if (!isDragging || templateSlots.isEmpty()) null
        else {
            val snapThreshold = 60f
            templateSlots.find { slot ->
                hypot(slot.x - localX, slot.y - localY) <= snapThreshold
            }
        }
    }

    LaunchedEffect(currentNearestSlot) {
        onHoverSlotChanged(currentNearestSlot?.tag)
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 22.dp else if (isSelected) 14.dp else 6.dp,
        label = "cardElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f,
        label = "cardScale"
    )

    // Lectura de pista / señal activa de la carta
    val cueText = remember(card, placement.rotationDegrees) {
        card.activeZone?.text?.ifBlank { null }
            ?: card.faces.firstOrNull()?.zones?.firstOrNull()?.text?.ifBlank { null }
            ?: card.title
    }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localX * zoom).roundToInt(),
                    (localY * zoom).roundToInt()
                )
            }
            .size(width, height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = animatedRotation
                rotationY = animatedFlipRotation
                cameraDistance = 14f * density.density
            }
            .pointerInput(placement.cardId, zoom, templateSlots) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragDetected = false
                    val drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        localX += over.x / zoom
                        localY += over.y / zoom
                        dragDetected = true
                    }

                    if (drag != null || dragDetected) {
                        isDragging = true
                        val currentPointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == currentPointerId }
                            if (dragChange == null || !dragChange.pressed) break
                            val delta = dragChange.position - dragChange.previousPosition
                            localX += delta.x / zoom
                            localY += delta.y / zoom
                            dragChange.consume()
                        }

                        isDragging = false
                        onHoverSlotChanged(null)

                        // Aplicar snap magnético si está cerca de un slot
                        val snapThreshold = 60f
                        val nearest = templateSlots.find { slot ->
                            hypot(slot.x - localX, slot.y - localY) <= snapThreshold
                        }

                        val finalX = nearest?.x ?: localX
                        val finalY = nearest?.y ?: localY
                        localX = finalX
                        localY = finalY
                        onMove(placement.cardId, finalX, finalY, nearest?.tag)
                    } else {
                        // Fue un toque limpio (tap sin superar touch slop)
                        onCardClick()
                    }
                }
            }
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (currentNearestSlot != null) 3.dp else if (isConnectingSource) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                color = if (currentNearestSlot != null) Color(0xFF00E5FF)
                    else if (isConnectingSource) Color(0xFFE53935)
                    else if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        val isShowingBack = animatedFlipRotation > 90f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (isShowingBack) {
                        rotationY = 180f // Compensar reflejo para que la imagen o texto del reverso no se inviertan
                    }
                }
        ) {
            if (!isShowingBack) {
                // Frente de la carta
                val frontImage = card.faces.firstOrNull()?.imagePath ?: card.faces.firstOrNull()?.reversedImagePath
                if (frontImage != null) {
                    AsyncImage(
                        model = frontImage.toCoilModel(),
                        contentDescription = card.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = card.faces.firstOrNull()?.name ?: "Frente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Banner inferior translúcido para lectura rápida de la pista activa
                if (!cueText.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = cueText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (9 * zoom.coerceIn(0.7f, 1.3f)).sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            } else {
                // Reverso de la carta
                if (backImage != null) {
                    AsyncImage(
                        model = backImage.toCoilModel(),
                        contentDescription = "Reverso",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = "Reverso",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renderiza el fondo del tapete con texturas de casino/taberna y rejilla de puntos.
 */
@Composable
internal fun PlaymatBackground(theme: PlaymatTheme) {
    val (primaryColor, secondaryColor) = when (theme) {
        PlaymatTheme.FELT_GREEN -> Color(0xFF1B4D3E) to Color(0xFF0F2E25)
        PlaymatTheme.FELT_RED -> Color(0xFF6B1D2F) to Color(0xFF3F101B)
        PlaymatTheme.RUSTIC_WOOD -> Color(0xFF3E2723) to Color(0xFF261715)
        PlaymatTheme.PARCHMENT_LEATHER -> Color(0xFFD7CCC8) to Color(0xFFA1887F)
        PlaymatTheme.DARK_MINIMAL -> Color(0xFF1A1A22) to Color(0xFF101016)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    radius = 1600f
                )
            )
    ) {
        // Microrejilla de puntos sutiles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            val dotRadius = 1.2.dp.toPx()
            val dotColor = Color.White.copy(alpha = 0.08f)

            var x = 0f
            while (x < size.width) {
                var y = 0f
                while (y < size.height) {
                    drawCircle(color = dotColor, radius = dotRadius, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
    }
}

/**
 * Renderiza siluetas punteadas magnéticas según la plantilla activa.
 */
@Composable
internal fun PlaymatTemplateSilhouettes(
    template: PlaymatTemplateType,
    zoom: Float,
    highlightedSlotTag: String? = null
) {
    val slots = remember(template) { getTemplateSlots(template) }
    if (slots.isEmpty()) return

    val slotW = 120.dp * zoom
    val slotH = 180.dp * zoom

    slots.forEach { slot ->
        val isHighlighted = slot.tag == highlightedSlotTag
        TemplateSlot(
            x = slot.x * zoom,
            y = slot.y * zoom,
            width = slotW,
            height = slotH,
            label = slot.label,
            isHighlighted = isHighlighted
        )
    }
}

@Composable
private fun TemplateSlot(
    x: Float,
    y: Float,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    label: String,
    isHighlighted: Boolean = false
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .size(width, height)
            .border(
                width = if (isHighlighted) 2.5.dp else 1.5.dp,
                color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                if (isHighlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.04f),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(4.dp)
        )
    }
}

@Composable
internal fun PlaymatStickyNoteItem(
    note: PlaymatStickyNote,
    zoom: Float,
    onDelete: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    var localX by remember(note.id, note.x) { mutableFloatStateOf(note.x) }
    var localY by remember(note.id, note.y) { mutableFloatStateOf(note.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(note.x, note.y) {
        if (!isDragging) {
            localX = note.x
            localY = note.y
        }
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localX * zoom).roundToInt(),
                    (localY * zoom).roundToInt()
                )
            }
            .size(100.dp * zoom, 100.dp * zoom)
            .pointerInput(note.id, zoom) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onMove(localX, localY)
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX += dragAmount.x / zoom
                        localY += dragAmount.y / zoom
                    }
                )
            }
            .shadow(if (isDragging) 10.dp else 4.dp, RoundedCornerShape(4.dp)),
        color = Color(0xFFFFF59D),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = (11 * zoom).sp,
                    color = Color(0xFF212121)
                ),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Borrar nota",
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
internal fun PlaymatTableChipItem(
    chip: PlaymatTableChip,
    zoom: Float
) {
    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (chip.x * zoom).roundToInt(),
                    (chip.y * zoom).roundToInt()
                )
            }
            .shadow(3.dp, RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "${chip.tableName}: ${chip.rollResultText}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Sobrecarga retrocompatible de [PlaymatCanvasView] para preservar compatibilidad con código existente.
 */
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
