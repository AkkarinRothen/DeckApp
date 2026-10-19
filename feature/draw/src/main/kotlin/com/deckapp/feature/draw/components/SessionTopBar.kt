package com.deckapp.feature.draw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.MythicSession
import com.deckapp.core.model.Session
import com.deckapp.core.ui.icons.DeckIcons
import com.deckapp.feature.draw.SessionViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionTopBar(
    session: Session?,
    sessionElapsedMinutes: Long,
    canUndo: Boolean,
    mainViewMode: SessionViewMode,
    linkedMythicSession: MythicSession?,
    onSessionEnd: () -> Unit,
    onUndo: () -> Unit,
    onSetMainViewMode: (SessionViewMode) -> Unit,
    onOpenSceneFraming: () -> Unit,
    onOpenChoirDialog: () -> Unit,
    onOpenIncursionDialog: () -> Unit,
    onOpenChoirProcedures: () -> Unit,
    onOpenStoryEngineGuide: () -> Unit = {},
    onOpenMicrosettingBuilder: () -> Unit = {},
    onOpenSpotlight: () -> Unit,
    onShowDiceRoller: () -> Unit,
    onHistoryClick: () -> Unit,
    onOpenSessionConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showChoirButtons = session?.gameSystems?.any { it.contains("Choir", ignoreCase = true) } == true
    val showStoryEngineButtons = session?.gameSystems?.any { it.contains("Story Engine", ignoreCase = true) || it.contains("Deck of Worlds", ignoreCase = true) } == true
    var showOverflowMenu by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session?.name ?: "Sesión",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    val elapsedText = remember(sessionElapsedMinutes) {
                        val h = sessionElapsedMinutes / 60
                        val m = sessionElapsedMinutes % 60
                        if (h > 0) "${h}h ${m}m" else "${m}m"
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = elapsedText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (mainViewMode == SessionViewMode.MYTHIC_LOG) {
                    linkedMythicSession?.let { mythic ->
                        Text(
                            "Mythic: ${mythic.name} (Caos ${mythic.chaosFactor})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onSessionEnd) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Finalizar o salir de la sesión")
            }
        },
        actions = {
            // Deshacer acción si es aplicable
            AnimatedVisibility(visible = canUndo) {
                IconButton(
                    onClick = onUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isCompactScreen = configuration.screenWidthDp < 500
            var showModeMenu by remember { mutableStateOf(false) }

            if (isCompactScreen) {
                Box {
                    FilledTonalButton(
                        onClick = { showModeMenu = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 4.dp)
                    ) {
                        val (icon, label) = when (mainViewMode) {
                            SessionViewMode.BOARD -> DeckIcons.Board to "Mesa"
                            SessionViewMode.TACTICAL -> DeckIcons.Tactical to "Táctico"
                            SessionViewMode.MAP -> DeckIcons.Map to "Mapa"
                            SessionViewMode.MYTHIC_LOG -> DeckIcons.Journal to "Mythic"
                        }
                        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    DropdownMenu(
                        expanded = showModeMenu,
                        onDismissRequest = { showModeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mesa de Juego") },
                            leadingIcon = { Icon(DeckIcons.Board, contentDescription = null) },
                            trailingIcon = if (mainViewMode == SessionViewMode.BOARD) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showModeMenu = false
                                onSetMainViewMode(SessionViewMode.BOARD)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mapa Táctico") },
                            leadingIcon = { Icon(DeckIcons.Tactical, contentDescription = null) },
                            trailingIcon = if (mainViewMode == SessionViewMode.TACTICAL) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showModeMenu = false
                                onSetMainViewMode(SessionViewMode.TACTICAL)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hexploración") },
                            leadingIcon = { Icon(DeckIcons.Map, contentDescription = null) },
                            trailingIcon = if (mainViewMode == SessionViewMode.MAP) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                            } else null,
                            onClick = {
                                showModeMenu = false
                                onSetMainViewMode(SessionViewMode.MAP)
                            }
                        )
                        if (linkedMythicSession != null) {
                            DropdownMenuItem(
                                text = { Text("Oráculo Mythic") },
                                leadingIcon = { Icon(DeckIcons.Journal, contentDescription = null) },
                                trailingIcon = if (mainViewMode == SessionViewMode.MYTHIC_LOG) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                } else null,
                                onClick = {
                                    showModeMenu = false
                                    onSetMainViewMode(SessionViewMode.MYTHIC_LOG)
                                }
                            )
                        }
                    }
                }
            } else {
                // Switcher de 4 iconos para pantallas amplias y horizontal
                Row(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSetMainViewMode(SessionViewMode.BOARD) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (mainViewMode == SessionViewMode.BOARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Icon(DeckIcons.Board, contentDescription = "Mesa de Juego", modifier = Modifier.size(18.dp)) }

                    IconButton(
                        onClick = { onSetMainViewMode(SessionViewMode.TACTICAL) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (mainViewMode == SessionViewMode.TACTICAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Icon(DeckIcons.Tactical, contentDescription = "Mapa Táctico", modifier = Modifier.size(18.dp)) }

                    IconButton(
                        onClick = { onSetMainViewMode(SessionViewMode.MAP) },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (mainViewMode == SessionViewMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Icon(DeckIcons.Map, contentDescription = "Hexploración", modifier = Modifier.size(18.dp)) }

                    IconButton(
                        onClick = { onSetMainViewMode(SessionViewMode.MYTHIC_LOG) },
                        enabled = linkedMythicSession != null,
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (mainViewMode == SessionViewMode.MYTHIC_LOG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) { Icon(DeckIcons.Journal, contentDescription = "Oráculo Mythic", modifier = Modifier.size(18.dp)) }
                }
            }

            // Búsqueda Rápida Spotlight
            IconButton(
                onClick = onOpenSpotlight,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(DeckIcons.Search, contentDescription = "Búsqueda Rápida Spotlight", modifier = Modifier.size(20.dp))
            }

            // Lanzador de Dados Rápido
            IconButton(
                onClick = onShowDiceRoller,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(DeckIcons.Dice, contentDescription = "Lanzador de dados", modifier = Modifier.size(20.dp))
            }

            // Menú de Más Opciones (Secundarios y Sistema)
            Box {
                IconButton(
                    onClick = { showOverflowMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", modifier = Modifier.size(20.dp))
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Encuadre de Escena") },
                        leadingIcon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenSceneFraming()
                        }
                    )

                    if (showChoirButtons) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Choir: Procedimientos") },
                            leadingIcon = { Icon(DeckIcons.Guide, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenChoirProcedures()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Choir: Inquebrantable") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFD4AF37)) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenChoirDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Choir: Incursión") },
                            leadingIcon = { Icon(DeckIcons.Map, contentDescription = null, tint = Color(0xFFD4AF37)) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenIncursionDialog()
                            }
                        )
                        HorizontalDivider()
                    }

                    if (showStoryEngineButtons) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Story Engine: Guía de Pasos") },
                            leadingIcon = { Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenStoryEngineGuide()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Story Engine: Creador Microsettings") },
                            leadingIcon = { Icon(DeckIcons.Board, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showOverflowMenu = false
                                onOpenMicrosettingBuilder()
                            }
                        )
                        HorizontalDivider()
                    }

                    DropdownMenuItem(
                        text = { Text("Historial de Sesión") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        enabled = session != null,
                        onClick = {
                            showOverflowMenu = false
                            onHistoryClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ajustes de Sesión") },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        onClick = {
                            showOverflowMenu = false
                            onOpenSessionConfig()
                        }
                    )
                }
            }
        }
    )
}
