package com.deckapp.feature.draw.components.playmat

import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.core.ui.util.toCoilModel
import kotlin.math.hypot
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Representación geométrica de una ranura dentro de una plantilla temática.
 */

/**
 * Silueta guía translúcida ("Ghost Preview") que muestra en tiempo real dónde encajará la pieza.
 */
@Composable
internal fun PlaymatGhostSilhouette(
    x: Float,
    y: Float,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    zoom: Float,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    accentColor: Color = Color(0xFF00E5FF),
    label: String? = null
) {
    Box(
        modifier = Modifier
            .offset { IntOffset((x * zoom).roundToInt(), (y * zoom).roundToInt()) }
            .size(width, height)
            .border(
                width = 2.dp,
                color = accentColor.copy(alpha = 0.85f),
                shape = shape
            )
            .background(
                accentColor.copy(alpha = 0.15f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (9 * zoom.coerceIn(0.7f, 1.3f)).sp,
                    fontWeight = FontWeight.Bold
                ),
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
internal fun PlaymatCardItem(
    placement: PlaymatCardPlacement,
    card: Card,
    backImage: String?,
    isSelected: Boolean,
    isConnectingSource: Boolean = false,
    isMovementEnabled: Boolean = true,
    zoom: Float,
    templateSlots: List<TemplateSlotDef>,
    snapToGrid: Boolean = false,
    gridType: PlaymatGridType = PlaymatGridType.NONE,
    cellSizeDp: Float = 48f,
    decks: List<PlaymatDeckPlacement> = emptyList(),
    onCardClick: () -> Unit,
    onMove: (cardId: Long, finalX: Float, finalY: Float, slotTag: String?) -> Unit,
    onHoverSlotChanged: (String?) -> Unit,
    onReturnToDeck: (Long) -> Unit = {},
    onDiscardCard: (Long) -> Unit = {},
    onDropTargetHoverChanged: (deckId: Long?, isDiscard: Boolean) -> Unit = { _, _ -> }
) {
    val view = LocalView.current
    val width = 120.dp * zoom
    val height = 180.dp * zoom

    var localX by remember(placement.cardId, placement.x) { mutableFloatStateOf(placement.x) }
    var localY by remember(placement.cardId, placement.y) { mutableFloatStateOf(placement.y) }
    var isDragging by remember { mutableStateOf(false) }

    // Física de inclinación dinámica ("tilt") proporcional a la velocidad del arrastre
    var tiltZ by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var tiltX by remember { mutableFloatStateOf(0f) }

    val animatedTiltZ by animateFloatAsState(
        targetValue = if (isDragging) tiltZ else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardTiltZ"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = if (isDragging) tiltY else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardTiltY"
    )
    val animatedTiltX by animateFloatAsState(
        targetValue = if (isDragging) tiltX else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardTiltX"
    )

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

    // Detección de ranura más cercana de la plantilla
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

    // Detección de zonas de soltado inteligentes (Mazo original o Pila de descarte)
    val originStackId = remember(card) { card.originDeckId ?: card.stackId }
    val originDeck = remember(decks, originStackId) { decks.firstOrNull { it.stackId == originStackId } }

    val cardCenterX = localX + 60f
    val cardCenterY = localY + 90f

    val isOverDeck = remember(isDragging, originDeck, cardCenterX, cardCenterY) {
        if (!isDragging || originDeck == null) false
        else hypot((originDeck.x + 60f) - cardCenterX, (originDeck.y + 90f) - cardCenterY) <= 85f
    }

    val discardPileX = originDeck?.let { it.discardX ?: (it.x + 150f) } ?: 0f
    val discardPileY = originDeck?.let { it.discardY ?: it.y } ?: 0f

    val isOverDiscard = remember(isDragging, originDeck, cardCenterX, cardCenterY, discardPileX, discardPileY) {
        if (!isDragging || originDeck == null) false
        else hypot((discardPileX + 60f) - cardCenterX, (discardPileY + 90f) - cardCenterY) <= 85f
    }

    LaunchedEffect(isOverDeck, isOverDiscard, originStackId) {
        if (isDragging) {
            onDropTargetHoverChanged(
                if (isOverDeck || isOverDiscard) originStackId else null,
                isOverDiscard
            )
        }
    }

    // Coordenadas de silueta guía (Ghost Preview) si hay acople
    val ghostPreview = remember(isDragging, isOverDeck, isOverDiscard, currentNearestSlot, snapToGrid, gridType, cellSizeDp, localX, localY) {
        if (!isDragging || isOverDeck || isOverDiscard) null
        else if (currentNearestSlot != null) {
            Triple(currentNearestSlot.x, currentNearestSlot.y, currentNearestSlot.label)
        } else if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
            val snappedX = round(localX / cellSizeDp) * cellSizeDp
            val snappedY = round(localY / cellSizeDp) * cellSizeDp
            Triple(snappedX, snappedY, null)
        } else null
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 24.dp else if (isSelected) 14.dp else 6.dp,
        label = "cardElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isOverDeck || isOverDiscard) 0.96f else if (isDragging) 1.05f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    // Lectura de pista / señal activa de la carta
    val cueText = remember(card, placement.rotationDegrees) {
        card.activeZone?.text?.ifBlank { null }
            ?: card.faces.firstOrNull()?.zones?.firstOrNull()?.text?.ifBlank { null }
            ?: card.title
    }

    // Renderizar silueta guía ("Ghost Preview") debajo de la carta en movimiento
    if (ghostPreview != null) {
        PlaymatGhostSilhouette(
            x = ghostPreview.first,
            y = ghostPreview.second,
            width = width,
            height = height,
            zoom = zoom,
            shape = RoundedCornerShape(8.dp),
            accentColor = if (currentNearestSlot != null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
            label = ghostPreview.third
        )
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
                rotationZ = animatedRotation + animatedTiltZ
                rotationY = animatedFlipRotation + animatedTiltY
                rotationX = animatedTiltX
                cameraDistance = 14f * density.density
            }
            .pointerInput(placement.cardId, zoom, templateSlots, isMovementEnabled, snapToGrid, gridType, cellSizeDp, decks) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var dragDetected = false
                    val drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        if (isMovementEnabled) {
                            localX += over.x / zoom
                            localY += over.y / zoom
                            tiltZ = (over.x * 0.40f).coerceIn(-10f, 10f)
                            tiltY = (over.x * 0.35f).coerceIn(-12f, 12f)
                            tiltX = (-over.y * 0.35f).coerceIn(-12f, 12f)
                        }
                        dragDetected = true
                    }

                    if (drag != null || dragDetected) {
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val currentPointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.firstOrNull { it.id == currentPointerId }
                            if (dragChange == null || !dragChange.pressed) break
                            val delta = dragChange.position - dragChange.previousPosition
                            if (isMovementEnabled) {
                                localX += delta.x / zoom
                                localY += delta.y / zoom
                                tiltZ = (delta.x * 0.40f).coerceIn(-10f, 10f)
                                tiltY = (delta.x * 0.35f).coerceIn(-12f, 12f)
                                tiltX = (-delta.y * 0.35f).coerceIn(-12f, 12f)
                            }
                            dragChange.consume()
                        }

                        isDragging = false
                        tiltZ = 0f
                        tiltY = 0f
                        tiltX = 0f
                        onHoverSlotChanged(null)
                        onDropTargetHoverChanged(null, false)

                        if (!isMovementEnabled) {
                            localX = placement.x
                            localY = placement.y
                        } else {
                            val curCenterCardX = localX + 60f
                            val curCenterCardY = localY + 90f
                            val overOriginDeck = originDeck != null && hypot((originDeck.x + 60f) - curCenterCardX, (originDeck.y + 90f) - curCenterCardY) <= 85f
                            val curDiscardX = originDeck?.let { it.discardX ?: (it.x + 150f) } ?: 0f
                            val curDiscardY = originDeck?.let { it.discardY ?: it.y } ?: 0f
                            val overDiscard = originDeck != null && hypot((curDiscardX + 60f) - curCenterCardX, (curDiscardY + 90f) - curCenterCardY) <= 85f

                            if (overOriginDeck) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onReturnToDeck(placement.cardId)
                            } else if (overDiscard) {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDiscardCard(placement.cardId)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                val snapThreshold = 60f
                                val nearest = templateSlots.find { slot ->
                                    hypot(slot.x - localX, slot.y - localY) <= snapThreshold
                                }
                                val finalX = nearest?.x ?: if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                                    round(localX / cellSizeDp) * cellSizeDp
                                } else localX
                                val finalY = nearest?.y ?: if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                                    round(localY / cellSizeDp) * cellSizeDp
                                } else localY

                                localX = finalX
                                localY = finalY
                                onMove(placement.cardId, finalX, finalY, nearest?.tag)
                            }
                        }
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
                width = if (isOverDeck || isOverDiscard) 3.5.dp
                    else if (currentNearestSlot != null) 3.dp
                    else if (isConnectingSource) 3.dp
                    else if (isSelected) 2.5.dp
                    else 1.dp,
                color = if (isOverDeck) Color(0xFF00E5FF)
                    else if (isOverDiscard) Color(0xFFFF5252)
                    else if (currentNearestSlot != null) Color(0xFF00E5FF)
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

        // Indicador visual luminoso al sobrevolar una zona de soltado inteligente
        if (isOverDeck || isOverDiscard) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOverDeck) Color(0xDD004D40) else Color(0xDD8B0000),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isOverDeck) Icons.Default.Layers else Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (isOverDeck) "Devolver al mazo" else "Descartar",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
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
    isMovementEnabled: Boolean = true,
    snapToGrid: Boolean = false,
    gridType: PlaymatGridType = PlaymatGridType.NONE,
    cellSizeDp: Float = 48f,
    onDelete: () -> Unit,
    onMove: (Float, Float) -> Unit
) {
    val view = LocalView.current
    var localX by remember(note.id, note.x) { mutableFloatStateOf(note.x) }
    var localY by remember(note.id, note.y) { mutableFloatStateOf(note.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(note.x, note.y) {
        if (!isDragging) {
            localX = note.x
            localY = note.y
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 4.dp,
        label = "noteElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1.0f,
        label = "noteScale"
    )

    // Ghost Preview si se ajusta a la cuadrícula
    if (isDragging && snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
        val snappedX = round(localX / cellSizeDp) * cellSizeDp
        val snappedY = round(localY / cellSizeDp) * cellSizeDp
        PlaymatGhostSilhouette(
            x = snappedX,
            y = snappedY,
            width = 100.dp * zoom,
            height = 100.dp * zoom,
            zoom = 1f,
            shape = RoundedCornerShape(4.dp),
            accentColor = Color(0xFFFFD54F)
        )
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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(note.id, zoom, isMovementEnabled, snapToGrid, gridType, cellSizeDp) {
                if (isMovementEnabled) detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onDragEnd = {
                        isDragging = false
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val finalX = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                            round(localX / cellSizeDp) * cellSizeDp
                        } else localX
                        val finalY = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                            round(localY / cellSizeDp) * cellSizeDp
                        } else localY
                        localX = finalX
                        localY = finalY
                        onMove(finalX, finalY)
                    },
                    onDragCancel = {
                        isDragging = false
                        localX = note.x
                        localY = note.y
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX += dragAmount.x / zoom
                        localY += dragAmount.y / zoom
                    }
                )
            }
            .shadow(elevation, RoundedCornerShape(4.dp)),
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
    zoom: Float,
    isMovementEnabled: Boolean = true,
    snapToGrid: Boolean = false,
    gridType: PlaymatGridType = PlaymatGridType.NONE,
    cellSizeDp: Float = 48f,
    onMove: (Float, Float) -> Unit = { _, _ -> }
) {
    val view = LocalView.current
    var localX by remember(chip.id, chip.x) { mutableFloatStateOf(chip.x) }
    var localY by remember(chip.id, chip.y) { mutableFloatStateOf(chip.y) }
    var isDragging by remember(chip.id) { mutableStateOf(false) }

    LaunchedEffect(chip.x, chip.y) {
        if (!isDragging) { localX = chip.x; localY = chip.y }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 10.dp else 3.dp,
        label = "chipElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.05f else 1.0f,
        label = "chipScale"
    )

    // Ghost Preview si se ajusta a la cuadrícula
    if (isDragging && snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
        val snappedX = round(localX / cellSizeDp) * cellSizeDp
        val snappedY = round(localY / cellSizeDp) * cellSizeDp
        PlaymatGhostSilhouette(
            x = snappedX,
            y = snappedY,
            width = 110.dp * zoom,
            height = 36.dp * zoom,
            zoom = 1f,
            shape = RoundedCornerShape(12.dp),
            accentColor = MaterialTheme.colorScheme.tertiary
        )
    }

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    (localX * zoom).roundToInt(),
                    (localY * zoom).roundToInt()
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(chip.id, zoom, isMovementEnabled, snapToGrid, gridType, cellSizeDp) {
                if (isMovementEnabled) detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    },
                    onDragEnd = {
                        isDragging = false
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val finalX = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                            round(localX / cellSizeDp) * cellSizeDp
                        } else localX
                        val finalY = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                            round(localY / cellSizeDp) * cellSizeDp
                        } else localY
                        localX = finalX
                        localY = finalY
                        onMove(finalX, finalY)
                    },
                    onDragCancel = { isDragging = false; localX = chip.x; localY = chip.y },
                    onDrag = { change, amount ->
                        change.consume()
                        localX += amount.x / zoom
                        localY += amount.y / zoom
                    }
                )
            }
            .shadow(elevation, RoundedCornerShape(12.dp)),
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

