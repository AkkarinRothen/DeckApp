package com.deckapp.feature.tables.library.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.PaperCard
import com.deckapp.core.ui.components.categoryEmblem
import com.deckapp.core.model.TableDensity
import com.deckapp.core.ui.theme.LocalTableAppearance

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableGridItem(
    table: RandomTable,
    onClick: () -> Unit,
    onQuickRoll: () -> Unit,
    onTogglePin: () -> Unit,
    onDuplicate: (() -> Unit)? = null,
    onInvertRanges: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    quickRollResult: TableRollResult? = null,
    onDismissResult: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val compact = LocalTableAppearance.current.density == TableDensity.COMPACT
    val contentPadding = if (compact) 12.dp else 16.dp

    val containerColor = when {
        quickRollResult != null -> MaterialTheme.colorScheme.secondaryContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val accentColor = table.tags.firstOrNull()?.let { Color(it.color) }
        ?: MaterialTheme.colorScheme.primary

    PaperCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 168.dp else 188.dp),
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = containerColor,
        shape = MaterialTheme.shapes.medium,
        elevation = if (isSelected) 8.dp else 1.dp,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(Modifier.matchParentSize().drawBehind {
            drawRect(accentColor, size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height))
        })
        AnimatedContent(
            targetState = quickRollResult,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "QuickRollContent"
        ) { result ->
            if (result != null) {
                // --- Vista de resultado inline ---
                Column(
                    modifier = Modifier.padding(contentPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = table.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissResult, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = result.rollValue.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = result.resolvedText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Surface(
                        onClick = onQuickRoll,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = MaterialTheme.shapes.small,
                            modifier = Modifier.heightIn(min = 48.dp).fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Casino, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondary)
                            Spacer(Modifier.width(8.dp))
                            Text("TIRAR DE NUEVO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // --- Vista normal ---
                Column(
                    modifier = Modifier.padding(contentPadding).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(categoryEmblem(table.category), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                            table.tags.take(2).forEach { tag ->
                                Surface(
                                    color = Color(tag.color).copy(alpha = 0.2f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onTogglePin, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (table.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Pin",
                                tint = if (table.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
                        Text(
                            text = table.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (table.description.isNotBlank()) {
                            Text(
                                text = table.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                minLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            onClick = onQuickRoll,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Casino, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("TIRAR", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.MoreVert, "Opciones de tabla", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Editar") },
                                    onClick = { showMenu = false; onClick() },
                                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                                )
                                if (onDuplicate != null) {
                                    DropdownMenuItem(
                                        text = { Text("Duplicar") },
                                        onClick = { showMenu = false; onDuplicate() },
                                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) }
                                    )
                                }
                                if (onInvertRanges != null) {
                                    DropdownMenuItem(
                                        text = { Text("Invertir rangos") },
                                        onClick = { showMenu = false; onInvertRanges() },
                                        leadingIcon = { Icon(Icons.Default.SwapVert, null) }
                                    )
                                }
                                if (onDelete != null) {
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                        onClick = { showMenu = false; onDelete() },
                                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
