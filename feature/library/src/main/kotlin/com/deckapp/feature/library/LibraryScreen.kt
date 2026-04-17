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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Description
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
import com.deckapp.core.model.CollectionIcon
import com.deckapp.core.model.SearchResultType
import com.deckapp.core.ui.components.*

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

    // Snackbar para mensajes generales
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Snackbar de borrado con opción Deshacer (C-4)
    LaunchedEffect(uiState.pendingDeleteName) {
        val name = uiState.pendingDeleteName ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "\"$name\" eliminado",
            actionLabel = "Deshacer",
            duration = SnackbarDuration.Long
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDeletion()
        }
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Mazos, 1: Tablas, 2: Colecciones
    
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var resourceToAddToCollection by remember { mutableStateOf<Pair<Long, SearchResultType>?>(null) }

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

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, color, icon ->
                viewModel.createCollection(name, color, icon)
                showCreateCollectionDialog = false
            }
        )
    }

    resourceToAddToCollection?.let { (resourceId, type) ->
        AddToCollectionDialog(
            collections = uiState.allCollections,
            onDismiss = { resourceToAddToCollection = null },
            onSelect = { colId ->
                viewModel.addResourceToCollection(colId, resourceId, type)
                resourceToAddToCollection = null
            },
            onCreateNewCollection = {
                resourceToAddToCollection = null
                showCreateCollectionDialog = true
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

            if (uiState.searchQuery.length >= 2 && uiState.searchResults.isNotEmpty()) {
                Text(
                    text = "Resultados globales (${uiState.searchResults.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp)
                ) {
                    items(uiState.searchResults.size) { index ->
                        val result = uiState.searchResults[index]
                        Card(
                            onClick = {
                                if (result.type == SearchResultType.CARD) {
                                    result.parentId?.let { onDeckClick(it) }
                                }
                            },
                            modifier = Modifier.width(200.dp).fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (result.type) {
                                        SearchResultType.CARD -> Icons.Default.Layers
                                        SearchResultType.TABLE -> Icons.Default.Casino
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = result.subtitle ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(top = 12.dp))
            }
            // ── Pestañas de Navegación ────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Mazos", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Tablas", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Baúl", modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Chips: filtro por tag + toggle Archivados (Solo para Mazos) ──
            if (selectedTab == 0) {
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
            }

            // ── Grid de Contenido ─────────────────────────────────────
            when {
                uiState.isLoading || (selectedTab == 2 && uiState.isLoadingCollections) -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
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
                        when (selectedTab) {
                            0 -> { // Mazos
                                gridItems(uiState.filteredDecks, key = { "deck_${it.id}" }) { deck ->
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
                                        onDelete = { viewModel.scheduleDeletion(deck.id) },
                                        onAddToCollection = { resourceToAddToCollection = deck.id to SearchResultType.DECK },
                                        cardCount = uiState.deckCardCounts[deck.id] ?: 0
                                    )
                                }
                            }
                            1 -> { // Tablas
                                gridItems(uiState.allTables, key = { "table_${it.id}" }) { table ->
                                    Card(
                                        onClick = { /* Navegar a tabla */ },
                                        onLongClick = { resourceToAddToCollection = table.id to SearchResultType.TABLE },
                                        modifier = Modifier.fillMaxWidth().height(100.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(table.name, style = MaterialTheme.typography.titleSmall)
                                            Text(table.rollFormula, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                            2 -> { // Colecciones (Baúl)
                                gridItems(uiState.allCollections, key = { "col_${it.id}" }) { collection ->
                                    CollectionGridItem(
                                        collection = collection,
                                        onClick = { viewModel.setActiveCollection(collection.id) },
                                        onLongClick = { /* Opciones de colección */ }
                                    )
                                }
                                
                                // Botón para añadir nueva colección
                                item {
                                    OutlinedCard(
                                        onClick = { showCreateCollectionDialog = true },
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Add, null)
                                                Text("Nueva Colección", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, CollectionIcon) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(CollectionIcon.CHEST) }
    var selectedColor by remember { mutableStateOf(0xFF6200EE.toInt()) } // Violeta por defecto

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Colección") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Icono", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CollectionIcon.values().forEach { icon ->
                        item {
                            FilterChip(
                                selected = selectedIcon == icon,
                                onClick = { selectedIcon = icon },
                                label = { Icon(icon.toIcon(), null, modifier = Modifier.size(20.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, selectedIcon) },
                enabled = name.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddToCollectionDialog(
    collections: List<com.deckapp.core.model.Collection>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreateNewCollection: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir al Baúl") },
        text = {
            if (collections.isEmpty()) {
                Text("No tienes colecciones creadas todavía.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    collections.forEach { col ->
                        item {
                            ListItem(
                                headlineContent = { Text(col.name) },
                                leadingContent = { Icon(col.icon.toIcon(), null, tint = Color(col.color)) },
                                modifier = Modifier.combinedClickable(onClick = { onSelect(col.id) })
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNewCollection) { Text("Nueva Colección") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
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
