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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.deckapp.feature.draw.components.NotesTabMode
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
import com.deckapp.feature.draw.components.WikiQuickPreviewDialog

import com.deckapp.feature.draw.components.SidebarNavigationRail
import com.deckapp.feature.draw.components.SessionBottomFloatingDock
import com.deckapp.feature.draw.components.SessionHotbarDock
import com.deckapp.feature.draw.components.SessionTopHudCapsule
import com.deckapp.feature.draw.components.HotbarActionDialog
import com.deckapp.feature.draw.components.SceneFramingDialog
import com.deckapp.feature.draw.components.DeckWorkspace
import com.deckapp.feature.draw.components.SessionTopBar
import com.deckapp.feature.draw.components.SessionBottomBar
import com.deckapp.feature.draw.components.SessionDialogsHost
import com.deckapp.feature.draw.components.clocks.SessionClocksOverlay
import com.deckapp.feature.draw.components.clocks.ClockEditDialog
import com.deckapp.feature.draw.components.clocks.ClockImpactDialog
import com.deckapp.feature.draw.components.threads.SessionThreadsOverlay
import com.deckapp.feature.draw.components.threads.ThreadEditDialog
import com.deckapp.feature.draw.components.threads.MythicCharacterEditDialog
import com.deckapp.feature.draw.components.combat.SessionCombatFloatingHud
import com.deckapp.feature.draw.components.spotlight.SessionSpotlightDialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

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
            add(SidebarTool.COMBAT to if (uiState.hasActiveEncounter) Icons.Default.CrisisAlert else Icons.Default.SportsKabaddi)
        }
    }

    val rightTools = remember(uiState.linkedMythicSession) {
        buildList {
            add(SidebarTool.NOTES to Icons.Default.EditNote)
            add(SidebarTool.REFERENCES to Icons.AutoMirrored.Filled.MenuBook)
            add(SidebarTool.WIKI to Icons.Default.Book)
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
    val keepScreenOn = com.deckapp.core.ui.theme.LocalGeneralPreferences.current.keepScreenOn
    DisposableEffect(activity, keepScreenOn) {
        if (keepScreenOn) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var showChoirDialog by remember { mutableStateOf(false) }
    var showIncursionDialog by remember { mutableStateOf(false) }
    var returnToGuideAfterIncursion by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && (event.key == Key.K || event.key == Key.F)) {
                    viewModel.openSpotlight()
                    true
                } else false
            },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
            SessionTopBar(
                session = uiState.session,
                sessionElapsedMinutes = uiState.sessionElapsedMinutes,
                canUndo = uiState.canUndo,
                mainViewMode = uiState.mainViewMode,
                linkedMythicSession = uiState.linkedMythicSession,
                onSessionEnd = onSessionEnd,
                onUndo = viewModel::undoLastDraw,
                onSetMainViewMode = viewModel::setMainViewMode,
                onOpenSceneFraming = viewModel::openSceneFraming,
                onOpenChoirDialog = { showChoirDialog = true },
                onOpenIncursionDialog = { showIncursionDialog = true },
                onOpenChoirProcedures = viewModel.choirGuideDelegate::open,
                onOpenSpotlight = viewModel::openSpotlight,
                onShowDiceRoller = onShowDiceRoller,
                onHistoryClick = { uiState.session?.id?.let(onHistoryClick) },
                onOpenSessionConfig = viewModel::openSessionConfig
            )
            if (uiState.session?.gameSystems?.any { it.contains("Choir", true) } == true) {
                com.deckapp.feature.draw.components.ChoirProcedureBar(
                    uiState.boardMemory.choirGuide, viewModel.choirGuideDelegate::open
                )
            }
            }
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
                SidebarTool.REFERENCES -> "Compendio & Reglas"
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
                        onRollTable = viewModel::rollActiveTable,
                        onOpenDeckPicker = viewModel::openDeckPicker
                    )
                    SidebarTool.TABLES -> TablesTab(
                        sessionId = uiState.sessionId,
                        onCreateTable = { id -> onCreateTable(id ?: 0L) },
                        onEditTable = onEditTable,
                        onImportTable = { onImportTable(uiState.sessionId) }
                    )
                    SidebarTool.COMBAT -> {
                        if (uiState.activeEncounter != null) {
                            CombatTab(
                                encounter = uiState.activeEncounter!!,
                                players = uiState.playerParticipants,
                                log = uiState.combatLog,
                                onChoirCharacterCommand = viewModel::updateChoirCharacter,
                                onChoirStateChange = viewModel::updateChoirCombatState,
                                onApplyDamage = viewModel::applyDamage,
                                onNextTurn = viewModel::nextTurn,
                                onToggleCondition = viewModel::toggleCondition,
                                onEndEncounter = viewModel::endEncounter,
                                onAddPlayer = viewModel::addPlayerParticipant,
                                onRemovePlayer = viewModel::removePlayerParticipant,
                                onChoirCombatAftermath = { creatureId, request ->
                                    viewModel.applyChoirCombatAftermath(creatureId, request)
                                }
                            )
                        } else {
                            CombatEmptyState(
                                onStartQuickCombat = { viewModel.startQuickCombat() }
                            )
                        }
                    }
                    else -> {}
                }
            }

            val rightSidebarContent = @Composable {
                when(uiState.activeRightSidebar) {
                    SidebarTool.NOTES -> NotesTab(
                        notes = ChoirGuideNotes.visible(uiState.dmNotes),
                        isSaving = uiState.isSavingNotes,
                        onNotesChange = viewModel::updateNotes,
                        onAddQuickNote = viewModel::showQuickNote,
                        initialMode = NotesTabMode.NOTES
                    )
                    SidebarTool.REFERENCES -> ReferenceTab(
                        sessionGameSystems = uiState.session?.gameSystems ?: emptyList(),
                        onEditTable = onEditReferenceTable,
                        onEditRule = onEditSystemRule,
                        onNewTable = { onNewReferenceTable(uiState.session?.gameSystems?.firstOrNull() ?: "") },
                        onNewRule = { onNewSystemRule(uiState.session?.gameSystems?.firstOrNull() ?: "") }
                    )
                    SidebarTool.JOURNAL -> NotesTab(
                        notes = ChoirGuideNotes.visible(uiState.dmNotes),
                        isSaving = uiState.isSavingNotes,
                        onNotesChange = viewModel::updateNotes,
                        onAddQuickNote = viewModel::showQuickNote,
                        initialMode = NotesTabMode.ACTIVITY
                    )
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
                            peekingWikiEntry = uiState.wikiEntries.find { it.id == wikiId }
                        },
                        onStartEncounter = { encounterId ->
                            viewModel.startEncounterFromScene(encounterId)
                        },
                        onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                        onRollDiceClicked = viewModel::rollSmartDice,
                        onFramingClick = viewModel::openSceneFraming
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
                        } ?: Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Sin crónica Mythic vinculada", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Vincula o crea una crónica para gestionar hilos de trama y personajes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(onClick = viewModel::openSessionConfig) {
                                    Icon(Icons.Default.Link, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Vincular en Ajustes")
                                }
                            }
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
                                    onToolSelected = { tool ->
                                        if (uiState.isHotbarExpanded) viewModel.toggleHotbarExpanded()
                                        viewModel.tacticalMapDelegate.setTacticalTool(tool)
                                    },
                                    onAoeShapeSelected = viewModel.tacticalMapDelegate::setAoeShape,
                                    onAoeRadiusSelected = viewModel.tacticalMapDelegate::setAoeRadius,
                                    onPersistAoE = viewModel.tacticalMapDelegate::persistCurrentAoE,
                                    onClearPersistentAoEs = viewModel.tacticalMapDelegate::clearPersistentAoEs,
                                    onAoEPositionChanged = viewModel.tacticalMapDelegate::updateAoEPosition,
                                    onTokenSelected = { tok ->
                                        if (uiState.isHotbarExpanded) viewModel.toggleHotbarExpanded()
                                        viewModel.tacticalMapDelegate.selectToken(tok)
                                    },
                                    onTokenMoved = viewModel.tacticalMapDelegate::moveToken,
                                    onTokenHpChanged = viewModel.tacticalMapDelegate::updateTokenHp,
                                    onTokenConditionToggled = viewModel.tacticalMapDelegate::toggleCondition,
                                    onTokenSizeChanged = viewModel.tacticalMapDelegate::updateTokenSize,
                                    onTokenElevationChanged = viewModel.tacticalMapDelegate::updateTokenElevation,
                                    onTokenVisibilityToggled = viewModel.tacticalMapDelegate::toggleTokenVisibility,
                                    onApplyDamageToToken = viewModel.tacticalMapDelegate::applyDamageToToken,
                                    onApplyHealingToToken = viewModel.tacticalMapDelegate::applyHealingToToken,
                                    onApplyTempHpToToken = viewModel.tacticalMapDelegate::applyTempHpToToken,
                                    onOpenDamageCalculator = viewModel.tacticalMapDelegate::openDamageCalculator,
                                    onCloseDamageCalculator = viewModel.tacticalMapDelegate::closeDamageCalculator,
                                    onPruneFloatingTexts = viewModel.tacticalMapDelegate::pruneExpiredFloatingCombatTexts,
                                    onOpenNpc = { npcId ->
                                        npcHistory.clear()
                                        uiState.npcs.find { it.id == npcId }?.let { npcHistory.add(it) }
                                    },
                                    onDeleteToken = viewModel.tacticalMapDelegate::deleteToken,
                                    onPinClicked = { pin ->
                                        if (uiState.isHotbarExpanded) viewModel.toggleHotbarExpanded()
                                        viewModel.tacticalMapDelegate.selectPin(pin)
                                    },
                                    onMapClicked = { x, y ->
                                        if (uiState.isHotbarExpanded) viewModel.toggleHotbarExpanded()
                                        viewModel.tacticalMapDelegate.openCreatePin(x, y)
                                    },
                                    onAddToken = viewModel.tacticalMapDelegate::addToken,
                                    onAddTokenFromNpc = viewModel.tacticalMapDelegate::addTokenFromNpc,
                                    onSyncEncounter = viewModel.tacticalMapDelegate::syncTokensFromEncounter,
                                    onUpdateGridCalibration = viewModel.tacticalMapDelegate::updateGridCalibration,
                                    onUpdateMapImage = viewModel.tacticalMapDelegate::updateMapImage,
                                    onStartGuidedCalibration = viewModel.tacticalMapDelegate::startGuidedCalibration,
                                    onGuidedCalibrationPoint = viewModel.tacticalMapDelegate::handleGuidedCalibrationPoint,
                                    onCreateDefaultMap = viewModel.tacticalMapDelegate::createDefaultMap,
                                    onOpenMapSelector = viewModel.tacticalMapDelegate::openMapSelector,
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
                                    onRollSpecificPinTable = viewModel.tacticalMapDelegate::rollPinTable,
                                    onTogglePinSecret = viewModel.tacticalMapDelegate::togglePinSecret,
                                    onEditPin = viewModel.tacticalMapDelegate::openEditPin,
                                    onRollInlineFormula = viewModel.tacticalMapDelegate::rollInlineFormula,
                                    onDismissPin = { viewModel.tacticalMapDelegate.selectPin(null) },
                                    onPinMoved = viewModel.tacticalMapDelegate::movePin,
                                    onCreatePinFromPreset = viewModel.tacticalMapDelegate::createPinFromPreset,
                                    onPinPresetSelected = viewModel.tacticalMapDelegate::setActivePinPreset,
                                    onResetRoomCounter = { viewModel.tacticalMapDelegate.setNextRoomNumber(1) },
                                    onToggleShowSecretPins = viewModel.tacticalMapDelegate::toggleShowSecretPins,
                                    onToggleStickyPin = viewModel.tacticalMapDelegate::toggleStickyPin,
                                    onToggleStickyPinMinimized = viewModel.tacticalMapDelegate::toggleStickyPinMinimized,
                                    onTogglePinLocked = viewModel.tacticalMapDelegate::togglePinLocked,
                                    onDeletePin = viewModel.tacticalMapDelegate::deletePin,
                                    onFocusPinCoordinates = viewModel.tacticalMapDelegate::focusCameraOnCoordinates,
                                    dmNotes = ChoirGuideNotes.visible(uiState.dmNotes),
                                    onNotesChanged = viewModel::updateNotes,
                                    onToggleSidePanel = viewModel.tacticalMapDelegate::toggleSidePanel,
                                    onSetSidePanelOpen = viewModel.tacticalMapDelegate::setSidePanelOpen,
                                    onSetSidePanelTab = viewModel.tacticalMapDelegate::setSidePanelTab,
                                    onSetSidePanelDice = viewModel.tacticalMapDelegate::setSidePanelDice,
                                    onRollSidePanelDice = viewModel.tacticalMapDelegate::rollSidePanelDice,
                                    onRollSidePanelFateCheck = viewModel.tacticalMapDelegate::rollSidePanelFateCheck,
                                    onRollSidePanelRandomEvent = viewModel.tacticalMapDelegate::rollSidePanelRandomEvent,
                                    onWikiSearchQueryChanged = viewModel.tacticalMapDelegate::setWikiSearchQuery,
                                    isToolbarMinimized = (uiState.mainViewMode == SessionViewMode.TACTICAL && uiState.isHotbarExpanded),
                                    onToggleToolbarMinimized = {
                                        if (uiState.isHotbarExpanded) viewModel.toggleHotbarExpanded()
                                    },
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
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                        Text("Sin Mapa de Hexploración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Esta sesión no tiene un mapa hexagonal vinculado para explorar casillas y terreno.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Button(onClick = viewModel::openSessionConfig) {
                                            Icon(Icons.Default.Settings, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Vincular Mapa en Ajustes")
                                        }
                                    }
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
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                        Text("Sin Sesión Mythic Vinculada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Vincula una aventura Mythic para gestionar Factor Caos, lista de hilos y personajes.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Button(onClick = viewModel::openSessionConfig) {
                                            Icon(Icons.Default.Settings, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Configurar Sesión")
                                        }
                                    }
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
                                    onWikiClick = { wikiId ->
                                        peekingWikiEntry = uiState.wikiEntries.find { it.id == wikiId }
                                    },
                                    onStartEncounter = { encounterId ->
                                        viewModel.startEncounterFromScene(encounterId)
                                    },
                                    onRollDice = viewModel::rollSmartDice,
                                    onPrepareSceneForBoard = viewModel::prepareSceneForBoard,
                                    isSidebarOpen = isAnySidebarOpen,
                                    onCardClick = { cardId -> onCardClick(cardId, uiState.sessionId) }
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

            val topHudEncounter = if (uiState.mainViewMode == SessionViewMode.TACTICAL) null else uiState.activeEncounter
            val threads = uiState.mythicState?.threads ?: emptyList()
            val characters = uiState.mythicState?.characters ?: emptyList()
            val topHudCapsuleContent = @Composable {
                SessionTopHudCapsule(
                    encounter = topHudEncounter,
                    players = uiState.playerParticipants,
                    onNextTurn = viewModel::nextTurn,
                    onChoirCharacterCommand = viewModel::updateChoirCharacter,
                    onChoirStateChange = viewModel::updateChoirCombatState,
                    onApplyDamage = viewModel::applyDamage,
                    onToggleCondition = viewModel::toggleCondition,
                    onEndCombat = viewModel::endEncounter,
                    onCopyRoundToDiary = viewModel::copyCombatRoundToDiary,
                    clocks = uiState.tensionClocks,
                    onTickClock = viewModel::tickClock,
                    onAddClock = viewModel::openCreateClockDialog,
                    onEditClock = viewModel::openEditClockDialog,
                    onResetClock = viewModel::resetClock,
                    onDeleteClock = viewModel::deleteClock,
                    onTriggerClockNow = viewModel::triggerClockNow,
                    onCopyClockToDiary = viewModel::copyClockImpactToDiary,
                    threads = threads,
                    characters = characters,
                    selectedThreadsTab = uiState.selectedThreadsTab,
                    lastRandomPick = uiState.lastRandomMythicPick,
                    onSelectThreadsTab = viewModel::selectThreadsTab,
                    onRollRandom = viewModel::rollRandomThreadOrCharacter,
                    onClearRandomPick = viewModel::clearRandomMythicPick,
                    onCopyRandomPickToDiary = viewModel::copyLastRandomPickToDiary,
                    onToggleThreadResolved = viewModel::toggleMythicThreadStatus,
                    onAddThread = viewModel::openCreateThreadDialog,
                    onEditThread = viewModel::openEditThreadDialog,
                    onDeleteThread = viewModel::deleteMythicThread,
                    onAddCharacter = viewModel::openCreateCharacterDialog,
                    onEditCharacter = viewModel::openEditCharacterDialog,
                    onDeleteCharacter = viewModel::deleteMythicCharacter,
                    onCopySummaryToDiary = viewModel::copyThreadsSummaryToDiary,
                    onSendItemToNotes = viewModel::sendRawToNotes,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLeftSidebarOpen) {
                        Row(modifier = Modifier.fillMaxHeight().widthIn(min = 260.dp, max = 320.dp)) {
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
                        Column(modifier = Modifier.fillMaxSize()) {
                            topHudCapsuleContent()
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                centralWorkspaceContent()
                            }
                        }
                    }

                    if (isRightSidebarOpen) {
                        Row(modifier = Modifier.fillMaxHeight().widthIn(min = 260.dp, max = 320.dp)) {
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
                                    SidebarTool.REFERENCES -> "Reglas"
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
                // Portrait: central content with top capsule in normal layout flow and tools bar at bottom
                Column(modifier = Modifier.fillMaxSize()) {
                    topHudCapsuleContent()
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        centralWorkspaceContent()
                    }
                    SessionBottomBar(
                        activeTool = if (isLeftSidebarOpen) uiState.activeLeftSidebar else uiState.activeRightSidebar,
                        hasActiveEncounter = uiState.activeEncounter != null,
                        hasLinkedMythic = uiState.linkedMythicSession != null,
                        onToolClick = { tool ->
                            if (tool in listOf(SidebarTool.DECKS, SidebarTool.TABLES, SidebarTool.COMBAT)) {
                                viewModel.toggleLeftSidebar(tool)
                            } else {
                                viewModel.toggleRightSidebar(tool)
                            }
                        }
                    )
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
                    val isNotesOrJournal = uiState.activeRightSidebar == SidebarTool.NOTES ||
                            uiState.activeRightSidebar == SidebarTool.JOURNAL
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.toggleRightSidebar(SidebarTool.NONE) },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isNotesOrJournal),
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
                                .fillMaxHeight(if (isNotesOrJournal) 0.92f else 0.72f)
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


            // Action Hotbar / Quick Roll Dock (Sprint 59)
            val hotbarActions = uiState.session?.hotbarActions ?: defaultHotbarActions()
            val hotbarBottomPadding = if (!isLandscape) {
                if (uiState.isHotbarExpanded) 60.dp else 64.dp
            } else if (uiState.mainViewMode == SessionViewMode.TACTICAL) {
                if (uiState.isHotbarExpanded) 12.dp else 80.dp
            } else 12.dp
            SessionHotbarDock(
                actions = hotbarActions,
                isExpanded = uiState.isHotbarExpanded,
                onToggleExpanded = viewModel::toggleHotbarExpanded,
                onExecuteAction = viewModel::executeHotbarAction,
                onAddAction = viewModel::openCreateHotbarAction,
                onEditAction = viewModel::openEditHotbarAction,
                modifier = Modifier
                    .align(if (uiState.isHotbarExpanded) Alignment.BottomCenter else Alignment.BottomEnd)
                    .then(if (uiState.isHotbarExpanded) Modifier.fillMaxWidth() else Modifier.wrapContentSize())
                    .padding(bottom = hotbarBottomPadding)
            )


        }
    }

    SessionDialogsHost(
        uiState = uiState,
        viewModel = viewModel,
        showChoirDialog = showChoirDialog,
        onDismissChoirDialog = { showChoirDialog = false },
        showIncursionDialog = showIncursionDialog,
        onDismissIncursionDialog = {
            showIncursionDialog = false
            if (returnToGuideAfterIncursion) viewModel.choirGuideDelegate.open()
            returnToGuideAfterIncursion = false
        },
        onOpenIncursionFromGuide = {
            viewModel.choirGuideDelegate.close()
            returnToGuideAfterIncursion = true
            showIncursionDialog = true
        },
        peekingWikiEntry = peekingWikiEntry,
        onDismissWikiPreview = { peekingWikiEntry = null },
        quickViewNpc = quickViewNpc,
        onDismissNpcPreview = { npcHistory.clear() },
        peekingTable = peekingTable,
        onDismissTablePreview = { peekingTable = null }
    )
}

@Composable
fun CombatEmptyState(
    onStartQuickCombat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.SportsKabaddi,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Sin combate activo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Inicia un combate rápido para añadir participantes, seguir turnos y gestionar iniciativa y puntos de golpe.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStartQuickCombat,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FlashOn, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Iniciar Combate Rápido")
        }
    }
}

@Composable
fun JournalTab(
    notes: String,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (notes.isBlank()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Diario de Viaje Vacío",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "La bitácora registrará automáticamente asaltos de combate, eventos del oráculo y tus apuntes narrativos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onEditNotes) {
                Icon(Icons.Default.EditNote, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escribir Primera Entrada")
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Registro Cronológico",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onEditNotes) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
