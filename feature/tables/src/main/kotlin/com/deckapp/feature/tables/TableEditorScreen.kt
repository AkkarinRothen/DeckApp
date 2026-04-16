package com.deckapp.feature.tables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollMode

/**
 * Pantalla para crear o editar una tabla aleatoria.
 *
 * Accedida desde:
 * - FAB en [TablesTab] → nueva tabla (tableId = -1)
 * - Long-press en item de lista → editar tabla existente (tableId = id)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    
    // Selector de Sub-tabla
    if (uiState.pickingEntryIndex != null) {
        TablePickerDialog(
            tables = uiState.availableTables,
            onDismiss = { viewModel.cancelPicking() },
            onTableSelected = { viewModel.linkSubTable(it) }
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
                    Text(
                        "Etiquetas",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.toggleTag(tag) },
                                label = { Text(tag.name) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color(tag.color).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(tag.color)
                                )
                            )
                        }
                        
                        var showTagMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showTagMenu = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir etiqueta")
                        }
                        
                        DropdownMenu(
                            expanded = showTagMenu,
                            onDismissRequest = { showTagMenu = false }
                        ) {
                            if (uiState.allTags.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hay etiquetas creadas") },
                                    onClick = { },
                                    enabled = false
                                )
                            }
                            uiState.allTags.forEach { tag ->
                                val isSelected = tag in uiState.tags
                                DropdownMenuItem(
                                    text = { Text(tag.name) },
                                    onClick = { 
                                        viewModel.toggleTag(tag)
                                        showTagMenu = false 
                                    },
                                    trailingIcon = {
                                        if (isSelected) Icon(Icons.Default.Check, null)
                                    }
                                )
                            }
                        }
                    }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // La fórmula solo importa en modo RANGE; en WEIGHTED se ignora
                        OutlinedTextField(
                            value = uiState.rollFormula,
                            onValueChange = { viewModel.setRollFormula(it) },
                            label = { Text("Fórmula") },
                            placeholder = { Text("1d6") },
                            singleLine = true,
                            enabled = uiState.rollMode == TableRollMode.RANGE,
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
                    if (uiState.rollMode == TableRollMode.WEIGHTED) {
                        Text(
                            "Modo Peso: la fórmula se ignora. El resultado se elige por probabilidad relativa.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
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
                        onRemove = { viewModel.removeEntry(index) },
                        onLinkSubTable = { viewModel.startPickingSubTable(index) },
                        onUnlinkSubTable = { viewModel.unlinkSubTable(index) }
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
    onRemove: () -> Unit,
    onLinkSubTable: () -> Unit,
    onUnlinkSubTable: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
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

            // Enlazar Sub-tabla
            IconButton(onClick = if (entry.subTableId == null) onLinkSubTable else onUnlinkSubTable) {
                Icon(
                    if (entry.subTableId == null) Icons.Default.Link else Icons.Default.LinkOff,
                    contentDescription = "Enlazar tabla",
                    tint = if (entry.subTableId == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            // Eliminar
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar entrada",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Indicador de sub-tabla
        if (entry.subTableId != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Link, null, modifier = Modifier.size(14.dp), 
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Llamada a: ${entry.subTableRef ?: "Tabla"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
}

@Composable
fun TablePickerDialog(
    tables: List<RandomTable>,
    onDismiss: () -> Unit,
    onTableSelected: (RandomTable) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTables = remember(searchQuery, tables) {
        tables.filter { table ->
            table.name.contains(searchQuery, ignoreCase = true) || 
            table.tags.any { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar sub-tabla") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Buscar tabla…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                if (filteredTables.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No se encontraron tablas", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredTables.size) { index ->
                            val table = filteredTables[index]
                            ListItem(
                                headlineContent = { Text(table.name) },
                                supportingContent = { 
                                    Text(table.tags.joinToString(", ") { it.name }.ifBlank { "Sin etiquetas" }) 
                                },
                                modifier = Modifier.clickable { onTableSelected(table) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
