package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.ui.components.SmartText

@Composable
internal fun TableEntriesDialog(
    table: RandomTable,
    onDismissRequest: () -> Unit,
    npcNames: List<String>,
    tableNames: List<String>,
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String) -> Unit
) {
    var query by rememberSaveable(table.id) { mutableStateOf("") }
    val entries = remember(table.entries, query) { filterTableEntries(table.entries, query) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(table.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Buscar en las entradas") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${entries.size} de ${table.entries.size} entradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when {
                    table.entries.isEmpty() -> Text("Esta tabla todavía no tiene entradas.")
                    entries.isEmpty() -> Text("No hay entradas que coincidan con la búsqueda.")
                    else -> key(query) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(entries) { entry ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (table.rollMode == TableRollMode.RANGE || table.rollMode == TableRollMode.WEIGHTED) {
                                        Text(
                                            text = if (table.rollMode == TableRollMode.WEIGHTED) "Peso ${entry.weight}"
                                                else if (entry.minRoll == entry.maxRoll) entry.minRoll.toString()
                                                else "${entry.minRoll}–${entry.maxRoll}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    SmartText(
                                        text = entry.text,
                                        npcNames = npcNames,
                                        tableNames = tableNames,
                                        onTagClicked = onTagClicked,
                                        onDiceClicked = onDiceClicked
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismissRequest) { Text("Cerrar") } }
    )
}
