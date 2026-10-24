package com.deckapp.feature.draw.components.playmat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.PlaymatDeckPlacement
import com.deckapp.core.ui.util.toCoilModel
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renderizado de un mazo físico en el tapete 2D con efecto de pila 3D y extracción gestual.
 */
@Composable
fun PlaymatDeckItem(
    placement: PlaymatDeckPlacement,
    deckName: String,
    availableCount: Int,
    totalCount: Int,
    backImage: String?,
    zoom: Float,
    templateSlots: List<TemplateSlotDef>,
    isGroupLocked: Boolean = false,
    onTapDraw: () -> Unit,
    onDragDrawCardToPlaymat: (targetX: Float, targetY: Float, slotTag: String?) -> Unit,
    onMoveDeck: (newX: Float, newY: Float) -> Unit,
    onHoverSlotChanged: (String?) -> Unit,
    onShuffleDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val width = 120.dp * zoom
    val height = 180.dp * zoom

    var localDeckX by remember(placement.stackId, placement.x) { mutableFloatStateOf(placement.x) }
    var localDeckY by remember(placement.stackId, placement.y) { mutableFloatStateOf(placement.y) }
    var isMovingDeck by remember { mutableStateOf(false) }

    LaunchedEffect(placement.x, placement.y) {
        if (!isMovingDeck) {
            localDeckX = placement.x
            localDeckY = placement.y
        }
    }

    var isDraggingCardOut by remember { mutableStateOf(false) }
    var draggedCardX by remember { mutableFloatStateOf(placement.x) }
    var draggedCardY by remember { mutableFloatStateOf(placement.y) }
    var showContextMenu by remember { mutableStateOf(false) }

    val currentNearestSlot = remember(draggedCardX, draggedCardY, isDraggingCardOut, templateSlots) {
        if (!isDraggingCardOut || templateSlots.isEmpty()) null
        else {
            val snapThreshold = 60f
            templateSlots.find { slot ->
                hypot(slot.x - draggedCardX, slot.y - draggedCardY) <= snapThreshold
            }
        }
    }

    LaunchedEffect(currentNearestSlot) {
        if (isDraggingCardOut) {
            onHoverSlotChanged(currentNearestSlot?.tag)
        }
    }

    val layerCount = remember(availableCount) {
        if (availableCount <= 0) 0
        else min(4, 1 + (availableCount / 8))
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (localDeckX * zoom).roundToInt(),
                    (localDeckY * zoom).roundToInt()
                )
            }
            .size(width, height)
    ) {
        // Capas inferiores para generar relieve de mazo físico 3D
        for (i in layerCount downTo 1) {
            val layerOffsetPx = (i * 2.5f * zoom)
            Box(
                modifier = Modifier
                    .offset { IntOffset(layerOffsetPx.roundToInt(), layerOffsetPx.roundToInt()) }
                    .size(width, height)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1E24).copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }

        // Mazo superior interactivo
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(8.dp))
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .pointerInput(placement.stackId, zoom, availableCount, templateSlots, placement.groupId, placement.isLocked, isGroupLocked) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val moveHandleSize = 40.dp.toPx()
                        val startedOnMoveHandle = down.position.x >= size.width - moveHandleSize && down.position.y <= moveHandleSize
                        var dragDetected = false
                        var currentDragX = localDeckX
                        var currentDragY = localDeckY

                        val drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                            change.consume()
                            if (startedOnMoveHandle) {
                                if (isGroupLocked || placement.isLocked) {
                                    dragDetected = true
                                } else {
                                    isMovingDeck = true
                                    currentDragX += over.x / zoom
                                    currentDragY += over.y / zoom
                                    dragDetected = true
                                }
                            } else if (availableCount > 0) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                isDraggingCardOut = true
                                currentDragX += over.x / zoom
                                currentDragY += over.y / zoom
                                draggedCardX = currentDragX
                                draggedCardY = currentDragY
                                dragDetected = true
                            }
                        }

                        if ((drag != null || dragDetected) && startedOnMoveHandle && (isGroupLocked || placement.isLocked)) {
                            isMovingDeck = false
                            isDraggingCardOut = false
                            localDeckX = placement.x
                            localDeckY = placement.y
                        } else if ((drag != null || dragDetected) && startedOnMoveHandle) {
                            val currentPointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == currentPointerId }
                                if (dragChange == null || !dragChange.pressed) break
                                val delta = dragChange.position - dragChange.previousPosition
                                currentDragX += delta.x / zoom
                                currentDragY += delta.y / zoom
                                localDeckX = currentDragX
                                localDeckY = currentDragY
                                dragChange.consume()
                            }
                            isMovingDeck = false
                            localDeckX = currentDragX
                            localDeckY = currentDragY
                            onMoveDeck(currentDragX, currentDragY)
                        } else if ((drag != null || dragDetected) && availableCount > 0) {
                            val currentPointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == currentPointerId }
                                if (dragChange == null || !dragChange.pressed) break
                                val delta = dragChange.position - dragChange.previousPosition
                                currentDragX += delta.x / zoom
                                currentDragY += delta.y / zoom
                                draggedCardX = currentDragX
                                draggedCardY = currentDragY
                                dragChange.consume()
                            }

                            isDraggingCardOut = false
                            onHoverSlotChanged(null)

                            val snapThreshold = 60f
                            val nearest = templateSlots.find { slot ->
                                hypot(slot.x - draggedCardX, slot.y - draggedCardY) <= snapThreshold
                            }

                            val finalX = nearest?.x ?: draggedCardX
                            val finalY = nearest?.y ?: draggedCardY
                            onDragDrawCardToPlaymat(finalX, finalY, nearest?.tag)
                        } else {
                            // El cuerpo roba; la agarradera abre las opciones y solo se mueve al arrastrarla.
                            if (startedOnMoveHandle) {
                                showContextMenu = true
                            } else if (availableCount > 0) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onTapDraw()
                            } else {
                                showContextMenu = true
                            }
                        }
                    }
                },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (availableCount > 0) {
                    if (backImage != null) {
                        AsyncImage(
                            model = backImage.toCoilModel(),
                            contentDescription = deckName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    // Mazo vacío
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = "Agotado",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Agotado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Cabecera superior con nombre del mazo
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.68f)
                ) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (10 * zoom.coerceIn(0.75f, 1.25f)).sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Arrastra desde esta esquina para mover el mazo; desde el centro para colocar una carta",
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
                )

                // Badge inferior con cantidad de cartas
                Surface(
                    shape = CircleShape,
                    color = if (availableCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "$availableCount",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Botón menú contextual rápido (esquina inferior izquierda)
                IconButton(
                    onClick = { showContextMenu = true },
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones de mazo",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Menú desplegable contextual del mazo
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Barajar mazo") },
                leadingIcon = { Icon(Icons.Default.Shuffle, null) },
                onClick = {
                    showContextMenu = false
                    onShuffleDeck()
                }
            )
            DropdownMenuItem(
                text = { Text("Robar carta") },
                leadingIcon = { Icon(Icons.Default.Add, null) },
                enabled = availableCount > 0,
                onClick = {
                    showContextMenu = false
                    onTapDraw()
                }
            )
        }
    }

    // Carta flotante proyectada mientras se arrastra desde el mazo hacia el tapete
    if (isDraggingCardOut) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (draggedCardX * zoom).roundToInt(),
                        (draggedCardY * zoom).roundToInt()
                    )
                }
                .size(width, height)
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = if (currentNearestSlot != null) 3.dp else 1.5.dp,
                    color = if (currentNearestSlot != null) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (backImage != null) {
                AsyncImage(
                    model = backImage.toCoilModel(),
                    contentDescription = "Robando...",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
