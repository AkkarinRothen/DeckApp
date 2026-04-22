package com.deckapp.feature.reference

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.Manual
import com.deckapp.feature.reference.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceListScreen(
    onNavigateBack: () -> Unit,
    onEditTable: (Long) -> Unit,
    onEditRule: (Long) -> Unit,
    onOpenManual: (Long) -> Unit,
    onNewTable: () -> Unit,
    onNewRule: () -> Unit,
    viewModel: ReferenceListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearching by remember { mutableStateOf(false) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                // Por ahora usamos el nombre del archivo como título
                viewModel.addManual(it.lastPathSegment ?: "Manual", it.toString(), "General")
            }
        }
    )

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            if (uiState.activePage == 2) {
                FloatingActionButton(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Añadir manual")
                }
            } else {
                ReferenceSpeedDial(
                    onNewTableClick = onNewTable,
                    onNewRuleClick = onNewRule
                )
            }
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
                Tab(
                    selected = uiState.activePage == 2,
                    onClick = { viewModel.setPage(2) },
                    text = { Text("Manuales") }
                )
            }

            when (uiState.activePage) {
                0 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.tables, key = { it.id }) { table ->
                        ReferenceTableCard(
                            table = table,
                            onClick = { onEditTable(table.id) },
                            onLongClick = { viewModel.deleteTable(table.id) }
                        )
                    }
                }
                1 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.rules, key = { it.id }) { rule ->
                        SystemRuleCard(
                            rule = rule,
                            onClick = { onEditRule(rule.id) },
                            onLongClick = { viewModel.deleteRule(rule.id) }
                        )
                    }
                }
                2 -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.manuals.isEmpty()) {
                        item {
                            Text(
                                "No has añadido manuales todavía. Pulsa el botón + para añadir un archivo PDF.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(uiState.manuals, key = { it.id }) { manual ->
                        ManualItemCard(
                            manual = manual,
                            onClick = { onOpenManual(manual.id) },
                            onDelete = { viewModel.deleteManual(manual) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualItemCard(
    manual: Manual,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(manual.title, style = MaterialTheme.typography.titleMedium)
                Text(manual.gameSystem, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
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
