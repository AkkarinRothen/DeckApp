package com.deckapp.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.CardStack
import com.deckapp.core.ui.components.DeckCoverCard
import com.deckapp.core.ui.components.SelectionActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onDeckClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    onManageTags: () -> Unit,
    onAddToSession: (Long) -> Unit,
    onEncounterLibrary: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var confirmDeleteDeck by remember { mutableStateOf<CardStack?>(null) }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Diálogo confirmación de borrado
    confirmDeleteDeck?.let { deck ->
        AlertDialog(
            onDismissRequest = { confirmDeleteDeck = null },
            title = { Text("Eliminar mazo") },
            text = {
                Text("¿Eliminar \"${deck.name}\"? Se borrarán todas sus cartas e imágenes. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDeck(deck.id)
                        confirmDeleteDeck = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDeck = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo de fusión — selector de mazo destino
    if (uiState.mergeSourceDeckId != null) {
        val sourceName = uiState.allDecks.find { it.id == uiState.mergeSourceDeckId }?.name ?: ""
        MergeTargetDialog(
            sourceName = sourceName,
            candidates = uiState.mergeCandidates,
            onSelect = { targetId -> viewModel.confirmMerge(targetId) },
            onDismiss = { viewModel.cancelMerge() }
        )
    }


    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }
    var showBulkTagMenu by remember { mutableStateOf(false) }

    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            title = { Text("¿Borrar seleccionados?") },
            text = { Text("Se eliminarán permanentemente ${uiState.selectedDeckIds.size} mazos y todas sus cartas. Esta acción no se puede deshacer.") },
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
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.selectedDeckIds.isNotEmpty()) "${uiState.selectedDeckIds.size} seleccionados" else "Biblioteca") },
                actions = {
                    if (uiState.selectedDeckIds.isEmpty()) {
                        IconButton(onClick = onImportClick) {
                            Icon(Icons.Default.Add, contentDescription = "Importar mazo")
                        }
                        
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Gestionar Etiquetas") },
                                onClick = {
                                    showMenu = false
                                    onManageTags()
                                },
                                leadingIcon = { Icon(Icons.Default.Tag, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Encuentros Preparados") },
                                onClick = {
                                    showMenu = false
                                    onEncounterLibrary()
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
                            )
                        }
                    } else {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar selección")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.selectedDeckIds.isNotEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SelectionActionBar(
                        count = uiState.selectedDeckIds.size,
                        onClear = { viewModel.clearSelection() },
                        onDelete = { showBulkDeleteConfirmation = true },
                        onArchive = { viewModel.bulkArchive(it) },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Barra de búsqueda ─────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar mazos…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Chips: filtro por tag + toggle Archivados ─────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = uiState.showArchived,
                        onClick = { viewModel.toggleShowArchived() },
                        label = { Text("Archivados") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Archive,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
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
            Spacer(Modifier.height(8.dp))

            // ── Grid de mazos ─────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.allDecks.isEmpty() && !uiState.showArchived -> {
                    EmptyLibraryState(onImportClick = onImportClick)
                }
                uiState.displayedDecks.isEmpty() && uiState.showArchived -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay mazos archivados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.filteredDecks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Sin resultados",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { viewModel.clearFilters() }) {
                                Text("Limpiar filtros")
                            }
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(uiState.filteredDecks, key = { it.id }) { deck ->
                            val isSelected = deck.id in uiState.selectedDeckIds
                            DeckCoverCard(
                                deck = deck,
                                isSelected = isSelected,
                                onLongClick = { viewModel.toggleDeckSelection(deck.id) },
                                onClick = {
                                    if (uiState.selectedDeckIds.isNotEmpty()) {
                                        viewModel.toggleDeckSelection(deck.id)
                                    } else {
                                        onDeckClick(deck.id)
                                    }
                                },
                                onAddToSession = { onAddToSession(deck.id) },
                                onDuplicate = if (!uiState.showArchived && uiState.selectedDeckIds.isEmpty()) ({ viewModel.duplicateDeck(deck.id) }) else null,
                                onMergeWith = if (!uiState.showArchived && uiState.selectedDeckIds.isEmpty()) ({ viewModel.startMerge(deck.id) }) else null,
                                onArchive = { viewModel.archiveDeck(deck.id, !deck.isArchived) },
                                onDelete = { confirmDeleteDeck = deck },
                                cardCount = uiState.deckCardCounts[deck.id] ?: 0
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeTargetDialog(
    sourceName: String,
    candidates: List<CardStack>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fusionar \"$sourceName\" en…") },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    "No hay otros mazos disponibles como destino.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {
                    candidates.forEach { deck ->
                        item(key = deck.id) {
                            TextButton(
                                onClick = { onSelect(deck.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = deck.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EmptyLibraryState(onImportClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No hay mazos todavía", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Importá tu primer mazo desde una carpeta de imágenes o un PDF",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onImportClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Importar mazo")
        }
    }
}
