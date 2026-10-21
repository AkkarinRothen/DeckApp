package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.PlaymatClockPlacement
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Renderizado de un Reloj de Progreso (Progress Clock estilo Blades in the Dark / PBTA / TTRPG).
 * Permite seguir tensión, sigilo, alertas o proyectos en 4, 6 u 8 cuñas radiales.
 * Soporta arrastre a 120 FPS, tap para avanzar y conexión a la red de hilos narrativos.
 */
@Composable
fun PlaymatClockItem(
    clock: PlaymatClockPlacement,
    isSelected: Boolean,
    isConnectingSource: Boolean,
    zoom: Float,
    onClockClick: () -> Unit,
    onMove: (clockId: String, finalX: Float, finalY: Float) -> Unit,
    onAdvanceProgress: () -> Unit,
    onReduceProgress: () -> Unit,
    onResetProgress: () -> Unit,
    onStartConnection: () -> Unit,
    onRemove: () -> Unit,
    onDismissMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clockSize = 76.dp * zoom

    var localX by remember(clock.id, clock.x) { mutableFloatStateOf(clock.x) }
    var localY by remember(clock.id, clock.y) { mutableFloatStateOf(clock.y) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(clock.x, clock.y) {
        if (!isDragging) {
            localX = clock.x
            localY = clock.y
        }
    }

    val progressColor = remember(clock.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(clock.colorHex))
        } catch (e: Exception) {
            Color(0xFFE53935)
        }
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
            // Disco del Reloj interactivo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(clockSize)
                    .shadow(
                        elevation = if (isDragging) 16.dp else if (isSelected) 10.dp else 4.dp,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(Color(0xFF18191E))
                    .border(
                        width = if (isConnectingSource) 3.5.dp else if (isSelected) 2.5.dp else 1.5.dp,
                        color = if (isConnectingSource) Color(0xFFE53935)
                        else if (isSelected) MaterialTheme.colorScheme.primary
                        else Color(0xFF4A4B54),
                        shape = CircleShape
                    )
                    .pointerInput(clock.id, zoom) {
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
                                onMove(clock.id, localX, localY)
                            } else {
                                onClockClick()
                            }
                        }
                    }
            ) {
                // Dibujo de Cuñas y Segmentos del Reloj
                Canvas(modifier = Modifier.fillMaxSize().padding(3.dp * zoom)) {
                    val segments = clock.totalSegments.coerceIn(4, 8)
                    val sweepAngle = 360f / segments
                    val startAngleBase = -90f // 12 o'clock

                    // Fondo de cuñas vacías
                    drawCircle(
                        color = Color(0xFF22232A),
                        radius = size.minDimension / 2f
                    )

                    // Cuñas llenas
                    for (i in 0 until clock.filledSegments.coerceIn(0, segments)) {
                        val startAngle = startAngleBase + (i * sweepAngle)
                        drawArc(
                            color = progressColor,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(size.width, size.height)
                        )
                    }

                    // Líneas divisorias radiales
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension / 2f
                    for (i in 0 until segments) {
                        val angleDeg = startAngleBase + (i * sweepAngle)
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val endX = center.x + (radius * cos(angleRad)).toFloat()
                        val endY = center.y + (radius * sin(angleRad)).toFloat()
                        drawLine(
                            color = Color(0xFF101014),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 2.5f * zoom,
                            cap = StrokeCap.Round
                        )
                    }

                    // Borde exterior del reloj
                    drawCircle(
                        color = Color(0xFF6B6C76),
                        radius = radius,
                        style = Stroke(width = 1.5f * zoom)
                    )

                    // Perno o eje central
                    drawCircle(
                        color = Color(0xFF121216),
                        radius = 5.dp.toPx() * zoom
                    )
                    drawCircle(
                        color = progressColor,
                        radius = 2.5.dp.toPx() * zoom
                    )
                }
            }

            Spacer(Modifier.height(3.dp * zoom))

            // Etiqueta de Título y Progreso
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.85f),
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, progressColor.copy(alpha = 0.6f)),
                modifier = Modifier.widthIn(max = 110.dp * zoom)
            ) {
                Text(
                    text = "${clock.title} (${clock.filledSegments}/${clock.totalSegments})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (8.5f * zoom.coerceIn(0.7f, 1.3f)).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Menú Contextual Flotante al estar seleccionado
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-44).dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClockActionIconButton(
                        icon = Icons.Default.Share,
                        description = "Conectar Hilo",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onStartConnection
                    )
                    ClockActionIconButton(
                        icon = Icons.Default.Add,
                        description = "+1 Progreso",
                        tint = progressColor,
                        onClick = onAdvanceProgress
                    )
                    ClockActionIconButton(
                        icon = Icons.Default.Remove,
                        description = "-1 Progreso",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onReduceProgress
                    )
                    ClockActionIconButton(
                        icon = Icons.Default.RestartAlt,
                        description = "Vaciar Reloj",
                        tint = Color(0xFFFFB300),
                        onClick = onResetProgress
                    )
                    ClockActionIconButton(
                        icon = Icons.Default.DeleteOutline,
                        description = "Retirar Reloj",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onRemove
                    )
                    ClockActionIconButton(
                        icon = Icons.Default.Close,
                        description = "Cerrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onDismissMenu
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockActionIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(28.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}
