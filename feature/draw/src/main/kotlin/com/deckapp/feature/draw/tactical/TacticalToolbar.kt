package com.deckapp.feature.draw.tactical

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.TacticalMeasureShape
import com.deckapp.core.model.TacticalTerrainType

@Composable
fun TacticalToolbar(
    activeTool: TacticalTool,
    activeAoeShape: TacticalMeasureShape,
    aoeRadiusFt: Int,
    isFogEnabled: Boolean = false,
    fogBrushMode: FogBrushMode = FogBrushMode.REVEAL,
    isFogDmView: Boolean = true,
    activeTerrainBrush: TacticalTerrainType = TacticalTerrainType.DIFFICULT,
    routeWaypointsCount: Int = 0,
    currentRouteDistanceFt: Int = 0,
    selectedTokenSpeedFt: Int? = null,
    onToolSelected: (TacticalTool) -> Unit,
    onAoeShapeSelected: (TacticalMeasureShape) -> Unit,
    onAoeRadiusSelected: (Int) -> Unit,
    onPersistAoE: () -> Unit,
    onToggleFog: () -> Unit = {},
    onFogBrushModeSelected: (FogBrushMode) -> Unit = {},
    onRevealAllFog: () -> Unit = {},
    onResetFog: () -> Unit = {},
    onToggleFogDmView: () -> Unit = {},
    onTerrainBrushSelected: (TacticalTerrainType) -> Unit = {},
    onClearAllTerrain: () -> Unit = {},
    onRemoveLastRouteWaypoint: () -> Unit = {},
    onCommitRouteMovement: () -> Unit = {},
    onClearRoute: () -> Unit = {},
    onAddTokenClicked: () -> Unit,
    onCalibrateGridClicked: () -> Unit,
    onOpenMapSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Sub-barra contextual de Niebla de Guerra (Fog of War)
        AnimatedVisibility(
            visible = activeTool == TacticalTool.FOG,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Switch de activación de niebla
                    FilterChip(
                        selected = isFogEnabled,
                        onClick = onToggleFog,
                        label = { Text(if (isFogEnabled) "Niebla ACTIVA" else "Niebla INACTIVA", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                if (isFogEnabled) Icons.Default.Cloud else Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    // Pincel Revelar / Ocultar
                    FilterChip(
                        selected = fogBrushMode == FogBrushMode.REVEAL,
                        onClick = { onFogBrushModeSelected(FogBrushMode.REVEAL) },
                        label = { Text("Revelar", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = fogBrushMode == FogBrushMode.HIDE,
                        onClick = { onFogBrushModeSelected(FogBrushMode.HIDE) },
                        label = { Text("Ocultar", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    // Vista DM (semitransparente) vs Vista Jugador (100% opaca)
                    FilterChip(
                        selected = isFogDmView,
                        onClick = onToggleFogDmView,
                        label = { Text(if (isFogDmView) "Vista DM (70%)" else "Vista Jugador", fontSize = 11.sp) }
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    // Botones de acción masiva
                    FilledTonalButton(
                        onClick = onRevealAllFog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Revelar Todo", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onResetFog,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Cubrir Todo", fontSize = 11.sp)
                    }
                }
            }
        }

        // Sub-barra contextual de opciones de Área de Efecto (AoE)
        AnimatedVisibility(
            visible = activeTool == TacticalTool.AOE,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Formas
                    FilterChip(
                        selected = activeAoeShape == TacticalMeasureShape.CIRCLE,
                        onClick = { onAoeShapeSelected(TacticalMeasureShape.CIRCLE) },
                        label = { Text("Círculo", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Circle, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeAoeShape == TacticalMeasureShape.CONE,
                        onClick = { onAoeShapeSelected(TacticalMeasureShape.CONE) },
                        label = { Text("Cono", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.ChangeHistory, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeAoeShape == TacticalMeasureShape.LINE,
                        onClick = { onAoeShapeSelected(TacticalMeasureShape.LINE) },
                        label = { Text("Línea", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.HorizontalRule, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeAoeShape == TacticalMeasureShape.BOX,
                        onClick = { onAoeShapeSelected(TacticalMeasureShape.BOX) },
                        label = { Text("Cubo", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Square, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    // Tamaños rápidos
                    listOf(15, 20, 30, 60).forEach { size ->
                        FilterChip(
                            selected = aoeRadiusFt == size,
                            onClick = { onAoeRadiusSelected(size) },
                            label = { Text("${size}ft", fontSize = 11.sp) }
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    // Botón para fijar plantilla
                    FilledTonalButton(
                        onClick = onPersistAoE,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Fijar", fontSize = 11.sp)
                    }
                }
            }
        }

        // Sub-barra contextual de Terreno Dificultoso y Obstáculos
        AnimatedVisibility(
            visible = activeTool == TacticalTool.TERRAIN,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activeTerrainBrush == TacticalTerrainType.DIFFICULT,
                        onClick = { onTerrainBrushSelected(TacticalTerrainType.DIFFICULT) },
                        label = { Text("Difícil (x2 coste)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Terrain, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeTerrainBrush == TacticalTerrainType.WALL,
                        onClick = { onTerrainBrushSelected(TacticalTerrainType.WALL) },
                        label = { Text("Muro", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeTerrainBrush == TacticalTerrainType.HAZARD,
                        onClick = { onTerrainBrushSelected(TacticalTerrainType.HAZARD) },
                        label = { Text("Peligro", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                    FilterChip(
                        selected = activeTerrainBrush == TacticalTerrainType.NORMAL,
                        onClick = { onTerrainBrushSelected(TacticalTerrainType.NORMAL) },
                        label = { Text("Borrar", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )

                    VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 2.dp))

                    OutlinedButton(
                        onClick = onClearAllTerrain,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Limpiar Terreno", fontSize = 11.sp)
                    }
                }
            }
        }

        // Sub-barra contextual de Planificación de Ruta
        AnimatedVisibility(
            visible = activeTool == TacticalTool.ROUTE,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val speedText = if (selectedTokenSpeedFt != null) " / ${selectedTokenSpeedFt}ft" else ""
                    val isOverBudget = selectedTokenSpeedFt != null && currentRouteDistanceFt > selectedTokenSpeedFt
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isOverBudget) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Ruta: ${currentRouteDistanceFt}ft$speedText",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOverBudget) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onRemoveLastRouteWaypoint,
                        enabled = routeWaypointsCount > 1,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Deshacer", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = onCommitRouteMovement,
                        enabled = routeWaypointsCount > 1,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Mover Token", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onClearRoute,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Cancelar", fontSize = 11.sp)
                    }
                }
            }
        }

        // Barra de herramientas principal
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mover / Navegar
                IconButton(
                    onClick = { onToolSelected(TacticalTool.NAVIGATE) },
                    colors = if (activeTool == TacticalTool.NAVIGATE) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.PanTool, contentDescription = "Pan/Zoom")
                }

                // Seleccionar y mover tokens
                IconButton(
                    onClick = { onToolSelected(TacticalTool.SELECT) },
                    colors = if (activeTool == TacticalTool.SELECT) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = "Seleccionar Token")
                }

                // Regla de Medición
                IconButton(
                    onClick = { onToolSelected(TacticalTool.MEASURE) },
                    colors = if (activeTool == TacticalTool.MEASURE) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = "Medir Distancia")
                }

                // Plantilla de Área de Efecto (AoE)
                IconButton(
                    onClick = { onToolSelected(TacticalTool.AOE) },
                    colors = if (activeTool == TacticalTool.AOE) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = "Plantilla AoE")
                }

                // Pines de Dungeon / Room Keying
                IconButton(
                    onClick = { onToolSelected(TacticalTool.PIN) },
                    colors = if (activeTool == TacticalTool.PIN) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Pines de Dungeon")
                }

                // Niebla de Guerra (Fog of War)
                IconButton(
                    onClick = { onToolSelected(TacticalTool.FOG) },
                    colors = if (activeTool == TacticalTool.FOG) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = "Niebla de Guerra")
                }

                // Terreno Dificultoso y Obstáculos
                IconButton(
                    onClick = { onToolSelected(TacticalTool.TERRAIN) },
                    colors = if (activeTool == TacticalTool.TERRAIN) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.Terrain, contentDescription = "Pincel de Terreno")
                }

                // Planificar Ruta de Movimiento
                IconButton(
                    onClick = { onToolSelected(TacticalTool.ROUTE) },
                    colors = if (activeTool == TacticalTool.ROUTE) {
                        IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    } else IconButtonDefaults.iconButtonColors()
                ) {
                    Icon(Icons.Default.AltRoute, contentDescription = "Planificar Ruta")
                }

                VerticalDivider(modifier = Modifier.height(28.dp).padding(horizontal = 2.dp))

                // Añadir Token
                IconButton(
                    onClick = onAddTokenClicked,
                    colors = IconButtonDefaults.filledTonalIconButtonColors()
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Añadir Token")
                }

                // Calibrar Grilla
                IconButton(
                    onClick = onCalibrateGridClicked
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "Calibrar Grilla")
                }

                // Opciones del Mapa / Cambiar imagen
                IconButton(
                    onClick = onOpenMapSettingsClicked
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Fondo de Mapa")
                }
            }
        }
    }
}
