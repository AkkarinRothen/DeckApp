package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.core.ui.util.toCoilModel
import com.deckapp.feature.draw.components.BoardBlockMeta

@Composable
fun MicrosettingBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    compactInFolder: Boolean = false,
    onOpenBuilder: () -> Unit = {},
    onGenerate: () -> Unit = {},
    onRotateSlot: (MicrosettingSlotType) -> Unit = {},
    onRedrawSlot: (MicrosettingSlotType) -> Unit = {},
    onSendToNotes: (MicrosettingCluster) -> Unit = {},
    onOpenCard: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cluster = remember(item.noteContent) {
        MicrosettingCodec.decode(item.noteContent)
    }

    if (cluster == null || cluster.items.isEmpty()) {
        // Estado vacío: invitar al DM a generar su primer microsetting
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = meta.color,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Generador de Microsetting",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Combina 6 cartas del Deck of Worlds. Cada mazo resuelve sus opciones reales: una, dos o cuatro pistas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(
                    onClick = onGenerate,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = meta.color.copy(alpha = 0.15f),
                        contentColor = meta.color
                    )
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generar Microsetting (6 cartas)")
                }
            }
        }
        return
    }

    // Modo compacto (Folder o Tamaño S)
    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = cluster.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onGenerate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Casino, contentDescription = "Redibujar todo", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onOpenBuilder, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Abrir creador", modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = cluster.items.joinToString(" · ") { "${it.slotType.displayName}: ${it.title}" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }

    // Modo Normal / Completo (M y L)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Barra de acciones superiores
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cluster.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = meta.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onGenerate,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Redibujar", fontSize = 12.sp)
                }

                FilledTonalButton(
                    onClick = { onSendToNotes(cluster) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("A Notas", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onOpenBuilder,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "Modo brújula", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Grilla responsiva de cartas del cluster
        val columns = if (item.size == SessionBoardItemSize.L) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cluster.items.chunked(columns).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { cardItem ->
                        Box(modifier = Modifier.weight(1f)) {
                            MicrosettingCardBlockItem(
                                item = cardItem,
                                onRotate = { onRotateSlot(cardItem.slotType) },
                                onRedraw = { onRedrawSlot(cardItem.slotType) },
                                onCardClick = { onOpenCard(cardItem.cardId) }
                            )
                        }
                    }
                    // Rellenar espacios vacíos en la última fila para alinear
                    if (rowItems.size < columns) {
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicrosettingCardBlockItem(
    item: MicrosettingCardItem,
    onRotate: () -> Unit,
    onRedraw: () -> Unit,
    onCardClick: () -> Unit
) {
    val slotColor = Color(item.slotType.colorHex)
    val orientationLabel = item.optionLabels.getOrElse(
        item.optionRotations.indexOf(item.rotation).coerceAtLeast(0)
    ) { "Opción" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, slotColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Cabecera de la ranura: Badge de tipo + Botón de rotar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = slotColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = item.slotType.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = slotColor,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onRotate,
                        enabled = item.optionCount > 1,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Siguiente opción ($orientationLabel)",
                            tint = slotColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedraw,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Redibujar carta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Imagen cuadrada con rotación activa + Título
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    AsyncImage(
                        model = toCoilModel(item.imagePath),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(item.rotation.toFloat())
                    )
                    // Mini chip de orientación
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = orientationLabel.first().toString(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.activeCue.isNotBlank()) {
                        Text(
                            text = "“${item.activeCue}”",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = slotColor,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
