package com.deckapp.feature.deck

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.LowPriority
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.graphicsLayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import java.io.File
import com.deckapp.core.ui.components.CardThumbnail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DeckDetailScreen(
    onBack: () -> Unit,
    onCardClick: (Long) -> Unit,
    onAddCard: (Long) -> Unit,
    onDeckDuplicated: (Long) -> Unit = {},
    viewModel: DeckDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    
    var confirmDeleteCard by remember { mutableStateOf<Card?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

    // Lista local mutable para el drag & drop — se sincroniza desde el ViewModel cuando
    // NO estamos en modo reordenamiento (cambios externos: borrar, añadir carta).
    val localCards: SnapshotStateList<Card> = remember { mutableStateListOf() }
    LaunchedEffect(uiState.filteredCards, uiState.isReorderMode) {
        if (!uiState.isReorderMode) {
            localCards.clear()
            localCards.addAll(uiState.filteredCards)
        }
    }

    // Launcher para guardar el ZIP
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportToZip(it) } }

    // Launcher para elegir imagen de dorso
    val backImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setBackImage(it.toString()) }
    }

    // Launcher para elegir imagen de portada
    val coverImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCoverImage(it.toString()) }
    }

    // Mostrar mensajes de éxito/error
    LaunchedEffect(uiState.errorMessage, uiState.exportSuccessMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.exportSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Navegar al mazo duplicado cuando esté listo
    LaunchedEffect(uiState.duplicatedDeckId) {
        val newId = uiState.duplicatedDeckId
        if (newId != null) {
            viewModel.onDuplicatedNavHandled()
            onDeckDuplicated(newId)
        }
    }

    // Diálogo para añadir tag
    if (showAddTagDialog) {
        AddTagDialog(
            onConfirm = { name ->
                viewModel.addTag(name)
                showAddTagDialog = false
            },
            onDismiss = { showAddTagDialog = false }
        )
    }

    // Diálogo de confirmación de borrado de carta
    confirmDeleteCard?.let { card ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCard = null },
            title = { Text("Eliminar carta") },
            text = { Text("¿Eliminar \"${card.title}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCard(card.id)
                        confirmDeleteCard = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCard = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (uiState.showMergeDialog) {
        MergeDeckDialog(
            decks = uiState.availableDecks,
            onConfirm = { sourceDeckId ->
                viewModel.mergeWithDeck(sourceDeckId)
            },
            onDismiss = { viewModel.showMergeDialog(false) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deck?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    // Botón reordenar / listo — solo visible si hay ≥2 cartas y el mazo no está archivado
                    if ((uiState.deck?.isArchived != true) && uiState.cards.size >= 2) {
                        if (uiState.isReorderMode) {
                            IconButton(onClick = { viewModel.saveReorder(localCards.map { it.id }) }) {
                                Icon(Icons.Default.Check, contentDescription = "Guardar orden", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            val isFiltered = uiState.suitFilter != null
                            IconButton(
                                onClick = { 
                                    if (!isFiltered) viewModel.toggleReorderMode()
                                    else {
                                        // Opcional: Mostrar snackbar avisando que no se puede reordenar con filtros
                                    }
                                },
                                enabled = !isFiltered
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LowPriority, 
                                    contentDescription = "Reordenar cartas",
                                    tint = if (isFiltered) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) 
                                           else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.showConfigSheet(true) },
                        enabled = uiState.deck?.isArchived != true
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    if (uiState.isDuplicating) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text("Duplicando…")
                                        }
                                    } else {
                                        Text("Duplicar mazo")
                                    }
                                },
                                onClick = {
                                    showOverflow = false
                                    viewModel.duplicateDeck()
                                },
                                enabled = !uiState.isDuplicating && uiState.deck?.isArchived != true
                            )

                            DropdownMenuItem(
                                text = {
                                    if (uiState.isMerging) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text("Fusionando…")
                                        }
                                    } else {
                                        Text("Fusionar mazo")
                                    }
                                },
                                onClick = {
                                    showOverflow = false
                                    viewModel.showMergeDialog(true)
                                },
                                enabled = !uiState.isMerging && uiState.deck?.isArchived != true
                            )

                            DropdownMenuItem(
                                text = {
                                    if (uiState.isExporting) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Text("Exportando…")
                                        }
                                    } else {
                                        Text("Exportar ZIP")
                                    }
                                },
                                onClick = {
                                    showOverflow = false
                                    uiState.deck?.let {
                                        createZipLauncher.launch("${it.name}.zip")
                                    }
                                },
                                enabled = !uiState.isExporting
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            uiState.deck?.let { deck ->
                if (!deck.isArchived) {
                    FloatingActionButton(onClick = { onAddCard(deck.id) }) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva carta")
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.deck == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Mazo no encontrado") }

            else -> {
                val deck = uiState.deck!!
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // Cover header
                    val coverPath = deck.coverImagePath
                    if (coverPath != null) {
                        AsyncImage(
                            model = File(coverPath),
                            contentDescription = deck.name,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Card count
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.cards.size} cartas",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (deck.description.isNotBlank()) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = deck.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Tags del mazo (Solo vista, se editan en Config)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        deck.tags.forEach { tag ->
                            AssistChip(
                                onClick = { viewModel.showConfigSheet(true) },
                                label = { Text(tag.name) }
                            )
                        }
                    }

                    // Chips de filtro por palo (C-3) — solo si hay palos definidos
                    if (uiState.availableSuits.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.suitFilter == null,
                                onClick = { viewModel.setSuitFilter(null) },
                                label = { Text("Todos") }
                            )
                            uiState.availableSuits.forEach { suit ->
                                FilterChip(
                                    selected = uiState.suitFilter == suit,
                                    onClick = { viewModel.setSuitFilter(suit) },
                                    label = { Text(suit) }
                                )
                            }
                        }
                    }

                    if (uiState.isReorderMode) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Modo Reordenamiento",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Usa las flechas o arrastra el icono superior de cada carta.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Button(
                                    onClick = { viewModel.saveReorder(localCards.map { it.id }) },
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Listo", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Mantén presionada una carta para eliminarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    HorizontalDivider()

                    if (uiState.cards.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay cartas en este mazo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val gridState = rememberLazyGridState()
                        val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
                            localCards.add(to.index, localCards.removeAt(from.index))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        // Lógica para mover un paso
                        val onMoveStep = { index: Int, delta: Int ->
                            val targetIndex = index + delta
                            if (targetIndex in localCards.indices) {
                                localCards.add(targetIndex, localCards.removeAt(index))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }

                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(localCards, key = { it.id }) { card ->
                                val currentIndex = localCards.indexOf(card)
                                
                                ReorderableItem(reorderState, key = card.id) { isDragging ->
                                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                                    
                                    Box(
                                        modifier = Modifier.shadow(elevation, RoundedCornerShape(8.dp))
                                    ) {
                                        CardThumbnail(
                                            card = card,
                                            modifier = if (uiState.isReorderMode) {
                                                Modifier // No draggable aquí para evitar activación accidental
                                            } else {
                                                Modifier.combinedClickable(
                                                    onClick = { onCardClick(card.id) },
                                                    onLongClick = { confirmDeleteCard = card }
                                                )
                                            },
                                            height = 120.dp,
                                            showTitle = true,
                                            aspectRatio = uiState.deck?.aspectRatio
                                                ?: com.deckapp.core.model.CardAspectRatio.STANDARD,
                                            showModeBadge = true
                                        )

                                        if (uiState.isReorderMode) {
                                            // Handle de Arrastre (Área específica)
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .longPressDraggableHandle(
                                                        onDragStarted = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    ),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                                            ) {
                                                Icon(
                                                    Icons.Default.DragHandle,
                                                    "Arrastrar",
                                                    modifier = Modifier.padding(4.dp).size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }

                                            // Flechas de movimiento manual
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .padding(bottom = 24.dp), // Encima del título
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                if (currentIndex > 0) {
                                                    Surface(
                                                        onClick = { onMoveStep(currentIndex, -1) },
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(28.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                } else {
                                                    Spacer(Modifier.size(28.dp))
                                                }

                                                if (currentIndex < localCards.size - 1) {
                                                    Surface(
                                                        onClick = { onMoveStep(currentIndex, 1) },
                                                        shape = CircleShape,
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(28.dp),
                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                } else {
                                                    Spacer(Modifier.size(28.dp))
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

    if (uiState.showConfigSheet) {
        DeckConfigSheet(
            deck = uiState.deck,
            onDismiss = { viewModel.showConfigSheet(false) },
            onUpdateName = viewModel::updateName,
            onUpdateDescription = viewModel::updateDescription,
            onUpdateDrawMode = viewModel::updateDrawMode,
            onUpdateAspectRatio = viewModel::updateAspectRatio,
            onToggleFaceDown = viewModel::toggleDrawFaceDown,
            onPickBackImage = { backImageLauncher.launch("image/*") },
            onPickCoverImage = { coverImageLauncher.launch("image/*") },
            onAddTag = viewModel::addTag,
            onRemoveTag = viewModel::removeTag,
            onToggleArchived = { 
                viewModel.toggleArchived()
                viewModel.showConfigSheet(false)
                onBack() // Navegar atrás si se archiva (opcional)
            },
            onDeleteDeck = {
                viewModel.deleteDeck()
                viewModel.showConfigSheet(false)
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DeckConfigSheet(
    deck: com.deckapp.core.model.CardStack?,
    onDismiss: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateDescription: (String) -> Unit,
    onUpdateDrawMode: (com.deckapp.core.model.DrawMode) -> Unit,
    onUpdateAspectRatio: (com.deckapp.core.model.CardAspectRatio) -> Unit,
    onToggleFaceDown: (Boolean) -> Unit,
    onPickBackImage: () -> Unit,
    onPickCoverImage: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onToggleArchived: () -> Unit,
    onDeleteDeck: () -> Unit
) {
    if (deck == null) return
    val sheetState = rememberModalBottomSheetState()
    var nameText by remember { mutableStateOf(deck.name) }
    var descText by remember { mutableStateOf(deck.description) }
    var tagText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(
                "Configuración del mazo",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // --- Metadatos Básicos ---
            Text("General", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = nameText,
                onValueChange = { 
                    nameText = it
                    onUpdateName(it)
                },
                label = { Text("Nombre del mazo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(Modifier.height(12.dp))
            
            OutlinedTextField(
                value = descText,
                onValueChange = { 
                    descText = it
                    onUpdateDescription(it)
                },
                label = { Text("Descripción / Lore") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Spacer(Modifier.height(24.dp))

            // --- Visuales ---
            Text("Aparición y Estilo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            
            ListItem(
                headlineContent = { Text("Imagen de portada") },
                supportingContent = { Text("Se muestra en la cabecera y biblioteca") },
                leadingContent = { Icon(Icons.Default.Image, null) },
                trailingContent = {
                    TextButton(onClick = onPickCoverImage) {
                        Text(if (deck.coverImagePath != null) "Cambiar" else "Elegir")
                    }
                }
            )

            ListItem(
                headlineContent = { Text("Imagen de reverso") },
                supportingContent = { Text("Para cartas boca abajo y animaciones") },
                leadingContent = { Icon(Icons.Default.Collections, null) },
                trailingContent = {
                    TextButton(onClick = onPickBackImage) {
                        Text(if (deck.backImagePath != null) "Cambiar" else "Elegir")
                    }
                }
            )

            Text(
                "Proporción de cartas", 
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.deckapp.core.model.CardAspectRatio.entries.forEach { ratio ->
                    FilterChip(
                        selected = deck.aspectRatio == ratio,
                        onClick = { onUpdateAspectRatio(ratio) },
                        label = { Text(ratio.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- Mecánicas de Juego ---
            Text("Reglas de Robo", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            
            Text(
                "Ubicación del robo", 
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                com.deckapp.core.model.DrawMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = deck.drawMode == mode,
                        onClick = { onUpdateDrawMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = com.deckapp.core.model.DrawMode.entries.size)
                    ) {
                        Text(when(mode) {
                            com.deckapp.core.model.DrawMode.TOP -> "Tope"
                            com.deckapp.core.model.DrawMode.BOTTOM -> "Fondo"
                            com.deckapp.core.model.DrawMode.RANDOM -> "Azar"
                        })
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Robar boca abajo") },
                supportingContent = { Text("Las cartas se ocultan al DM al ser robadas") },
                trailingContent = {
                    Switch(checked = deck.drawFaceDown, onCheckedChange = onToggleFaceDown)
                }
            )

            Spacer(Modifier.height(24.dp))

            // --- Tags ---
            Text("Organización", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        deck.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { onRemoveTag(tag.id) },
                                label = { Text(tag.name) },
                                icon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text("Añadir etiqueta...") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { 
                                if (tagText.isNotBlank()) {
                                    onAddTag(tagText)
                                    tagText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, null)
                            }
                        },
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // --- Zona de Peligro ---
            Text("Gestión Avanzada", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = onToggleArchived,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(if (deck.isArchived) Icons.Default.Unarchive else Icons.Default.Archive, null)
                Spacer(Modifier.width(8.dp))
                Text(if (deck.isArchived) "Desarchivar Mazo" else "Archivar Mazo")
            }
            
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteForever, null)
                Spacer(Modifier.width(8.dp))
                Text("Eliminar permanentemente")
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar mazo?") },
            text = { Text("Esto borrará el mazo \"${deck.name}\" y todas sus cartas. Esta acción es irreversible.") },
            confirmButton = {
                TextButton(onClick = onDeleteDeck) {
                    Text("Eliminar todo", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MergeDeckDialog(
    decks: List<com.deckapp.core.model.CardStack>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fusionar mazo") },
        text = {
            if (decks.isEmpty()) {
                Text("No hay otros mazos disponibles para fusionar.")
            } else {
                Column {
                    Text("Selecciona un mazo para añadir sus cartas a este mazo:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(decks) { deck ->
                            Card(
                                onClick = { onConfirm(deck.id) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (deck.coverImagePath != null) {
                                        AsyncImage(
                                            model = File(deck.coverImagePath!!),
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Text(deck.name, style = MaterialTheme.typography.titleSmall)
                                }
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
private fun AddTagDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir tag") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Nombre del tag") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
