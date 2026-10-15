package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.BoardBlockMeta
import com.deckapp.feature.draw.components.CardItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    cards: List<Card>,
    availableCount: Int,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    sessionId: Long,
    compactInFolder: Boolean,
    onDraw: () -> Unit,
    onShuffle: () -> Unit,
    onReset: () -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onCardClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        // Layout S: Ultra compacto y responsivo
        CompactDeckBlockContent(
            item = item,
            meta = meta,
            cards = cards,
            availableCount = availableCount,
            aspectRatio = aspectRatio,
            backImagePath = backImagePath,
            onDraw = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDraw()
            },
            onDiscard = onDiscard,
            onRevealCard = onRevealCard,
            onCardClick = onCardClick,
            modifier = modifier
        )
    } else {
        // Layout M y L
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera interactiva
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = meta.color.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${cards.size} en mano",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = meta.color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "· $availableCount en mazo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Acciones rápidas en la cabecera
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cards.isNotEmpty()) {
                        // Botón revelar todas si alguna está oculta
                        val unrevealedCount = cards.count { !it.isRevealed }
                        if (unrevealedCount > 0) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cards.filter { !it.isRevealed }.forEach { onRevealCard(it.id) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Revelar todas",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Botón de descarte masivo de mano
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                cards.forEach { onDiscard(it.id) }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Descartar mano",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Botón primario Robar
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDraw()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = meta.color),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Robar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Robar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Carrusel de cartas en mano (M y L)
            if (cards.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it != SwipeToDismissBoxValue.Settled) {
                                    onDiscard(card.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                )
                            }
                        ) {
                            CardItem(
                                card = card,
                                aspectRatio = aspectRatio,
                                showTitle = true,
                                backImagePath = backImagePath,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onCardClick(card.id)
                                },
                                onDiscard = { onDiscard(card.id) },
                                onReveal = { onRevealCard(card.id) },
                                onRollTable = {}
                            )
                        }
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sin cartas en la mesa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Botones de acción extendidos en tamaño L (Grande)
            if (item.size == SessionBoardItemSize.L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onShuffle()
                        },
                        border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = meta.color),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Barajar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Barajar", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        },
                        border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = meta.color),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reiniciar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reiniciar", style = MaterialTheme.typography.labelLarge)
                    }

                    if (cards.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                cards.forEach { onDiscard(it.id) }
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Descartar", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Descartar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDeckBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    cards: List<Card>,
    availableCount: Int,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    onDraw: () -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onCardClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (cards.isNotEmpty()) {
        val lastCard = cards.last()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniatura de la última carta robada (clickable para inspección)
            Surface(
                modifier = Modifier
                    .width(36.dp)
                    .aspectRatio(aspectRatio.ratio.coerceIn(0.6f, 1.4f))
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCardClick(lastCard.id)
                    },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, meta.color.copy(alpha = 0.4f)),
                shadowElevation = 2.dp
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (lastCard.isRevealed) {
                        AsyncImage(
                            model = lastCard.activeFace.imagePath?.let { File(it) },
                            contentDescription = lastCard.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = backImagePath?.let { File(it) },
                            contentDescription = "Dorso",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentScale = ContentScale.Crop
                        )
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(14.dp)
                        )
                    }
                }
            }

            // Info de la carta y mazo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCardClick(lastCard.id)
                    }
            ) {
                Text(
                    text = if (lastCard.isRevealed) lastCard.title else "Carta oculta",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${cards.size} en mano · $availableCount mazo",
                    style = MaterialTheme.typography.labelSmall,
                    color = meta.color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Acciones rápidas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!lastCard.isRevealed) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRevealCard(lastCard.id)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Revelar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                FilledIconButton(
                    onClick = onDraw,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Robar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        // Mano vacía: layout S original enriquecido
        Row(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "0 en mano",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$availableCount restantes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = meta.color
                )
            }

            FilledIconButton(
                onClick = onDraw,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Robar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
