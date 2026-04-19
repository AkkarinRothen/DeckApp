@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.deckapp.feature.draw

import android.annotation.SuppressLint
import android.app.Activity
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.deckapp.core.ui.components.MarkdownToolbar
import com.deckapp.feature.draw.components.ResourceManagerDialog
import com.deckapp.feature.draw.components.SessionConfigSheet
import com.deckapp.feature.encounters.CombatTab
import com.deckapp.feature.reference.ReferenceTab
import com.deckapp.feature.tables.TablesTab
import com.deckapp.feature.tables.TablesViewModel
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
    onImportTable: () -> Unit = {},
    onExplorePacks: () -> Unit = {},
    onEditReferenceTable: (Long) -> Unit = {},
    onEditSystemRule: (Long) -> Unit = {},
    onNewReferenceTable: (String) -> Unit = { _ -> },
    onNewSystemRule: (String) -> Unit = { _ -> },
    viewModel: SessionViewModel = hiltViewModel(),
    tablesViewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tablesUiState by tablesViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tabCount = if (uiState.hasActiveEncounter) 6 else 5
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
                @SuppressLint("MissingPermission")
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
    var fabExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(pagerState.currentPage) { fabExpanded = false }

    // Quick Note dialog
    if (uiState.showQuickNoteDialog) {
        QuickNoteDialog(
            onConfirm = { text -> viewModel.addQuickNote(text) },
            onDismiss = { viewModel.dismissQuickNote() }
        )
    }

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

    // Resource Manager Dialog
    if (uiState.showResourceManager) {
        ResourceManagerDialog(
            allDecks = uiState.allDecks,
            allTables = uiState.allTables,
            deckCardCounts = uiState.deckCardCounts,
            selectedDeckIds = uiState.deckRefs.map { it.stackId }.toSet(),
            selectedTableIds = uiState.tablesInSession.map { it.id }.toSet(),
            onToggleDeck = { viewModel.toggleDeckInSession(it) },
            onToggleTable = { viewModel.toggleTableInSession(it) },
            onDismiss = { viewModel.dismissResourceManager() }
        )
    }

    // Session Config Sheet
    if (uiState.showSessionConfig) {
        SessionConfigSheet(
            selectedSystems = uiState.session?.gameSystems ?: emptyList(),
            onSystemsChanged = { viewModel.updateGameSystems(it) },
            availableSystems = uiState.availableSystems,
            onDismiss = { viewModel.dismissSessionConfig() }
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
                                text = { Text("Robar por palo…") },
                                onClick = { menuExpanded = false; viewModel.openDrawBySuit() },
                                enabled = uiState.selectedDeckId != null
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
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Exportar historial (.txt)") },
                                onClick = {
                                    menuExpanded = false
                                    exportLogLauncher.launch("log_${uiState.session?.name ?: "sesion"}.txt")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Gestionar recursos…") },
                                onClick = { menuExpanded = false; viewModel.openResourceManager() }
                            )
                            DropdownMenuItem(
                                text = { Text("Configuración sesión…") },
                                onClick = { menuExpanded = false; viewModel.openSessionConfig() }
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
                hasActiveTable = tablesUiState.activeTable != null,
                expanded = fabExpanded,
                isSimplifiedMode = uiState.isSimplifiedMode,
                onToggleExpand = { fabExpanded = !fabExpanded },
                onDraw = { fabExpanded = false; viewModel.drawCard() },
                onPeek = { fabExpanded = false; viewModel.peekTopCard() },
                onDeal = { fabExpanded = false; showDealDialog = true },
                onShufflePile = { fabExpanded = false; viewModel.shufflePileBack(uiState.selectedDeckId) },
                onRoll = {
                    val activeTable = tablesUiState.activeTable
                    if (activeTable != null) {
                        tablesViewModel.rollTable(activeTable.id, uiState.session?.id)
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Abre una tabla para usar TIRAR")
                        }
                    }
                },
                onNote = { viewModel.showQuickNote() }
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
                if (uiState.deckRefs.size > 1) {
                    DeckBar(
                        deckRefs = uiState.deckRefs,
                        deckNames = uiState.deckNames,
                        deckCounts = uiState.deckCardCounts,
                        selectedDeckId = uiState.selectedDeckId,
                        onSelect = { viewModel.selectDeck(it) }
                    )
                }
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
                        text = { Text("Pilas") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 2,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                        text = { Text("Tablas") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 3,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                        text = { Text("Ref.") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 4,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(4) } },
                        text = { Text("Notas") }
                    )
                    if (uiState.hasActiveEncounter) {
                        Tab(
                            selected = pagerState.currentPage == 5,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(5) } },
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
                        0 -> DeckWorkspace(
                            uiState = uiState,
                            onCardClick = onCardClick,
                            onDiscard = { viewModel.discardCard(it) },
                            onRevealCard = { viewModel.revealCard(it) },
                            onToggleCollapse = { viewModel.toggleDeckCollapse(it) },
                            onInteractWithDeck = { viewModel.setLastInteractedDeck(it) },
                            onDrawFromDeck = { viewModel.drawCardFromDeck(it) },
                            onResetDeck = { viewModel.resetSingleDeck(it) },

                            onShuffle = { viewModel.shufflePileBack(it) },
                            onRollTable = { tablesViewModel.rollTable(it, uiState.session?.id) },
                            onManageResources = { viewModel.openResourceManager() }
                        )
                        1 -> PilasTab(
                            uiState = uiState,
                            onReturnToHand = { viewModel.returnToHand(it) },
                            onReturnToDeck = { viewModel.returnToDeck(it) },
                            onShuffleBack = { viewModel.shufflePileBack(it) }
                        )
                        2 -> TablesTab(
                            sessionId = uiState.session?.id,
                            onCreateTable = onCreateTable,
                            onImportTable = onImportTable
                        )
                        3 -> ReferenceTab(
                            sessionGameSystems = uiState.session?.gameSystems ?: emptyList(),
                            onEditTable = onEditReferenceTable,
                            onEditRule = onEditSystemRule,
                            onNewTable = { onNewReferenceTable(uiState.session?.gameSystems?.firstOrNull() ?: "") },
                            onNewRule = { onNewSystemRule(uiState.session?.gameSystems?.firstOrNull() ?: "") }
                        )
                        4 -> NotesTab(
                            notes = uiState.dmNotes,
                            isSaving = uiState.isSavingNotes,
                            onNotesChange = { viewModel.updateNotes(it) }
                        )
                        5 -> uiState.activeEncounter?.let { encounter ->
                            CombatTab(
                                encounter = encounter,
                                players = uiState.playerParticipants,
                                log = uiState.combatLog,
                                onApplyDamage = { id, delta -> viewModel.applyDamage(id, delta) },
                                onNextTurn = { viewModel.nextTurn() },
                                onToggleCondition = { id, cond -> viewModel.toggleCondition(id, cond) },
                                onEndEncounter = { viewModel.endEncounter() },
                                onAddPlayer = { name, init -> viewModel.addPlayerParticipant(name, init) },
                                onRemovePlayer = { id -> viewModel.removePlayerParticipant(id) }
                            )
                        } ?: SessionTabPlaceholder(label = "Combate")
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

            // ── C-5: Animación de barajar ─────────────────────────────────
            AnimatedVisibility(
                visible = uiState.isShuffling,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f),
                modifier = Modifier.align(Alignment.Center)
            ) {
                ShuffleAnimationOverlay()
            }
        }
    }

    // ── D-2: Sheet "Robar por palo" ───────────────────────────────────────────
    if (uiState.showDrawBySuitSheet) {
        DrawBySuitSheet(
            suits = uiState.availableSuitsForDraw,
            onSuitSelected = { viewModel.drawCardBySuit(it) },
            onDismiss = { viewModel.closeDrawBySuitSheet() }
        )
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
    hasActiveTable: Boolean,
    expanded: Boolean,
    isSimplifiedMode: Boolean,
    onToggleExpand: () -> Unit,
    onDraw: () -> Unit,
    onPeek: () -> Unit,
    onDeal: () -> Unit,
    onShufflePile: () -> Unit,
    onRoll: () -> Unit,
    onNote: () -> Unit
) {
    val containerColor = if (currentPage == 2 && !hasActiveTable)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    else
        MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.BottomCenter) {
        // Sub-acciones — solo en Tab 0 (Mazos) y si NO está en modo simplificado
        AnimatedVisibility(
            visible = currentPage == 0 && expanded && !isSimplifiedMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit  = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 96.dp)
            ) {
                SmallFabWithLabel(Icons.Default.Visibility, "Pico", onPeek)
                SmallFabWithLabel(Icons.Default.Style, "Repartir", onDeal)
                SmallFabWithLabel(Icons.Default.History, "Barajar descarte", onShufflePile)
            }
        }

        // FAB principal
        val label = when {
            currentPage == 0 && expanded && !isSimplifiedMode -> "CERRAR"
            currentPage == 0 -> "ROBAR"
            currentPage == 1 -> "BARAJAR"
            currentPage == 2 -> "TIRAR"
            currentPage == 3 -> "NOTA"
            else -> "ROBAR"
        }
        val primaryAction: () -> Unit = when {
            currentPage == 0 && isSimplifiedMode -> onDraw
            currentPage == 0 -> onToggleExpand
            currentPage == 1 -> onDraw // "Barajar" reutiliza onDraw? Revisar SessionScreen call
            currentPage == 2 -> onRoll
            currentPage == 3 -> onNote
            else -> onDraw
        }

        LargeFloatingActionButton(
            onClick = primaryAction,
            shape = CircleShape,
            containerColor = containerColor
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (currentPage == 0 && deckRefsSize > 1 && !expanded) {
                    Text(
                        text = if (selectedDeckName.length > 12)
                            selectedDeckName.take(12) + "…" else selectedDeckName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                if (currentPage == 2 && hasActiveTable) {
                    Text(
                        text = "tabla activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Tab 0 — DeckWorkspace (Bento Clusters, Sprint 14.5)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckWorkspace(
    uiState: SessionUiState,
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onDiscard: (cardId: Long) -> Unit,
    onRevealCard: (cardId: Long) -> Unit,
    onToggleCollapse: (stackId: Long) -> Unit,
    onInteractWithDeck: (stackId: Long) -> Unit,
    onDrawFromDeck: (stackId: Long) -> Unit,
    onResetDeck: (stackId: Long) -> Unit,
    onShuffle: (Long?) -> Unit,
    onRollTable: (Long) -> Unit,
    onManageResources: () -> Unit
) {
    var pileExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.deckRefs.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No hay mazos en esta sesión",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onManageResources) {
                        Icon(Icons.Default.Style, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gestionar recursos")
                    }
                }
            }
        } else {
            // ── Clusters de mazos ─────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.deckRefs, key = { it.stackId }) { ref ->
                    val cardsInCluster = uiState.handByDeck[ref.stackId] ?: emptyList()
                    val isCollapsed = ref.stackId in uiState.collapsedDeckIds
                    val available = uiState.deckCardCounts[ref.stackId] ?: 0
                    val deckName = uiState.deckNames[ref.stackId] ?: "Mazo"

                    DeckClusterItem(
                        deckName = deckName,
                        cards = cardsInCluster,
                        availableCount = available,
                        isCollapsed = isCollapsed,
                        showTitle = uiState.session?.showCardTitles ?: true,
                        aspectRatio = uiState.deckAspectRatios[ref.stackId] ?: CardAspectRatio.STANDARD,
                        backImagePath = uiState.deckBackImages[ref.stackId],
                        sessionId = uiState.session?.id ?: 0L,
                        isSimplifiedMode = uiState.isSimplifiedMode,
                        onToggleCollapse = { onToggleCollapse(ref.stackId) },
                        onInteract = { onInteractWithDeck(ref.stackId) },
                        onDraw = { onDrawFromDeck(ref.stackId) },
                        onCardClick = onCardClick,
                        onDiscard = onDiscard,
                        onRevealCard = onRevealCard,
                        onRollTable = onRollTable
                    )
                }
            }
        }

        // ── Tray de descarte ──────────────────────────────────────────────────
        PileTray(
            cardCount = uiState.pile.size,
            expanded = pileExpanded,
            onToggle = { pileExpanded = !pileExpanded },
            pile = uiState.pile,
            onShuffle = { onShuffle(uiState.selectedDeckId) }
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Cluster de un mazo
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeckClusterItem(
    deckName: String,
    cards: List<Card>,
    availableCount: Int,
    isCollapsed: Boolean,
    showTitle: Boolean,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    sessionId: Long,
    isSimplifiedMode: Boolean,
    onToggleCollapse: () -> Unit,
    onInteract: () -> Unit,
    onDraw: () -> Unit,
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onDiscard: (cardId: Long) -> Unit,
    onRevealCard: (cardId: Long) -> Unit,
    onRollTable: (Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // ── Header del cluster ────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isSimplifiedMode) {
                            onDraw()
                            onInteract()
                        } else {
                            onToggleCollapse()
                        }
                    }
                    .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (cards.isEmpty())
                            "$availableCount disponibles"
                        else
                            "${cards.size} en mano · $availableCount disponibles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isSimplifiedMode) {
                    IconButton(
                        onClick = { onToggleCollapse() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isCollapsed) "Expandir" else "Colapsar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Cuerpo del cluster (animado) ──────────────────────────────
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (cards.isEmpty()) {
                    // Slot vacío: call-to-action visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                if (isSimplifiedMode) {
                                    onDraw()
                                }
                                onInteract()
                            }
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isSimplifiedMode) "Pulsa para ROBAR" else "Selecciona y pulsa ROBAR",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Cartas en FlowRow — se adaptan a múltiples filas
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy((-16).dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cards.forEach { card ->
                            CompactCardItem(
                                card = card,
                                showTitle = showTitle,
                                aspectRatio = aspectRatio,
                                backImagePath = backImagePath,
                                onTap = {
                                    onInteract()
                                    if (card.isRevealed) onCardClick(card.id, sessionId)
                                    else onRevealCard(card.id)
                                },
                                onDiscard = { onDiscard(card.id) },
                                onRollTable = onRollTable
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Carta compacta (120dp, para clusters del Workspace)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun CompactCardItem(
    card: Card,
    showTitle: Boolean,
    aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD,
    backImagePath: String? = null,
    onTap: () -> Unit,
    onDiscard: () -> Unit,
    onRollTable: (Long) -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 180f
    val cardHeight = 120.dp
    val cardWidth = cardHeight * aspectRatio.ratio
    val faceDown = !card.isRevealed

    Box(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .graphicsLayer { translationX = offsetX; alpha = if (offsetX < -80f) 0.6f else 1f }
            .clip(MaterialTheme.shapes.small)
            .clickable { onTap() }
            .pointerInput("compact-swipe-${card.id}") {
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
        if (faceDown && backImagePath != null) {
            AsyncImage(
                model = File(backImagePath ?: ""),
                contentDescription = "Dorso",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val activeFace = card.activeFace
            if (activeFace.imagePath != null) {
                AsyncImage(
                    model = File(activeFace.imagePath ?: ""),
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Indicador de linkedTable
        if (card.linkedTableId != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = "Tabla enlazada",
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}


// ──────────────────────────────────────────────────────────────────────────────
// Tabs placeholder (Sprint 5/6/7 los rellenan)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab(
    notes: String,
    isSaving: Boolean,
    onNotesChange: (String) -> Unit
) {
    var isPreview by remember { mutableStateOf(false) }
    
    // Usamos TextFieldValue para manejar selección y cursor (necesario para el toolbar)
    var textValue by remember { 
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(notes)) 
    }
    
    // Sync local state with domain state (when coming from VM)
    LaunchedEffect(notes) {
        if (textValue.text != notes) {
            textValue = textValue.copy(text = notes)
        }
    }

    // Auto-save debounce
    LaunchedEffect(textValue.text) {
        if (textValue.text == notes) return@LaunchedEffect
        delay(800)
        onNotesChange(textValue.text)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // ── Header con controles ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Notas de la sesión",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                // Indicador de guardado
                AnimatedVisibility(
                    visible = isSaving,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Guardando…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                if (!isSaving && textValue.text.isNotEmpty()) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

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

        Spacer(Modifier.height(4.dp))

        // ── Editor / Preview ─────────────────────────────────────────
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                if (isPreview) {
                    Box(Modifier.weight(1f).fillMaxSize().padding(16.dp)) {
                        if (textValue.text.isBlank()) {
                            Text(
                                "Nada para mostrar. Pulsa 'Editar' para empezar.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            MarkdownText(
                                markdown = textValue.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    // Toolbar solo visible en modo edición y cuando hay foco (implícito en el diseño)
                    MarkdownToolbar(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = { Text("Utiliza Markdown para dar formato…") },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Quick Note dialog
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickNoteDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ElectricBolt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Nota rápida") 
            }
        },
        text = {
            Column {
                Text(
                    "Se añadirá automáticamente a tus notas con un timestamp.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("¿Qué acaba de pasar?") },
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
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
    backImagePath: String? = null,
    onTap: () -> Unit,
    onReveal: () -> Unit,
    onDiscard: () -> Unit,
    onRollTable: (Long) -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val dismissThreshold = 200f
    val faceDown = !card.isRevealed

    Box(
        modifier = Modifier
            .clickable { if (faceDown) onReveal() else onTap() }
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
        if (faceDown) {
            // Cara del dorso — imagen personalizada del mazo o placeholder
            val cardHeight = 160.dp
            val cardWidth = cardHeight * aspectRatio.ratio
            Box(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .graphicsLayer { translationX = offsetX }
                    .clip(MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                if (backImagePath != null) {
                    AsyncImage(
                        model = File(backImagePath),
                        contentDescription = "Dorso",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Overlay "toca para revelar"
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Toca para revelar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        } else {
            CardThumbnail(
                card = card,
                height = 160.dp,
                showTitle = showTitle,
                aspectRatio = aspectRatio,
                modifier = Modifier.graphicsLayer { translationX = offsetX }
            )

            // Botón de tirada vinculada (si existe)
            val tableId = card.linkedTableId
            if (tableId != null) {
                IconButton(
                    onClick = { onRollTable(tableId) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Tirar tabla vinculada",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

        }

        // Badge con nombre del mazo (solo en sesiones multi-mazo)
        if (deckBadge != null && !faceDown) {
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
    pile: List<Card>,
    onShuffle: () -> Unit
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (cardCount > 0) {
                        TextButton(
                            onClick = onShuffle
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Barajar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown
                        else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
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

// ──────────────────────────────────────────────────────────────────────────────
// Tab 1 — Pilas (Descarte)
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PilasTab(
    uiState: SessionUiState,
    onReturnToHand: (Long) -> Unit,
    onReturnToDeck: (Long) -> Unit,
    onShuffleBack: (Long?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.pile.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "La pila de descarte está vacía",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Header con acciones globales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${uiState.pile.size} cartas descartadas",
                    style = MaterialTheme.typography.titleMedium
                )
                Button(onClick = { onShuffleBack(null) }) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Barajar todo")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.pile, key = { it.id }) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CardThumbnail(
                                card = card,
                                height = 60.dp,
                                aspectRatio = uiState.deckAspectRatios[card.stackId] ?: com.deckapp.core.model.CardAspectRatio.STANDARD
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(card.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    uiState.deckNames[card.stackId] ?: "Mazo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onReturnToDeck(card.id) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Al mazo")
                            }
                            IconButton(onClick = { onReturnToHand(card.id) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "A la mano")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// C-5 — Animación de barajar
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShuffleAnimationOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "shuffle")
    val fan by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fan"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 12.dp,
        modifier = Modifier.padding(32.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tres cartas con rotaciones escalonadas que oscilan
            Box(
                modifier = Modifier.size(width = 110.dp, height = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(6.dp)
                val cardMod = Modifier.size(width = 56.dp, height = 76.dp)
                // Carta izquierda
                Surface(
                    modifier = cardMod.graphicsLayer {
                        rotationZ = -12f + fan
                        translationX = -22f
                    },
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {}
                // Carta derecha
                Surface(
                    modifier = cardMod.graphicsLayer {
                        rotationZ = 12f - fan
                        translationX = 22f
                    },
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {}
                // Carta central (encima)
                Surface(
                    modifier = cardMod.graphicsLayer { rotationZ = fan * 0.3f },
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                ) {}
            }
            Text(
                text = "Barajando…",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// D-2 — Sheet "Robar por palo"
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawBySuitSheet(
    suits: List<String>,
    onSuitSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Robar por palo",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (suits.isEmpty()) {
                Text(
                    "No hay cartas disponibles con palo definido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                suits.forEach { suit ->
                    OutlinedButton(
                        onClick = { onSuitSelected(suit) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(suit, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallFabWithLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 2.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}
