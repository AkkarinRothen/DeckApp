package com.deckapp.feature.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deckapp.feature.reference.components.ImportPreviewSheet
import com.deckapp.feature.reference.components.ImportSourceSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceTableEditorScreen(
    tableId: Long,
    prefilledSystem: String = "",
    onNavigateBack: () -> Unit,
    viewModel: ReferenceTableEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showImportSource by remember { mutableStateOf(false) }

    LaunchedEffect(tableId) {
        viewModel.loadTable(tableId, prefilledSystem)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (tableId == -1L) "Nueva tabla" else "Editar tabla") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.save()
                            onNavigateBack()
                        },
                        enabled = uiState.name.isNotBlank() && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Metadata
            item {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChanged,
                        label = { Text("Nombre de la tabla *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = uiState.gameSystem,
                            onValueChange = viewModel::onSystemChanged,
                            label = { Text("Sistema") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = uiState.category,
                            onValueChange = viewModel::onCategoryChanged,
                            label = { Text("Categoría") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::onDescriptionChanged,
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }

            // Columns Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("COLUMNAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        TextButton(onClick = viewModel::addColumn) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("Añadir columna")
                        }
                    }
                    
                    uiState.columns.forEachIndexed { index, column ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = column.header,
                                onValueChange = { viewModel.updateColumnHeader(index, it) },
                                modifier = Modifier.weight(1f),
                                label = { Text("Encabezado ${index + 1}") },
                                singleLine = true
                            )
                            IconButton(
                                onClick = { viewModel.removeColumn(index) },
                                enabled = uiState.columns.size > 1
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Rows Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("FILAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Row {
                            TextButton(onClick = { showImportSource = true }) {
                                Icon(Icons.Default.Upload, contentDescription = null)
                                Text("Importar")
                            }
                            TextButton(onClick = viewModel::addRow) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text("Añadir fila")
                            }
                        }
                    }

                    // Grid Editor
                    val horizontalScrollState = rememberScrollState()
                    Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                        Column {
                            // Header Row (Visual only)
                            Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp)) {
                                uiState.columns.forEach { col ->
                                    Text(
                                        text = col.header,
                                        modifier = Modifier.width(120.dp).padding(horizontal = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(48.dp)) // For delete button space
                            }

                            // Data Rows
                            uiState.rows.forEachIndexed { rowIndex, row ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.cells.forEachIndexed { colIndex, cellValue ->
                                        Box(
                                            modifier = Modifier
                                                .width(120.dp)
                                                .padding(horizontal = 4.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(8.dp)
                                        ) {
                                            BasicTextField(
                                                value = cellValue,
                                                onValueChange = { viewModel.updateCell(rowIndex, colIndex, it) },
                                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.removeRow(rowIndex) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Eliminar fila", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportSource) {
        ImportSourceSheet(
            onDismiss = { showImportSource = false },
            onCsvSelected = viewModel::prepareImportFromCsv,
            onMarkdownPaste = viewModel::prepareImportFromMarkdown,
            onImageSelected = { viewModel.prepareImportFromImage(it) }
        )
    }

    if (uiState.importPreviewData != null || uiState.isImportLoading || uiState.importError != null) {
        ImportPreviewSheet(
            importData = uiState.importPreviewData,
            isLoading = uiState.isImportLoading,
            error = uiState.importError,
            onDismiss = viewModel::cancelImport,
            onConfirm = { headers, replace ->
                viewModel.applyImport(headers, replace)
            }
        )
    }
}
