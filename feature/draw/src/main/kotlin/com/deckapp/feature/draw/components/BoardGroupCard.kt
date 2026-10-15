package com.deckapp.feature.draw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.SessionBoardGroup

@Composable
fun BoardGroupCard(
    group: SessionBoardGroup,
    itemCount: Int,
    previewLines: List<String>,
    isEditing: Boolean,
    isDragTarget: Boolean,
    hasRecentActivity: Boolean,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = remember(group.accentColorHex, group.name, group.id) {
        group.boardAccentColor()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isEditing) 110.dp else 126.dp)
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isDragTarget) 2.dp else 1.dp,
            color = if (isDragTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        tonalElevation = if (isDragTarget) 8.dp else 2.dp,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Artwork
            BoardGroupArtworkBackground(
                group = group,
                accentColor = accentColor,
                compact = true
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Icon + Name + Badge + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = accentColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = group.boardIcon(isOpen = !group.isCollapsed),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.padding(6.dp).size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (hasRecentActivity) {
                                    Surface(
                                        modifier = Modifier.size(7.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = accentColor
                                    ) {}
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (itemCount == 1) "1 elemento" else "$itemCount elementos",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Header Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isEditing) {
                            IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Renombrar", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            IconButton(onClick = onQuickAdd, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar elemento", modifier = Modifier.size(20.dp), tint = accentColor)
                            }
                            IconButton(onClick = onToggleCollapsed, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Abrir carpeta",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Preview Content (Chips horizontales compactos)
                if (!isEditing) {
                    if (group.isCollapsed) {
                        Text(
                            text = "Contenido oculto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                    } else if (previewLines.isEmpty()) {
                        Text(
                            text = "Carpeta vacía",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(previewLines) { line ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = accentColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
                                ) {
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

fun SessionBoardGroup.boardAccentColor(): Color {
    val hex = accentColorHex
    if (!hex.isNullOrBlank()) {
        try {
            return Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {}
    }
    return Color(0xFF6750A4)
}

fun SessionBoardGroup.boardIcon(isOpen: Boolean): ImageVector {
    return if (isOpen) Icons.Default.FolderOpen else Icons.Default.Folder
}

@Composable
fun BoardGroupArtworkBackground(
    group: SessionBoardGroup,
    accentColor: Color,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                )
            )
    )
}

