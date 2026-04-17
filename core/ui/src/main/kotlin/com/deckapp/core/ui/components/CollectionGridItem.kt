package com.deckapp.core.ui.components

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
import com.deckapp.core.model.Collection
import com.deckapp.core.model.CollectionIcon

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CollectionGridItem(
    collection: Collection,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else CardDefaults.cardColors()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo con el color de la colección
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(collection.color).copy(alpha = 0.7f),
                                Color(collection.color)
                            )
                        )
                    )
            )

            // Icono central grande
            Icon(
                imageVector = collection.icon.toIcon(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .offset(y = (-10).dp),
                tint = Color.White.copy(alpha = 0.8f)
            )

            // Info en la base
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Text(
                    text = collection.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${collection.resourceCount} recursos",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            // Indicador de selección
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
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
    CollectionIcon.BOOK -> Icons.Default.MenuBook
    CollectionIcon.MAP -> Icons.Default.Map
    CollectionIcon.SKULL -> Icons.Default.Dangerous
    CollectionIcon.BAG -> Icons.Default.ShoppingBag
    CollectionIcon.SWORDS -> Icons.Default.Hardware
    CollectionIcon.MOUNTAIN -> Icons.Default.Terrain
    CollectionIcon.PEOPLE -> Icons.Default.Groups
    CollectionIcon.FOLDER -> Icons.Default.Folder
}
