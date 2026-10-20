package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Indicador visual y cinta métrica de distancias tácticas para el Tapete de Juego.
 * Proyecta una línea elástica punteada y cápsula HUD con la distancia en pies (5ft/celda)
 * y número de casillas recorridas.
 */
@Composable
fun PlaymatMeasurementRuler(
    startPos: Offset?,
    currentPos: Offset?,
    cellSizeDp: Float,
    zoom: Float,
    cameraX: Float,
    cameraY: Float,
    modifier: Modifier = Modifier
) {
    if (startPos == null || currentPos == null) return

    val density = LocalDensity.current.density
    val startScreen = Offset((startPos.x * zoom) + cameraX, (startPos.y * zoom) + cameraY)
    val currentScreen = Offset((currentPos.x * zoom) + cameraX, (currentPos.y * zoom) + cameraY)

    val deltaX = currentPos.x - startPos.x
    val deltaY = currentPos.y - startPos.y
    val distCanvas = hypot(deltaX, deltaY)

    val effectiveCellSize = cellSizeDp.coerceAtLeast(16f)
    val cellsCount = (distCanvas / effectiveCellSize)
    val roundedCells = cellsCount.roundToInt()
    val feet = roundedCells * 5

    if (roundedCells <= 0 && distCanvas < 20f) return

    Box(modifier = modifier.fillMaxSize()) {
        // Línea punteada táctica
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color(0xFF00E5FF)
            val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)

            // Línea de medición
            drawLine(
                color = strokeColor,
                start = startScreen,
                end = currentScreen,
                strokeWidth = 3f * density,
                pathEffect = pathEffect
            )

            // Círculo en origen y destino
            drawCircle(
                color = strokeColor,
                radius = 5f * density,
                center = startScreen
            )
            drawCircle(
                color = strokeColor,
                radius = 6f * density,
                center = currentScreen
            )
        }

        // Píldora HUD con la distancia medida
        val midX = (startScreen.x + currentScreen.x) / 2f
        val midY = (startScreen.y + currentScreen.y) / 2f

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (midX - 45).roundToInt(),
                        (midY - 32).roundToInt()
                    )
                }
                .background(
                    color = Color(0xEE10141D),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF00E5FF),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "$feet ft ($roundedCells c)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
