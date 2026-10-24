package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.model.state.PlaymatActions

private data class GroupablePlaymatItem(val id: String, val title: String)

@Composable
internal fun PlaymatGroupsDialog(
    surface: PlaymatSurface,
    actions: PlaymatActions,
    deckNames: Map<Long, String>,
    onJumpToGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedGroupId by remember(surface.id) { mutableStateOf(surface.groups.firstOrNull()?.id) }
    val selectedGroup = surface.groups.firstOrNull { it.id == selectedGroupId }
    var groupName by remember(surface.id, selectedGroupId) { mutableStateOf(selectedGroup?.name.orEmpty()) }
    val entries = remember(surface) {
        buildList {
            surface.cards.forEach { add(GroupablePlaymatItem("card:${it.cardId}", "Carta #${it.cardId}")) }
            surface.decks.forEach { deck ->
                val deckName = deckNames[deck.stackId] ?: "Mazo #${deck.stackId}"
                add(GroupablePlaymatItem("deck:${deck.stackId}", deckName))
                if (deck.discardX != null || deck.discardY != null) add(GroupablePlaymatItem("discard:${deck.stackId}", "Descarte · $deckName"))
            }
            surface.tokens.forEach { add(GroupablePlaymatItem("token:${it.id}", "Ficha: ${it.name}")) }
            surface.stickyNotes.forEach { add(GroupablePlaymatItem("note:${it.id}", "Nota: ${it.text.take(30)}")) }
            surface.tableChips.forEach { add(GroupablePlaymatItem("chip:${it.id}", "Resultado: ${it.tableName}")) }
            surface.clocks.forEach { add(GroupablePlaymatItem("clock:${it.id}", "Reloj: ${it.title}")) }
        }
    }
    val entryIds = remember(entries) { entries.mapTo(mutableSetOf()) { it.id } }
    val selectedItems = remember(surface.id, selectedGroupId, surface, entryIds) {
        mutableStateOf(selectedGroup?.memberIds.orEmpty().intersect(entryIds))
    }
    var confirmUngroup by remember { mutableStateOf(false) }

    if (confirmUngroup && selectedGroup != null) {
        AlertDialog(
            onDismissRequest = { confirmUngroup = false },
            title = { Text("Desagrupar ${selectedGroup.name}?") },
            text = { Text("Las piezas permanecerán en el playmat, pero dejarán de moverse juntas.") },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDeletePlaymatGroup(selectedGroup.id)
                    selectedGroupId = null
                    groupName = ""
                    selectedItems.value = emptySet()
                    confirmUngroup = false
                }) { Text("Desagrupar") }
            },
            dismissButton = { TextButton(onClick = { confirmUngroup = false }) { Text("Cancelar") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grupos del playmat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grupos · ${surface.groups.size}", style = MaterialTheme.typography.titleSmall)
                LazyColumn(Modifier.heightIn(max = 120.dp)) {
                    lazyItems(surface.groups, key = { "group:${it.id}" }) { group ->
                        val count = group.memberIds.count { it in entryIds }
                        ListItem(
                            headlineContent = { Text(group.name, maxLines = 1) },
                            supportingContent = {
                                Text(if (count == 0) "Vacío · sin piezas" else "$count ${if (count == 1) "pieza" else "piezas"}")
                            },
                            leadingContent = {
                                Icon(if (group.isHidden) Icons.Default.VisibilityOff else Icons.Default.Folder, null)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onJumpToGroup(group.id) }, enabled = count > 0) {
                                        Icon(Icons.Default.CenterFocusStrong, "Ir al grupo")
                                    }
                                    IconButton(onClick = { actions.onTogglePlaymatGroupVisibility(group.id) }) {
                                        Icon(if (group.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            if (group.isHidden) "Mostrar grupo" else "Ocultar grupo")
                                    }
                                    IconButton(onClick = { actions.onTogglePlaymatGroupLock(group.id) }) {
                                        Icon(if (group.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            if (group.isLocked) "Desbloquear grupo" else "Bloquear grupo")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedGroupId = group.id
                                groupName = group.name
                                selectedItems.value = group.memberIds
                            }
                        )
                    }
                    if (surface.groups.isEmpty()) item { Text("Todavía no hay grupos.", modifier = Modifier.padding(12.dp)) }
                }
                if (surface.groups.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedGroup?.name ?: "Nuevo grupo")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            surface.groups.forEach { group ->
                                DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                    selectedGroupId = group.id
                                    expanded = false
                                })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedGroup != null) {
                        TextButton(onClick = { confirmUngroup = true }) {
                            Icon(Icons.Default.DeleteOutline, null)
                            Text("Desagrupar")
                        }
                    }
                }
                Text("Toca para incluir o quitar piezas del grupo", style = MaterialTheme.typography.labelMedium)
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        val checked = entry.id in selectedItems.value
                        ListItem(
                            headlineContent = { Text(entry.title, maxLines = 1) },
                            leadingContent = { Checkbox(checked, onCheckedChange = { value ->
                                selectedItems.value = if (value) selectedItems.value + entry.id else selectedItems.value - entry.id
                            }) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                    if (entries.isEmpty()) item { Text("Todavía no hay piezas en esta mesa.", modifier = Modifier.padding(12.dp)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = groupName.trim()
                val ids = selectedItems.value
                if (ids.isNotEmpty() && name.isNotEmpty()) {
                    if (selectedGroup != null) {
                        actions.onRenamePlaymatGroup(selectedGroup.id, name)
                        actions.onSetPlaymatGroupMembers(selectedGroup.id, ids)
                    } else actions.onCreatePlaymatGroup(name, ids)
                }
                onDismiss()
            }) { Text(if (selectedGroup == null) "Crear grupo" else "Guardar grupo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
