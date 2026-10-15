package com.deckapp.feature.mythic.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread

@Composable
fun MythicContextTab(
    characters: List<MythicCharacter>,
    threads: List<MythicThread>,
    onAddCharacter: (String) -> Unit,
    onDeleteCharacter: (Long) -> Unit,
    onUpdateCharacterNotes: (Long, String) -> Unit,
    onSendCharacterToNotes: (MythicCharacter) -> Unit,
    onAddThread: (String) -> Unit,
    onUpdateThreadDescription: (Long, String) -> Unit,
    onToggleThread: (Long, Boolean) -> Unit,
    onDeleteThread: (Long) -> Unit,
    onSendThreadToNotes: (MythicThread) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddCharacterDialog by remember { mutableStateOf(false) }
    var showAddThreadDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HILOS / TRAMAS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HILOS Y TRAMAS (${threads.count { !it.isResolved }}/${threads.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { showAddThreadDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Hilo", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (threads.isEmpty()) {
            item {
                Text(
                    text = "No hay tramas registradas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(threads, key = { it.id }) { thread ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (thread.isResolved)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = thread.isResolved,
                            onCheckedChange = { onToggleThread(thread.id, it) }
                        )
                        Text(
                            text = thread.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (thread.isResolved) TextDecoration.LineThrough else null
                            ),
                            color = if (thread.isResolved)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { onSendThreadToNotes(thread) }) {
                            Icon(Icons.Default.PostAdd, contentDescription = "Enviar a notas", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { onDeleteThread(thread.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // --- PERSONAJES Y FACCIONES ---
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONAJES Y FACCIONES (${characters.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = { showAddCharacterDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir Personaje", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (characters.isEmpty()) {
            item {
                Text(
                    text = "No hay personajes en la lista de Mythic",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(characters, key = { it.id }) { character ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onSendCharacterToNotes(character) }) {
                                Icon(Icons.Default.PostAdd, contentDescription = "Enviar a notas", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onDeleteCharacter(character.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        if (character.notes.isNotBlank() || expanded) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = character.notes.ifBlank { "Sin notas adicionales" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showAddThreadDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddThreadDialog = false },
            title = { Text("Nuevo Hilo / Trama") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Descripción del hilo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onAddThread(text.trim())
                            showAddThreadDialog = false
                        }
                    }
                ) {
                    Text("AÑADIR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddThreadDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showAddCharacterDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCharacterDialog = false },
            title = { Text("Nuevo Personaje / Facción") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (text.isNotBlank()) {
                            onAddCharacter(text.trim())
                            showAddCharacterDialog = false
                        }
                    }
                ) {
                    Text("AÑADIR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCharacterDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }
}
