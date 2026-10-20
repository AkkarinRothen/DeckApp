package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*

/**
 * Barra de herramientas superior flotante para el Tapete de Cartas (Playmat).
 */
@Composable
fun PlaymatToolbar(
    activeSurface: PlaymatSurface,
    allSurfaces: List<PlaymatSurface>,
    onSelectSurface: (String) -> Unit,
    onCreateSurface: (String, PlaymatTheme, PlaymatTemplateType) -> Unit,
    onDeleteSurface: (String) -> Unit,
    onSetTheme: (PlaymatTheme) -> Unit,
    onSetTemplate: (PlaymatTemplateType) -> Unit,
    onResetCamera: () -> Unit,
    onAddStickyNote: () -> Unit,
    onOpenQuickDraw: () -> Unit,
    onOpenTokenPicker: (() -> Unit)? = null,
    onToggleDiceRoller: (() -> Unit)? = null,
    onExportSnapshot: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showSurfaceMenu by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showTemplateMenu by remember { mutableStateOf(false) }
    var showNewSurfaceDialog by remember { mutableStateOf(false) }
    var newSurfaceName by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Selector de Superficie / Mesa
            Box {
                FilledTonalButton(
                    onClick = { showSurfaceMenu = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(activeSurface.name, style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showSurfaceMenu,
                    onDismissRequest = { showSurfaceMenu = false }
                ) {
                    Text(
                        text = "Mesas en Sesión",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    allSurfaces.forEach { surface ->
                        DropdownMenuItem(
                            text = { Text(surface.name) },
                            leadingIcon = {
                                if (surface.id == activeSurface.id) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.Dashboard, null, modifier = Modifier.size(16.dp))
                                }
                            },
                            trailingIcon = if (allSurfaces.size > 1) {
                                {
                                    IconButton(onClick = { onDeleteSurface(surface.id) }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            } else null,
                            onClick = {
                                showSurfaceMenu = false
                                onSelectSurface(surface.id)
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text("+ Nueva Mesa") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            showSurfaceMenu = false
                            newSurfaceName = ""
                            showNewSurfaceDialog = true
                        }
                    )
                }
            }

            // Selector de Plantilla Temática
            Box {
                IconButton(
                    onClick = { showTemplateMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridGoldenratio,
                        contentDescription = "Plantilla",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showTemplateMenu,
                    onDismissRequest = { showTemplateMenu = false }
                ) {
                    Text(
                        text = "Moldes y Disposición",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    PlaymatTemplateType.entries.forEach { template ->
                        val label = when (template) {
                            PlaymatTemplateType.FREEFORM -> "Mesa Libre (Sin molde)"
                            PlaymatTemplateType.DECK_OF_WORLDS_MICROSETTING -> "Deck of Worlds: Microsetting (1+5)"
                            PlaymatTemplateType.STORY_ENGINE_PREMISE -> "Story Engine: Premisa (4 cartas)"
                            PlaymatTemplateType.TAROT_THREE_CARDS -> "Tirada 3 Cartas (Pasado/Presente/Futuro)"
                            PlaymatTemplateType.ORACLE_CROSS -> "Oráculo en Cruz (5 cartas)"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            trailingIcon = if (activeSurface.template == template) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showTemplateMenu = false
                                onSetTemplate(template)
                            }
                        )
                    }
                }
            }

            // Selector de Fondo / Textura
            Box {
                IconButton(
                    onClick = { showThemeMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Fondo",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    Text(
                        text = "Fondo del Tapete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    PlaymatTheme.entries.forEach { theme ->
                        val (label, color) = when (theme) {
                            PlaymatTheme.FELT_GREEN -> "Fieltro Verde" to Color(0xFF1B4D3E)
                            PlaymatTheme.FELT_RED -> "Fieltro Rojo" to Color(0xFF6B1D2F)
                            PlaymatTheme.RUSTIC_WOOD -> "Madera Rústica" to Color(0xFF3E2723)
                            PlaymatTheme.PARCHMENT_LEATHER -> "Pergamino Antiguo" to Color(0xFFD7CCC8)
                            PlaymatTheme.DARK_MINIMAL -> "Dark Mode Minimal" to Color(0xFF1E1E24)
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(color, CircleShape)
                                )
                            },
                            trailingIcon = if (activeSurface.backgroundTheme == theme) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showThemeMenu = false
                                onSetTheme(theme)
                            }
                        )
                    }
                }
            }

            // Botón Añadir Nota Adhesiva
            IconButton(
                onClick = onAddStickyNote,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "Añadir Nota",
                    modifier = Modifier.size(16.dp)
                )
            }

            // Botón Robar Directo al Tapete
            IconButton(
                onClick = onOpenQuickDraw,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCard,
                    contentDescription = "Robar Carta",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Botón Añadir Token de Personaje
            if (onOpenTokenPicker != null) {
                IconButton(
                    onClick = onOpenTokenPicker,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Añadir Token",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Botón Lanzador de Dados en el Tapete
            if (onToggleDiceRoller != null) {
                IconButton(
                    onClick = onToggleDiceRoller,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Lanzar Dados",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Reset Cámara
            IconButton(
                onClick = onResetCamera,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Centrar Mesa",
                    modifier = Modifier.size(16.dp)
                )
            }

            // Exportar Captura PNG
            if (onExportSnapshot != null) {
                IconButton(
                    onClick = onExportSnapshot,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Compartir Tapete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    if (showNewSurfaceDialog) {
        AlertDialog(
            onDismissRequest = { showNewSurfaceDialog = false },
            title = { Text("Nueva Mesa de Juego") },
            text = {
                OutlinedTextField(
                    value = newSurfaceName,
                    onValueChange = { newSurfaceName = it },
                    label = { Text("Nombre de la mesa") },
                    placeholder = { Text("Ej: Mazmorra Nivel 1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNewSurfaceDialog = false
                        onCreateSurface(
                            newSurfaceName,
                            activeSurface.backgroundTheme,
                            PlaymatTemplateType.FREEFORM
                        )
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSurfaceDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
