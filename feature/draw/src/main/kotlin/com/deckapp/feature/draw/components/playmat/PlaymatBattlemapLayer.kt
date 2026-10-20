package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.deckapp.core.model.PlaymatAtmosphericPreset
import com.deckapp.core.ui.util.toCoilModel

/**
 * Capa de renderizado de Fondos de Escena, Battlemaps y Presets Atmosféricos.
 * Soporta imágenes locales de mapas tácticos (PNG/JPG) o texturas vectoriales de atmósfera.
 */
@Composable
fun PlaymatBattlemapLayer(
    customImageUri: String?,
    preset: PlaymatAtmosphericPreset,
    opacity: Float,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    zoom: Float,
    modifier: Modifier = Modifier
) {
    if (customImageUri == null && preset == PlaymatAtmosphericPreset.NONE) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = opacity.coerceIn(0.1f, 1f)
            }
    ) {
        if (!customImageUri.isNullOrBlank()) {
            AsyncImage(
                model = customImageUri.toCoilModel(),
                contentDescription = "Battlemap de Fondo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX * zoom
                        translationY = offsetY * zoom
                    }
            )
        } else {
            // Renderizado de Presets Atmosféricos Procedurales
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                when (preset) {
                    PlaymatAtmosphericPreset.DUNGEON_STONE -> {
                        // Baldosas de mazmorra de piedra tallada
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF2C2F36), Color(0xFF17181C)),
                                center = Offset(width / 2f, height / 2f),
                                radius = size.minDimension
                            )
                        )
                        val tileSize = 72f * density * zoom
                        val cols = (width / tileSize).toInt() + 1
                        val rows = (height / tileSize).toInt() + 1
                        for (c in 0..cols) {
                            for (r in 0..rows) {
                                val x = c * tileSize
                                val y = r * tileSize
                                drawRect(
                                    color = Color(0xFF101114),
                                    topLeft = Offset(x, y),
                                    size = Size(tileSize, tileSize),
                                    style = Stroke(width = 1.5f * density)
                                )
                                // Detalle de losas agrietadas
                                drawLine(
                                    color = Color(0x22FFFFFF),
                                    start = Offset(x + 2f, y + 2f),
                                    end = Offset(x + tileSize - 2f, y + 2f),
                                    strokeWidth = 1f * density
                                )
                            }
                        }
                    }

                    PlaymatAtmosphericPreset.MEDIEVAL_TAVERN -> {
                        // Tablones de madera de taberna con vetas cálidas
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF382315), Color(0xFF23150B))
                            )
                        )
                        val plankHeight = 36f * density * zoom
                        val rows = (height / plankHeight).toInt() + 1
                        for (r in 0..rows) {
                            val y = r * plankHeight
                            drawLine(
                                color = Color(0xFF140B05),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 2f * density
                            )
                        }
                    }

                    PlaymatAtmosphericPreset.MYSTIC_FOREST -> {
                        // Musgo y fronda nocturna de bosque místico
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF1B3828), Color(0xFF0A180F)),
                                center = Offset(width / 2f, height / 2f),
                                radius = size.minDimension
                            )
                        )
                    }

                    PlaymatAtmosphericPreset.PARCHMENT_MAP -> {
                        // Cartografía en pergamino antiguo
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFDCC8A0), Color(0xFF9E845A)),
                                center = Offset(width / 2f, height / 2f),
                                radius = size.minDimension
                            )
                        )
                    }

                    PlaymatAtmosphericPreset.NONE -> Unit
                }
            }
        }
    }
}
