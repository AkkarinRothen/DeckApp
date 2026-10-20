package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.deckapp.core.model.PlaymatGridStyle
import com.deckapp.core.model.PlaymatGridType
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Capa de renderizado de cuadrícula táctica espacial 2D (Cuadrados 5ft / Hexágonos).
 * Dibuja líneas tenues o puntos de intersección sobre el tapete ajustados a la cámara y zoom.
 */
@Composable
fun PlaymatGridLayer(
    gridType: PlaymatGridType,
    gridStyle: PlaymatGridStyle,
    cellSizeDp: Float,
    cameraX: Float,
    cameraY: Float,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    if (gridType == PlaymatGridType.NONE) return

    val gridLineColor = Color.White.copy(alpha = 0.16f)
    val gridDotColor = Color(0xFF00E5FF).copy(alpha = 0.35f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val effectiveCellSize = cellSizeDp * density * zoom
        if (effectiveCellSize < 8f) return@Canvas // Evitar sobrecarga en zooms lejanos

        val width = size.width
        val height = size.height

        when (gridType) {
            PlaymatGridType.SQUARE -> {
                val offsetX = (cameraX % effectiveCellSize + effectiveCellSize) % effectiveCellSize
                val offsetY = (cameraY % effectiveCellSize + effectiveCellSize) % effectiveCellSize

                if (gridStyle == PlaymatGridStyle.LINES) {
                    // Líneas verticales
                    var x = offsetX
                    while (x <= width) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 1f * density
                        )
                        x += effectiveCellSize
                    }

                    // Líneas horizontales
                    var y = offsetY
                    while (y <= height) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f * density
                        )
                        y += effectiveCellSize
                    }
                } else {
                    // Puntos de intersección (Dots)
                    var x = offsetX
                    while (x <= width) {
                        var y = offsetY
                        while (y <= height) {
                            drawCircle(
                                color = gridDotColor,
                                radius = 2f * density,
                                center = Offset(x, y)
                            )
                            y += effectiveCellSize
                        }
                        x += effectiveCellSize
                    }
                }
            }

            PlaymatGridType.HEXAGONAL -> {
                val radius = effectiveCellSize / 2f
                val hexHeight = sqrt(3.0).toFloat() * radius
                val hexWidth = 1.5f * radius

                val startCol = -2
                val endCol = (width / hexWidth).toInt() + 2
                val startRow = -2
                val endRow = (height / hexHeight).toInt() + 2

                for (col in startCol..endCol) {
                    val cx = (col * hexWidth) + (cameraX % (hexWidth * 2f))
                    val rowOffset = if (kotlin.math.abs(col) % 2 == 1) hexHeight / 2f else 0f

                    for (row in startRow..endRow) {
                        val cy = (row * hexHeight) + rowOffset + (cameraY % hexHeight)

                        if (gridStyle == PlaymatGridStyle.LINES) {
                            val path = Path()
                            for (i in 0 until 6) {
                                val angleDeg = 60.0 * i - 30.0
                                val angleRad = Math.toRadians(angleDeg)
                                val px = cx + (radius * cos(angleRad)).toFloat()
                                val py = cy + (radius * sin(angleRad)).toFloat()
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            drawPath(
                                path = path,
                                color = gridLineColor,
                                style = Stroke(width = 1f * density)
                            )
                        } else {
                            drawCircle(
                                color = gridDotColor,
                                radius = 2f * density,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }

            PlaymatGridType.NONE -> Unit
        }
    }
}
