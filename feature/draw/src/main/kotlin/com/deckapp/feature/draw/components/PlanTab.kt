package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Npc
import com.deckapp.core.model.Scene
import com.deckapp.core.model.WikiEntry
import com.deckapp.core.ui.components.MarkdownText

@Composable
fun PlanTab(
    scenes: List<Scene>,
    npcs: List<Npc>,
    wikiEntries: List<WikiEntry>,
    onToggleCompletion: (Long, Boolean) -> Unit,
    onRollTable: (Long) -> Unit,
    onDrawCard: (Long) -> Unit,
    onNpcClick: (Long) -> Unit,
    onWikiClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (scenes.isEmpty()) {
        // ... (Empty State)
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.HistoryEdu,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text("Sin plan preparado", style = MaterialTheme.typography.titleSmall)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "ESCENAS DE AVENTURA",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            itemsIndexed(scenes, key = { _, s -> s.id }) { index, scene ->
                SceneLiveItem(
                    scene = scene,
                    npcs = npcs,
                    wikiEntries = wikiEntries,
                    isLast = index == scenes.size - 1,
                    onToggleCompletion = { onToggleCompletion(scene.id, it) },
                    onRollTable = onRollTable,
                    onDrawCard = onDrawCard,
                    onNpcClick = onNpcClick,
                    onWikiClick = onWikiClick
                )
            }
        }
    }
}

@Composable
private fun SceneLiveItem(
    scene: Scene,
    npcs: List<Npc>,
    wikiEntries: List<WikiEntry>,
    isLast: Boolean,
    onToggleCompletion: (Boolean) -> Unit,
    onRollTable: (Long) -> Unit,
    onDrawCard: (Long) -> Unit,
    onNpcClick: (Long) -> Unit,
    onWikiClick: (Long) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline... (código existente omitido para brevedad en el TargetContent, pero lo incluiré completo)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (scene.isCompleted) MaterialTheme.colorScheme.primary else (if(scene.isAlternative) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant)),
                contentAlignment = Alignment.Center
            ) {
                if (scene.isCompleted) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
                else Text((scene.sortOrder + 1).toString(), style = MaterialTheme.typography.labelSmall)
            }
            if (!isLast) Box(Modifier.width(2.dp).weight(1f).padding(vertical = 4.dp).background(MaterialTheme.colorScheme.outlineVariant))
        }

        Spacer(Modifier.width(12.dp))

        Card(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
                .weight(1f)
                .alpha(if (scene.isCompleted) 0.6f else 1f),
            colors = CardDefaults.cardColors(
                containerColor = if (scene.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (scene.isCompleted) 0.dp else 1.dp),
            // Estilo alternativo via borde dashed (simulado con alfa y grosor)
            border = if (scene.isAlternative && !scene.isCompleted) 
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) 
                else CardDefaults.outlinedCardBorder()
        ) {
            Column {
                // Mood Image
                if (!scene.imagePath.isNullOrBlank()) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(scene.imagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = scene.isCompleted, onCheckedChange = onToggleCompletion, modifier = Modifier.size(24.dp).padding(end = 12.dp))
                        Text(
                            text = if (scene.isAlternative) "⌥ ${scene.title}" else scene.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, null, modifier = Modifier.size(20.dp))
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            MarkdownText(
                                markdown = if (scene.content.isBlank()) "_Sin notas de preparación._" else scene.content,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(Modifier.height(16.dp))
                            
                            SceneActionChips(
                                content = scene.content,
                                linkedTableId = scene.linkedTableId,
                                linkedDeckId = scene.linkedDeckId,
                                npcs = npcs,
                                wikiEntries = wikiEntries,
                                onRollTable = onRollTable,
                                onDrawCard = onDrawCard,
                                onNpcClick = onNpcClick,
                                onWikiClick = onWikiClick
                            )
                        }
                    }
                }
            }
        }
    }
}
