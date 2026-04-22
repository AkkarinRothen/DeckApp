package com.deckapp.feature.draw

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.deckapp.feature.draw.components.PlanTab
import com.deckapp.feature.draw.components.WikiTab
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.ResourceManagerDialog
import com.deckapp.feature.draw.components.SessionConfigSheet
import com.deckapp.feature.draw.components.CardItem
import com.deckapp.feature.draw.components.PileTray
import com.deckapp.feature.draw.components.NotesTab
import com.deckapp.feature.draw.components.NpcsTab
import com.deckapp.feature.tables.TablesTab
import com.deckapp.feature.reference.ReferenceTab
import com.deckapp.feature.encounters.CombatTab
import com.deckapp.feature.draw.components.PlanTab
import com.deckapp.core.ui.components.BentoSidebar
import com.deckapp.core.ui.components.EdgeTab
import com.deckapp.feature.hexploration.components.HexcrawlView
import com.deckapp.feature.mythic.components.MythicCenterView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onSessionEnd: () -> Unit,
    onBrowseDeck: (Long) -> Unit,
    onCreateTable: (Long) -> Unit,
    onImportTable: (Long) -> Unit,
    onEditTable: (Long) -> Unit,
    onEditReferenceTable: (Long) -> Unit,
    onEditSystemRule: (Long) -> Unit,
    onNewReferenceTable: (String) -> Unit,
    onNewSystemRule: (String) -> Unit,
    onShowDiceRoller: () -> Unit,
    onShowMythicOracle: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Wake lock and other logic...
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.session?.name ?: "Sesión", style = MaterialTheme.typography.titleMedium)
                        if (uiState.mainViewMode == SessionViewMode.MYTHIC_LOG) {
                            uiState.linkedMythicSession?.let { mythic ->
                                Text("Mythic: ${mythic.name} (Caos ${mythic.chaosFactor})", 
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onSessionEnd) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    // Switcher de Modo Central (Mapa vs Oráculo)
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                    ) {
                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.MAP) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.Default.Map, null) }
                        
                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.MYTHIC_LOG) },
                            enabled = uiState.linkedMythicSession != null,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.MYTHIC_LOG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.MenuBook, null) }
                    }

                    IconButton(onClick = onShowDiceRoller) { Icon(Icons.Default.Casino, null) }
                    IconButton(onClick = { viewModel.openSessionConfig() }) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // --- CAPA 1: WORKSPACE CENTRAL ---
            Crossfade(targetState = uiState.mainViewMode, label = "MainViewTransition") { mode ->
                when (mode) {
                    SessionViewMode.MAP -> {
                        uiState.hplState?.let { hpl ->
                            HexcrawlView(
                                uiState = hpl,
                                onTileClick = { /* Ver detalle hex */ },
                                onTileLongPress = { /* Menú contextual */ },
                                onExploreTile = { /* Explorar */ },
                                onEmptySpaceClick = { _, _ -> },
                                onStateChanged = { _, _ -> },
                                onStartNewDay = viewModel::startHplNewDay,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("VINCULA UN MAPA EN CONFIGURACIÓN", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    SessionViewMode.MYTHIC_LOG -> {
                        uiState.mythicState?.let { mythic ->
                            MythicCenterView(
                                uiState = mythic,
                                onFateCheck = { /* Mostrar Fate Sheet */ },
                                onQuickCheck = viewModel::quickMythicCheck,
                                onSceneChange = { /* Actualizar escena */ },
                                onChaosChange = viewModel::updateMythicChaos,
                                onCheckScene = { /* Scene Check */ },
                                onFinishScene = { /* Finish Scene */ },
                                modifier = Modifier.fillMaxSize().padding(16.dp)
                            )
                        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("VINCULA UNA SESIÓN MYTHIC", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // --- CAPA 2: PESTAÑAS DE BORDE (EDGE TABS) ---
            
            // Lado Izquierdo (Mecánicas)
            Column(
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EdgeTab(
                    icon = Icons.Default.Style,
                    label = "Mazos",
                    isActive = uiState.activeLeftSidebar == SidebarTool.DECKS,
                    alignment = Alignment.CenterStart,
                    onClick = { viewModel.toggleLeftSidebar(SidebarTool.DECKS) }
                )
                EdgeTab(
                    icon = Icons.Default.Casino,
                    label = "Tablas",
                    isActive = uiState.activeLeftSidebar == SidebarTool.TABLES,
                    alignment = Alignment.CenterStart,
                    onClick = { viewModel.toggleLeftSidebar(SidebarTool.TABLES) }
                )
                if (uiState.hasActiveEncounter) {
                    EdgeTab(
                        icon = Icons.Default.CrisisAlert,
                        label = "Combate",
                        isActive = uiState.activeLeftSidebar == SidebarTool.COMBAT,
                        alignment = Alignment.CenterStart,
                        onClick = { viewModel.toggleLeftSidebar(SidebarTool.COMBAT) }
                    )
                }
            }

            // Lado Derecho (Narrativa)
            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EdgeTab(
                    icon = Icons.Default.EditNote,
                    label = "Notas",
                    isActive = uiState.activeRightSidebar == SidebarTool.NOTES,
                    alignment = Alignment.CenterEnd,
                    onClick = { viewModel.toggleRightSidebar(SidebarTool.NOTES) }
                )
                EdgeTab(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Wiki",
                    isActive = uiState.activeRightSidebar == SidebarTool.WIKI,
                    alignment = Alignment.CenterEnd,
                    onClick = { viewModel.toggleRightSidebar(SidebarTool.WIKI) }
                )
                EdgeTab(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "Diario",
                    isActive = uiState.activeRightSidebar == SidebarTool.JOURNAL,
                    alignment = Alignment.CenterEnd,
                    onClick = { viewModel.toggleRightSidebar(SidebarTool.JOURNAL) }
                )
                EdgeTab(
                    icon = Icons.Default.Groups,
                    label = "NPCs",
                    isActive = uiState.activeRightSidebar == SidebarTool.NPCS,
                    alignment = Alignment.CenterEnd,
                    onClick = { viewModel.toggleRightSidebar(SidebarTool.NPCS) }
                )
                EdgeTab(
                    icon = Icons.Default.HistoryEdu,
                    label = "Plan",
                    isActive = uiState.activeRightSidebar == SidebarTool.PLAN,
                    alignment = Alignment.CenterEnd,
                    onClick = { viewModel.toggleRightSidebar(SidebarTool.PLAN) }
                )
            }

            // --- CAPA 3: SIDEBARS DESPLEGABLES ---

            // Sidebar Izquierdo: MECÁNICAS
            BentoSidebar(
                isVisible = uiState.activeLeftSidebar != SidebarTool.NONE,
                alignment = Alignment.CenterStart,
                title = when(uiState.activeLeftSidebar) {
                    SidebarTool.DECKS -> "Mazos de Cartas"
                    SidebarTool.TABLES -> "Tablas Aleatorias"
                    SidebarTool.COMBAT -> "Tracker de Combate"
                    else -> ""
                },
                onClose = { viewModel.toggleLeftSidebar(SidebarTool.NONE) }
            ) {
                when(uiState.activeLeftSidebar) {
                    SidebarTool.DECKS -> DeckWorkspace(
                        uiState = uiState,
                        onToggleCollapse = { viewModel.toggleDeckCollapse(it) },
                        onDraw = { viewModel.drawCardFromDeck(it) },
                        onCardClick = onCardClick,
                        onDiscard = { viewModel.discardCard(it) },
                        onRevealCard = { viewModel.revealCard(it) },
                        onRollTable = viewModel::rollActiveTable
                    )
                    SidebarTool.TABLES -> TablesTab(
                        sessionId = uiState.sessionId,
                        onCreateTable = { id -> onCreateTable(id ?: 0L) },
                        onEditTable = onEditTable,
                        onImportTable = { onImportTable(uiState.sessionId) }
                    )
                    SidebarTool.COMBAT -> if (uiState.activeEncounter != null) {
                        CombatTab(
                            encounter = uiState.activeEncounter!!,
                            players = uiState.playerParticipants,
                            log = uiState.combatLog,
                            onApplyDamage = viewModel::applyDamage,
                            onNextTurn = viewModel::nextTurn,
                            onToggleCondition = viewModel::toggleCondition,
                            onEndEncounter = viewModel::endEncounter,
                            onAddPlayer = viewModel::addPlayerParticipant,
                            onRemovePlayer = viewModel::removePlayerParticipant
                        )
                    }
                    else -> {}
                }
            }

            // Sidebar Derecho: NARRATIVA
            BentoSidebar(
                isVisible = uiState.activeRightSidebar != SidebarTool.NONE,
                alignment = Alignment.CenterEnd,
                title = when(uiState.activeRightSidebar) {
                    SidebarTool.NOTES -> "Notas de Sesión"
                    SidebarTool.JOURNAL -> "Diario de Viaje"
                    SidebarTool.WIKI -> "Lore del Mundo"
                    SidebarTool.NPCS -> "Personajes (NPCs)"
                    SidebarTool.PLAN -> "Plan de Sesión"
                    SidebarTool.MYTHIC_CONTEXT -> "Hilos y Personajes"
                    else -> ""
                },
                onClose = { viewModel.toggleRightSidebar(SidebarTool.NONE) }
            ) {
                when(uiState.activeRightSidebar) {
                    SidebarTool.NOTES -> NotesTab(
                        notes = uiState.dmNotes,
                        isSaving = uiState.isSavingNotes,
                        onNotesChange = viewModel::updateNotes
                    )
                    SidebarTool.JOURNAL -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Log Cronológico de HPL", style = MaterialTheme.typography.bodyMedium)
                    }
                    SidebarTool.WIKI -> WikiTab()
                    SidebarTool.NPCS -> NpcsTab(
                        npcs = uiState.npcs,
                        playingPath = uiState.playingNpcVoicePath,
                        onPlayVoice = viewModel::playNpcVoice
                    )
                    SidebarTool.PLAN -> PlanTab(
                        scenes = uiState.scenes,
                        npcs = uiState.npcs,
                        wikiEntries = uiState.wikiEntries,
                        onToggleCompletion = viewModel::toggleSceneCompletion,
                        onRollTable = viewModel::rollSceneTable,
                        onDrawCard = viewModel::drawSceneCard,
                        onNpcClick = { /* Navegar a NPC Editor si fuera necesario, o mostrar overlay */ },
                        onWikiClick = { /* Navegar a Wiki */ }
                    )
                    SidebarTool.MYTHIC_CONTEXT -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Listas de Mythic GME", style = MaterialTheme.typography.bodyMedium)
                    }
                    else -> {}
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator() 
                }
            }
        }
    }

    if (uiState.showSessionConfig) {
        SessionConfigSheet(
            selectedSystems = uiState.session?.gameSystems ?: emptyList(),
            onSystemsChanged = viewModel::updateGameSystems,
            availableSystems = uiState.availableSystems,
            linkedMythicSession = uiState.linkedMythicSession,
            allMythicSessions = uiState.allMythicSessions,
            onLinkMythic = viewModel::linkMythicSession,
            onDismiss = viewModel::dismissSessionConfig
        )
    }
}

@Composable
private fun DeckWorkspace(
    uiState: SessionUiState,
    onToggleCollapse: (Long) -> Unit,
    onDraw: (Long) -> Unit,
    onCardClick: (Long, Long) -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onRollTable: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(uiState.deckRefs, key = { it.stackId }) { ref ->
            val cardsInCluster = uiState.handByDeck[ref.stackId] ?: emptyList()
            DeckClusterItem(
                deckName = uiState.deckNames[ref.stackId] ?: "Mazo",
                cards = cardsInCluster,
                availableCount = uiState.deckCardCounts[ref.stackId] ?: 0,
                isCollapsed = ref.stackId in uiState.collapsedDeckIds,
                showTitle = uiState.session?.showCardTitles ?: true,
                aspectRatio = uiState.deckAspectRatios[ref.stackId] ?: CardAspectRatio.STANDARD,
                backImagePath = uiState.deckBackImages[ref.stackId],
                sessionId = uiState.session?.id ?: 0L,
                onToggleCollapse = { onToggleCollapse(ref.stackId) },
                onDraw = { onDraw(ref.stackId) },
                onCardClick = onCardClick,
                onDiscard = onDiscard,
                onRevealCard = onRevealCard,
                onRollTable = onRollTable
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onToggleCollapse: () -> Unit,
    onDraw: () -> Unit,
    onCardClick: (Long, Long) -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onRollTable: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleCollapse() }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(deckName, style = MaterialTheme.typography.titleSmall)
                    Text("${cards.size} en mano · $availableCount libres", style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDraw) { Icon(Icons.Default.Add, null) }
            }

            if (!isCollapsed && cards.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it != SwipeToDismissBoxValue.Settled) {
                                    onDiscard(card.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Color.Red else Color.Red
                                Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp)))
                            }
                        ) {
                            CardItem(
                                card = card,
                                aspectRatio = aspectRatio,
                                showTitle = showTitle,
                                backImagePath = backImagePath,
                                onClick = { onCardClick(card.id, sessionId) },
                                onDiscard = { onDiscard(card.id) },
                                onReveal = { onRevealCard(card.id) },
                                onRollTable = onRollTable
                            )
                        }
                    }
                }
            }
        }
    }
}
