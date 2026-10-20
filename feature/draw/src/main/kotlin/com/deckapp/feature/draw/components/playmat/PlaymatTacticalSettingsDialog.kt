package com.deckapp.feature.draw.components.playmat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.PlaymatAtmosphericPreset
import com.deckapp.core.model.PlaymatGridStyle
import com.deckapp.core.model.PlaymatGridType
import com.deckapp.core.model.PlaymatSurface

/**
 * Diálogo de configuración táctica del Tapete:
 * Controla Fondos de Escena, Battlemaps de galería, Presets atmosféricos,
 * Cuadrícula (Cuadrada/Hexagonal/Puntos) y Snapping magnético.
 */
@Composable
fun PlaymatTacticalSettingsDialog(
    surface: PlaymatSurface,
    onDismissRequest: () -> Unit,
    onSetBackgroundMap: (uri: String?, preset: PlaymatAtmosphericPreset) -> Unit,
    onSetBackgroundTransform: (opacity: Float, scale: Float) -> Unit,
    onToggleBackgroundLock: () -> Unit,
    onSetGridConfig: (gridType: PlaymatGridType, gridStyle: PlaymatGridStyle, cellSizeDp: Float, snapToGrid: Boolean) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(surface.backgroundPreset) }
    var customUri by remember { mutableStateOf(surface.customImageUri) }
    var opacity by remember { mutableFloatStateOf(surface.backgroundOpacity) }
    var scale by remember { mutableFloatStateOf(surface.backgroundScale) }
    var isLocked by remember { mutableStateOf(surface.isBackgroundLocked) }

    var gridType by remember { mutableStateOf(surface.gridType) }
    var gridStyle by remember { mutableStateOf(surface.gridStyle) }
    var cellSize by remember { mutableFloatStateOf(surface.cellSizeDp) }
    var snapToGrid by remember { mutableStateOf(surface.snapToGrid) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            customUri = uri.toString()
            selectedPreset = PlaymatAtmosphericPreset.NONE
            onSetBackgroundMap(uri.toString(), PlaymatAtmosphericPreset.NONE)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.GridOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Fondo y Cuadrícula Táctica",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Selector de Fondo y Battlemap
                Text(
                    text = "Mapa de Batalla / Fondo de Escena",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Subir Mapa")
                    }

                    if (customUri != null || selectedPreset != PlaymatAtmosphericPreset.NONE) {
                        OutlinedButton(
                            onClick = {
                                customUri = null
                                selectedPreset = PlaymatAtmosphericPreset.NONE
                                onSetBackgroundMap(null, PlaymatAtmosphericPreset.NONE)
                            }
                        ) {
                            Text("Limpiar")
                        }
                    }
                }

                // Presets atmosféricos
                Text("Presets Atmosféricos", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        PlaymatAtmosphericPreset.DUNGEON_STONE to "Mazmorra",
                        PlaymatAtmosphericPreset.MEDIEVAL_TAVERN to "Taberna",
                        PlaymatAtmosphericPreset.MYSTIC_FOREST to "Bosque",
                        PlaymatAtmosphericPreset.PARCHMENT_MAP to "Pergamino"
                    )
                    presets.forEach { (preset, label) ->
                        val isSelected = selectedPreset == preset && customUri == null
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedPreset = preset
                                customUri = null
                                onSetBackgroundMap(null, preset)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Sliders de Opacidad y Escala del fondo
                if (customUri != null || selectedPreset != PlaymatAtmosphericPreset.NONE) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacidad del Mapa", style = MaterialTheme.typography.labelSmall)
                            Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = opacity,
                            onValueChange = {
                                opacity = it
                                onSetBackgroundTransform(opacity, scale)
                            },
                            valueRange = 0.2f..1.0f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Escala del Mapa", style = MaterialTheme.typography.labelSmall)
                            Text(String.format("%.1fx", scale), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = scale,
                            onValueChange = {
                                scale = it
                                onSetBackgroundTransform(opacity, scale)
                            },
                            valueRange = 0.5f..3.0f
                        )

                        // Candado de Bloqueo
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bloquear mapa (evitar arrastre)", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isLocked,
                                onCheckedChange = {
                                    isLocked = it
                                    onToggleBackgroundLock()
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // 2. Configuración de Cuadrícula Táctica
                Text(
                    text = "Cuadrícula Táctica (Grid)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Tipo de cuadrícula
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PlaymatGridType.NONE to "Ninguna",
                        PlaymatGridType.SQUARE to "Cuadrados (5ft)",
                        PlaymatGridType.HEXAGONAL to "Hexágonos"
                    ).forEach { (type, label) ->
                        val isSelected = gridType == type
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                gridType = type
                                onSetGridConfig(gridType, gridStyle, cellSize, snapToGrid)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                if (gridType != PlaymatGridType.NONE) {
                    // Estilo de cuadrícula (Líneas / Puntos)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            PlaymatGridStyle.LINES to "Líneas Tenues",
                            PlaymatGridStyle.DOTS to "Puntos (Dots)"
                        ).forEach { (style, label) ->
                            val isSelected = gridStyle == style
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    gridStyle = style
                                    onSetGridConfig(gridType, gridStyle, cellSize, snapToGrid)
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Tamaño de Celda
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tamaño de Casilla", style = MaterialTheme.typography.labelSmall)
                            Text("${cellSize.toInt()} dp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = cellSize,
                            onValueChange = {
                                cellSize = it
                                onSetGridConfig(gridType, gridStyle, cellSize, snapToGrid)
                            },
                            valueRange = 32f..96f
                        )
                    }

                    // Acople Magnético (Snap to Grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Alinear a Casillas (Snap)", style = MaterialTheme.typography.bodyMedium)
                            Text("Acopla tokens y cartas automáticamente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = snapToGrid,
                            onCheckedChange = {
                                snapToGrid = it
                                onSetGridConfig(gridType, gridStyle, cellSize, snapToGrid)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("Listo")
            }
        }
    )
}
