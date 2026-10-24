package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import kotlinx.coroutines.launch

/**
 * Barra de herramientas superior flotante organizada por categorías para el Tapete (Playmat).
 * Agrupa las opciones en:
 * 1. 🌍 Mundo & Entorno (Mesas, Fondos, Moldes, Cuadrícula, Regla, Centrar)
 * 2. 🎭 Entidades (Cartas, Tokens, Relojes, Notas Adhesivas)
 * 3. 🎲 Mecánicas (Dados, Bitácora/Notas, Snapshot)
 * Incluye accesos directos de 1-toque para Robo rápido, Bitácora y Modo Zen.
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
    onManageDecks: (() -> Unit)? = null,
    onOpenTokenPicker: (() -> Unit)? = null,
    onOpenClockCreator: (() -> Unit)? = null,
    onOpenTacticalSettings: (() -> Unit)? = null,
    onToggleRuler: (() -> Unit)? = null,
    isRulerActive: Boolean = false,
    onToggleDiceRoller: (() -> Unit)? = null,
    onExportSnapshot: (() -> Unit)? = null,
    onToggleZenMode: (() -> Unit)? = null,
    onToggleSessionDrawer: (() -> Unit)? = null,
    isSessionDrawerOpen: Boolean = false,
    onManageGroups: (() -> Unit)? = null,
    isGroupSelectionMode: Boolean = false,
    selectedGroupMemberCount: Int = 0,
    onToggleGroupSelection: (() -> Unit)? = null,
    onCreateSelectedGroup: (() -> Unit)? = null,
    onToggleMinimap: (() -> Unit)? = null,
    isMinimapOpen: Boolean = false,
    onSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val toolbarScrollState = rememberScrollState()
    val toolbarScope = rememberCoroutineScope()
    var showSurfaceMenu by remember { mutableStateOf(false) }
    var showWorldMenu by remember { mutableStateOf(false) }
    var showEntitiesMenu by remember { mutableStateOf(false) }
    var showMechanicsMenu by remember { mutableStateOf(false) }
    var showNewSurfaceDialog by remember { mutableStateOf(false) }
    var newSurfaceName by remember { mutableStateOf("") }

    // Submenús dentro de Mundo
    var showThemeSubmenu by remember { mutableStateOf(false) }
    var showTemplateSubmenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.padding(8.dp).widthIn(max = 760.dp).fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(toolbarScrollState)
                .padding(start = 8.dp, end = 38.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (onSearch != null) {
                FilledTonalIconButton(onClick = onSearch, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar piezas", modifier = Modifier.size(16.dp))
                }
            }
            if (onManageGroups != null) {
                FilledTonalIconButton(onClick = onManageGroups, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FolderCopy, contentDescription = "Grupos", modifier = Modifier.size(16.dp))
                }
            }
            if (onToggleMinimap != null) {
                FilledTonalIconButton(onClick = onToggleMinimap, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Explore, contentDescription = if (isMinimapOpen) "Cerrar vista general" else "Abrir vista general", modifier = Modifier.size(16.dp))
                }
            }
            if (onToggleGroupSelection != null) {
                FilledTonalButton(
                    onClick = onToggleGroupSelection,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(if (isGroupSelectionMode) Icons.Default.Close else Icons.Default.SelectAll, null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isGroupSelectionMode) "Cancelar" else "Seleccionar", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isGroupSelectionMode && onCreateSelectedGroup != null) {
                FilledTonalButton(
                    onClick = onCreateSelectedGroup,
                    enabled = selectedGroupMemberCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) { Text("Agrupar ($selectedGroupMemberCount)", style = MaterialTheme.typography.labelSmall) }
            }
            // --- Selector de Mesa Activa ---
            Box {
                FilledTonalButton(
                    onClick = { showSurfaceMenu = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = activeSurface.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
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

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // --- 🌍 CATEGORÍA 1: MUNDO & ENTORNO ---
            Box {
                OutlinedButton(
                    onClick = { showWorldMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (activeSurface.gridType != PlaymatGridType.NONE || activeSurface.customImageUri != null) {
                            Color(0xFF00E5FF)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mundo", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showWorldMenu,
                    onDismissRequest = {
                        showWorldMenu = false
                        showThemeSubmenu = false
                        showTemplateSubmenu = false
                    }
                ) {
                    Text(
                        text = "🌍 Entorno y Atmósfera",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    // Cuadrícula y Battlemaps
                    if (onOpenTacticalSettings != null) {
                        DropdownMenuItem(
                            text = { Text("Cuadrícula y Battlemap") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.GridOn,
                                    contentDescription = null,
                                    tint = if (activeSurface.gridType != PlaymatGridType.NONE) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showWorldMenu = false
                                onOpenTacticalSettings()
                            }
                        )
                    }

                    // Fondo del Tapete
                    DropdownMenuItem(
                        text = { Text("Fondo del Tapete (${activeSurface.backgroundTheme.name.replace('_', ' ')})") },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { showThemeSubmenu = !showThemeSubmenu }
                    )
                    if (showThemeSubmenu) {
                        PlaymatTheme.entries.forEach { theme ->
                            val (label, color) = when (theme) {
                                PlaymatTheme.FELT_GREEN -> "Fieltro Verde" to Color(0xFF1B4D3E)
                                PlaymatTheme.FELT_RED -> "Fieltro Rojo" to Color(0xFF6B1D2F)
                                PlaymatTheme.RUSTIC_WOOD -> "Madera Rústica" to Color(0xFF3E2723)
                                PlaymatTheme.PARCHMENT_LEATHER -> "Pergamino Antiguo" to Color(0xFFD7CCC8)
                                PlaymatTheme.DARK_MINIMAL -> "Dark Mode Minimal" to Color(0xFF1E1E24)
                            }
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 16.dp),
                                text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = {
                                    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                },
                                trailingIcon = if (activeSurface.backgroundTheme == theme) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    showWorldMenu = false
                                    showThemeSubmenu = false
                                    onSetTheme(theme)
                                }
                            )
                        }
                    }

                    // Moldes y Plantillas
                    DropdownMenuItem(
                        text = { Text("Molde Temático") },
                        leadingIcon = { Icon(Icons.Default.GridGoldenratio, contentDescription = null) },
                        trailingIcon = { Icon(Icons.Default.ChevronRight, null) },
                        onClick = { showTemplateSubmenu = !showTemplateSubmenu }
                    )
                    if (showTemplateSubmenu) {
                        PlaymatTemplateType.entries.forEach { template ->
                            val label = when (template) {
                                PlaymatTemplateType.FREEFORM -> "Mesa Libre"
                                PlaymatTemplateType.DECK_OF_WORLDS_MICROSETTING -> "Deck of Worlds"
                                PlaymatTemplateType.STORY_ENGINE_PREMISE -> "Story Engine"
                                PlaymatTemplateType.TAROT_THREE_CARDS -> "Tarot 3 Cartas"
                                PlaymatTemplateType.ORACLE_CROSS -> "Oráculo en Cruz"
                            }
                            DropdownMenuItem(
                                modifier = Modifier.padding(start = 16.dp),
                                text = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                trailingIcon = if (activeSurface.template == template) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    showWorldMenu = false
                                    showTemplateSubmenu = false
                                    onSetTemplate(template)
                                }
                            )
                        }
                    }

                    // Regla de Medición
                    if (onToggleRuler != null) {
                        DropdownMenuItem(
                            text = { Text(if (isRulerActive) "Desactivar Regla" else "Regla de Distancia") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Straighten,
                                    null,
                                    tint = if (isRulerActive) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                showWorldMenu = false
                                onToggleRuler()
                            }
                        )
                    }

                    // Centrar Cámara
                    DropdownMenuItem(
                        text = { Text("Centrar Cámara") },
                        leadingIcon = { Icon(Icons.Default.CenterFocusStrong, null) },
                        onClick = {
                            showWorldMenu = false
                            onResetCamera()
                        }
                    )
                }
            }

            // --- 🎭 CATEGORÍA 2: ENTIDADES DEL TAPETE ---
            Box {
                OutlinedButton(
                    onClick = { showEntitiesMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Entidades", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showEntitiesMenu,
                    onDismissRequest = { showEntitiesMenu = false }
                ) {
                    Text(
                        text = "🎭 Elementos en Mesa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    DropdownMenuItem(
                        text = { Text("Robar Carta al Tapete") },
                        leadingIcon = { Icon(Icons.Default.AddCard, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            showEntitiesMenu = false
                            onOpenQuickDraw()
                        }
                    )

                    if (onManageDecks != null) {
                        DropdownMenuItem(
                            text = { Text("Mazos visibles en esta mesa") },
                            leadingIcon = { Icon(Icons.Default.Layers, null) },
                            onClick = {
                                showEntitiesMenu = false
                                onManageDecks()
                            }
                        )
                    }

                    if (onOpenTokenPicker != null) {
                        DropdownMenuItem(
                            text = { Text("Añadir Token (PJ / PNJ)") },
                            leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                showEntitiesMenu = false
                                onOpenTokenPicker()
                            }
                        )
                    }

                    if (onOpenClockCreator != null) {
                        DropdownMenuItem(
                            text = { Text("Añadir Reloj de Tensión") },
                            leadingIcon = { Icon(Icons.Default.HourglassTop, null, tint = Color(0xFFFFB300)) },
                            onClick = {
                                showEntitiesMenu = false
                                onOpenClockCreator()
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Añadir Nota Adhesiva") },
                        leadingIcon = { Icon(Icons.Default.NoteAdd, null) },
                        onClick = {
                            showEntitiesMenu = false
                            onAddStickyNote()
                        }
                    )
                }
            }

            // --- 🎲 CATEGORÍA 3: MECÁNICAS & MESA ---
            Box {
                OutlinedButton(
                    onClick = { showMechanicsMenu = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mesa", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                }

                DropdownMenu(
                    expanded = showMechanicsMenu,
                    onDismissRequest = { showMechanicsMenu = false }
                ) {
                    Text(
                        text = "🎲 Mecánicas de Juego",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    if (onToggleDiceRoller != null) {
                        DropdownMenuItem(
                            text = { Text("Lanzador de Dados") },
                            leadingIcon = { Icon(Icons.Default.Casino, null, tint = MaterialTheme.colorScheme.tertiary) },
                            onClick = {
                                showMechanicsMenu = false
                                onToggleDiceRoller()
                            }
                        )
                    }

                    if (onToggleSessionDrawer != null) {
                        DropdownMenuItem(
                            text = { Text(if (isSessionDrawerOpen) "Cerrar Bitácora / Notas" else "Bitácora en Vivo & Notas") },
                            leadingIcon = { Icon(Icons.Default.AutoStories, null, tint = if (isSessionDrawerOpen) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMechanicsMenu = false
                                onToggleSessionDrawer()
                            }
                        )
                    }

                    if (onExportSnapshot != null) {
                        DropdownMenuItem(
                            text = { Text("Exportar Captura del Tapete") },
                            leadingIcon = { Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMechanicsMenu = false
                                onExportSnapshot()
                            }
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // --- ACCIONES DIRECTAS DE UN TOQUE (FAST ACTIONS) ---

            // 1. Robar Directo
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

            // 2. Bitácora / Notas de Mesa
            if (onToggleSessionDrawer != null) {
                IconButton(
                    onClick = onToggleSessionDrawer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = "Bitácora y Notas",
                        modifier = Modifier.size(16.dp),
                        tint = if (isSessionDrawerOpen) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 3. Modo Zen Inmersivo
            if (onToggleZenMode != null) {
                IconButton(
                    onClick = onToggleZenMode,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Spa,
                        contentDescription = "Modo Zen",
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF81C784)
                    )
                }
            }
        }
        if (toolbarScrollState.maxValue > 0) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 3.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                IconButton(
                    onClick = {
                        toolbarScope.launch {
                            val destination = if (toolbarScrollState.value == 0) toolbarScrollState.maxValue else 0
                            toolbarScrollState.animateScrollTo(destination)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (toolbarScrollState.value == 0) Icons.Default.MoreHoriz else Icons.Default.FirstPage,
                        contentDescription = if (toolbarScrollState.value == 0) "Ver más herramientas" else "Volver al inicio de herramientas",
                        modifier = Modifier.size(18.dp)
                    )
                }
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
