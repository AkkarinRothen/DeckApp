package com.deckapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.CardStack

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun DeckCoverCard(
    deck: CardStack,
    onClick: () -> Unit,
    onAddToSession: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onMergeWith: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    isSelected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    cardCount: Int = 0,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val showOverflow = onDelete != null || onDuplicate != null || onMergeWith != null || onArchive != null

    // V-10: Stack visual — back surfaces peek behind the main card
    Box(modifier = modifier.fillMaxWidth()) {
        // Carta más al fondo
        Surface(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = -4f
                    translationX = -6.dp.toPx()
                    translationY = 4.dp.toPx()
                }
                .alpha(0.45f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {}
        // Carta intermedia
        Surface(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = -2f
                    translationX = -3.dp.toPx()
                    translationY = 2.dp.toPx()
                }
                .alpha(0.65f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {}

        // Carta principal (frente)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = if (isSelected) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else CardDefaults.cardColors(),
            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column {
                // Cover image + badge de conteo + overlay de nombre
                BadgedBox(
                    badge = {
                        if (cardCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = cardCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (deck.coverImagePath != null) {
                            AsyncImage(
                                model = File(deck.coverImagePath ?: ""),
                                contentDescription = deck.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (deck.isArchived)
                                    androidx.compose.ui.graphics.ColorFilter.colorMatrix(
                                        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) }
                                    ) else null
                            )
                            if (deck.isArchived) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Archive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp).alpha(0.8f)
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = deck.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Overlay de gradiente con el nombre completo del mazo
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.80f))
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = deck.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Info: tipo + botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deck.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onAddToSession,
                        modifier = Modifier.size(32.dp),
                        enabled = !deck.isArchived
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Añadir a sesión",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (showOverflow) {
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Más opciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                onDuplicate?.let { action ->
                                    DropdownMenuItem(
                                        text = { Text("Duplicar mazo") },
                                        leadingIcon = {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            action()
                                        }
                                    )
                                }
                                onMergeWith?.let { action ->
                                    DropdownMenuItem(
                                        text = { Text("Fusionar en…") },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            action()
                                        }
                                    )
                                }
                                onArchive?.let { action ->
                                    DropdownMenuItem(
                                        text = { Text(if (deck.isArchived) "Restaurar mazo" else "Archivar mazo") },
                                        leadingIcon = {
                                            Icon(
                                                if (deck.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            action()
                                        }
                                    )
                                }
                                onDelete?.let { action ->
                                    DropdownMenuItem(
                                        text = { Text("Eliminar mazo") },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            action()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay de selección masiva
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
}
