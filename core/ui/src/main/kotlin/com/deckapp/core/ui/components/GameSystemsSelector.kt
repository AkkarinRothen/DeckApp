package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSystemsSelector(
    selectedSystems: List<String>,
    onSystemsChanged: (List<String>) -> Unit,
    availableSystems: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var customSystemName by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // "General" always exists or is implicitly handled. 
            // Usually we show it if selected or as a base option.

            (availableSystems + selectedSystems + "General").distinct().forEach { system ->
                val isSelected = selectedSystems.contains(system) || (system == "General" && selectedSystems.isEmpty())
                
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newList = if (isSelected) {
                            if (system != "General") selectedSystems - system else selectedSystems
                        } else {
                            selectedSystems + system
                        }
                        onSystemsChanged(newList.distinct())
                    },
                    label = { Text(system) }
                )
            }

            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir sistema")
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Añadir sistema custom") },
            text = {
                OutlinedTextField(
                    value = customSystemName,
                    onValueChange = { customSystemName = it },
                    label = { Text("Nombre del sistema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customSystemName.isNotBlank()) {
                            onSystemsChanged((selectedSystems + customSystemName.trim()).distinct())
                            customSystemName = ""
                        }
                        showAddDialog = false
                    },
                    enabled = customSystemName.isNotBlank()
                ) {
                    Text("Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
