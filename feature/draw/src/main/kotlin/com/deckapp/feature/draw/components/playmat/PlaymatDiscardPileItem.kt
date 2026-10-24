package com.deckapp.feature.draw.components.playmat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.PlaymatGridType
import com.deckapp.core.ui.util.toCoilModel
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Renderizado de una pila física de descarte asociada a un mazo en el tapete 2D.
 */
@Composable
fun PlaymatDiscardPileItem(
    stackId: Long,
    x: Float,
    y: Float,
    zoom: Float,
    isMovementEnabled: Boolean = true,
    isDropTargetHovered: Boolean = false,
    snapToGrid: Boolean = false,
    gridType: PlaymatGridType = PlaymatGridType.NONE,
    cellSizeDp: Float = 48f,
    piledCards: List<Card>,
    onReshuffleToDeck: () -> Unit,
    onMoveDiscardPile: (newX: Float, newY: Float) -> Unit,
    onInspectCard: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val width = 120.dp * zoom
    val height = 180.dp * zoom

    var localX by remember(stackId, x) { mutableFloatStateOf(x) }
    var localY by remember(stackId, y) { mutableFloatStateOf(y) }
    var isDragging by remember { mutableStateOf(false) }
    var showDiscardHistory by remember(stackId) { mutableStateOf(false) }

    LaunchedEffect(x, y) {
        if (!isDragging) {
            localX = x
            localY = y
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDropTargetHovered) 16.dp else if (isDragging) 14.dp else 6.dp,
        label = "discardElevation"
    )

    val topCard = piledCards.lastOrNull()

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (localX * zoom).roundToInt(),
                    (localY * zoom).roundToInt()
                )
            }
            .size(width, height)
            .clickable(enabled = piledCards.isNotEmpty()) { showDiscardHistory = true }
            .pointerInput(stackId, zoom, isMovementEnabled, snapToGrid, gridType, cellSizeDp) {
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
                        onMoveDiscardPile(finalX, finalY)
                    },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        localX += dragAmount.x / zoom
                        localY += dragAmount.y / zoom
                    }
                )
            }
    ) {
        if (topCard == null) {
            // Pila vacía: Silueta punteada
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(
                    width = if (isDropTargetHovered) 3.5.dp.toPx() else 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
                drawRoundRect(
                    color = if (isDropTargetHovered) Color(0xFFFF5252) else Color.White.copy(alpha = 0.25f),
                    size = size,
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = stroke
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isDropTargetHovered) Color(0x33FF5252) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Pila vacía",
                    tint = if (isDropTargetHovered) Color(0xFFFF5252) else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(if (isDropTargetHovered) 36.dp else 28.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isDropTargetHovered) "Soltar para descartar" else "Descarte",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDropTargetHovered) Color(0xFFFF5252) else Color.White.copy(alpha = 0.35f),
                    fontWeight = if (isDropTargetHovered) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Pila con cartas: Muestra la última carta descartada
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(-2.5f)
                    .shadow(elevation, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isDropTargetHovered) 3.5.dp else 1.dp,
                        color = if (isDropTargetHovered) Color(0xFFFF5252) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val frontImage = topCard.faces.firstOrNull()?.imagePath ?: topCard.faces.firstOrNull()?.reversedImagePath
                    if (frontImage != null) {
                        AsyncImage(
                            model = frontImage.toCoilModel(),
                            contentDescription = topCard.title,
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
                                text = topCard.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Último descarte",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Título superior translúcido
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "Descarte (${piledCards.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (9 * zoom.coerceIn(0.75f, 1.25f)).sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // Botón de re-barajar al mazo
                    FilledTonalIconButton(
                        onClick = onReshuffleToDeck,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.BottomEnd)
                            .padding(4.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recuperar al mazo",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Halo de absorción luminoso al sobrevolar para descartar
                    if (isDropTargetHovered) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xDD8B0000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Descartar",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDiscardHistory) {
            AlertDialog(
                onDismissRequest = { showDiscardHistory = false },
                title = { Text("Descarte · ${piledCards.size} cartas") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(piledCards.asReversed(), key = { it.id }) { card ->
                            val imagePath = card.activeFace.imagePath
                                ?: card.activeFace.reversedImagePath
                            ListItem(
                                headlineContent = {
                                    Text(card.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text("Tocá para ver la carta")
                                },
                                leadingContent = if (imagePath != null) {
                                    {
                                        AsyncImage(
                                            model = imagePath.toCoilModel(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(48.dp, 64.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showDiscardHistory = false
                                        onInspectCard(card.id)
                                    }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDiscardHistory = false }) { Text("Cerrar") }
                }
            )
        }
    }
}
