package com.deckapp.feature.importdeck.table.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.model.TableEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableReviewView(
    entries: List<TableEntry>,
    tableName: String,
    tableTag: String,
    onEntryChange: (Int, TableEntry) -> Unit,
    onNameChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onConfirm: () -> Unit,
    validationResult: RangeParser.ValidationResult?,
    lowConfidenceIndices: Set<Int>,
    tableProgress: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Revisando tabla: $tableProgress", style = MaterialTheme.typography.labelMedium)
            if (validationResult?.isValid == true) {
                Icon(Icons.Default.Check, "Válido", tint = Color.Green)
            } else {
                Icon(Icons.Default.Warning, "Errores en rangos", tint = MaterialTheme.colorScheme.error)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(
            value = tableName,
            onValueChange = onNameChange,
            label = { Text("Nombre de la tabla") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(8.dp))
        
        OutlinedTextField(
            value = tableTag,
            onValueChange = onTagChange,
            label = { Text("Categoría / Etiqueta") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(entries) { index, entry ->
                EntryRow(
                    entry = entry,
                    isLowConfidence = index in lowConfidenceIndices,
                    onChange = { onEntryChange(index, it) }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = validationResult?.isValid == true
        ) {
            Text("Continuar")
        }
    }
}

@Composable
private fun EntryRow(
    entry: TableEntry,
    isLowConfidence: Boolean,
    onChange: (TableEntry) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isLowConfidence) 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "${entry.minRoll}",
                onValueChange = { val v = it.toIntOrNull() ?: entry.minRoll; onChange(entry.copy(minRoll = v)) },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            Text("-", modifier = Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = "${entry.maxRoll}",
                onValueChange = { val v = it.toIntOrNull() ?: entry.maxRoll; onChange(entry.copy(maxRoll = v)) },
                modifier = Modifier.width(60.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = entry.text,
                onValueChange = { onChange(entry.copy(text = it)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}
