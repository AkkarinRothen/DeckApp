package com.deckapp.feature.encounters

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.EncounterCreature

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncounterEditorScreen(
    onBack: () -> Unit,
    viewModel: EncounterEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages() // Asumiendo que existe, si no lo añadiremos
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == 0L) "Nuevo Encuentro" else "Editar Encuentro") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancelar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }, enabled = !uiState.isSaving) {
                        if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        else Icon(Icons.Default.Save, contentDescription = "Guardar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EncounterBasicInfo(
                        name = uiState.name,
                        onNameChange = { viewModel.updateName(it) },
                        description = uiState.description,
                        onDescriptionChange = { viewModel.updateDescription(it) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Criaturas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { viewModel.addCreature() }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Añadir")
                        }
                    }
                }

                itemsIndexed(uiState.creatures) { index, creature ->
                    CreatureEditorItem(
                        creature = creature,
                        onUpdate = { viewModel.updateCreature(index, it) },
                        onRemove = { viewModel.removeCreature(index) }
                    )
                }
                
                item {
                    Spacer(Modifier.height(80.dp)) // Espacio para el fab o scroll final
                }
            }
        }
    }
}

@Composable
private fun EncounterBasicInfo(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nombre del encuentro") },
            placeholder = { Text("Ej: Emboscada de trasgos") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Descripción / Notas de preparación") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp)
        )
    }
}

@Composable
private fun CreatureEditorItem(
    creature: EncounterCreature,
    onUpdate: (EncounterCreature) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = creature.name,
                    onValueChange = { onUpdate(creature.copy(name = it)) },
                    label = { Text("Nombre") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = creature.maxHp.toString(),
                    onValueChange = { val hp = it.toIntOrNull() ?: 0; onUpdate(creature.copy(maxHp = hp, currentHp = hp)) },
                    label = { Text("HP Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = creature.armorClass.toString(),
                    onValueChange = { onUpdate(creature.copy(armorClass = it.toIntOrNull() ?: 10)) },
                    label = { Text("AC") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = creature.initiativeBonus.toString(),
                    onValueChange = { onUpdate(creature.copy(initiativeBonus = it.toIntOrNull() ?: 0)) },
                    label = { Text("Bono Init") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        }
    }
}
