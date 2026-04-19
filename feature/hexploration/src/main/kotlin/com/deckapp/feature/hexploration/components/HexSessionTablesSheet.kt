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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult

@Composable
fun HexSessionTablesSheet(
    weatherTableId: Long?,
    travelEventTableId: Long?,
    allTables: List<RandomTable>,
    recentRolls: List<TableRollResult>,
    onRoll: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Text("Tiradas rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }

        item {
            Text("Clima", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (weatherTableId != null) {
                val table = allTables.find { it.id == weatherTableId }
                TableQuickRollRow(tableName = table?.name ?: "Tabla #$weatherTableId") {
                    onRoll(weatherTableId)
                }
            } else {
                Text("Sin tabla de clima configurada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }

        item {
            Text("Eventos de viaje", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (travelEventTableId != null) {
                val table = allTables.find { it.id == travelEventTableId }
                TableQuickRollRow(tableName = table?.name ?: "Tabla #$travelEventTableId") {
                    onRoll(travelEventTableId)
                }
            } else {
                Text("Sin tabla de viaje configurada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (recentRolls.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Últimas tiradas", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(recentRolls) { roll ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(roll.tableName, style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold)
                    Text(roll.resolvedText, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
        }
    }
}

@Composable
private fun TableQuickRollRow(tableName: String, onRoll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(tableName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = onRoll) { Text("Tirar") }
    }
}
