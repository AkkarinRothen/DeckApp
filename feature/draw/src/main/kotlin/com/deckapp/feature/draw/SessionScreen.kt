package com.deckapp.feature.draw

import android.app.Activity
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.ui.components.CardThumbnail
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.feature.tables.TablesTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onSessionEnd: () -> Unit,
    onBrowseDeck: (deckId: Long) -> Unit = {},
    onCreateTable: () -> Unit = {},
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tabCount = if (uiState.hasActiveEncounter) 4 else 3
    val pagerState = rememberPagerState(pageCount = { tabCount })

    // Sync pager → ViewModel al cambiar de tab por gesto
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setActiveTab(pagerState.currentPage)
    }
    // Sync ViewModel → pager para navegación programática futura
    LaunchedEffect(uiState.activeTab) {
        if (pagerState.currentPage != uiState.activeTab) {
            pagerState.animateScrollToPage(uiState.activeTab)
        }
    }

    // Launcher para guardar el log
    val exportLogLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportLog(it, context.contentResolver) }
    }

    // Screen wake lock
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // Haptic al robar
    val prevHandSize = remember { mutableIntStateOf(0) }
    LaunchedEffect(uiState.hand.size) {
        if (uiState.hand.size > prevHandSize.intValue) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        prevHandSize.intValue = uiState.hand.size
    }

    // Snackbar
    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        viewModel.clearSnackbar()
    }

    // Navegar de vuelta cuando la sesión se finaliza
    LaunchedEffect(uiState.sessionEnded) {
        if (uiState.sessionEnded) onSessionEnd()
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showDealDialog by remember { mutableStateOf(false) }
    var showEndSessionDialog by remember { mutableStateOf(false) }

    if (showEndSessionDialog) {
        AlertDialog(
            onDismissRequest = { showEndSessionDialog = false },
            title = { Text("Finalizar sesión") },
            text = {
                Text("¿Finalizar \"${uiState.session?.name ?: "esta sesión"}\"? Las cartas en mano y descarte quedarán registradas en el historial.")
            },
            confirmButton = {
                Button(
                    onClick = { showEndSessionDialog = false; viewModel.endSession() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Finalizar") }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Peek dialog
    uiState.peekCard?.let { card ->
        PeekCardDialog(card = card, onDismiss = { viewModel.clearPeek() })
    }

    // Deal dialog
    if (showDealDialog) {
        DealCardsDialog(
            onConfirm = { count -> viewModel.dealCards(count); showDealDialog = false },
            onDismiss = { showDealDialog = false }
        )
    }

    val selectedDeckName = uiState.deckNames[uiState.selectedDeckId]
        ?: uiState.session?.name ?: "Sesión"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.session?.name ?: "Sesión",
                            style = MaterialTheme.typography.titleMedium
                        )
                        val summary = buildString {
                            append("${uiState.hand.size} en mano")
                            if (uiState.deckRefs.size > 1) {
                                uiState.deckRefs.forEach { ref ->
                                    val name = uiState.deckNames[ref.stackId]?.let {
                                        if (it.length > 10) it.take(10) + "…" else it
                                    } ?: "—"
                                    val count = uiState.deckCardCounts[ref.stackId] ?: 0
                                    append("  ·  $name $count")
                                }
                            }
                        }
                        val elapsed = uiState.sessionElapsedMinutes
                        val timerText = when {
                            elapsed < 1 -> "Iniciada recién"
                            elapsed < 60 -> "${elapsed}m"
                            else -> "${elapsed / 60}h ${elapsed % 60}m"
                        }
                        Text(
                            text = "$summary  ·  $timerText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.undo() }, enabled = uiState.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ver tope del mazo") },
                                onClick = { menuExpanded = false; viewModel.peekTopCard() }
                            )
                            DropdownMenuItem(
                                text = { Text("Ver mazo completo") },
                                onClick = {
                                    menuExpanded = false
                                    val deckId = uiState.selectedDeckId
                                    if (deckId != null) onBrowseDeck(deckId)
                                },
                                enabled = uiState.selectedDeckId != null
                            )
                            DropdownMenuItem(
                                text = { Text("Exportar historial (.txt)") },
                                onClick = {
                                    menuExpanded = false
                                    exportLogLauncher.launch("log_${uiState.session?.name ?: "sesion"}.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Repartir cartas…") },
                                onClick = { menuExpanded = false; showDealDialog = true }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Descartar mano completa") },
                                onClick = {
                                    menuExpanded = false
                                    uiState.hand.forEach { viewModel.discardCard(it.id) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Ordenar mano por valor") },
                                onClick = {
                                    menuExpanded = false
                                    viewModel.sortByValue()
                                },
                                enabled = uiState.hand.size > 1
                            )
                            DropdownMenuItem(
                                text = { Text("Resetear todos los mazos") },
                                onClick = { menuExpanded = false; viewModel.resetDeck() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.session?.showCardTitles == true)
                                            "Ocultar nombres de cartas"
                                        else "Mostrar nombres de cartas"
                                    )
                                },
                                onClick = { menuExpanded = false; viewModel.toggleCardTitles() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (uiState.nightMode) "Desactivar modo nocturno"
                                        else "Modo nocturno"
                                    )
                                },
                                onClick = { menuExpanded = false; viewModel.toggleNightMode() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Finalizar sesión",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = { menuExpanded = false; showEndSessionDialog = true }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            SessionFab(
                currentPage = pagerState.currentPage,
                selectedDeckName = selectedDeckName,
                deckRefsSize = uiState.deckRefs.size,
                onDraw = { viewModel.drawCard() },
                onRoll = { viewModel.rollActiveTable() },
                onNote = { viewModel.startQuickNote() }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Tab Row ──────────────────────────────────────────────
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Mazos") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Tablas") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("Notas") }
                    )
                    if (uiState.hasActiveEncounter) {
                        Tab(
                            selected = pagerState.currentPage == 3,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                            text = { Text("Combate") }
                        )
                    }
                }

                // ── Pager ────────────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    when (page) {
                        0 -> MazosTab(
                            uiState = uiState,
                            onCardClick = onCardClick,
                            onDiscard = { viewModel.discardCard(it) },
                            onSelectDeck = { viewModel.selectDeck(it) }
                        )
                        1 -> TablesTab(
                            sessionId = uiState.session?.id,
                            onCreateTable = onCreateTable
                        )
                        2 -> NotesTab(
                            notes = uiState.dmNotes,
                            onNotesChange = { viewModel.updateNotes(it) }
                        )
                        else -> SessionTabPlaceholder(label = "Combate")
                    }
                }
            }

            // ── Night mode overlay (sobre todos los tabs) ────────────────
            if (uiState.nightMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { viewModel.toggleNightMode() }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// FAB contextual
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SessionFab(
    currentPage: Int,
    selectedDeckName: String,
    deckRefsSize: Int,
    onDraw: () -> Unit,
    onRoll: () -> Unit,
    onNote: () -> Unit
) {
    val (label, action) = when (currentPage) {
        0 -> "ROBAR" to onDraw
        1 -> "TIRAR" to onRoll
        2 -> "NOTA" to onNote
        else -> "ROBAR" to onDraw
    }
    LargeFloatingActionButton(
        onClick = action,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            if (currentPage == 0 && deckRefsSize > 1) {
                Text(
                    text = if (selectedDeckName.length > 12)
                        selectedDeckName.take(12) + "…"
                    else selectedDeckName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab 0 — Mazos (contenido actual de SessionScreen)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun MazosTab(
    uiState: SessionUiState,
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onDiscard: (cardId: Long) -> Unit,
    onSelectDeck: (stackId: Long) -> Unit
) {
    var pileExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Mano ──────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                uiState.hand.isEmpty() -> Box(
                    Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Pulsa ROBAR para sacar una carta",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy((-24).dp)
                ) {
                    items(uiState.hand, key = { it.id }) { card ->
                        SwipeToDiscardCard(
                            card = card,
                            deckBadge = if (uiState.deckRefs.size > 1)
                                uiState.deckNames[card.stackId]
                            else null,
                            showTitle = uiState.session?.showCardTitles ?: true,
                            aspectRatio = uiState.deckAspectRatios[card.stackId]
                                ?: CardAspectRatio.STANDARD,
                            onTap = { onCardClick(card.id, uiState.session?.id ?: 0L) },
                            onDiscard = { onDiscard(card.id) }
                        )
                    }
                }
            }
        }

        // ── Barra de mazos (solo si hay más de 1 mazo en la sesión) ──
        if (uiState.deckRefs.size > 1) {
            DeckBar(
                deckRefs = uiState.deckRefs,
                deckNames = uiState.deckNames,
                deckCounts = uiState.deckCardCounts,
                selectedDeckId = uiState.selectedDeckId,
                onSelect = onSelectDeck
            )
        }

        // ── Tray de descarte ──────────────────────────────────────────
        PileTray(
            cardCount = uiState.pile.size,
            expanded = pileExpanded,
            onToggle = { pileExpanded = !pileExpanded },
            pile = uiState.pile
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tabs placeholder (Sprint 5/6/7 los rellenan)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    var isPreview by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(notes) }
    
    // Sync local state with domain state (when coming from VM)
    LaunchedEffect(notes) {
        if (text != notes) text = notes
    }

    // Auto-save debounce
    LaunchedEffect(text) {
        if (text == notes) return@LaunchedEffect
        delay(1000)
        onNotesChange(text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Notas de la sesión",
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(
                onClick = { isPreview = !isPreview },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = if (isPreview) Icons.Default.Edit else Icons.Default.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (isPreview) "Editar" else "Vista previa")
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            if (isPreview) {
                Box(Modifier.fillMaxSize().padding(12.dp)) {
                    if (text.isBlank()) {
                        Text(
                            "Nada para mostrar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        MarkdownText(
                            markdown = text,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Escribe tus notas aquí...") },
                    modifier = Modifier.fillMaxSize(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@Composable
private fun SessionTabPlaceholder(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label — Próximamente",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Barra de selección de mazo activo
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckBar(
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    deckCounts: Map<Long, Int>,
    selectedDeckId: Long?,
    onSelect: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(deckRefs, key = { it.stackId }) { ref ->
                val name = deckNames[ref.stackId] ?: "Mazo"
                val count = deckCounts[ref.stackId] ?: 0
                val isSelected = ref.stackId == selectedDeckId

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(ref.stackId) },
                    label = {
                        Column(
                            modifier = Modifier.padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$count disponibles",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Carta en mano con swipe para descartar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun SwipeToDiscardCard(
    card: Card,
    deckBadge: String?,
    showTitle: Boolean,
    aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD,
    onTap: () -> Unit,
    onDiscard: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 200f

    Box(
        modifier = Modifier
            .clickable { onTap() }
            .pointerInput("swipe-${card.id}") {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -dismissThreshold) onDiscard()
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceAtMost(0f)
                    }
                )
            }
    ) {
        CardThumbnail(
            card = card,
            height = 160.dp,
            showTitle = showTitle,
            aspectRatio = aspectRatio,
            modifier = Modifier.graphicsLayer { translationX = offsetX }
        )

        // Badge con nombre del mazo (solo en sesiones multi-mazo)
        if (deckBadge != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationX = offsetX }
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            ) {
                Text(
                    text = deckBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Diálogos: Peek y Repartir
// ──────────────────────────────────────────────────────────────────────────────

/** Muestra la imagen y nombre de la carta del tope del mazo sin robarla. */
@Composable
private fun PeekCardDialog(card: Card, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tope del mazo") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val imagePath = card.activeFace.imagePath
                if (imagePath != null) {
                    AsyncImage(
                        model = File(imagePath),
                        contentDescription = card.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Text(card.title, style = MaterialTheme.typography.titleMedium)
                val suit = card.suit
                if (!suit.isNullOrBlank()) {
                    Text(
                        suit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

/** Dialog para repartir N cartas del mazo seleccionado. */
@Composable
private fun DealCardsDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var countText by remember { mutableStateOf("") }
    val count = countText.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Repartir cartas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "¿Cuántas cartas querés robar del mazo seleccionado?",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { count?.let(onConfirm) },
                enabled = count != null && count > 0
            ) { Text("Robar ${if (count != null && count > 0) count else ""}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ──────────────────────────────────────────────────────────────────────────────
// Tray de descarte
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PileTray(
    cardCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    pile: List<Card>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Descarte ($cardCount)", style = MaterialTheme.typography.labelLarge)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                    else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (expanded) "Colapsar" else "Expandir"
                )
            }

            AnimatedVisibility(visible = expanded) {
                if (pile.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin cartas descartadas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(120.dp)
                    ) {
                        items(pile, key = { it.id }) { card ->
                            CardThumbnail(card = card, height = 96.dp, showTitle = false)
                        }
                    }
                }
            }
        }
    }
}
