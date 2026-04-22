package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Npc
import com.deckapp.core.model.WikiEntry

@Composable
fun SceneActionChips(
    content: String,
    linkedTableId: Long?,
    linkedDeckId: Long?,
    npcs: List<Npc>,
    wikiEntries: List<WikiEntry>,
    onRollTable: (Long) -> Unit,
    onDrawCard: (Long) -> Unit,
    onNpcClick: (Long) -> Unit,
    onWikiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // Detectar menciones de NPCs y Wiki por nombre (case insensitive)
    val detectedNpcs = remember(content, npcs) {
        if (content.isBlank()) emptyList<Npc>()
        else npcs.filter { npc -> content.contains(npc.name, ignoreCase = true) }
    }

    val detectedWiki = remember(content, wikiEntries) {
        if (content.isBlank()) emptyList<WikiEntry>()
        else wikiEntries.filter { entry -> content.contains(entry.title, ignoreCase = true) }
    }

    if (linkedTableId == null && linkedDeckId == null && detectedNpcs.isEmpty() && detectedWiki.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "REFERENCIAS Y ACCIONES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Triggers (Mecánica)
            linkedTableId?.let { tableId ->
                item {
                    ActionChip(
                        label = "Tirar Tabla",
                        icon = Icons.Default.Casino,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onRollTable(tableId) }
                    )
                }
            }

            linkedDeckId?.let { deckId ->
                item {
                    ActionChip(
                        label = "Robar Carta",
                        icon = Icons.Default.Style,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        onClick = { onDrawCard(deckId) }
                    )
                }
            }

            // Lore Mentions (Smart Tags)
            items(detectedNpcs) { npc ->
                ActionChip(
                    label = npc.name,
                    icon = Icons.Default.Person,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { onNpcClick(npc.id) }
                )
            }

            items(detectedWiki) { entry ->
                ActionChip(
                    label = entry.title,
                    icon = Icons.Default.AutoStories,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    onClick = { onWikiClick(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        icon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
