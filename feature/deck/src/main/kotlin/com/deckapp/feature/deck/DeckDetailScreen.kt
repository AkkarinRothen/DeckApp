package com.deckapp.feature.deck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.LaunchedEffect
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
    
    var confirmDeleteCard by remember { mutableStateOf<Card?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

    // Launcher para guardar el ZIP
    val createZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportToZip(it) } }

    // Launcher para elegir imagen de dorso
    val backImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // En una app real, copiaríamos el archivo a la carpeta interna
        // Por ahora usamos la URI directamente (requiere permisos persistentes o FileProvider)
        // Simplificamos asumiendo que el ViewModel manejará la persistencia o usaremos el path
        uri?.let { viewModel.setBackImage(it.toString()) }
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

                    // Tags del mazo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        deck.tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(tag.name) },
                                trailingIcon = if (deck.isArchived) null else {
                                    {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Quitar tag",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .combinedClickable(onClick = { viewModel.removeTag(tag.id) })
                                        )
                                    }
                                }
                            )
                        }
                        if (!deck.isArchived) {
                            AssistChip(
                                onClick = { showAddTagDialog = true },
                                label = { Text("+ Tag") }
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

                    Text(
                        text = "Mantén presionada una carta para eliminarla",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )

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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.filteredCards, key = { it.id }) { card ->
                                CardThumbnail(
                                    card = card,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onCardClick(card.id) },
                                        onLongClick = { confirmDeleteCard = card }
                                    ),
                                    height = 120.dp,
                                    showTitle = true,
                                    aspectRatio = uiState.deck?.aspectRatio
                                        ?: com.deckapp.core.model.CardAspectRatio.STANDARD,
                                    showModeBadge = true
                                )
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
            onUpdateDrawMode = viewModel::updateDrawMode,
            onUpdateAspectRatio = viewModel::updateAspectRatio,
            onToggleFaceDown = viewModel::toggleDrawFaceDown,
            onPickBackImage = { backImageLauncher.launch("image/*") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckConfigSheet(
    deck: com.deckapp.core.model.CardStack?,
    onDismiss: () -> Unit,
    onUpdateDrawMode: (com.deckapp.core.model.DrawMode) -> Unit,
    onUpdateAspectRatio: (com.deckapp.core.model.CardAspectRatio) -> Unit,
    onToggleFaceDown: (Boolean) -> Unit,
    onPickBackImage: () -> Unit
) {
    if (deck == null) return
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                "Configuración del mazo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- Draw Mode ---
            Text("Modo de robo", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                com.deckapp.core.model.DrawMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = deck.drawMode == mode,
                        onClick = { onUpdateDrawMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = com.deckapp.core.model.DrawMode.entries.size)
                    ) {
                        Text(
                            text = when(mode) {
                                com.deckapp.core.model.DrawMode.TOP -> "Tope"
                                com.deckapp.core.model.DrawMode.BOTTOM -> "Fondo"
                                com.deckapp.core.model.DrawMode.RANDOM -> "Azar"
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Face Down ---
            ListItem(
                headlineContent = { Text("Robar boca abajo") },
                supportingContent = { Text("Las cartas se roban mostrando su dorso por defecto") },
                trailingContent = {
                    Switch(
                        checked = deck.drawFaceDown,
                        onCheckedChange = { onToggleFaceDown(it) }
                    )
                }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // --- Aspect Ratio ---
            Text("Proporción de cartas", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
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

            // --- Back Image ---
            ListItem(
                headlineContent = { Text("Imagen de dorso") },
                supportingContent = { 
                    Text(if (deck.backImagePath != null) "Imagen personalizada activa" else "Sin imagen de dorso")
                },
                leadingContent = {
                    Icon(Icons.Default.Collections, contentDescription = null)
                },
                trailingContent = {
                    TextButton(onClick = onPickBackImage) {
                        Text(if (deck.backImagePath != null) "Cambiar" else "Elegir")
                    }
                }
            )
        }
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
