package com.deckapp.feature.tables

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.RandomTable
import com.deckapp.core.ui.components.SelectionActionBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablesTab(
    sessionId: Long?,
    onCreateTable: () -> Unit,
    onImportTable: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = viewModel.getExportJson()
        if (json.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(uiState.activeTable, sessionId) {
        if (uiState.activeTable != null && sessionId != null) {
            viewModel.loadRecentResults(sessionId)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.setSession(sessionId)
    }

    if (uiState.activeTable != null) {
        val activeTable = uiState.activeTable!!
        TableDetailSheet(
            table = activeTable,
            lastResult = uiState.lastResult,
            recentResults = uiState.recentResults,
            isRolling = uiState.isRolling,
            onRoll = { viewModel.rollTable(activeTable.id, sessionId) },
            onExport = {
                val safeName = activeTable.name
                    .replace(Regex("[^\\w\\s-]"), "")
                    .trim()
                    .replace(" ", "_")
                    .ifBlank { "tabla" }
                exportLauncher.launch("$safeName.json")
            },
            onDismiss = { viewModel.closeTable() }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (uiState.allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTagIds.isEmpty(),
                            onClick = { viewModel.clearFilters() },
                            label = { Text("Todos") }
                        )
                    }
                    items(uiState.allTags) { tag ->
                        FilterChip(
                            selected = tag.id in uiState.selectedTagIds,
                            onClick = { viewModel.toggleTagFilter(tag.id) },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(tag.color).copy(alpha = 0.2f),
                                selectedLabelColor = Color(tag.color)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (sessionId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (uiState.showAllTables) "Mostrando todas las tablas" else "Tablas de la sesión",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = uiState.showAllTables,
                        onCheckedChange = { viewModel.setShowAllTables(it) },
                        modifier = Modifier.scale(0.7f)
                    )
                }
            }

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
                            val isSelected = table.id in uiState.selectedTableIds
                            TableListItem(
                                table = table,
                                isSelected = isSelected,
                                isSelectionMode = uiState.selectedTableIds.isNotEmpty(),
                                onLongClick = { viewModel.toggleTableSelection(table.id) },
                                onClick = {
                                    if (uiState.selectedTableIds.isNotEmpty()) {
                                        viewModel.toggleTableSelection(table.id)
                                    } else {
                                        viewModel.openTable(table)
                                    }
                                },
                                onQuickRoll = { viewModel.rollTable(table.id, sessionId) },
                                onTogglePin = { viewModel.togglePin(table) }
                            )
                        }
                    }
                }
            }
        }

        if (uiState.selectedTableIds.isNotEmpty()) {
            SelectionActionBar(
                count = uiState.selectedTableIds.size,
                onClear = { viewModel.clearSelection() },
                onDelete = { viewModel.bulkDelete() },
                onTogglePin = { viewModel.bulkTogglePin(it) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = onImportTable,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.LibraryAdd, contentDescription = "Importar tabla")
                }

                FloatingActionButton(
                    onClick = onCreateTable
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva tabla")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableListItem(
    table: RandomTable,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickRoll: () -> Unit,
    onTogglePin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = when {
            isSelected -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            table.isPinned -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            else -> CardDefaults.cardColors()
        },
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    table.tags.take(3).forEach { tag ->
                        Surface(
                            color = Color(tag.color).copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(tag.color),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (table.tags.size > 3) {
                        Text(
                            text = "+${table.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (table.tags.isNotEmpty()) {
                        Text("·", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "${table.rollFormula} · ${table.entries.size} entradas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!isSelectionMode) {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (table.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (table.isPinned) "Quitar pin" else "Fijar tabla",
                        tint = if (table.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onQuickRoll) {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = "Tirar ${table.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onLongClick() }
                )
            }
        }
    }
}
