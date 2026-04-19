package com.deckapp.feature.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deckapp.feature.reference.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceListScreen(
    onNavigateBack: () -> Unit,
    onEditTable: (Long) -> Unit,
    onEditRule: (Long) -> Unit,
    onNewTable: () -> Unit,
    onNewRule: () -> Unit,
    viewModel: ReferenceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearching by remember { mutableStateOf(false) }

    if (uiState.showStarterPackDialog) {
        StarterPackDialog(
            packs = uiState.availablePacks,
            onDismiss = { viewModel.setShowStarterPackDialog(false) },
            onInstall = viewModel::installPack,
            onRemove = viewModel::removePack
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Buscar...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    } else {
                        Text("Referencias")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = if (isSearching) { { isSearching = false; viewModel.onSearchQueryChanged("") } } else onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearching = !isSearching }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { viewModel.setShowStarterPackDialog(true) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Packs de inicio")
                    }
                }
            )
        },
        floatingActionButton = {
            ReferenceSpeedDial(
                onNewTableClick = onNewTable,
                onNewRuleClick = onNewRule
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SystemFilterBar(
                availableSystems = uiState.availableSystems,
                activeFilters = uiState.activeSystemFilters,
                onToggleFilter = viewModel::toggleSystemFilter
            )

            TabRow(selectedTabIndex = uiState.activePage) {
                Tab(
                    selected = uiState.activePage == 0,
                    onClick = { viewModel.setPage(0) },
                    text = { Text("Tablas") }
                )
                Tab(
                    selected = uiState.activePage == 1,
                    onClick = { viewModel.setPage(1) },
                    text = { Text("Reglas") }
                )
            }

            if (uiState.activePage == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tables) { table ->
                        ReferenceTableCard(
                            table = table,
                            onClick = { onEditTable(table.id) },
                            onLongClick = { /* Dropdown menu logic */ }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.rules) { rule ->
                        SystemRuleCard(
                            rule = rule,
                            onClick = { onEditRule(rule.id) },
                            onLongClick = { /* Dropdown menu logic */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StarterPackDialog(
    packs: List<StarterPackInfo>,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Packs de Inicio") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                item {
                    Text("Añade tablas y reglas predefinidas para sistemas populares.")
                    Spacer(Modifier.height(16.dp))
                }
                items(packs) { pack ->
                    PackItem(pack.displayName, pack.assetName, pack.isInstalled, onInstall, onRemove)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
fun PackItem(
    name: String,
    assetName: String,
    isInstalled: Boolean,
    onInstall: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(if (isInstalled) "Instalado" else "Disponible") },
        trailingContent = {
            Row {
                if (isInstalled) {
                    IconButton(onClick = { onRemove(assetName) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar", tint = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { onInstall(assetName) }) { Text("Actualizar") }
                } else {
                    TextButton(onClick = { onInstall(assetName) }) { Text("Instalar") }
                }
            }
        }
    )
}
