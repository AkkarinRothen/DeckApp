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
import androidx.compose.foundation.shape.CircleShape
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
import com.deckapp.feature.tables.TableDetailSheet
import com.deckapp.feature.draw.components.NotesTab
import com.deckapp.feature.draw.components.NpcsTab
import com.deckapp.feature.tables.TablesTab
import com.deckapp.feature.reference.ReferenceTab
import com.deckapp.feature.encounters.CombatTab
import com.deckapp.core.ui.components.BentoSidebar
import com.deckapp.core.ui.session.SessionToolContext
import com.deckapp.core.ui.session.SessionCentralConnector
import com.deckapp.core.ui.components.EdgeTab
import com.deckapp.feature.hexploration.components.HexcrawlView
import com.deckapp.feature.mythic.components.MythicCenterView
import com.deckapp.feature.mythic.components.MythicContextTab
import com.deckapp.feature.draw.components.NpcQuickPreviewDialog
import com.deckapp.feature.draw.components.WikiQuickPreviewDialog

import com.deckapp.feature.draw.components.SidebarNavigationRail
import com.deckapp.feature.draw.components.SessionBottomFloatingDock
import com.deckapp.feature.draw.components.SessionHotbarDock
import com.deckapp.feature.draw.components.HotbarActionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    onCardClick: (cardId: Long, sessionId: Long) -> Unit,
    onSessionEnd: () -> Unit,
    onNavigateTo: (Any) -> Unit = {},
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
    onHistoryClick: (Long) -> Unit = {},
    tools: List<com.deckapp.core.ui.session.SessionToolConnector> = emptyList(),
    centralTools: List<com.deckapp.core.ui.session.SessionCentralConnector> = emptyList(),
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val npcHistory = remember { mutableStateListOf<Npc>() }
    val quickViewNpc = npcHistory.lastOrNull()
    var peekingWikiEntry by remember { mutableStateOf<WikiEntry?>(null) }
    var peekingTable by remember { mutableStateOf<RandomTable?>(null) }

    val leftTools = remember(uiState.hasActiveEncounter) {
        buildList {
            add(SidebarTool.DECKS to Icons.Default.Style)
            add(SidebarTool.TABLES to Icons.Default.Casino)
            if (uiState.hasActiveEncounter) {
                add(SidebarTool.COMBAT to Icons.Default.CrisisAlert)
            }
        }
    }

    val rightTools = remember(uiState.linkedMythicSession) {
        buildList {
            add(SidebarTool.NOTES to Icons.Default.EditNote)
            add(SidebarTool.WIKI to Icons.AutoMirrored.Filled.MenuBook)
            add(SidebarTool.JOURNAL to Icons.AutoMirrored.Filled.LibraryBooks)
            if (uiState.linkedMythicSession != null) {
                add(SidebarTool.MYTHIC_CONTEXT to Icons.Default.AutoAwesome)
            }
            add(SidebarTool.NPCS to Icons.Default.Groups)
            add(SidebarTool.PLAN to Icons.Default.HistoryEdu)
        }
    }

    // Wake lock and other logic...
    val activity = context as? Activity
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                viewModel.saveCheckpointOnPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.session?.name ?: "Sesión",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(8.dp))
                            val elapsedText = remember(uiState.sessionElapsedMinutes) {
                                val h = uiState.sessionElapsedMinutes / 60
                                val m = uiState.sessionElapsedMinutes % 60
                                if (h > 0) "${h}h ${m}m" else "${m}m"
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = elapsedText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                    // Undo button — visible only after a DRAW action
                    AnimatedVisibility(visible = uiState.canUndo) {
                        IconButton(onClick = { viewModel.undoLastDraw() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Deshacer último robo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Switcher de Modo Central (Mesa vs Mapa Táctico vs Hexploración vs Oráculo)
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                    ) {
                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.BOARD) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.BOARD) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.Default.Dashboard, contentDescription = "Mesa de Juego") }

                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.TACTICAL) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.TACTICAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.Default.GridOn, contentDescription = "Mapa Táctico de Batalla") }

                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.MAP) },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.MAP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.Default.Explore, contentDescription = "Mapa de Exploración HPL") }
                        
                        IconButton(
                            onClick = { viewModel.setMainViewMode(SessionViewMode.MYTHIC_LOG) },
                            enabled = uiState.linkedMythicSession != null,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = if (uiState.mainViewMode == SessionViewMode.MYTHIC_LOG) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Oráculo Mythic") }
                    }

                    IconButton(onClick = viewModel::openGlobalSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Búsqueda Rápida")
                    }
                    IconButton(onClick = onShowDiceRoller) { Icon(Icons.Default.Casino, null) }
                    IconButton(
                        onClick = { uiState.session?.id?.let(onHistoryClick) },
                        enabled = uiState.session != null
                    ) { Icon(Icons.Default.History, contentDescription = "Historial de sesión") }
                    IconButton(onClick = { viewModel.openSessionConfig() }) { Icon(Icons.Default.Settings, null) }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val isLeftSidebarOpen = uiState.activeLeftSidebar != SidebarTool.NONE
            val isRightSidebarOpen = uiState.activeRightSidebar != SidebarTool.NONE
            val isAnySidebarOpen = isLeftSidebarOpen || isRightSidebarOpen

            val leftTitle = when(uiState.activeLeftSidebar) {
                SidebarTool.DECKS -> "Mazos de Cartas"
                SidebarTool.TABLES -> "Tablas Aleatorias"
                SidebarTool.COMBAT -> "Tracker de Combate"
                else -> ""
            }
            
            val rightTitle = when(uiState.activeRightSidebar) {
                SidebarTool.NOTES -> "Notas de Sesión"
                SidebarTool.JOURNAL -> "Diario de Viaje"
                SidebarTool.WIKI -> "Lore del Mundo"
                SidebarTool.NPCS -> "Personajes (NPCs)"
                SidebarTool.PLAN -> "Plan de Sesión"
                SidebarTool.MYTHIC_CONTEXT -> "Hilos y Personajes"
                else -> ""
            }

            val leftSidebarContent = @Composable {
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

            val rightSidebarContent = @Composable {
                when(uiState.activeRightSidebar) {
                    SidebarTool.NOTES -> NotesTab(
                        notes = uiState.dmNotes,
                        isSaving = uiState.isSavingNotes,
                        onNotesChange = viewModel::updateNotes,
                        onAddQuickNote = viewModel::showQuickNote
                    )
                    SidebarTool.JOURNAL -> {
                        val journalTool = tools.find { it.id == "journal" }
                        if (journalTool != null) {
                            val context = SessionToolContext(
                                sessionId = uiState.sessionId,
                                onNavigateTo = { /* navegar */ },
                                onShowDiceRoller = onShowDiceRoller,
                                onRollTable = viewModel::rollActiveTable,
                                onDrawCard = viewModel::drawCardFromDeck,
                                onNpcClick = { npcId ->
                                    npcHistory.clear()
                                    uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                                },
                                onWikiClick = { /* wiki */ },
                                onStartEncounter = { encounterId ->
                                    viewModel.startEncounterFromScene(encounterId)
                                },
                                onRollDice = viewModel::rollSmartDice,
                                onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                                isSidebarOpen = isAnySidebarOpen
                            )
                            journalTool.content(context)
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Log Cronológico de HPL", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    SidebarTool.WIKI -> WikiTab()
                    SidebarTool.NPCS -> NpcsTab(
                        npcs = uiState.npcs,
                        playingPath = uiState.playingNpcVoicePath,
                        onPlayVoice = viewModel::playNpcVoice,
                        onNpcClick = { npcId ->
                            npcHistory.clear()
                            uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                        }
                    )
                    SidebarTool.PLAN -> PlanTab(
                        scenes = uiState.scenes,
                        focusedSceneId = uiState.focusedSceneId,
                        onFocusedSceneChange = viewModel::focusScene,
                        npcs = uiState.npcs,
                        wikiEntries = uiState.wikiEntries,
                        allDecks = uiState.allDecks,
                        allTables = uiState.allTables,
                        activeEncounter = uiState.activeEncounter,
                        onUpdateHp = { creature, targetHp ->
                            viewModel.applyDamage(creature.id.toLong(), targetHp - creature.currentHp)
                        },
                        onToggleCompletion = viewModel::toggleSceneCompletion,
                        onRollTable = viewModel::rollSceneTable,
                        onDrawCard = viewModel::drawSceneCard,
                        onNpcClick = { npcId ->
                            npcHistory.clear()
                            uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                        },
                        onWikiClick = { wikiId ->
                            // Open Wiki quick view
                        },
                        onStartEncounter = { encounterId ->
                            viewModel.startEncounterFromScene(encounterId)
                        },
                        onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                        onRollDiceClicked = viewModel::rollSmartDice
                    )
                    SidebarTool.MYTHIC_CONTEXT -> {
                        uiState.mythicState?.let { mythic ->
                            MythicContextTab(
                                characters = mythic.characters,
                                threads = mythic.threads,
                                onAddCharacter = viewModel::addMythicCharacter,
                                onDeleteCharacter = viewModel::deleteMythicCharacter,
                                onUpdateCharacterNotes = viewModel::updateMythicCharacterNotes,
                                onSendCharacterToNotes = viewModel::sendMythicCharacterToNotes,
                                onAddThread = viewModel::addMythicThread,
                                onUpdateThreadDescription = viewModel::updateMythicThreadDescription,
                                onToggleThread = viewModel::toggleMythicThreadStatus,
                                onDeleteThread = viewModel::deleteMythicThread,
                                onSendThreadToNotes = viewModel::sendMythicThreadToNotes,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Vincula una crónica de Mythic para ver el contexto", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    else -> {}
                }
            }

            val centralWorkspaceContent = @Composable {
                Crossfade(targetState = uiState.mainViewMode, label = "MainViewTransition") { mode ->
                    val toolId = when (mode) {
                        SessionViewMode.MAP -> "map"
                        SessionViewMode.TACTICAL -> "tactical"
                        SessionViewMode.BOARD -> "board"
                        SessionViewMode.MYTHIC_LOG -> "mythic_log"
                    }
                    val centralTool = centralTools.find { it.id == toolId }
                    if (centralTool != null) {
                        val context = SessionToolContext(
                            sessionId = uiState.sessionId,
                            onNavigateTo = { /* navegar */ },
                            onShowDiceRoller = onShowDiceRoller,
                            onRollTable = viewModel::rollActiveTable,
                            onDrawCard = viewModel::drawCardFromDeck,
                            onNpcClick = { npcId ->
                                npcHistory.clear()
                                uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                            },
                            onWikiClick = { /* wiki */ },
                            onStartEncounter = { encounterId ->
                                viewModel.startEncounterFromScene(encounterId)
                            },
                            onRollDice = viewModel::rollSmartDice,
                            onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                            isSidebarOpen = isAnySidebarOpen
                        )
                        centralTool.content(context)
                    } else {
                        when (mode) {
                            SessionViewMode.TACTICAL -> {
                                com.deckapp.feature.draw.tactical.TacticalMapView(
                                    uiState = uiState.tacticalState,
                                    npcs = uiState.npcs,
                                    activeEncounter = uiState.activeEncounter,
                                    distanceCalculator = viewModel.tacticalMapDelegate.distanceCalculator,
                                    onToolSelected = viewModel.tacticalMapDelegate::setTacticalTool,
                                    onAoeShapeSelected = viewModel.tacticalMapDelegate::setAoeShape,
                                    onAoeRadiusSelected = viewModel.tacticalMapDelegate::setAoeRadius,
                                    onPersistAoE = viewModel.tacticalMapDelegate::persistCurrentAoE,
                                    onClearPersistentAoEs = viewModel.tacticalMapDelegate::clearPersistentAoEs,
                                    onAoEPositionChanged = viewModel.tacticalMapDelegate::updateAoEPosition,
                                    onTokenSelected = viewModel.tacticalMapDelegate::selectToken,
                                    onTokenMoved = viewModel.tacticalMapDelegate::moveToken,
                                    onTokenHpChanged = viewModel.tacticalMapDelegate::updateTokenHp,
                                    onTokenConditionToggled = viewModel.tacticalMapDelegate::toggleCondition,
                                    onTokenSizeChanged = viewModel.tacticalMapDelegate::updateTokenSize,
                                    onTokenElevationChanged = viewModel.tacticalMapDelegate::updateTokenElevation,
                                    onTokenVisibilityToggled = viewModel.tacticalMapDelegate::toggleTokenVisibility,
                                    onOpenNpc = { npcId ->
                                        npcHistory.clear()
                                        uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                                    },
                                    onDeleteToken = viewModel.tacticalMapDelegate::deleteToken,
                                    onPinClicked = viewModel.tacticalMapDelegate::selectPin,
                                    onMapClicked = viewModel.tacticalMapDelegate::openCreatePin,
                                    onAddToken = viewModel.tacticalMapDelegate::addToken,
                                    onAddTokenFromNpc = viewModel.tacticalMapDelegate::addTokenFromNpc,
                                    onSyncEncounter = viewModel.tacticalMapDelegate::syncTokensFromEncounter,
                                    onUpdateGridCalibration = viewModel.tacticalMapDelegate::updateGridCalibration,
                                    onUpdateMapImage = viewModel.tacticalMapDelegate::updateMapImage,
                                    onStartGuidedCalibration = viewModel.tacticalMapDelegate::startGuidedCalibration,
                                    onGuidedCalibrationPoint = viewModel.tacticalMapDelegate::handleGuidedCalibrationPoint,
                                    onCreateDefaultMap = viewModel.tacticalMapDelegate::createDefaultMap,
                                    onSwitchMap = viewModel.tacticalMapDelegate::switchMap,
                                    onOpenCreateMapDialog = viewModel.tacticalMapDelegate::openCreateMapDialog,
                                    onOpenRenameMap = viewModel.tacticalMapDelegate::openRenameMapDialog,
                                    onDeleteMap = viewModel.tacticalMapDelegate::deleteMap,
                                    onTerrainBrushSelected = viewModel.tacticalMapDelegate::setTerrainBrush,
                                    onTerrainCellInteracted = viewModel.tacticalMapDelegate::interactTerrainCell,
                                    onClearAllTerrain = viewModel.tacticalMapDelegate::clearAllTerrain,
                                    onToggleFog = viewModel.tacticalMapDelegate::toggleFog,
                                    onFogBrushModeSelected = viewModel.tacticalMapDelegate::setFogBrushMode,
                                    onRevealAllFog = viewModel.tacticalMapDelegate::revealAllFog,
                                    onResetFog = viewModel.tacticalMapDelegate::resetFog,
                                    onToggleFogDmView = viewModel.tacticalMapDelegate::toggleFogDmView,
                                    onFogCellInteracted = viewModel.tacticalMapDelegate::interactFogCell,
                                    onAddRouteWaypoint = viewModel.tacticalMapDelegate::addRouteWaypoint,
                                    onRemoveLastRouteWaypoint = viewModel.tacticalMapDelegate::removeLastRouteWaypoint,
                                    onCommitRouteMovement = viewModel.tacticalMapDelegate::commitRouteMovement,
                                    onClearRoute = viewModel.tacticalMapDelegate::clearRoute,
                                    onResetTokenTurnMovement = viewModel.tacticalMapDelegate::resetTokenTurnMovement,
                                    onStartAttackTargeting = viewModel.tacticalMapDelegate::startAttackTargeting,
                                    onCancelTargeting = viewModel.tacticalMapDelegate::cancelTargeting,
                                    onExecuteAttack = viewModel.tacticalMapDelegate::executeAttack,
                                    onApplyAttackDamage = viewModel.tacticalMapDelegate::applyAttackDamage,
                                    onDismissAttackResolution = viewModel.tacticalMapDelegate::dismissAttackResolution,
                                    onDismissCustomAttackDialog = viewModel.tacticalMapDelegate::dismissCustomAttackDialog,
                                    onAdvanceCombatTurn = viewModel.tacticalMapDelegate::advanceCombatTurn,
                                    onPreviousCombatTurn = viewModel.tacticalMapDelegate::previousCombatTurn,
                                    onFocusOnCombatCreature = viewModel.tacticalMapDelegate::focusOnCombatCreature,
                                    onClearCameraFocus = viewModel.tacticalMapDelegate::clearCameraFocus,
                                    onNavigateBackToBreadcrumb = viewModel.tacticalMapDelegate::navigateBackToBreadcrumb,
                                    onNavigateToSubMap = viewModel.tacticalMapDelegate::navigateToSubMap,
                                    onSpawnEncounter = viewModel.tacticalMapDelegate::spawnEncounterOnMap,
                                    onRollPinTable = viewModel.tacticalMapDelegate::rollPinTable,
                                    onTogglePinSecret = viewModel.tacticalMapDelegate::togglePinSecret,
                                    onEditPin = viewModel.tacticalMapDelegate::openEditPin,
                                    onRollInlineFormula = viewModel.tacticalMapDelegate::rollInlineFormula,
                                    onDismissPin = { viewModel.tacticalMapDelegate.selectPin(null) },
                                    dmNotes = uiState.dmNotes,
                                    onNotesChanged = viewModel::updateNotes,
                                    onToggleSidePanel = viewModel.tacticalMapDelegate::toggleSidePanel,
                                    onSetSidePanelOpen = viewModel.tacticalMapDelegate::setSidePanelOpen,
                                    onSetSidePanelTab = viewModel.tacticalMapDelegate::setSidePanelTab,
                                    onSetSidePanelDice = viewModel.tacticalMapDelegate::setSidePanelDice,
                                    onRollSidePanelDice = viewModel.tacticalMapDelegate::rollSidePanelDice,
                                    onRollSidePanelFateCheck = viewModel.tacticalMapDelegate::rollSidePanelFateCheck,
                                    onRollSidePanelRandomEvent = viewModel.tacticalMapDelegate::rollSidePanelRandomEvent,
                                    onWikiSearchQueryChanged = viewModel.tacticalMapDelegate::setWikiSearchQuery,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
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
                            SessionViewMode.BOARD -> {
                                val ctx = SessionToolContext(
                                    sessionId = uiState.sessionId,
                                    onNavigateTo = { /* navegar */ },
                                    onShowDiceRoller = onShowDiceRoller,
                                    onRollTable = viewModel::rollActiveTable,
                                    onDrawCard = viewModel::drawCardFromDeck,
                                    onNpcClick = { npcId ->
                                        npcHistory.clear()
                                        uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                                    },
                                    onWikiClick = { /* wiki */ },
                                    onStartEncounter = { encounterId ->
                                        viewModel.startEncounterFromScene(encounterId)
                                    },
                                    onRollDice = viewModel::rollSmartDice,
                                    onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                                    isSidebarOpen = isAnySidebarOpen
                                )
                                val centralTool = centralTools.find { it.id == "board" }
                                if (centralTool != null) {
                                    centralTool.content(ctx)
                                } else {
                                    com.deckapp.feature.draw.session.BoardCentralTool.content(ctx)
                                }
                            }
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLeftSidebarOpen) {
                        Row(modifier = Modifier.fillMaxHeight().width(380.dp + 64.dp)) {
                            SidebarNavigationRail(
                                tools = leftTools,
                                activeTool = uiState.activeLeftSidebar,
                                onToolClick = { viewModel.toggleLeftSidebar(it) }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            BentoSidebar(
                                isVisible = true,
                                alignment = Alignment.CenterStart,
                                title = leftTitle,
                                onClose = { viewModel.toggleLeftSidebar(SidebarTool.NONE) },
                                modifier = Modifier.weight(1f)
                            ) {
                                leftSidebarContent()
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(start = 0.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            leftTools.forEach { (tool, icon) ->
                                val label = when(tool) {
                                    SidebarTool.DECKS -> "Mazos"
                                    SidebarTool.TABLES -> "Tablas"
                                    SidebarTool.COMBAT -> "Combate"
                                    else -> ""
                                }
                                EdgeTab(
                                    icon = icon,
                                    label = label,
                                    isActive = false,
                                    alignment = Alignment.CenterStart,
                                    onClick = { viewModel.toggleLeftSidebar(tool) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        centralWorkspaceContent()
                    }

                    if (isRightSidebarOpen) {
                        Row(modifier = Modifier.fillMaxHeight().width(380.dp + 64.dp)) {
                            BentoSidebar(
                                isVisible = true,
                                alignment = Alignment.CenterEnd,
                                title = rightTitle,
                                onClose = { viewModel.toggleRightSidebar(SidebarTool.NONE) },
                                modifier = Modifier.weight(1f)
                            ) {
                                rightSidebarContent()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            SidebarNavigationRail(
                                tools = rightTools,
                                activeTool = uiState.activeRightSidebar,
                                onToolClick = { viewModel.toggleRightSidebar(it) }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxHeight().padding(end = 0.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            rightTools.forEach { (tool, icon) ->
                                val label = when(tool) {
                                    SidebarTool.NOTES -> "Notas"
                                    SidebarTool.WIKI -> "Wiki"
                                    SidebarTool.JOURNAL -> "Diario"
                                    SidebarTool.MYTHIC_CONTEXT -> "Mythic"
                                    SidebarTool.NPCS -> "NPCs"
                                    SidebarTool.PLAN -> "Plan"
                                    else -> ""
                                }
                                EdgeTab(
                                    icon = icon,
                                    label = label,
                                    isActive = false,
                                    alignment = Alignment.CenterEnd,
                                    onClick = { viewModel.toggleRightSidebar(tool) }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                } // end Row
            } // end if (isLandscape)
            if (!isLandscape) {
                // Portrait: central content fills most of the screen
                // Tool panels open as ModalBottomSheet from the bottom
                Box(modifier = Modifier.fillMaxSize()) {
                    centralWorkspaceContent()
                }

                // Left tool ModalBottomSheet (Decks, Tables, Combat)
                if (isLeftSidebarOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.toggleLeftSidebar(SidebarTool.NONE) },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                        dragHandle = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BottomSheetDefaults.DragHandle()
                                Text(
                                    text = leftTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.72f)
                        ) {
                            leftSidebarContent()
                        }
                    }
                }

                // Right tool ModalBottomSheet (Notes, Wiki, Journal, NPCs, Plan, Mythic)
                if (isRightSidebarOpen) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.toggleRightSidebar(SidebarTool.NONE) },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
                        dragHandle = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BottomSheetDefaults.DragHandle()
                                Text(
                                    text = rightTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.72f)
                        ) {
                            rightSidebarContent()
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator() 
                }
            }

            quickViewNpc?.let { npc ->
                NpcQuickPreviewDialog(
                    npc = npc,
                    onDismiss = { npcHistory.clear() }
                )
            }

            // Action Hotbar / Quick Roll Dock (Sprint 59)
            val hotbarActions = uiState.session?.hotbarActions ?: defaultHotbarActions()
            SessionHotbarDock(
                actions = hotbarActions,
                isExpanded = uiState.isHotbarExpanded,
                onToggleExpanded = viewModel::toggleHotbarExpanded,
                onExecuteAction = viewModel::executeHotbarAction,
                onAddAction = viewModel::openCreateHotbarAction,
                onEditAction = viewModel::openEditHotbarAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            )
        }
    }

    peekingWikiEntry?.let { wiki ->
        WikiQuickPreviewDialog(
            wikiEntry = wiki,
            onDismiss = { peekingWikiEntry = null }
        )
    }

    peekingTable?.let { table ->
        val lastResult = uiState.recentTableRolls.firstOrNull { it.tableId == table.id }
        val recentResults = uiState.recentTableRolls.filter { it.tableId == table.id }
        TableDetailSheet(
            table = table,
            lastResult = lastResult,
            recentResults = recentResults,
            isRolling = false,
            onRoll = { viewModel.rollActiveTable(table.id) },
            onExport = {},
            onDismiss = { peekingTable = null }
        )
    }

    if (uiState.showSessionConfig) {
        SessionConfigSheet(
            sessionName = uiState.session?.name.orEmpty(),
            onRenameSession = viewModel::renameSession,
            selectedSystems = uiState.session?.gameSystems ?: emptyList(),
            onSystemsChanged = viewModel::updateGameSystems,
            availableSystems = uiState.availableSystems,
            boardViewLabels = uiState.session?.boardViewLabels ?: SessionBoardViewLabels(),
            onBoardViewLabelChanged = viewModel::updateBoardViewLabel,
            linkedMythicSession = uiState.linkedMythicSession,
            allMythicSessions = uiState.allMythicSessions,
            onLinkMythic = viewModel::linkMythicSession,
            onCreateAndLinkMythic = viewModel::createAndLinkMythicSession,
            linkedHexMapId = uiState.linkedHexMapId,
            allHexMaps = uiState.allHexMaps,
            onLinkHexMap = viewModel::linkHexMap,
            onResetFloatingPanels = viewModel::resetFloatingPanels,
            allDecks = uiState.allDecks,
            sessionDeckRefs = uiState.deckRefs,
            onToggleDeck = viewModel::toggleDeckInSession,
            onDismiss = viewModel::dismissSessionConfig
        )
    }

    if (uiState.showHotbarActionDialog) {
        val availableTables = uiState.tablesInSession.ifEmpty { uiState.allTables }
        HotbarActionDialog(
            action = uiState.editingHotbarAction,
            availableTables = availableTables,
            availableDecks = uiState.allDecks,
            onDismiss = viewModel::dismissHotbarActionDialog,
            onSave = viewModel::saveHotbarAction,
            onDelete = { actionId -> viewModel.deleteHotbarAction(actionId) }
        )
    }

    if (uiState.tacticalState.showCreatePinDialog) {
        com.deckapp.feature.draw.tactical.TacticalPinDialog(
            initialPin = uiState.tacticalState.pinToEdit,
            defaultCellX = uiState.tacticalState.pendingPinCoordinates?.first ?: 0f,
            defaultCellY = uiState.tacticalState.pendingPinCoordinates?.second ?: 0f,
            mapId = uiState.tacticalState.map?.id ?: 0L,
            availableTables = uiState.tacticalState.availableTables,
            availableEncounters = uiState.tacticalState.availableEncounters,
            availableWikiEntries = uiState.tacticalState.availableWikiEntries,
            availableMaps = uiState.tacticalState.availableMaps,
            onDismiss = viewModel.tacticalMapDelegate::dismissPinDialogs,
            onSavePin = viewModel.tacticalMapDelegate::savePin
        )
    }

    val selectedPinForSheet = uiState.tacticalState.selectedPin
    if (selectedPinForSheet != null && uiState.tacticalState.showPinDetailDialog) {
        com.deckapp.feature.draw.tactical.TacticalPinSheet(
            pin = selectedPinForSheet,
            availableTables = uiState.tacticalState.availableTables,
            availableEncounters = uiState.tacticalState.availableEncounters,
            availableWikiEntries = uiState.tacticalState.availableWikiEntries,
            onDismiss = { viewModel.tacticalMapDelegate.selectPin(null) },
            onEditPin = viewModel.tacticalMapDelegate::openEditPin,
            onDeletePin = viewModel.tacticalMapDelegate::deletePin,
            onUpdateExplorationState = { viewModel.tacticalMapDelegate.updatePinExplorationState(selectedPinForSheet.id, it) },
            onRollTable = { viewModel.tacticalMapDelegate.rollPinTable(selectedPinForSheet.id) },
            onDeleteDiscovery = { viewModel.tacticalMapDelegate.removePinDiscovery(selectedPinForSheet.id, it) },
            onStartEncounter = { encounterId -> viewModel.startEncounterFromScene(encounterId) },
            onPreviewWiki = { wiki -> viewModel.tacticalMapDelegate.setPreviewWikiEntry(wiki) }
        )
    }

    if (uiState.tacticalState.showCreateMapDialog) {
        com.deckapp.feature.draw.tactical.TacticalCreateMapDialog(
            scenes = uiState.scenes,
            activeSceneId = uiState.focusedSceneId,
            onDismiss = viewModel.tacticalMapDelegate::dismissMultimapDialogs,
            onCreateMap = { name, uri, gridType, cellSize, scaleFt, sceneId ->
                viewModel.tacticalMapDelegate.createNewMap(name, uri, gridType, cellSize, scaleFt, sceneId)
            }
        )
    }

    if (uiState.tacticalState.showMapSelectorSheet) {
        com.deckapp.feature.draw.tactical.TacticalMapSelectorSheet(
            availableMaps = uiState.tacticalState.availableMaps,
            currentMapId = uiState.tacticalState.map?.id,
            scenes = uiState.scenes,
            onSelectMap = viewModel.tacticalMapDelegate::switchMap,
            onCreateNewMap = viewModel.tacticalMapDelegate::openCreateMapDialog,
            onRenameMap = viewModel.tacticalMapDelegate::openRenameMapDialog,
            onLinkScene = viewModel.tacticalMapDelegate::openLinkSceneDialog,
            onDeleteMap = viewModel.tacticalMapDelegate::deleteMap,
            onDismiss = viewModel.tacticalMapDelegate::dismissMapSelector
        )
    }

    uiState.tacticalState.mapToRename?.let { map ->
        var newMapName by remember(map.id) { mutableStateOf(map.name) }
        AlertDialog(
            onDismissRequest = viewModel.tacticalMapDelegate::dismissMultimapDialogs,
            title = { Text("Renombrar Mapa") },
            text = {
                OutlinedTextField(
                    value = newMapName,
                    onValueChange = { newMapName = it },
                    label = { Text("Nombre del mapa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMapName.isNotBlank()) {
                            viewModel.tacticalMapDelegate.renameMap(map.id, newMapName)
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel.tacticalMapDelegate::dismissMultimapDialogs) {
                    Text("Cancelar")
                }
            }
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
