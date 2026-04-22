package com.deckapp.feature.wiki

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.WikiCategory
import com.deckapp.core.model.WikiEntry

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                } ?: "Crónicas del Reino") },
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
                onConfirm = { name, icon ->
                    viewModel.saveCategory(name, icon)
                    showAddCategoryDialog = false
                },
                onDismiss = { showAddCategoryDialog = false }
            )
        }

        Box(Modifier.padding(padding)) {
            if (uiState.selectedCategoryId == null) {
                DashboardHome(
                    uiState = uiState,
                    onCategoryClick = { viewModel.selectCategory(it.id) },
                    onEntryClick = onEntryClick
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
private fun DashboardHome(
    uiState: WikiUiState,
    onCategoryClick: (WikiCategory) -> Unit,
    onEntryClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Pinned Section
        if (uiState.pinnedEntries.isNotEmpty()) {
            item {
                SectionHeader("Destacados", Icons.Default.PushPin)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(uiState.pinnedEntries) { entry ->
                        PinnedEntryCard(entry, onClick = { onEntryClick(entry.id) })
                    }
                }
            }
        }

        // Recent Section
        if (uiState.recentEntries.isNotEmpty()) {
             item {
                SectionHeader("Actualizado Recientemente", Icons.Default.History)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.recentEntries.take(3).forEach { entry ->
                        RecentEntryItem(entry, onClick = { onEntryClick(entry.id) })
                    }
                }
            }
        }

        // Category Grid
        item {
            SectionHeader("Explorar por Categoría", Icons.Default.GridView)
            Spacer(Modifier.height(8.dp))
        }
        
        item {
            CategoryGrid(
                categories = uiState.categories,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PinnedEntryCard(entry: WikiEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(160.dp).height(100.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (entry.imagePath != null) {
                AsyncImage(
                    model = entry.imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().alpha(0.6f)
                )
                Box(Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 50f
                    )
                ))
            }
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (entry.imagePath != null) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp).align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
private fun RecentEntryItem(entry: WikiEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.imagePath != null) {
                    AsyncImage(
                        model = entry.imagePath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Description, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text("Editado recientemente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text("No hay categorías. Empieza por crear el origen del mundo.", 
             style = MaterialTheme.typography.bodyMedium, 
             color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        // Rediseño de grilla: Grid manual dentro del dashboard
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            categories.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { category ->
                        CategoryCard(
                            category = category, 
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(category: WikiCategory, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = getWikiIcon(category.iconName),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(category.name, style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text("${category.entryCount} entradas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
                    modifier = Modifier.clickable { onEntryClick(entry.id) }.clip(RoundedCornerShape(8.dp)),
                    trailingContent = { Icon(Icons.Default.KeyboardArrowRight, null) },
                    leadingContent = {
                        if (entry.isPinned) Icon(Icons.Default.PushPin, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("Description") }
    
    val icons = listOf("Description", "Map", "Shield", "Person", "History", "Castle", "AutoStories", "MenuBook")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text("Icono", style = MaterialTheme.typography.labelSmall)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    icons.forEach { iconName ->
                        FilterChip(
                            selected = selectedIcon == iconName,
                            onClick = { selectedIcon = iconName },
                            label = { Icon(getWikiIcon(iconName), null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, selectedIcon) }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun getWikiIcon(name: String): ImageVector {
    return when(name) {
        "Map" -> Icons.Default.Map
        "Shield" -> Icons.Default.Shield
        "Person" -> Icons.Default.Person
        "History" -> Icons.Default.History
        "Castle" -> Icons.Default.Fort // Fort as fallback for Castle
        "AutoStories" -> Icons.Default.AutoStories
        "MenuBook" -> Icons.Default.MenuBook
        else -> Icons.Default.Description
    }
}
