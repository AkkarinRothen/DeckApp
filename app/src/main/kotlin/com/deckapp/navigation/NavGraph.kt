package com.deckapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import com.deckapp.feature.deck.CardEditorScreen
import com.deckapp.feature.deck.CardViewScreen
import com.deckapp.feature.deck.DeckDetailScreen
import com.deckapp.feature.draw.SessionScreen
import com.deckapp.feature.encounters.EncounterEditorScreen
import com.deckapp.feature.encounters.EncounterListScreen
import com.deckapp.feature.importdeck.ImportScreen
import com.deckapp.feature.library.LibraryScreen
import com.deckapp.feature.library.TagManagerScreen
import com.deckapp.feature.session.SessionHistoryScreen
import com.deckapp.feature.session.SessionListScreen
import com.deckapp.feature.session.SessionSetupScreen
import com.deckapp.feature.settings.SettingsScreen
import com.deckapp.feature.importdeck.table.TableImportScreen
import com.deckapp.feature.npcs.NpcEditorScreen
import com.deckapp.feature.npcs.NpcListScreen
import com.deckapp.feature.wiki.WikiHomeScreen
import com.deckapp.feature.wiki.WikiEntryEditorScreen
import com.deckapp.feature.tables.TableEditorScreen
import com.deckapp.feature.reference.ReferenceListScreen
import com.deckapp.feature.reference.ReferenceTableEditorScreen
import com.deckapp.feature.reference.RuleEditorScreen
import com.deckapp.feature.hexploration.HexMapListScreen
import com.deckapp.feature.hexploration.HexMapEditorScreen
import com.deckapp.feature.hexploration.HexMapSessionScreen

@Composable
fun DeckAppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Determina si estamos en una tab del bottom nav (oculta el bottom nav en sub-pantallas)
    val showBottomNav = currentDestination?.let {
        it.hasRoute(LibraryRoute::class) ||
        it.hasRoute(SessionListRoute::class) ||
        it.hasRoute(TablesListRoute::class) ||
        it.hasRoute(SettingsRoute::class)
    } ?: true

    // Determina si hay sesión activa para el FAB
    val isInSession = currentDestination?.hasRoute(SessionRoute::class) ?: false

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(LibraryRoute::class) == true,
                        onClick = {
                            navController.navigate(LibraryRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null) },
                        label = { Text("Biblioteca") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(SessionListRoute::class) == true,
                        onClick = {
                            navController.navigate(SessionListRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Style, contentDescription = null) },
                        label = { Text("Sesión") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(TablesListRoute::class) == true,
                        onClick = {
                            navController.navigate(TablesListRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Casino, contentDescription = null) },
                        label = { Text("Tablas") }
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hasRoute(SettingsRoute::class) == true,
                        onClick = {
                            navController.navigate(SettingsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Ajustes") }
                    )
                }
            }
        },
        floatingActionButton = {
            if (showBottomNav) {
                FloatingActionButton(
                    onClick = { navController.navigate(SessionSetupRoute()) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = if (isInSession) "Robar carta" else "Nueva sesión")
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
                    onDeckClick = { deckId -> navController.navigate(DeckDetailRoute(deckId)) },
                    onImportClick = { navController.navigate(ImportRoute) },
                    onManageTags = { navController.navigate(TagManagerRoute) },
                    onAddToSession = { deckId -> navController.navigate(SessionSetupRoute(deckId)) },
                    onEncounterLibrary = { navController.navigate(EncounterListRoute) },
                    onNpcLibrary = { navController.navigate(NpcListRoute) },
                    onWikiClick = { navController.navigate(WikiRoute) },
                    onReferenceClick = { navController.navigate(ReferenceListRoute) },
                    onHexplorationClick = { navController.navigate(HexMapListRoute) }
                )
            }
            composable<SessionListRoute> {
                SessionListScreen(
                    onSessionClick = { sessionId -> navController.navigate(SessionRoute(sessionId)) },
                    onHistoryClick = { sessionId -> navController.navigate(SessionHistoryRoute(sessionId)) },
                    onNewSession = { navController.navigate(SessionSetupRoute()) }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen()
            }
            composable<DeckDetailRoute> { backStack ->
                val route = backStack.toRoute<DeckDetailRoute>()
                DeckDetailScreen(
                    onCardClick = { cardId -> navController.navigate(CardViewRoute(cardId)) },
                    onAddCard = { deckId -> navController.navigate(CardEditorRoute(deckId = deckId)) },
                    onBack = { navController.popBackStack() },
                    onDeckDuplicated = { newDeckId -> navController.navigate(DeckDetailRoute(newDeckId)) }
                )
            }
            composable<CardViewRoute> { backStack ->
                val route = backStack.toRoute<CardViewRoute>()
                CardViewScreen(
                    onEdit = { cardId -> navController.navigate(CardEditorRoute(cardId = cardId, deckId = -1L)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<CardEditorRoute> {
                CardEditorScreen(
                    onSaved = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<ImportRoute> {
                ImportScreen(
                    onImportSuccess = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<SessionSetupRoute> {
                SessionSetupScreen(
                    onSessionCreated = { sessionId ->
                        navController.navigate(SessionRoute(sessionId)) {
                            popUpTo(SessionSetupRoute::class) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<SessionRoute> {
                SessionScreen(
                    onCardClick = { cardId, sessionId -> navController.navigate(CardViewRoute(cardId, sessionId)) },
                    onSessionEnd = {
                        navController.navigate(SessionListRoute) {
                            popUpTo(SessionListRoute::class) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onBrowseDeck = { deckId -> navController.navigate(DeckDetailRoute(deckId)) },
                    onCreateTable = { navController.navigate(TableEditorRoute()) },
                    onImportTable = { navController.navigate(TableImportRoute) },
                    onEditReferenceTable = { id -> navController.navigate(ReferenceTableEditorRoute(tableId = id)) },
                    onEditSystemRule = { id -> navController.navigate(RuleEditorRoute(ruleId = id)) },
                    onNewReferenceTable = { system -> navController.navigate(ReferenceTableEditorRoute(prefilledSystem = system)) },
                    onNewSystemRule = { system -> navController.navigate(RuleEditorRoute(prefilledSystem = system)) }
                )
            }
            composable<TablesListRoute> {
               com.deckapp.feature.tables.library.TableLibraryScreen(
                   onTableClick = { id -> navController.navigate(TableEditorRoute(id)) },
                   onImportClick = { navController.navigate(TableImportRoute) },
                   onCreateTable = { navController.navigate(TableEditorRoute()) }
               )
            }
            composable<TableEditorRoute> {
                TableEditorScreen(onBack = { navController.popBackStack() })
            }
            composable<SessionHistoryRoute> {
                SessionHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable<TableImportRoute> {
                TableImportScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToTable = { tableId ->
                        navController.navigate(TableEditorRoute(tableId)) {
                            popUpTo(TableImportRoute) { inclusive = true }
                        }
                    }
                )
            }
            composable<TagManagerRoute> {
                TagManagerScreen(onBack = { navController.popBackStack() })
            }
            composable<EncounterListRoute> {
                EncounterListScreen(
                    onEncounterClick = { id -> navController.navigate(EncounterEditorRoute(id)) },
                    onStartEncounter = { 
                        navController.navigate(SessionListRoute) {
                            popUpTo(EncounterListRoute) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<EncounterEditorRoute> {
                EncounterEditorScreen(onBack = { navController.popBackStack() })
            }
            composable<NpcListRoute> {
                NpcListScreen(
                    onBack = { navController.popBackStack() },
                    onAddNpc = { navController.navigate(NpcEditorRoute()) },
                    onEditNpc = { id -> navController.navigate(NpcEditorRoute(id)) }
                )
            }
            composable<NpcEditorRoute> {
                NpcEditorScreen(onBack = { navController.popBackStack() })
            }
            composable<WikiRoute> {
                WikiHomeScreen(
                    onBack = { navController.popBackStack() },
                    onEntryClick = { id -> navController.navigate(WikiEntryRoute(entryId = id)) },
                    onAddEntry = { catId -> navController.navigate(WikiEntryRoute(categoryId = catId)) }
                )
            }
            composable<WikiEntryRoute> {
                WikiEntryEditorScreen(onBack = { navController.popBackStack() })
            }
            composable<ReferenceListRoute> {
                ReferenceListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditTable = { id -> navController.navigate(ReferenceTableEditorRoute(tableId = id)) },
                    onEditRule = { id -> navController.navigate(RuleEditorRoute(ruleId = id)) },
                    onNewTable = { navController.navigate(ReferenceTableEditorRoute()) },
                    onNewRule = { navController.navigate(RuleEditorRoute()) }
                )
            }
            composable<ReferenceTableEditorRoute> { backStack ->
                val route = backStack.toRoute<ReferenceTableEditorRoute>()
                ReferenceTableEditorScreen(
                    tableId = route.tableId,
                    prefilledSystem = route.prefilledSystem,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable<RuleEditorRoute> { backStack ->
                val route = backStack.toRoute<RuleEditorRoute>()
                RuleEditorScreen(
                    ruleId = route.ruleId,
                    prefilledSystem = route.prefilledSystem,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Hexploración
            composable<HexMapListRoute> {
                HexMapListScreen(
                    onMapClick = { id -> navController.navigate(HexMapEditorRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<HexMapEditorRoute> {
                HexMapEditorScreen(
                    onStartSession = { id -> navController.navigate(HexMapSessionRoute(id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable<HexMapSessionRoute> {
                HexMapSessionScreen(
                    onNavigateToEncounter = { id -> navController.navigate(EncounterEditorRoute(id)) },
                    onNavigateToEncounterList = { navController.navigate(EncounterListRoute) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
