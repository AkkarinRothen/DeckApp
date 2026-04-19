package com.deckapp.feature.reference

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deckapp.feature.reference.components.*

@Composable
fun ReferenceTab(
    sessionGameSystems: List<String>,
    onEditTable: (Long) -> Unit,
    onEditRule: (Long) -> Unit,
    onNewTable: () -> Unit,
    onNewRule: () -> Unit,
    viewModel: ReferenceTabViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTable by remember { mutableStateOf<com.deckapp.core.model.ReferenceTable?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage.collect { msg ->
            msg?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearSnackbar()
            }
        }
    }

    if (uiState.showStarterPackDialog) {
        StarterPackDialog(
            packs = uiState.availablePacks,
            onDismiss = { viewModel.setShowStarterPackDialog(false) },
            onInstall = viewModel::installPack,
            onRemove = viewModel::removePack
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Toolbar de búsqueda rápida
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Buscar en referencias...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(onClick = { viewModel.setShowStarterPackDialog(true) }) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Packs de inicio", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { padding ->

        // Pre-filtrar por sistemas de la sesión si no hay filtros activos
        val filteredTables = remember(uiState.referenceTables, uiState.activeSystemFilters, sessionGameSystems, uiState.searchQuery) {
            uiState.referenceTables.filter { table ->
                val systemMatch = if (uiState.activeSystemFilters.isNotEmpty()) {
                    uiState.activeSystemFilters.contains(table.gameSystem)
                } else {
                    sessionGameSystems.isEmpty() || sessionGameSystems.contains(table.gameSystem) || table.gameSystem == "General"
                }
                val queryMatch = uiState.searchQuery.isBlank() || table.name.contains(uiState.searchQuery, ignoreCase = true)
                systemMatch && queryMatch
            }
        }

        val filteredRules = remember(uiState.systemRules, uiState.activeSystemFilters, sessionGameSystems, uiState.searchQuery) {
            uiState.systemRules.filter { rule ->
                val systemMatch = if (uiState.activeSystemFilters.isNotEmpty()) {
                    uiState.activeSystemFilters.contains(rule.gameSystem)
                } else {
                    sessionGameSystems.isEmpty() || sessionGameSystems.contains(rule.gameSystem) || rule.gameSystem == "General"
                }
                val queryMatch = uiState.searchQuery.isBlank() || rule.title.contains(uiState.searchQuery, ignoreCase = true)
                systemMatch && queryMatch
            }
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredTables.isNotEmpty()) {
                item { Text("Tablas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                items(filteredTables) { table ->
                    ReferenceTableCard(
                        table = table,
                        onClick = { selectedTable = table },
                        onLongClick = { onEditTable(table.id) }
                    )
                }
            }

            if (filteredRules.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item { Text("Reglas", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                items(filteredRules) { rule ->
                    SystemRuleCard(
                        rule = rule,
                        onClick = { onEditRule(rule.id) },
                        onLongClick = { onEditRule(rule.id) }
                    )
                }
            }

            if (filteredTables.isEmpty() && filteredRules.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No se encontraron referencias", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    selectedTable?.let { table ->
        ReferenceQuickViewSheet(
            table = table,
            onDismiss = { selectedTable = null },
            onEditClick = { id ->
                selectedTable = null
                onEditTable(id)
            }
        )
    }
}
