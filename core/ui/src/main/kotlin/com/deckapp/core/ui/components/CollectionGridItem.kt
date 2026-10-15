package com.deckapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.DeckCollection
import com.deckapp.core.model.CollectionIcon
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CollectionGridItem(
    collection: DeckCollection,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEdit: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    DeckCard(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CollectionCover(collection)
            Surface(Modifier.align(Alignment.TopEnd).padding(4.dp), shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Edit, "Editar colección ${collection.name}", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            // Indicador de selección
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(12.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionIcon.toIcon(): ImageVector = when (this) {
    CollectionIcon.CHEST -> Icons.Default.Inventory2
    CollectionIcon.BOOK -> Icons.AutoMirrored.Filled.MenuBook
    CollectionIcon.MAP -> Icons.Default.Map
    CollectionIcon.SKULL -> Icons.Default.Dangerous
    CollectionIcon.BAG -> Icons.Default.ShoppingBag
    CollectionIcon.SWORDS -> Icons.Default.Shield
    CollectionIcon.MOUNTAIN -> Icons.Default.Terrain
    CollectionIcon.PEOPLE -> Icons.Default.Groups
    CollectionIcon.FOLDER -> Icons.Default.Folder
    CollectionIcon.ARCANE -> Icons.Default.AutoAwesome
    CollectionIcon.COMPASS -> Icons.Default.Explore
}
