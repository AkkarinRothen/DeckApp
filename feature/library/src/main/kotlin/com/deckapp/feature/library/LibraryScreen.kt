package com.deckapp.feature.library

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
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
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.CollectionIcon
import com.deckapp.core.model.SearchResultType
import com.deckapp.core.ui.components.*
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onDeckClick: (Long) -> Unit,
    onImportClick: () -> Unit,
    onManageTags: () -> Unit,
    onAddToSession: (Long) -> Unit,
    onEncounterLibrary: () -> Unit,
    onNpcLibrary: () -> Unit,
    onWikiClick: () -> Unit,
    onReferenceClick: () -> Unit,
    onHexplorationClick: () -> Unit = {},
    onShowDiceRoller: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
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
    var collectionToEdit by remember { mutableStateOf<com.deckapp.core.model.DeckCollection?>(null) }
    var collectionToDelete by remember { mutableStateOf<com.deckapp.core.model.DeckCollection?>(null) }
    var resourceToAddToCollection by remember { mutableStateOf<Pair<Long, SearchResultType>?>(null) }
    var tagPickerTargetIds by remember { mutableStateOf<List<Long>?>(null) }

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
        CollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, color, icon ->
                viewModel.createCollection(name, color, icon)
                showCreateCollectionDialog = false
            }
        )
    }

    collectionToEdit?.let { collection ->
        CollectionDialog(
            initialName = collection.name,
            initialIcon = collection.icon,
            initialColor = collection.color,
            title = "Editar Colección",
            confirmLabel = "Guardar",
            onDismiss = { collectionToEdit = null },
            onConfirm = { name, color, icon ->
                viewModel.updateCollection(collection.id, name, color, icon)
                collectionToEdit = null
            }
        )
    }

    collectionToDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text("¿Eliminar colección?") },
            text = { Text("Se eliminará la colección \"${collection.name}\". Los mazos y tablas dentro de ella NO se borrarán de tu biblioteca global.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCollection(collection.id)
                        collectionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) { Text("Cancelar") }
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

    tagPickerTargetIds?.let { targetIds ->
        TagSelectionDialog(
            allTags = uiState.allTags,
            onDismiss = { tagPickerTargetIds = null },
            onSelect = { tagId ->
                viewModel.addTagToDecks(targetIds, tagId)
                tagPickerTargetIds = null
            },
            onCreateNew = { name ->
                viewModel.createAndAddTag(targetIds, name)
                tagPickerTargetIds = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.selectedDeckIds.isNotEmpty()) "${uiState.selectedDeckIds.size} seleccionados" else "Biblioteca") },
                actions = {
                    if (uiState.selectedDeckIds.isEmpty()) {
                        val canReorder = (selectedTab == 0 || selectedTab == 1) && 
                                        uiState.searchQuery.isBlank() && 
                                        uiState.selectedTagIds.isEmpty()

                        if (canReorder) {
                            IconButton(onClick = { 
                                if (uiState.isReorderMode) {
                                    if (selectedTab == 0) viewModel.saveSortOrder() else viewModel.saveTableSortOrder()
                                } else {
                                    viewModel.toggleReorderMode()
                                }
                            }) {
                                Icon(
                                    imageVector = if (uiState.isReorderMode) Icons.Default.Save else Icons.Default.Sort,
                                    contentDescription = "Reordenar",
                                    tint = if (uiState.isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = onShowDiceRoller) {
                            Icon(Icons.Default.Casino, contentDescription = "Rodillo de dados")
                        }

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
                            DropdownMenuItem(
                                text = { Text("NPCs y Criaturas") },
                                onClick = {
                                    showMenu = false
                                    onNpcLibrary()
                                },
                                leadingIcon = { Icon(Icons.Default.Group, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Wiki del Mundo") },
                                onClick = {
                                    showMenu = false
                                    onWikiClick()
                                },
                                leadingIcon = { Icon(Icons.Default.MenuBook, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tablas y Reglas") },
                                onClick = {
                                    showMenu = false
                                    onReferenceClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Description, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Mapas Hex") },
                                onClick = {
                                    showMenu = false
                                    onHexplorationClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Layers, null) }
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
        floatingActionButton = {
            if (uiState.isReorderMode) {
                ExtendedFloatingActionButton(
                    onClick = { 
                        if (selectedTab == 0) viewModel.saveSortOrder() else viewModel.saveTableSortOrder()
                    },
                    icon = { Icon(Icons.Default.Save, null) },
                    text = { Text(if (selectedTab == 0) "Guardar Orden Mazos" else "Guardar Orden Tablas") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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
                        onAddTag = { tagPickerTargetIds = uiState.selectedDeckIds.toList() }
                    )
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
                placeholder = { Text("Buscar recursos…") },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                enabled = !uiState.isReorderMode
            )

            // ── Indicador de Colección Activa ─────────────────────────
            if (uiState.activeCollectionId != null) {
                val activeCol = uiState.allCollections.find { it.id == uiState.activeCollectionId }
                activeCol?.let { col ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = { viewModel.setActiveCollection(null) },
                            label = { Text("En: ${col.name}") },
                            icon = { Icon(col.icon.toIcon(), null, tint = Color(col.color), modifier = Modifier.size(18.dp)) }
                        )
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.setActiveCollection(null) }) {
                            Text("Limpiar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

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
                                when (result.type) {
                                    SearchResultType.CARD -> {
                                        result.parentId?.let { onDeckClick(it) }
                                    }
                                    SearchResultType.TABLE -> {
                                        selectedTab = 1 // Switch to Tables tab
                                        viewModel.setSearchQuery("") // Clear search to see the table (or keep it if it filters tables too)
                                        // TODO: Possible highlighting of the specific table
                                    }
                                    SearchResultType.BAUL -> {
                                        viewModel.setActiveCollection(result.id)
                                        viewModel.setSearchQuery("")
                                        selectedTab = 0 // Go to Mazos tab to see the filtered collection
                                    }
                                    else -> {}
                                }
                            },
                            modifier = Modifier.width(220.dp).fillMaxHeight(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (result.type) {
                                                SearchResultType.CARD -> Icons.Default.Layers
                                                SearchResultType.TABLE -> Icons.Default.Casino
                                                SearchResultType.BAUL -> Icons.Default.Archive
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
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
                                    if (!result.snippet.isNullOrBlank()) {
                                        Text(
                                            text = result.snippet ?: "",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
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
                    val deckGridState = rememberLazyGridState()
                    val deckReorderState = rememberReorderableLazyGridState(
                        lazyGridState = deckGridState,
                        onMove = { from, to ->
                            val newList = uiState.allDecks.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                            viewModel.updateDeckOrder(newList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                    
                    val tableGridState = rememberLazyGridState()
                    val tableReorderState = rememberReorderableLazyGridState(
                        lazyGridState = tableGridState,
                        onMove = { from, to ->
                            val newList = uiState.allTables.toMutableList().apply {
                                add(to.index, removeAt(from.index))
                            }
                            viewModel.updateTableOrder(newList)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = if (selectedTab == 0) deckGridState else tableGridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedTab) {
                            0 -> { // Mazos
                                gridItems(uiState.filteredDecks, key = { it.id }) { deck ->
                                    val isSelected = deck.id in uiState.selectedDeckIds
                                    
                                    ReorderableItem(deckReorderState, key = deck.id) { isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                        
                                        Surface(
                                            tonalElevation = elevation,
                                            shadowElevation = elevation,
                                            shape = MaterialTheme.shapes.medium,
                                            modifier = Modifier.draggableHandle(
                                                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                                enabled = uiState.isReorderMode
                                            )
                                        ) {
                                            DeckCoverCard(
                                                deck = deck,
                                                isSelected = isSelected,
                                                onLongClick = { if (!uiState.isReorderMode) viewModel.toggleDeckSelection(deck.id) },
                                                onClick = {
                                                    if (!uiState.isReorderMode) {
                                                        if (uiState.selectedDeckIds.isNotEmpty()) {
                                                            viewModel.toggleDeckSelection(deck.id)
                                                        } else {
                                                            onDeckClick(deck.id)
                                                        }
                                                    }
                                                },
                                                onAddToSession = { if (!uiState.isReorderMode) onAddToSession(deck.id) },
                                                onDuplicate = if (!uiState.isReorderMode && !uiState.showArchived && uiState.selectedDeckIds.isEmpty()) ({ viewModel.duplicateDeck(deck.id) }) else null,
                                                onMergeWith = if (!uiState.isReorderMode && !uiState.showArchived && uiState.selectedDeckIds.isEmpty()) ({ viewModel.startMerge(deck.id) }) else null,
                                                onArchive = { if (!uiState.isReorderMode) viewModel.archiveDeck(deck.id, !deck.isArchived) },
                                                onDelete = { if (!uiState.isReorderMode) viewModel.scheduleDeletion(deck.id) },
                                                onAddToCollection = { if (!uiState.isReorderMode) resourceToAddToCollection = deck.id to SearchResultType.DECK },
                                                onTag = { if (!uiState.isReorderMode) tagPickerTargetIds = listOf(deck.id) },
                                                cardCount = uiState.deckCardCounts[deck.id] ?: 0
                                            )
                                        }
                                    }
                                }
                            }
                            1 -> { // Tablas
                                gridItems(uiState.allTables, key = { it.id }) { table ->
                                    ReorderableItem(tableReorderState, key = table.id) { isDragging ->
                                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                        
                                        Card(
                                            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(100.dp)
                                                .draggableHandle(
                                                    onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                                    enabled = uiState.isReorderMode
                                                )
                                                .then(
                                                    if (!uiState.isReorderMode) {
                                                        Modifier.combinedClickable(
                                                            onClick = { /* Navegar a tabla */ },
                                                            onLongClick = { resourceToAddToCollection = table.id to SearchResultType.TABLE }
                                                        )
                                                    } else Modifier
                                                )
                                        ) {
                                            Column(Modifier.padding(12.dp)) {
                                                Text(table.name, style = MaterialTheme.typography.titleSmall)
                                                Text(table.rollFormula, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Colecciones (Baúl)
                                gridItems(uiState.allCollections, key = { "col_${it.id}" }) { collection ->
                                    CollectionGridItem(
                                        collection = collection,
                                        onClick = { 
                                            viewModel.setActiveCollection(collection.id)
                                            selectedTab = 0 // Auto-cambiar a Mazos para ver contenido
                                        },
                                        onLongClick = { collectionToDelete = collection },
                                        onEdit = { collectionToEdit = collection }
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
fun CollectionDialog(
    initialName: String = "",
    initialColor: Int = 0xFF6200EE.toInt(),
    initialIcon: CollectionIcon = CollectionIcon.CHEST,
    title: String = "Nueva Colección",
    confirmLabel: String = "Crear",
    onDismiss: () -> Unit,
    onConfirm: (String, Int, CollectionIcon) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedIcon by remember { mutableStateOf(initialIcon) }
    var selectedColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
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
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToCollectionDialog(
    collections: List<com.deckapp.core.model.DeckCollection>,
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
fun TagSelectionDialog(
    allTags: List<com.deckapp.core.model.Tag>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    onCreateNew: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTags = remember(searchQuery, allTags) {
        if (searchQuery.isBlank()) allTags
        else allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val exactMatch = remember(searchQuery, allTags) {
        allTags.any { it.name.equals(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar Etiqueta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar o crear etiqueta…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredTags.isEmpty() && searchQuery.isNotBlank() && !exactMatch) {
                    TextButton(
                        onClick = { onCreateNew(searchQuery) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Crear nueva: \"$searchQuery\"")
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(filteredTags.size) { index ->
                            val tag = filteredTags[index]
                            ListItem(
                                headlineContent = { Text(tag.name) },
                                leadingContent = { 
                                    Box(
                                        Modifier.size(16.dp)
                                            .clip(CircleShape)
                                            .background(Color(tag.color))
                                    ) 
                                },
                                modifier = Modifier.clickable { onSelect(tag.id) }
                            )
                        }
                    }
                    
                    if (searchQuery.isNotBlank() && !exactMatch) {
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        TextButton(
                            onClick = { onCreateNew(searchQuery) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear nueva: \"$searchQuery\"")
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
