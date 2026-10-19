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
import androidx.compose.runtime.*
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
    discardCount: Int = 0,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    sessionId: Long,
    compactInFolder: Boolean,
    onDraw: () -> Unit,
    onShuffle: () -> Unit,
    onShuffleDiscards: () -> Unit = onShuffle,
    onReset: () -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onCardClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var showDiscardHandConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    if (showDiscardHandConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardHandConfirm = false },
            title = { Text("¿Descartar mano?") },
            text = { Text("Se enviarán ${cards.size} carta(s) de tu mano a la pila de descarte.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardHandConfirm = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        cards.forEach { onDiscard(it.id) }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Descartar ${cards.size}")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardHandConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("¿Reiniciar mazo?") },
            text = {
                val handText = if (cards.isNotEmpty()) "${cards.size} en mano" else null
                val discardText = if (discardCount > 0) "$discardCount descartadas" else null
                val details = listOfNotNull(handText, discardText).joinToString(" y ")
                Text(
                    if (details.isNotBlank()) "Se devolverán al mazo todas las cartas ($details) y se barajará por completo."
                    else "¿Deseas barajar y reiniciar el mazo por completo?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReset()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reiniciar mazo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (item.size == SessionBoardItemSize.S || compactInFolder) {
        // Layout S: Ultra compacto y responsivo
        CompactDeckBlockContent(
            item = item,
            meta = meta,
            cards = cards,
            availableCount = availableCount,
            discardCount = discardCount,
            aspectRatio = aspectRatio,
            backImagePath = backImagePath,
            onDraw = {
                if (availableCount > 0) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDraw()
                }
            },
            onDiscard = onDiscard,
            onRevealCard = onRevealCard,
            onCardClick = onCardClick,
            modifier = modifier
        )
    } else {
        // Layout M y L: 3 Zonas Claramente Delimitadas
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Zona 1 & 2: Cabecera con Estado y Acciones de Robo
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Zona 1: Chips de Estado
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

                    if (discardCount > 0) {
                        Text(
                            text = "· $discardCount desc.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Zona 2: Acciones directas (Revelar todas / Descartar mano / Robar)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (cards.isNotEmpty()) {
                        val unrevealedCount = cards.count { !it.isRevealed }
                        if (unrevealedCount > 0) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cards.filter { !it.isRevealed }.forEach { onRevealCard(it.id) }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Revelar todas",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                showDiscardHandConfirm = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Descartar mano",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Botón Primario de Robo
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDraw()
                        },
                        enabled = availableCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = meta.color,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = if (availableCount > 0) "Robar" else "Agotado",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (availableCount > 0) "Robar" else "Agotado",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Zona 3: Carrusel de Cartas en Mano
            if (cards.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
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
                            text = if (availableCount > 0) "Mano vacía · Pulsa Robar para sacar una carta" else "Mazo agotado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Barra de Controles Extendidos en tamaño L (Grande)
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
                        Text("Barajar ($availableCount)", style = MaterialTheme.typography.labelMedium)
                    }

                    if (discardCount > 0) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onShuffleDiscards()
                            },
                            border = BorderStroke(1.dp, meta.color.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = meta.color),
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recuperar", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Recuperar ($discardCount)", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showResetConfirm = true
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Reiniciar", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reiniciar", style = MaterialTheme.typography.labelMedium)
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
    discardCount: Int = 0,
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
                            model = com.deckapp.core.ui.util.toCoilModel(lastCard.activeFace.imagePath),
                            contentDescription = lastCard.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = com.deckapp.core.ui.util.toCoilModel(backImagePath),
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

                    if (cards.size > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(meta.color, RoundedCornerShape(topStart = 4.dp))
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${cards.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
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
                val descText = if (discardCount > 0) " · $discardCount desc." else ""
                Text(
                    text = "${cards.size} en mano · $availableCount mazo$descText",
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
                    enabled = availableCount > 0,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = meta.color,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Robar",
                        tint = if (availableCount > 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
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
                    text = if (availableCount > 0) "$availableCount restantes" else "Agotado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (availableCount > 0) meta.color else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }

            FilledIconButton(
                onClick = onDraw,
                enabled = availableCount > 0,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = meta.color,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Robar",
                    tint = if (availableCount > 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
