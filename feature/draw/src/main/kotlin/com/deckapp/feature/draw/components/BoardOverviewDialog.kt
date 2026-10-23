package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.SessionBoardGroup
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemType
import com.deckapp.core.model.SessionBoardView

@Composable
internal fun BoardOverviewDialog(
    groups: List<SessionBoardGroup>,
    boardItems: List<SessionBoardItem>,
    activityStartedAt: Long,
    lastUsedAt: Map<Long, Long>,
    itemTitle: (SessionBoardItem) -> String,
    viewLabel: (SessionBoardView) -> String,
    isEditing: Boolean,
    onMoveGroup: (Long, Int) -> Unit,
    onJumpToGroup: (SessionBoardGroup) -> Unit,
    onRemoveItem: (SessionBoardItem) -> Unit,
    onDismiss: () -> Unit
) {
    var confirmRemoval by remember { mutableStateOf<SessionBoardItem?>(null) }
    val itemByKey = remember(boardItems) {
        boardItems.filter { it.resourceId != null }
            .groupBy { it.boardView to it.type to it.resourceId }
    }
    val duplicateCandidates = remember(boardItems) {
        itemByKey.values.filter { it.size > 1 }.flatMap { it.drop(1) }.distinctBy { it.id }
    }
    val now = System.currentTimeMillis()
    val oldUnusedCandidates = remember(boardItems, lastUsedAt, activityStartedAt) {
        boardItems.filter {
            activityStartedAt > 0L && it.createdAt >= activityStartedAt &&
                now - it.createdAt >= 30L * 24 * 60 * 60 * 1000 && it.id !in lastUsedAt
        }
    }
    val missingResourceCandidates = remember(boardItems) {
        boardItems.filter { it.type in setOf(SessionBoardItemType.TABLE, SessionBoardItemType.DECK, SessionBoardItemType.WIKI_MAP) && it.resourceId == null }
    }
    val attentionCandidates = (duplicateCandidates + oldUnusedCandidates + missingResourceCandidates).distinctBy { it.id }
    val attentionCount = attentionCandidates.size
    val orderedGroups = groups.sortedWith(compareBy<SessionBoardGroup> { it.boardView.ordinal }.thenBy { it.position })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resumen del tablero") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val suggestionLabel = if (attentionCount == 1) "sugerencia" else "sugerencias"
                Text("${boardItems.size} bloques · ${groups.size} grupos · $attentionCount $suggestionLabel")
                    Text("Revisa duplicados, bloques sin recurso o sin uso registrado. El uso se registra desde esta versión.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(orderedGroups, key = { _, group -> "group_${group.id}" }) { _, group ->
                        val viewGroups = orderedGroups.filter { it.boardView == group.boardView }
                        val groupIndex = viewGroups.indexOfFirst { it.id == group.id }
                        val groupItems = if (group.autoFilterType != null) {
                            boardItems.filter { it.boardView == group.boardView && it.type.name == group.autoFilterType }
                        } else boardItems.filter { it.boardView == group.boardView && it.groupId == group.id }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onJumpToGroup(group) }, modifier = Modifier.weight(1f)) {
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, maxLines = 1)
                                    Text("${viewLabel(group.boardView)} · ${groupItems.size} bloques${if (groupItems.isEmpty()) " · vacía" else ""}",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = "Ir al grupo")
                            }
                            if (isEditing) {
                                IconButton(onClick = { onMoveGroup(group.id, -1) }, enabled = groupIndex > 0) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Mover ${group.name} arriba")
                                }
                                IconButton(onClick = { onMoveGroup(group.id, 1) }, enabled = groupIndex < viewGroups.lastIndex) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Mover ${group.name} abajo")
                                }
                            }
                        }
                    }
                    if (groups.isEmpty()) item { Text("Todavía no hay grupos.", modifier = Modifier.padding(8.dp)) }
                    if (attentionCandidates.isNotEmpty()) {
                        item { HorizontalDivider(Modifier.padding(vertical = 6.dp)) }
                        item { Text("Para revisar", style = MaterialTheme.typography.titleSmall) }
                        items(attentionCandidates, key = { "attention_${it.id}" }) { item ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(itemTitle(item), maxLines = 1)
                                    val reason = when {
                                        item in duplicateCandidates -> "Posible duplicado"
                                        item in missingResourceCandidates -> "Sin recurso vinculado"
                                        else -> "Sin uso registrado en 30 días"
                                    }
                                    Text(reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { confirmRemoval = item }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Quitar sugerencia")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )

    confirmRemoval?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmRemoval = null },
            title = { Text("Quitar bloque del tablero?") },
            text = { Text("Se quitará ${itemTitle(item)}. El recurso original seguirá en tu biblioteca.") },
            confirmButton = {
                Button(onClick = { onRemoveItem(item); confirmRemoval = null }) { Text("Quitar") }
            },
            dismissButton = { TextButton(onClick = { confirmRemoval = null }) { Text("Cancelar") } }
        )
    }
}
