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
                                enabled = !uiState.isDuplicating
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
                                enabled = !uiState.isMerging
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
                FloatingActionButton(onClick = { onAddCard(deck.id) }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva carta")
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
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Quitar tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .combinedClickable(onClick = { viewModel.removeTag(tag.id) })
                                    )
                                }
                            )
                        }
                        AssistChip(
                            onClick = { showAddTagDialog = true },
                            label = { Text("+ Tag") }
                        )
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
                            items(uiState.cards, key = { it.id }) { card ->
                                CardThumbnail(
                                    card = card,
                                    modifier = Modifier.combinedClickable(
                                        onClick = { onCardClick(card.id) },
                                        onLongClick = { confirmDeleteCard = card }
                                    ),
                                    height = 120.dp,
                                    showTitle = true,
                                    aspectRatio = uiState.deck?.aspectRatio
                                        ?: com.deckapp.core.model.CardAspectRatio.STANDARD
                                )
                            }
                        }
                    }
                }
            }
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
                                            model = File(deck.coverImagePath),
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
