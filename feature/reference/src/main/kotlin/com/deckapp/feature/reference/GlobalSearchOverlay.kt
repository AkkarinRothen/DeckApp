package com.deckapp.feature.reference

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.domain.usecase.SearchResult
import com.deckapp.core.domain.usecase.GlobalSearchResultType
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchOverlay(
    onNavigateToResult: (SearchResult) -> Unit,
    onDismiss: () -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChanged,
                placeholder = { Text("Buscar reglas, tablas, NPCs...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            if (uiState.query.isNotBlank()) {
                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        }

        if (uiState.isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.results.isEmpty() && uiState.query.isNotBlank() && !uiState.isSearching) {
                item {
                    Text(
                        "No se encontraron resultados para '${uiState.query}'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(uiState.results) { result ->
                SearchResultItem(result = result, onClick = { 
                    onNavigateToResult(result)
                    onDismiss()
                })
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultItem(result: SearchResult, onClick: () -> Unit) {
    val icon = when (result.type) {
        GlobalSearchResultType.TABLE -> Icons.Default.Casino
        GlobalSearchResultType.RULE -> Icons.Default.Description
        GlobalSearchResultType.MANUAL -> Icons.Default.PictureAsPdf
        GlobalSearchResultType.NPC -> Icons.Default.Person
        GlobalSearchResultType.WIKI -> Icons.Default.Book
    }

    val typeLabel = when (result.type) {
        GlobalSearchResultType.TABLE -> "Tabla"
        GlobalSearchResultType.RULE -> "Regla"
        GlobalSearchResultType.MANUAL -> "Manual"
        GlobalSearchResultType.NPC -> "NPC"
        GlobalSearchResultType.WIKI -> "Wiki"
    }

    ListItem(
        headlineContent = { Text(result.title) },
        supportingContent = { Text("${typeLabel} • ${result.subtitle}") },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier
            .dragAndDropSource {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        startTransfer(
                            DragAndDropTransferData(
                                clipData = ClipData.newPlainText(
                                    "search_result",
                                    Json.encodeToString(result)
                                )
                            )
                        )
                    }
                )
            }
    )
    HorizontalDivider(thickness = 0.5.dp)
}
