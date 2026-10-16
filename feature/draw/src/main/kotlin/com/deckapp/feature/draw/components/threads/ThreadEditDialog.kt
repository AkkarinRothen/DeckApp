package com.deckapp.feature.draw.components.threads

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread

/**
 * Diálogo modal para crear o editar un Hilo de Trama (Mythic Thread).
 */
@Composable
fun ThreadEditDialog(
    thread: MythicThread?,
    onDismiss: () -> Unit,
    onSave: (id: Long, description: String) -> Unit
) {
    var description by remember(thread) { mutableStateOf(thread?.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (thread == null || thread.id == 0L) "Nuevo Hilo de Trama" else "Editar Hilo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Los Hilos representan metas, misterios, peligros abiertos o tramas activas en tu aventura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción del Hilo") },
                    placeholder = { Text("Ej. Encontrar la cura para el veneno, Escapar del calabozo") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = description.trim()
                    if (trimmed.isNotBlank()) {
                        onSave(thread?.id ?: 0L, trimmed)
                    }
                },
                enabled = description.isNotBlank()
            ) {
                Text("Guardar Hilo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Diálogo modal para crear o editar un Personaje de la Lista Mítica (Mythic Character).
 */
@Composable
fun MythicCharacterEditDialog(
    character: MythicCharacter?,
    onDismiss: () -> Unit,
    onSave: (id: Long, name: String, notes: String) -> Unit
) {
    var name by remember(character) { mutableStateOf(character?.name.orEmpty()) }
    var notes by remember(character) { mutableStateOf(character?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (character == null || character.id == 0L) "Nuevo PNJ / Amenaza" else "Editar Personaje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del PNJ / Criatura") },
                    placeholder = { Text("Ej. Capitán Vane, El Archimago, Manada de Huargos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas o Rasgos Clave") },
                    placeholder = { Text("Motivación, actitud, relación con los PJs...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = name.trim()
                    if (trimmedName.isNotBlank()) {
                        onSave(character?.id ?: 0L, trimmedName, notes.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Guardar PNJ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
