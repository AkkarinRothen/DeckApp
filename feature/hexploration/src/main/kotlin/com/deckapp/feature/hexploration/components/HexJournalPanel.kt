package com.deckapp.feature.hexploration.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.HexActivityType
import com.deckapp.core.model.HexDay

@Composable
fun HexJournalPanel(
    mapName: String,
    days: List<HexDay>,
    onUpdateNotes: (HexDay, String) -> Unit,
    onExport: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Diario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = onExport) {
                    Text("Exportar")
                }
            }
            HorizontalDivider()
        }

        if (days.isEmpty()) {
            item {
                Text(
                    "Todavía no hay días registrados. Toca + para iniciar el primero.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        items(days, key = { it.id }) { day ->
            DayCard(
                day = day,
                onUpdateNotes = onUpdateNotes
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun DayCard(
    day: HexDay,
    onUpdateNotes: (HexDay, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var notes by rememberSaveable(day.id) { mutableStateOf(day.notes) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Día ${day.dayNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (!expanded && day.activitiesLog.isNotEmpty()) {
                    val preview = day.activitiesLog.take(2)
                        .joinToString(" · ") { activityIcon(it.type) + " " + it.description }
                    Text(
                        preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "${day.activitiesLog.size} act.",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Colapsar" else "Expandir"
                )
            }
        }

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (day.activitiesLog.isNotEmpty()) {
                    day.activitiesLog.forEach { entry ->
                        Text(
                            "${activityIcon(entry.type)} ${entry.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        "Sin actividades registradas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && notes != day.notes) {
                                onUpdateNotes(day, notes)
                            }
                        },
                    label = { Text("Notas del narrador") },
                    placeholder = { Text("¿Qué pasó hoy?") },
                    minLines = 2,
                    maxLines = 6
                )
            }
        }
    }
}

private fun activityIcon(type: HexActivityType): String = when (type) {
    HexActivityType.TRAVEL       -> "🚶"
    HexActivityType.EXPLORE      -> "🗺"
    HexActivityType.RECONNOITER  -> "🔍"
    HexActivityType.MAP_AREA     -> "📜"
    HexActivityType.FORTIFY_CAMP -> "⛺"
    HexActivityType.CUSTOM       -> "•"
}
