package com.deckapp.feature.tables.components

import com.deckapp.core.ui.components.DeckTextButton

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.ui.components.PaperSurface
import com.deckapp.core.ui.components.TableReferenceHeader
import com.deckapp.core.ui.components.TableReferenceRow

/** Compact manual-style reference. Only RANGE rolls identify an entry by dice value. */
@Composable
fun TableEntriesPreview(table: RandomTable, lastRoll: Int?) {
    var expanded by rememberSaveable(table.id) { mutableStateOf(false) }
    if (table.entries.isEmpty()) return
    DeckTextButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .semantics { stateDescription = if (expanded) "Expandido" else "Contraído" }
    ) {
        Text(if (expanded) "Ocultar entradas" else "Ver entradas (${table.entries.size})")
    }
    if (!expanded) return

    val entries = remember(table.entries, table.rollMode) {
        if (table.rollMode == TableRollMode.SEQUENTIAL || table.rollMode == TableRollMode.MACRO)
            table.entries.sortedBy { it.sortOrder } else table.entries
    }
    val highlightedIndex = if (table.rollMode == TableRollMode.RANGE && lastRoll != null)
        entries.indexOfFirst { lastRoll in it.minRoll..it.maxRoll } else -1

    val rangeLabel = when (table.rollMode) {
        TableRollMode.RANGE -> "Rango"
        TableRollMode.WEIGHTED -> "Peso"
        else -> "Paso"
    }
    PaperSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        textureAlpha = 0.04f
    ) {
        Column {
            TableReferenceHeader(rangeLabel)
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
                itemsIndexed(entries) { index, entry ->
                    val selected = index == highlightedIndex
                    val label = when (table.rollMode) {
                        TableRollMode.RANGE -> if (entry.minRoll == entry.maxRoll) "${entry.minRoll}" else "${entry.minRoll}–${entry.maxRoll}"
                        TableRollMode.WEIGHTED -> "${entry.weight}"
                        else -> "${index + 1}"
                    }
                    TableReferenceRow(
                        range = label,
                        text = entry.text,
                        alternate = index % 2 == 1,
                        selected = selected,
                        subTableLabel = if (entry.subTableId != null) entry.subTableRef ?: "Subtabla vinculada" else null
                    )
                    if (index < entries.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}
