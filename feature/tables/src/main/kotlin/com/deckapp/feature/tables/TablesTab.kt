package com.deckapp.feature.tables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.RandomTable

/**
 * Tab de Tablas Aleatorias dentro de SessionScreen (page 1 del HorizontalPager).
 *
 * Responsabilidades:
 * - Listar tablas filtradas por categoría y búsqueda
 * - Tirada rápida inline con botón 🎲
 * - Abrir [TableDetailSheet] para historial + tirada detallada
 * - Botón flotante → [TableEditorScreen] para crear tablas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesTab(
    sessionId: Long?,
    onCreateTable: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(uiState.activeTable, sessionId) {
        if (uiState.activeTable != null && sessionId != null) {
            viewModel.loadRecentResults(sessionId)
        }
    }

    // BottomSheet de detalle
    if (uiState.activeTable != null) {
        TableDetailSheet(
            table = uiState.activeTable!!,
            lastResult = uiState.lastResult,
            recentResults = uiState.recentResults,
            isRolling = uiState.isRolling,
            onRoll = { viewModel.rollTable(uiState.activeTable!!.id, sessionId) },
            onDismiss = { viewModel.closeTable() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Barra de búsqueda ─────────────────────────────────────────
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Buscar tablas…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Chips de categoría ────────────────────────────────────────
            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Lista de tablas ───────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                viewModel.filteredTables().isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No hay tablas",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = onCreateTable) {
                                Text("Crear primera tabla")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(viewModel.filteredTables(), key = { it.id }) { table ->
                            TableListItem(
                                table = table,
                                onOpen = { viewModel.openTable(table) },
                                onQuickRoll = { viewModel.rollTable(table.id, sessionId) }
                            )
                        }
                    }
                }
            }
        }

        // FAB para crear nueva tabla
        FloatingActionButton(
            onClick = onCreateTable,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp)  // 88dp = por encima del FAB principal
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva tabla")
        }
    }
}

@Composable
private fun TableListItem(
    table: RandomTable,
    onOpen: () -> Unit,
    onQuickRoll: () -> Unit
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (table.category.isNotBlank()) {
                        Text(
                            text = table.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${table.rollFormula} · ${table.entries.size} entradas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onQuickRoll) {
                Icon(
                    Icons.Default.Casino,
                    contentDescription = "Tirar ${table.name}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
