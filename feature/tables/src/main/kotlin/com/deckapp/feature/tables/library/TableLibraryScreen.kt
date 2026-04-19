package com.deckapp.feature.tables.library

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.feature.tables.TableDetailSheet
import com.deckapp.feature.tables.TablesViewModel
import com.deckapp.feature.tables.TablePackInfo
import com.deckapp.core.ui.components.SelectionActionBar
import com.deckapp.feature.tables.library.components.TableGridItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.core.animateDpAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableLibraryScreen(
    onTableClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    onCreateTable: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (uiState.showPackDialog) {
        TablePackDialog(
            packs = uiState.availableTablePacks,
            onDismiss = { viewModel.setShowPackDialog(false) },
            onInstall = viewModel::installTablePack,
            onRemove = viewModel::removeTablePack
        )
    }

    if (uiState.activeTable != null) {
        TableDetailSheet(
            table = uiState.activeTable!!,
            lastResult = uiState.lastResult,
            recentResults = emptyList(), 
            isRolling = uiState.isRolling,
            onRoll = { viewModel.rollTable(uiState.activeTable!!.id, sessionId = null) },
            onExport = { /* Opcional */ },
            onDismiss = { viewModel.closeTable() }
        )
    }

    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    var showBulkTagMenu by remember { mutableStateOf(false) }
 
    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            title = { Text("¿Borrar seleccionadas?") },
            text = { Text("Se eliminarán permanentemente ${uiState.selectedTableIds.size} tablas.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.bulkDelete()
                        showBulkDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Borrar todo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) { Text("Cancelar") }
            }
        )
    }
 
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        if (uiState.selectedTableIds.isNotEmpty()) "${uiState.selectedTableIds.size} seleccionadas" else "Biblioteca de Tablas",
                        fontWeight = FontWeight.ExtraBold
                    ) 
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (uiState.selectedTableIds.isEmpty()) {
                        IconButton(onClick = { viewModel.setShowPackDialog(true) }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Packs de tablas")
                        }
                        IconButton(onClick = onImportClick) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Importar")
                        }
                    } else {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancelar")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedTableIds.isEmpty()) {
                FloatingActionButton(onClick = onCreateTable) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva tabla")
                }
            }
        },
        bottomBar = {
            if (uiState.selectedTableIds.isNotEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SelectionActionBar(
                        count = uiState.selectedTableIds.size,
                        onClear = { viewModel.clearSelection() },
                        onDelete = { showBulkDeleteConfirmation = true },
                        onTogglePin = { viewModel.bulkTogglePin(it) },
                        onAddTag = { showBulkTagMenu = true }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tablas…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )

            // Selector de Categorías
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { viewModel.setCategoryFilter(null) },
                        label = { Text("Todas") }
                    )
                }
                uiState.availableCategories.forEach { category ->
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.setCategoryFilter(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredTables.isEmpty()) {
                EmptyLibraryState(
                    hasFilters = uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != null,
                    onClearFilters = { viewModel.clearFilters() },
                    onExplorePacks = { viewModel.setShowPackDialog(true) }
                )
            } else {
                val gridState = rememberLazyGridState()
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    uiState.groupedTables.forEach { (category, tables) ->
                        if (uiState.selectedCategory == null) {
                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        items(tables, key = { it.id }) { table ->
                            val isSelected = table.id in uiState.selectedTableIds
                            TableGridItem(
                                table = table,
                                isSelected = isSelected,
                                onLongClick = { viewModel.toggleTableSelection(table.id) },
                                onClick = {
                                    if (uiState.selectedTableIds.isNotEmpty()) {
                                        viewModel.toggleTableSelection(table.id)
                                    } else {
                                        onTableClick(table.id)
                                    }
                                },
                                onQuickRoll = { viewModel.rollTable(table.id, sessionId = null) },
                                onTogglePin = { viewModel.togglePin(table) },
                                onDuplicate = { viewModel.duplicateTable(table.id) },
                                onInvertRanges = { viewModel.invertTable(table.id) },
                                onDelete = { viewModel.deleteTable(table.id) },
                                quickRollResult = uiState.quickRollResults[table.id],
                                onDismissResult = { viewModel.clearQuickRoll(table.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TablePackDialog(
    packs: List<TablePackInfo>,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Packs de Tablas") },
        text = {
            if (packs.isEmpty()) {
                Text("No hay packs de tablas disponibles.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(packs, key = { it.assetName }) { pack ->
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(pack.displayName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (pack.isInstalled) "Instalado" else "Disponible",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (pack.isInstalled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (pack.isInstalled) {
                                    TextButton(onClick = { onInstall(pack.assetName) }) { Text("Actualizar") }
                                    TextButton(
                                        onClick = { onRemove(pack.assetName) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) { Text("Quitar") }
                                } else {
                                    TextButton(onClick = { onInstall(pack.assetName) }) { Text("Instalar") }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun EmptyLibraryState(hasFilters: Boolean, onClearFilters: () -> Unit, onExplorePacks: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasFilters) Icons.Default.SearchOff else Icons.Default.Casino,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (hasFilters) "Sin resultados" else "¡No hay tablas todavía!",
            style = MaterialTheme.typography.titleLarge
        )
        
        if (hasFilters) {
            TextButton(onClick = onClearFilters) { Text("Limpiar filtros") }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Puedes crear tus propias tablas o activar packs de inicio.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onExplorePacks) {
                Icon(Icons.Default.AutoAwesome, null)
                Spacer(Modifier.width(8.dp))
                Text("Explorar packs de tablas")
            }
        }
    }
}
