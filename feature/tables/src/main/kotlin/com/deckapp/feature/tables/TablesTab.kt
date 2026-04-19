package com.deckapp.feature.tables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.SelectionActionBar
import com.deckapp.feature.tables.library.TablePackDialog
import com.deckapp.feature.tables.library.components.TableGridItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TablesTab(
    sessionId: Long?,
    onCreateTable: () -> Unit,
    onImportTable: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var showExportDialog by remember { mutableStateOf(false) }

    if (uiState.showPackDialog) {
        com.deckapp.feature.tables.library.TablePackDialog(
            packs = uiState.availableTablePacks,
            onDismiss = { viewModel.setShowPackDialog(false) },
            onInstall = viewModel::installTablePack,
            onRemove = viewModel::removeTablePack
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val data = uiState.exportData
        if (data != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(data) }
                viewModel.clearExportData()
            } catch (_: Exception) { }
        }
    }

    // Al detectar datos de exportación nuevos, lanzamos el selector de archivos
    LaunchedEffect(uiState.exportData) {
        if (uiState.exportData != null && uiState.exportFilename != null) {
            exportLauncher.launch(uiState.exportFilename!!)
        }
    }

    LaunchedEffect(uiState.activeTable, sessionId) {
        if (uiState.activeTable != null && sessionId != null) {
            viewModel.loadRecentResults(sessionId)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.setSession(sessionId)
    }

    if (uiState.activeTable != null) {
        val activeTable = uiState.activeTable!!
        TableDetailSheet(
            table = activeTable,
            lastResult = uiState.lastResult,
            recentResults = uiState.recentResults,
            isRolling = uiState.isRolling,
            onRoll = { viewModel.rollTable(activeTable.id, sessionId) },
            onExport = { showExportDialog = true },
            onDismiss = { viewModel.closeTable() }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar Tabla") },
            text = { Text("Elige el formato de exportación:") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.exportTable(ExportFormat.CSV)
                    showExportDialog = false 
                }) { Text("CSV (Excel/VTT)") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.exportTable(ExportFormat.MARKDOWN)
                    showExportDialog = false 
                }) { Text("Markdown (Notas)") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar tablas…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(onClick = { viewModel.toggleViewMode() }) {
                    Icon(
                        if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewModule,
                        contentDescription = "Cambiar vista"
                    )
                }
            }

            if (uiState.allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTagIds.isEmpty(),
                            onClick = { viewModel.clearFilters() },
                            label = { Text("Todos") }
                        )
                    }
                    items(uiState.allTags) { tag ->
                        FilterChip(
                            selected = tag.id in uiState.selectedTagIds,
                            onClick = { viewModel.toggleTagFilter(tag.id) },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(tag.color).copy(alpha = 0.2f),
                                selectedLabelColor = Color(tag.color)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (sessionId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (uiState.showAllTables) "Mostrando todas las tablas" else "Tablas de la sesión",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = uiState.showAllTables,
                        onCheckedChange = { viewModel.setShowAllTables(it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }

                if (uiState.sessionRollLog.isNotEmpty()) {
                    RollLogPanel(
                        log = uiState.sessionRollLog,
                        expanded = uiState.rollLogExpanded,
                        onToggleExpanded = { viewModel.toggleRollLogExpanded() },
                        onClear = { viewModel.clearSessionRollLog(sessionId) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.groupedTables.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No hay tablas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Crea una o activa un pack predefinido",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(24.dp))
                            
                            Button(onClick = onCreateTable) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Crear tabla")
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            OutlinedButton(onClick = { viewModel.setShowPackDialog(true) }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Explorar packs de tablas")
                            }
                        }
                    }
                }
                uiState.isGridView -> {
                    // Vista de Cuadrícula con Sticky Headers
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(160.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        uiState.groupedTables.forEach { (bundle, tables) ->
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = bundle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(tables.size) { index ->
                                val table = tables[index]
                                TableGridItem(
                                    table = table,
                                    isSelected = table.id in uiState.selectedTableIds,
                                    onClick = {
                                        if (uiState.selectedTableIds.isNotEmpty()) {
                                            viewModel.toggleTableSelection(table.id)
                                        } else if (uiState.isSimplifiedMode) {
                                            viewModel.rollTable(table.id, sessionId)
                                        } else {
                                            viewModel.openTable(table)
                                        }
                                    },
                                    onLongClick = {
                                        if (uiState.selectedTableIds.isEmpty() && uiState.isSimplifiedMode) {
                                            viewModel.openTable(table)
                                        } else {
                                            viewModel.toggleTableSelection(table.id)
                                        }
                                    },
                                    onQuickRoll = { viewModel.rollTable(table.id, sessionId) },
                                    onTogglePin = { viewModel.togglePin(table) },
                                    quickRollResult = uiState.quickRollResults[table.id],
                                    onDismissResult = { viewModel.clearQuickRoll(table.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Vista de Lista con Sticky Headers
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        uiState.groupedTables.forEach { (bundle, tables) ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = bundle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                            items(tables, key = { it.id }) { table ->
                                val isSelected = table.id in uiState.selectedTableIds
                                TableListItem(
                                    table = table,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.selectedTableIds.isNotEmpty(),
                                    onLongClick = { 
                                        if (uiState.selectedTableIds.isEmpty() && uiState.isSimplifiedMode) {
                                            viewModel.openTable(table)
                                        } else {
                                            viewModel.toggleTableSelection(table.id)
                                        }
                                    },
                                    onClick = {
                                        if (uiState.selectedTableIds.isNotEmpty()) {
                                            viewModel.toggleTableSelection(table.id)
                                        } else if (uiState.isSimplifiedMode) {
                                            viewModel.rollTable(table.id, sessionId)
                                        } else {
                                            viewModel.openTable(table)
                                        }
                                    },
                                    onQuickRoll = { viewModel.rollTable(table.id, sessionId) },
                                    onTogglePin = { viewModel.togglePin(table) },
                                    quickRollResult = uiState.quickRollResults[table.id],
                                    onDismissResult = { viewModel.clearQuickRoll(table.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedTableIds.isNotEmpty()) {
            SelectionActionBar(
                count = uiState.selectedTableIds.size,
                onClear = { viewModel.clearSelection() },
                onDelete = { viewModel.bulkDelete() },
                onTogglePin = { viewModel.bulkTogglePin(it) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = { viewModel.setShowPackDialog(true) },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Packs de tablas")
                }

                SmallFloatingActionButton(
                    onClick = onImportTable,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = "Importar tabla")
                }

                FloatingActionButton(
                    onClick = onCreateTable
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva tabla")
                }
            }
        }
    }
}

@Composable
private fun RollLogPanel(
    log: List<TableRollResult>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Historial de sesión · ${log.size} tiradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar historial", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onToggleExpanded, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(log, key = { it.id }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = result.rollValue.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = result.tableName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.4f)
                            )
                            Text(
                                text = result.resolvedText,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableListItem(
    table: RandomTable,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickRoll: () -> Unit,
    onTogglePin: () -> Unit,
    quickRollResult: TableRollResult? = null,
    onDismissResult: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = when {
            isSelected -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            table.isPinned -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            else -> CardDefaults.cardColors()
        },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (quickRollResult != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = quickRollResult.rollValue.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = quickRollResult.resolvedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        table.tags.take(3).forEach { tag ->
                            Surface(
                                color = Color(tag.color).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(tag.color),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (table.tags.size > 3) {
                            Text(
                                text = "+${table.tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (table.tags.isNotEmpty()) Text("·", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${table.rollFormula} · ${table.entries.size} entradas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                if (quickRollResult != null) {
                    IconButton(onClick = onDismissResult, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (table.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = if (table.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onQuickRoll) {
                    Icon(
                        if (quickRollResult != null) Icons.Default.Refresh else Icons.Default.Casino,
                        contentDescription = "Tirar ${table.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onLongClick() }
                )
            }
        }
    }
}
