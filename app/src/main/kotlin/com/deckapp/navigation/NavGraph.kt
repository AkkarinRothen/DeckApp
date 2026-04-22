package com.deckapp.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import com.deckapp.feature.deck.*
import com.deckapp.feature.draw.*
import com.deckapp.feature.encounters.*
import com.deckapp.feature.importdeck.*
import com.deckapp.feature.library.*
import com.deckapp.feature.session.*
import com.deckapp.feature.settings.SettingsScreen
import com.deckapp.feature.npcs.*
import com.deckapp.feature.wiki.*
import com.deckapp.feature.tables.TableEditorScreen
import com.deckapp.feature.reference.*
import com.deckapp.feature.hexploration.*
import com.deckapp.feature.mythic.*
import com.deckapp.feature.dice.DiceRollerBottomSheet
import com.deckapp.core.ui.components.QuickNoteOverlay
import com.deckapp.app.QuickNoteViewModel
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.deckapp.core.domain.usecase.GlobalSearchResultType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckAppNavHost(
    quickNoteViewModel: QuickNoteViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    var showDiceRoller by remember { mutableStateOf(false) }
    val diceRollerSheetState = rememberModalBottomSheetState()

    var showMythicOracle by remember { mutableStateOf(false) }
    val mythicOracleSheetState = rememberModalBottomSheetState()
    var activeMythicSessionId by remember { mutableStateOf<Long?>(null) }

    var showGlobalSearch by remember { mutableStateOf(false) }
    val globalSearchSheetState = rememberModalBottomSheetState()

    var showQuickNote by remember { mutableStateOf(false) }

    if (showDiceRoller) {
        ModalBottomSheet(onDismissRequest = { showDiceRoller = false }, sheetState = diceRollerSheetState) {
            DiceRollerBottomSheet(onDismiss = { showDiceRoller = false })
        }
    }

    if (showMythicOracle) {
        // MythicOracleOverlay no existe
    }

    if (showGlobalSearch) {
        ModalBottomSheet(onDismissRequest = { showGlobalSearch = false }, sheetState = globalSearchSheetState) {
            GlobalSearchOverlay(
                onNavigateToResult = { result ->
                    when (result.type) {
                        GlobalSearchResultType.TABLE -> navController.navigate(ReferenceTableEditorRoute(tableId = result.id))
                        GlobalSearchResultType.RULE -> navController.navigate(RuleEditorRoute(ruleId = result.id))
                        GlobalSearchResultType.MANUAL -> navController.navigate(ManualViewerRoute(manualId = result.id))
                        GlobalSearchResultType.NPC -> navController.navigate(NpcEditorRoute(npcId = result.id))
                        GlobalSearchResultType.WIKI -> navController.navigate(WikiEntryRoute(entryId = result.id))
                    }
                    showGlobalSearch = false
                },
                onDismiss = { showGlobalSearch = false }
            )
        }
    }

    if (showQuickNote) {
        QuickNoteOverlay(
            onDismiss = { showQuickNote = false },
            onSave = { content -> 
                val currentMythicId = if (currentDestination?.hasRoute(MythicSessionRoute::class) == true) {
                    backStackEntry?.toRoute<MythicSessionRoute>()?.sessionId
                } else activeMythicSessionId

                quickNoteViewModel.saveNote(content, mythicSessionId = currentMythicId)
                showQuickNote = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { showQuickNote = true })
        },
        bottomBar = {
            if (currentDestination?.hasRoute(ImportRoute::class) != true &&
                currentDestination?.hasRoute(TableImportRoute::class) != true) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(LibraryRoute::class) == true,
                        onClick = {
                            navController.navigate(LibraryRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null) },
                        label = { Text("Librería") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(SessionListRoute::class) == true,
                        onClick = { navController.navigate(SessionListRoute) },
                        icon = { Icon(Icons.Default.PlayArrow, null) },
                        label = { Text("Sesiones") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { showGlobalSearch = true },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Buscar") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
                        onClick = { navController.navigate(SettingsRoute) },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Ajustes") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LibraryRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<LibraryRoute> {
                LibraryScreen(
                    onDeckClick = { id -> navController.navigate(DeckDetailRoute(deckId = id)) },
                    onImportClick = { navController.navigate(ImportRoute) },
                    onManageTags = { /* TODO */ },
                    onAddToSession = { id -> /* TODO */ },
                    onEncounterLibrary = { navController.navigate(EncounterListRoute) },
                    onNpcLibrary = { navController.navigate(NpcListRoute) },
                    onWikiClick = { navController.navigate(WikiRoute) },
                    onReferenceClick = { navController.navigate(ReferenceListRoute) },
                    onHexplorationClick = { navController.navigate(HexMapListRoute) },
                    onShowDiceRoller = { showDiceRoller = true }
                )
            }

            composable<ImportRoute> {
                ImportScreen(
                    onBack = { navController.popBackStack() },
                    onImportSuccess = { id -> navController.navigate(DeckDetailRoute(id)) }
                )
            }

            composable<DeckDetailRoute> {
                val args = it.toRoute<DeckDetailRoute>()
                DeckDetailScreen(
                    onBack = { navController.popBackStack() },
                    onCardClick = { cardId -> navController.navigate(CardViewRoute(cardId = cardId)) },
                    onAddCard = { deckId -> navController.navigate(CardEditorRoute(deckId = deckId)) }
                )
            }

            composable<SessionListRoute> {
                SessionListScreen(
                    onSessionClick = { id -> navController.navigate(SessionRoute(sessionId = id)) },
                    onNewSession = { navController.navigate(SessionSetupRoute()) },
                    onPlannerClick = { id -> navController.navigate(SessionPlannerRoute(sessionId = id)) },
                    onHistoryClick = { id -> navController.navigate(SessionHistoryRoute(sessionId = id)) },
                    onAnalyticsClick = { navController.navigate(GlobalAnalyticsRoute) }
                )
            }

            composable<SessionHistoryRoute> {
                SessionHistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<GlobalAnalyticsRoute> {
                GlobalAnalyticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<SessionRoute> {
                val args = it.toRoute<SessionRoute>()
                SessionScreen(
                    onCardClick = { cardId, sessionId -> navController.navigate(CardViewRoute(cardId = cardId, sessionId = sessionId)) },
                    onSessionEnd = { navController.popBackStack() },
                    onBrowseDeck = { deckId -> navController.navigate(DeckDetailRoute(deckId = deckId)) },
                    onCreateTable = { sessionId -> navController.navigate(TableEditorRoute(sessionId = sessionId)) },
                    onImportTable = { sessionId -> navController.navigate(TableImportRoute(sessionId = sessionId)) },
                    onEditTable = { id -> navController.navigate(TableEditorRoute(tableId = id)) },
                    onEditReferenceTable = { id -> navController.navigate(ReferenceTableEditorRoute(tableId = id)) },
                    onEditSystemRule = { id -> navController.navigate(RuleEditorRoute(ruleId = id)) },
                    onNewReferenceTable = { sys -> navController.navigate(ReferenceTableEditorRoute(prefilledSystem = sys)) },
                    onNewSystemRule = { sys -> navController.navigate(RuleEditorRoute(prefilledSystem = sys)) },
                    onShowDiceRoller = { showDiceRoller = true },
                    onShowMythicOracle = { showMythicOracle = true }
                )
            }
            
            composable<EncounterListRoute> {
                EncounterListScreen(
                    onBack = { navController.popBackStack() },
                    onEncounterClick = { id -> navController.navigate(EncounterEditorRoute(encounterId = id)) },
                    onStartEncounter = { /* TODO */ }
                )
            }
            
            composable<EncounterEditorRoute> {
                val args = it.toRoute<EncounterEditorRoute>()
                EncounterEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<NpcListRoute> {
                NpcListScreen(
                    onBack = { navController.popBackStack() },
                    onEditNpc = { id -> navController.navigate(NpcEditorRoute(npcId = id)) },
                    onAddNpc = { navController.navigate(NpcEditorRoute(npcId = 0L)) }
                )
            }
            
            composable<NpcEditorRoute> {
                NpcEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable<WikiRoute> {
                WikiHomeScreen(
                    onBack = { navController.popBackStack() },
                    onEntryClick = { id -> navController.navigate(WikiEntryRoute(entryId = id)) },
                    onAddEntry = { catId -> navController.navigate(WikiEntryRoute(categoryId = catId)) }
                )
            }
            
            composable<WikiEntryRoute> {
                WikiEntryEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<TableImportRoute> {
                val args = it.toRoute<TableImportRoute>()
                com.deckapp.feature.importdeck.table.TableImportScreen(
                    sessionId = args.sessionId,
                    onBack = { navController.popBackStack() },
                    onNavigateToTable = { id -> navController.navigate(TableEditorRoute(tableId = id)) }
                )
            }

            composable<ReferenceListRoute> {
                ReferenceListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditTable = { id -> navController.navigate(ReferenceTableEditorRoute(tableId = id)) },
                    onEditRule = { id -> navController.navigate(RuleEditorRoute(ruleId = id)) },
                    onOpenManual = { id -> navController.navigate(ManualViewerRoute(manualId = id)) },
                    onNewTable = { navController.navigate(ReferenceTableEditorRoute()) },
                    onNewRule = { navController.navigate(RuleEditorRoute()) }
                )
            }

            composable<ReferenceTableEditorRoute> {
                TableEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<TableEditorRoute> {
                TableEditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<RuleEditorRoute> {
                val args = it.toRoute<RuleEditorRoute>()
                RuleEditorScreen(
                    ruleId = args.ruleId,
                    prefilledSystem = args.prefilledSystem,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ManualViewerRoute> {
                val args = it.toRoute<ManualViewerRoute>()
                ManualViewerScreen(
                    manualId = args.manualId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable<HexMapListRoute> {
                HexMapListScreen(
                    onBack = { navController.popBackStack() },
                    onMapClick = { id -> navController.navigate(HexMapSessionRoute(mapId = id)) }
                )
            }

            composable<HexMapSessionRoute> {
                val args = it.toRoute<HexMapSessionRoute>()
                HexMapSessionScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToEncounter = { id -> navController.navigate(EncounterEditorRoute(encounterId = id)) },
                    onNavigateToEncounterList = { navController.navigate(EncounterListRoute) },
                    onShowMythicOracle = { 
                        activeMythicSessionId = null
                        showMythicOracle = true 
                    }
                )
            }

            composable<MythicListRoute> {
                MythicSessionListScreen(
                    onNavigateToSession = { id -> navController.navigate(MythicSessionRoute(sessionId = id)) }
                )
            }

            composable<MythicSessionRoute> {
                val args = it.toRoute<MythicSessionRoute>()
                MythicSessionScreen(
                    onBack = { navController.popBackStack() },
                    onShowDiceRoller = { showDiceRoller = true },
                    onNavigateToHelp = { /* TODO */ }
                )
            }

            composable<SettingsRoute> {
                SettingsScreen()
            }

            // Rutas adicionales
            composable<CardViewRoute> {
                CardViewScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(CardEditorRoute(cardId = id, deckId = 0L)) }
                )
            }

            composable<CardEditorRoute> {
                CardEditorScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable<SessionSetupRoute> {
                SessionSetupScreen(
                    onBack = { navController.popBackStack() },
                    onSessionCreated = { id -> navController.navigate(SessionRoute(id)) }
                )
            }

            composable<SessionPlannerRoute> {
                SessionPlannerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
