package com.deckapp.feature.draw.components.threads

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread

enum class ThreadsOverlayTab {
    THREADS, CHARACTERS
}

/**
 * Overlay flotante colapsable de Hilos de Trama y Lista de PNJs (Threats & Threads Tracker).
 * Permite monitorear misterios abiertos, misiones y amenazas activas, así como sortearlos con d100
 * para eventos aleatorios de Mythic GME 2e desde cualquier vista de la mesa.
 */
@Composable
fun SessionThreadsOverlay(
    threads: List<MythicThread>,
    characters: List<MythicCharacter>,
    isExpanded: Boolean,
    selectedTab: ThreadsOverlayTab,
    lastRandomPick: String?,
    onToggleExpanded: () -> Unit,
    onSelectTab: (ThreadsOverlayTab) -> Unit,
    onRollRandom: () -> Unit,
    onClearRandomPick: () -> Unit,
    onCopyRandomPickToDiary: () -> Unit,
    onToggleThreadResolved: (threadId: Long, isResolved: Boolean) -> Unit,
    onAddThread: () -> Unit,
    onEditThread: (MythicThread) -> Unit,
    onDeleteThread: (threadId: Long) -> Unit,
    onAddCharacter: () -> Unit,
    onEditCharacter: (MythicCharacter) -> Unit,
    onDeleteCharacter: (characterId: Long) -> Unit,
    onCopySummaryToDiary: () -> Unit,
    onSendItemToNotes: (String) -> Unit,
    showHeaderPill: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val activeThreadsCount = remember(threads) { threads.count { !it.isResolved } }
    val charactersCount = remember(characters) { characters.size }

    Column(
        modifier = modifier.widthIn(max = 330.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // ── Pastilla de Activación (Collapsed / Header Pill) ──────────────────
        if (showHeaderPill) {
            Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xEB161920),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleExpanded()
                }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = "Hilos & PNJs",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Badge de conteo combinado
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$activeThreadsCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

        // ── Panel Flotante Expandido ──────────────────────────────────────────
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = tween(220)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it / 3 }, animationSpec = tween(180)) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF2161920),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cabecera del Panel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Hilos & Amenazas (Mythic)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedTab == ThreadsOverlayTab.THREADS) onAddThread() else onAddCharacter()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Añadir",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleExpanded,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Selector de Pestañas y Botón de Sorteo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedTab == ThreadsOverlayTab.THREADS,
                                onClick = { onSelectTab(ThreadsOverlayTab.THREADS) },
                                label = { Text("Hilos ($activeThreadsCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                            FilterChip(
                                selected = selectedTab == ThreadsOverlayTab.CHARACTERS,
                                onClick = { onSelectTab(ThreadsOverlayTab.CHARACTERS) },
                                label = { Text("PNJs ($charactersCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            )
                        }

                        // Botón de Sorteo d100
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRollRandom()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sortear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Banner de Resultado Sorteado (si existe)
                    AnimatedVisibility(
                        visible = lastRandomPick != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        lastRandomPick?.let { pickText ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pickText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = onCopyRandomPickToDiary,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = "Copiar al Diario",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = onClearRandomPick,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Descartar",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Lista de Contenido según la pestaña activa
                    if (selectedTab == ThreadsOverlayTab.THREADS) {
                        if (threads.isEmpty()) {
                            EmptyThreadsState(onAdd = onAddThread, label = "No hay hilos registrados. Pulsa '+' para añadir una meta o peligro.")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(threads, key = { it.id }) { thread ->
                                    ThreadItemRow(
                                        thread = thread,
                                        onToggleResolved = { isRes ->
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onToggleThreadResolved(thread.id, isRes)
                                        },
                                        onEdit = { onEditThread(thread) },
                                        onDelete = { onDeleteThread(thread.id) },
                                        onSendToNotes = { onSendItemToNotes("[Hilo Mítico: ${thread.description}]") }
                                    )
                                }
                            }
                        }
                    } else {
                        if (characters.isEmpty()) {
                            EmptyThreadsState(onAdd = onAddCharacter, label = "No hay PNJs registrados en Mythic. Pulsa '+' para añadir aliados o rivales.")
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 340.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(characters, key = { it.id }) { char ->
                                    CharacterItemRow(
                                        character = char,
                                        onEdit = { onEditCharacter(char) },
                                        onDelete = { onDeleteCharacter(char.id) },
                                        onSendToNotes = { onSendItemToNotes("[PNJ Mítico: ${char.name}]\n${char.notes}".trim()) }
                                    )
                                }
                            }
                        }
                    }

                    // Barra Inferior: Resumen al Diario
                    OutlinedButton(
                        onClick = onCopySummaryToDiary,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Registrar Estado en Diario", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadItemRow(
    thread: MythicThread,
    onToggleResolved: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendToNotes: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (thread.isResolved) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (thread.isResolved) Color.Transparent else Color.White.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = thread.isResolved,
                onCheckedChange = onToggleResolved,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = thread.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (thread.isResolved) Color.White.copy(alpha = 0.4f) else Color.White,
                textDecoration = if (thread.isResolved) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar Hilo") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar a Notas") },
                        onClick = {
                            menuExpanded = false
                            onSendToNotes()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterItemRow(
    character: MythicCharacter,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSendToNotes: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (character.notes.isNotBlank()) {
                    Text(
                        text = character.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar PNJ") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Enviar a Notas") },
                        onClick = {
                            menuExpanded = false
                            onSendToNotes()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyThreadsState(onAdd: () -> Unit, label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            OutlinedButton(
                onClick = onAdd,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Crear Primero", fontSize = 11.sp)
            }
        }
    }
}
