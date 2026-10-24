package com.deckapp.feature.draw.components.playmat

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.deckapp.core.model.PlaymatGridType
import com.deckapp.core.model.PlaymatTokenPlacement
import com.deckapp.core.model.PlaymatTokenType
import com.deckapp.core.ui.util.toCoilModel
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Renderizado de una ficha/token circular de personaje (PJ/PNJ) sobre el tapete 2D.
 * Incluye física de arrastre desacoplada a 120 FPS, micro-barra de PV, anillo de facción
 * y menú contextual para enlazar hilos o ajustar vida.
 */
@Composable
fun PlaymatTokenItem(
    token: PlaymatTokenPlacement,
    isSelected: Boolean,
    isConnectingSource: Boolean,
    zoom: Float,
    isMovementEnabled: Boolean = true,
    snapToGrid: Boolean = false,
    gridType: PlaymatGridType = PlaymatGridType.NONE,
    cellSizeDp: Float = 48f,
    onTokenClick: () -> Unit,
    onMove: (tokenId: String, finalX: Float, finalY: Float) -> Unit,
    onDragProgress: (start: androidx.compose.ui.geometry.Offset?, current: androidx.compose.ui.geometry.Offset?) -> Unit = { _, _ -> },
    onStartConnection: () -> Unit,
    onEditHp: () -> Unit,
    onToggleFaction: () -> Unit,
    onRemove: () -> Unit,
    onDismissMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val size = 64.dp * zoom

    var localX by remember(token.id, token.x) { mutableFloatStateOf(token.x) }
    var localY by remember(token.id, token.y) { mutableFloatStateOf(token.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(token.x, token.y) {
        if (!isDragging) {
            localX = token.x
            localY = token.y
        }
    }

    val elevation by animateDpAsState(
        targetValue = if (isDragging) 18.dp else if (isSelected) 10.dp else 4.dp,
        label = "tokenElevation"
    )

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.08f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tokenScale"
    )

    val ringColor = remember(token.ringColorHex, token.tokenType) {
        try {
            Color(android.graphics.Color.parseColor(token.ringColorHex))
        } catch (e: Exception) {
            when (token.tokenType) {
                PlaymatTokenType.ALLY -> Color(0xFF4CAF50)
                PlaymatTokenType.NEUTRAL -> Color(0xFFFFB300)
                PlaymatTokenType.ENEMY -> Color(0xFFE53935)
            }
        }
    }

    val hpFraction = if (token.maxHp > 0) (token.currentHp.toFloat() / token.maxHp.toFloat()).coerceIn(0f, 1f) else 1f
    val hpBarColor = if (hpFraction <= 0.25f) Color(0xFFE53935)
    else if (hpFraction <= 0.5f) Color(0xFFFFB300)
    else Color(0xFF4CAF50)

    // Silueta guía fantasma si está activo el ajuste a cuadrícula
    if (isDragging && snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
        val snappedX = round(localX / cellSizeDp) * cellSizeDp
        val snappedY = round(localY / cellSizeDp) * cellSizeDp
        PlaymatGhostSilhouette(
            x = snappedX,
            y = snappedY,
            width = size,
            height = size,
            zoom = 1f,
            shape = CircleShape,
            accentColor = ringColor
        )
    }

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (localX * zoom).roundToInt(),
                    (localY * zoom).roundToInt()
                )
            }
            .wrapContentSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentSize()
        ) {
            // Disco circular del Token
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(token.id, zoom, isMovementEnabled, snapToGrid, gridType, cellSizeDp) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var dragDetected = false
                            val drag = awaitTouchSlopOrCancellation(down.id) { change, over ->
                                change.consume()
                                if (isMovementEnabled) {
                                    localX += over.x / zoom
                                    localY += over.y / zoom
                                }
                                dragDetected = true
                            }

                            if (drag != null || dragDetected) {
                                isDragging = true
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                val startOffset = androidx.compose.ui.geometry.Offset(localX + 32f, localY + 32f)
                                onDragProgress(startOffset, androidx.compose.ui.geometry.Offset(localX + 32f, localY + 32f))
                                val currentPointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.firstOrNull { it.id == currentPointerId }
                                    if (dragChange == null || !dragChange.pressed) break
                                    val delta = dragChange.position - dragChange.previousPosition
                                    if (isMovementEnabled) {
                                        localX += delta.x / zoom
                                        localY += delta.y / zoom
                                    }
                                    dragChange.consume()
                                    onDragProgress(startOffset, androidx.compose.ui.geometry.Offset(localX + 32f, localY + 32f))
                                }
                                isDragging = false
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onDragProgress(null, null)
                                if (isMovementEnabled) {
                                    val finalX = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                                        round(localX / cellSizeDp) * cellSizeDp
                                    } else localX
                                    val finalY = if (snapToGrid && gridType != PlaymatGridType.NONE && cellSizeDp > 0f) {
                                        round(localY / cellSizeDp) * cellSizeDp
                                    } else localY
                                    localX = finalX
                                    localY = finalY
                                    onMove(token.id, finalX, finalY)
                                } else {
                                    localX = token.x
                                    localY = token.y
                                }
                            } else {
                                // Toque limpio
                                onTokenClick()
                            }
                        }
                    }
                    .shadow(
                        elevation = elevation,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        width = if (isConnectingSource) 3.5.dp else if (isSelected) 3.dp else 2.5.dp,
                        color = if (isConnectingSource) Color(0xFFE53935)
                        else if (isSelected) MaterialTheme.colorScheme.primary
                        else ringColor,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Imagen o Inicial
                if (!token.imagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = token.imagePath.toCoilModel(),
                        contentDescription = token.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = token.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Micro-barra de salud en el borde inferior del token
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.75f)
                        .padding(bottom = 4.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(hpFraction)
                            .background(hpBarColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Etiqueta de nombre y PV numérico
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, ringColor.copy(alpha = 0.6f)),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = token.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${token.currentHp}/${token.maxHp}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = hpBarColor,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Menú radial contextual al seleccionar el token
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-44).dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TokenActionButton(
                        icon = Icons.Default.Share,
                        description = "Conectar Hilo",
                        onClick = onStartConnection
                    )
                    TokenActionButton(
                        icon = Icons.Default.Favorite,
                        description = "Ajustar PV",
                        onClick = onEditHp
                    )
                    TokenActionButton(
                        icon = Icons.Default.Palette,
                        description = "Cambiar Facción",
                        onClick = onToggleFaction
                    )
                    TokenActionButton(
                        icon = Icons.Default.DeleteOutline,
                        description = "Retirar de la Mesa",
                        onClick = onRemove,
                        isDestructive = true
                    )
                    TokenActionButton(
                        icon = Icons.Default.Close,
                        description = "Cerrar",
                        onClick = onDismissMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenActionButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(16.dp)
        )
    }
}
