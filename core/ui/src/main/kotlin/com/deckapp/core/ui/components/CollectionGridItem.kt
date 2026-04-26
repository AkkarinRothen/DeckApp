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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 4.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo con el color de la colección y un gradiente suave
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(collection.color).copy(alpha = 0.6f),
                                Color(collection.color).copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            // Decoración: Círculo sutil de fondo
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.Center)
                    .offset(x = 40.dp, y = (-30).dp),
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(60.dp)
            ) {}

            // Icono central grande con sombra
            Icon(
                imageVector = collection.icon.toIcon(),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .align(Alignment.Center)
                    .offset(y = (-12).dp),
                tint = Color.White
            )

            // Botón de edición discreto
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            // Info en la base con efecto de "vidrio" (Glassmorphism)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = collection.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${collection.resourceCount} recursos",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Indicador de selección
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(12.dp),
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
    CollectionIcon.BOOK -> Icons.AutoMirrored.Filled.MenuBook
    CollectionIcon.MAP -> Icons.Default.Map
    CollectionIcon.SKULL -> Icons.Default.Dangerous
    CollectionIcon.BAG -> Icons.Default.ShoppingBag
    CollectionIcon.SWORDS -> Icons.Default.Hardware
    CollectionIcon.MOUNTAIN -> Icons.Default.Terrain
    CollectionIcon.PEOPLE -> Icons.Default.Groups
    CollectionIcon.FOLDER -> Icons.Default.Folder
}
