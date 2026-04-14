package com.deckapp.feature.tables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode

/**
 * Pantalla para crear o editar una tabla aleatoria.
 *
 * Accedida desde:
 * - FAB en [TablesTab] → nueva tabla (tableId = -1)
 * - Long-press en item de lista → editar tabla existente (tableId = id)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorScreen(
    onBack: () -> Unit,
    viewModel: TableEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    // Preview de tirada
    uiState.previewResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPreview() },
            title = { Text("Vista previa de tirada") },
            text = {
                Column {
                    Text("Resultado: ${result.rollValue}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(result.resolvedText, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPreview() }) { Text("Cerrar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPreview(); viewModel.previewRoll() }) {
                    Text("Tirar de nuevo")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewTable) "Nueva tabla" else "Editar tabla") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.previewRoll() }, enabled = uiState.entries.isNotEmpty()) {
                        Icon(Icons.Default.Casino, contentDescription = "Vista previa")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.save() },
                text = { if (uiState.isSaving) Text("Guardando…") else Text("Guardar") },
                icon = { if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp)) else Icon(Icons.Default.Add, null) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Metadatos ─────────────────────────────────────────────────
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.setName(it) },
                        label = { Text("Nombre de la tabla *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = { viewModel.setCategory(it) },
                        label = { Text("Categoría") },
                        placeholder = { Text("Encuentros, Nombres, Clima…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = { viewModel.setDescription(it) },
                        label = { Text("Descripción") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.rollFormula,
                            onValueChange = { viewModel.setRollFormula(it) },
                            label = { Text("Fórmula") },
                            placeholder = { Text("1d6") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Modo",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = uiState.rollMode == TableRollMode.RANGE,
                                    onClick = { viewModel.setRollMode(TableRollMode.RANGE) }
                                )
                                Text("Rango", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.width(8.dp))
                                RadioButton(
                                    selected = uiState.rollMode == TableRollMode.WEIGHTED,
                                    onClick = { viewModel.setRollMode(TableRollMode.WEIGHTED) }
                                )
                                Text("Peso", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // ── Sección de entradas ──────────────────────────────────────
                item {
                    Text(
                        "Entradas",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Soporta [NdM] para dados inline y @NombreTabla para sub-tablas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                itemsIndexed(uiState.entries) { index, entry ->
                    EntryRow(
                        entry = entry,
                        index = index,
                        rollMode = uiState.rollMode,
                        onUpdate = { viewModel.updateEntry(index, it) },
                        onRemove = { viewModel.removeEntry(index) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { viewModel.addEntry() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Añadir entrada")
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    entry: TableEntry,
    index: Int,
    rollMode: TableRollMode,
    onUpdate: (TableEntry) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rango o peso
            if (rollMode == TableRollMode.RANGE) {
                OutlinedTextField(
                    value = entry.minRoll.toString(),
                    onValueChange = { onUpdate(entry.copy(minRoll = it.toIntOrNull() ?: entry.minRoll)) },
                    label = { Text("Min") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(64.dp)
                )
                Text("–", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = entry.maxRoll.toString(),
                    onValueChange = { onUpdate(entry.copy(maxRoll = it.toIntOrNull() ?: entry.maxRoll)) },
                    label = { Text("Max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(64.dp)
                )
            } else {
                OutlinedTextField(
                    value = entry.weight.toString(),
                    onValueChange = { onUpdate(entry.copy(weight = it.toIntOrNull() ?: entry.weight)) },
                    label = { Text("Peso") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(72.dp)
                )
            }

            // Texto
            OutlinedTextField(
                value = entry.text,
                onValueChange = { onUpdate(entry.copy(text = it)) },
                label = { Text("Texto") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            // Eliminar
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar entrada",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
