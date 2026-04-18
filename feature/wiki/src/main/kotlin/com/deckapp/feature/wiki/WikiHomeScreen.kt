package com.deckapp.feature.wiki

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.WikiCategory
import com.deckapp.core.model.WikiEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiHomeScreen(
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    onAddEntry: (Long) -> Unit,
    viewModel: WikiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.selectedCategoryId?.let { id -> 
                    uiState.categories.find { it.id == id }?.name ?: "Wiki"
                } ?: "Wiki del Mundo") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.selectedCategoryId != null) {
                            viewModel.selectCategory(null)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedCategoryId == null) {
                FloatingActionButton(onClick = { showAddCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
                }
            } else {
                FloatingActionButton(onClick = { onAddEntry(uiState.selectedCategoryId!!) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Nueva entrada")
                }
            }
        }
    ) { padding ->
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onConfirm = { name ->
                    viewModel.saveCategory(name)
                    showAddCategoryDialog = false
                },
                onDismiss = { showAddCategoryDialog = false }
            )
        }

        Box(Modifier.padding(padding)) {
            if (uiState.selectedCategoryId == null) {
                CategoryGrid(
                    categories = uiState.categories,
                    onCategoryClick = { viewModel.selectCategory(it.id) }
                )
            } else {
                EntryList(
                    entries = uiState.entries,
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<WikiCategory>,
    onCategoryClick: (WikiCategory) -> Unit
) {
    if (categories.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay categorías. Crea una para empezar el Lore.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryCard(category = category, onClick = { onCategoryClick(category) })
            }
        }
    }
}

@Composable
private fun CategoryCard(category: WikiCategory, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description, // TODO: Map iconName to Icon
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(category.name, style = MaterialTheme.typography.titleMedium)
            Text("${category.entryCount} entradas", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun EntryList(
    entries: List<WikiEntry>,
    onEntryClick: (Long) -> Unit
) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Esta categoría está vacía.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries) { entry ->
                ListItem(
                    headlineContent = { Text(entry.title) },
                    supportingContent = { Text(entry.content, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.clickable { onEntryClick(entry.id) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun AddCategoryDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Categoría") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
