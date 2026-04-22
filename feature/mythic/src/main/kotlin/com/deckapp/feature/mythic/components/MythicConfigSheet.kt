package com.deckapp.feature.mythic.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MythicConfigSheet(
    sessionName: String,
    actionTableId: Long?,
    subjectTableId: Long?,
    allTables: List<RandomTable>,
    onSaveTables: (Long?, Long?) -> Unit,
    onDeleteSession: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedActionId by remember { mutableStateOf(actionTableId) }
    var selectedSubjectId by remember { mutableStateOf(subjectTableId) }
    var showActionPicker by remember { mutableStateOf(false) }
    var showSubjectPicker by remember { mutableStateOf(false) }

    if (showActionPicker) {
        TablePickerDialog(
            title = "Tabla de Acción",
            tables = allTables,
            onSelected = { selectedActionId = it; showActionPicker = false },
            onDismiss = { showActionPicker = false }
        )
    }

    if (showSubjectPicker) {
        TablePickerDialog(
            title = "Tabla de Sujeto",
            tables = allTables,
            onSelected = { selectedSubjectId = it; showSubjectPicker = false },
            onDismiss = { showSubjectPicker = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Configuración: $sessionName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tablas de Eventos Aleatorios", style = MaterialTheme.typography.labelLarge)
            
            TablePickerButton(
                label = "Tabla de Acción",
                tableName = allTables.find { it.id == selectedActionId }?.name ?: "No seleccionada",
                onClick = { showActionPicker = true }
            )

            TablePickerButton(
                label = "Tabla de Sujeto",
                tableName = allTables.find { it.id == selectedSubjectId }?.name ?: "No seleccionada",
                onClick = { showSubjectPicker = true }
            )
        }

        Button(
            onClick = {
                onSaveTables(selectedActionId, selectedSubjectId)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar cambios")
        }

        HorizontalDivider()

        TextButton(
            onClick = onDeleteSession,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Eliminar esta sesión")
        }
    }
}

@Composable
private fun TablePickerButton(label: String, tableName: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(tableName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TablePickerDialog(
    title: String,
    tables: List<RandomTable>,
    onSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item {
                    ListItem(
                        headlineContent = { Text("Ninguna", color = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { onSelected(null) }
                    )
                }
                items(tables) { table ->
                    ListItem(
                        headlineContent = { Text(table.name) },
                        supportingContent = { Text(table.category) },
                        modifier = Modifier.clickable { onSelected(table.id) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
