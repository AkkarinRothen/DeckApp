package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.*

@Composable
internal fun BoardInspirationTray(
    entries: List<BoardInspiration>,
    candidates: List<BoardInspiration>,
    busy: Boolean,
    onCapture: (BoardInspiration) -> Unit,
    onStatus: (String, InspirationStatus) -> Unit,
    onSendToDiary: (String) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf(InspirationStatus.PENDING.name) }
    var inspected by remember { mutableStateOf<BoardInspiration?>(null) }
    val filter = InspirationStatus.valueOf(filterName)
    val savedIds = entries.map { it.id }.toSet()
    OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text("Inspiración (${entries.count { it.status == InspirationStatus.PENDING }})")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { if (!busy) open = false },
            title = { Text("Bandeja de inspiración") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(InspirationStatus.entries) { status ->
                            FilterChip(selected = filter == status, onClick = { filterName = status.name },
                                label = { Text(statusLabel(status)) }, modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                    val visible = entries.filter { it.status == filter }
                    if (visible.isEmpty()) {
                        Text(if (filter == InspirationStatus.PENDING) "Guardá tiradas, consultas o cartas para usarlas en tu historia."
                            else "No hay inspiraciones en este estado.")
                    }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visible, key = { it.id }) { item ->
                            OutlinedCard(onClick = { inspected = item }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(sourceLabel(item.source), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    if (item.sceneTitle.isNotBlank()) Text(item.sceneTitle, style = MaterialTheme.typography.labelSmall)
                                    Text(item.text, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    InspirationActions(item, busy, onStatus, onSendToDiary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { adding = true }, enabled = !busy) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Guardar resultado")
            } },
            dismissButton = { TextButton(onClick = { open = false }, enabled = !busy) { Text("Cerrar") } }
        )
    }
    if (open && adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Resultados disponibles") },
            text = {
                if (candidates.isEmpty()) Text("Hacé una tirada, consultá el oráculo o revelá una carta para guardarla aquí.")
                else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(candidates, key = { it.id }) { item ->
                        OutlinedCard(onClick = { inspected = item }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(sourceLabel(item.source), style = MaterialTheme.typography.labelSmall)
                                Text(item.text, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { onCapture(item) }, enabled = !busy && item.id !in savedIds,
                                    modifier = Modifier.heightIn(min = 48.dp)) { Text(if (item.id in savedIds) "Ya guardada" else "Guardar") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { adding = false }) { Text("Volver") } }
        )
    }
    inspected?.let { snapshot ->
        val item = entries.firstOrNull { it.id == snapshot.id } ?: snapshot
        AlertDialog(
            onDismissRequest = { inspected = null },
            title = { Text(item.title) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sourceLabel(item.source), style = MaterialTheme.typography.labelMedium)
                    if (item.sceneTitle.isNotBlank()) Text("Escena: ${item.sceneTitle}", style = MaterialTheme.typography.labelSmall)
                    item.imagePath?.let { path ->
                        AsyncImage(model = path, contentDescription = item.title, contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(160.dp))
                    }
                    SelectionContainer { Text(item.text, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                if (item.id !in savedIds) TextButton(onClick = { onCapture(item) }, enabled = !busy) { Text("Guardar inspiración") }
                else InspirationActions(item, busy, onStatus, onSendToDiary)
            },
            dismissButton = { TextButton(onClick = { inspected = null }, enabled = !busy) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun InspirationActions(item: BoardInspiration, busy: Boolean,
    onStatus: (String, InspirationStatus) -> Unit, onSendToDiary: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (item.status == InspirationStatus.PENDING) {
            TextButton(onClick = { onSendToDiary(item.id) }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Usar → diario") }
        }
        Box {
            TextButton(onClick = { menu = true }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Estado")
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                InspirationStatus.entries.filter { it != item.status }.forEach { status ->
                    DropdownMenuItem(text = { Text(when (status) {
                        InspirationStatus.PENDING -> "Restaurar a pendientes"
                        InspirationStatus.USED -> "Marcar como usada"
                        InspirationStatus.DISCARDED -> "Descartar"
                    }) }, onClick = { menu = false; onStatus(item.id, status) })
                }
            }
        }
    }
}

private fun sourceLabel(source: InspirationSource) = when (source) {
    InspirationSource.TABLE -> "Tabla"
    InspirationSource.ORACLE -> "Oráculo"
    InspirationSource.CARD -> "Carta"
}

private fun statusLabel(status: InspirationStatus) = when (status) {
    InspirationStatus.PENDING -> "Pendientes"
    InspirationStatus.USED -> "Usadas"
    InspirationStatus.DISCARDED -> "Descartadas"
}
