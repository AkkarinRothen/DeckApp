package com.deckapp.feature.tables.library

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.feature.tables.TableDetailSheet
import com.deckapp.feature.tables.TablesViewModel
import com.deckapp.core.ui.components.SelectionActionBar
import com.deckapp.feature.tables.library.components.TableGridItem

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
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar para mensajes
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // BottomSheet para resultados de tiradas rápidas
    if (uiState.activeTable != null) {
        TableDetailSheet(
            table = uiState.activeTable!!,
            lastResult = uiState.lastResult,
            recentResults = emptyList(), 
            isRolling = uiState.isRolling,
            onRoll = { viewModel.rollTable(uiState.activeTable!!.id, sessionId = null) },
            onExport = { /* Opcional: Compartir JSON */ },
            onDismiss = { viewModel.closeTable() }
        )
    }

    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    var showBulkTagMenu by remember { mutableStateOf(false) }
 
    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            title = { Text("¿Borrar seleccionadas?") },
            text = { Text("Se eliminarán permanentemente ${uiState.selectedTableIds.size} tablas. Esta acción no se puede deshacer.") },
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
                TextButton(onClick = { showBulkDeleteConfirmation = false }) {
                    Text("Cancelar")
                }
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
                    
                    DropdownMenu(
                        expanded = showBulkTagMenu,
                        onDismissRequest = { showBulkTagMenu = false }
                    ) {
                        uiState.allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    viewModel.bulkAddTag(tag.id)
                                    showBulkTagMenu = false
                                }
                            )
                        }
                        if (uiState.allTags.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No hay etiquetas") },
                                onClick = {},
                                enabled = false
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Barra de búsqueda
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tablas…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.large,
                singleLine = true
            )

            // Selector de Etiquetas (Tags)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.allTags.forEach { tag ->
                    item(key = tag.id) {
                        FilterChip(
                            selected = tag.id in uiState.selectedTagIds,
                            onClick = { viewModel.toggleTagFilter(tag.id) },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            // Grilla de Tablas (Bento Grid)
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredTables.isEmpty()) {
                EmptyLibraryState(
                    hasFilters = uiState.searchQuery.isNotEmpty() || uiState.selectedTagIds.isNotEmpty(),
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.clearFilters()
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.filteredTables, key = { it.id }) { table ->
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
                            onDuplicate = if (uiState.selectedTableIds.isEmpty()) ({ viewModel.duplicateTable(table.id) }) else null,
                            onDelete = { viewModel.deleteTable(table.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(hasFilters: Boolean, onClearFilters: () -> Unit) {
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
        Text(
            text = if (hasFilters) "Prueba con otros términos o categorías" else "Importa manuales o crea tus propias tablas aleatorias.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (hasFilters) {
            TextButton(onClick = onClearFilters) {
                Text("Limpiar filtros")
            }
        }
    }
}
