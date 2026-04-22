package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.deckapp.core.model.WikiEntry
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.feature.wiki.WikiViewModel
import com.deckapp.feature.wiki.WikiUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WikiTab(
    viewModel: WikiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedEntry by remember { mutableStateOf<WikiEntry?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = selectedEntry,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "WikiNavigation"
        ) { entry ->
            if (entry != null) {
                WikiEntryDetail(
                    entry = entry,
                    onBack = { selectedEntry = null },
                    onWikiLinkClicked = { title ->
                        // Intentar buscar la entrada por título
                        // Esto es simplificado, idealmente el ViewModel debería tener un findByTitle
                        val target = uiState.entries.find { it.title.equals(title, ignoreCase = true) }
                        if (target != null) selectedEntry = target
                    }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        placeholder = { Text("Buscar Lore...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = { 
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null) }
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )

                    val filteredEntries = uiState.entries.filter { 
                        it.title.contains(searchQuery, ignoreCase = true) || 
                        it.content.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredEntries) { entry ->
                            WikiEntryItem(
                                entry = entry,
                                onClick = { selectedEntry = entry }
                            )
                        }
                        
                        if (filteredEntries.isEmpty() && searchQuery.isNotEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No se encontró nada con ese nombre.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WikiEntryItem(entry: WikiEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.title, fontWeight = FontWeight.Bold) },
        supportingContent = { 
            Text(
                entry.content, 
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            ) 
        },
        leadingContent = {
            if (entry.imagePath != null) {
                AsyncImage(
                    model = entry.imagePath,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun WikiEntryDetail(
    entry: WikiEntry,
    onBack: () -> Unit,
    onWikiLinkClicked: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Text("Detalle de Lore", style = MaterialTheme.typography.titleMedium)
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (entry.imagePath != null) {
                item {
                    AsyncImage(
                        model = entry.imagePath,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
            
            item {
                Text(entry.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                MarkdownText(
                    markdown = entry.content,
                    onWikiLinkClicked = onWikiLinkClicked
                )
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}
