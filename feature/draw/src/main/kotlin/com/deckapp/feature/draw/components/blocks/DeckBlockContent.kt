package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
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
    onRotateCard: (Long) -> Unit = {},
    onDrawBySuit: () -> Unit = {},
    spreadResult: CardSpreadResult? = null,
    onDrawSpread: ((CardSpread) -> Unit)? = null,
    onClearSpread: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var showDiscardHandConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var shuffleAnimationRun by remember { mutableIntStateOf(0) }
    var isShuffling by remember { mutableStateOf(false) }
    var confirmNewTarokka by remember { mutableStateOf(false) }

    LaunchedEffect(shuffleAnimationRun) {
        if (shuffleAnimationRun > 0) {
            isShuffling = true
            delay(1_300)
            isShuffling = false
        }
    }

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
                        shuffleAnimationRun++
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

    if (confirmNewTarokka) {
        AlertDialog(
            onDismissRequest = { confirmNewTarokka = false },
            title = { Text("¿Empezar otra tirada?") },
            text = { Text("Las cartas actuales seguirán en la mano. Se limpiarán las cinco posiciones para la nueva tirada.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewTarokka = false
                    onClearSpread?.invoke()
                }) { Text("Empezar otra") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewTarokka = false }) { Text("Cancelar") }
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
            onDrawBySuit = onDrawBySuit,
            spreadResult = spreadResult,
            onDrawSpread = onDrawSpread,
            onClearSpread = { confirmNewTarokka = true },
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
            // Estado y acciones quedan en filas separadas para evitar que compitan por ancho.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DeckCountPill(
                    count = cards.size,
                    label = "En mano",
                    color = meta.color,
                    modifier = Modifier.weight(1f)
                )
                DeckCountPill(
                    count = availableCount,
                    label = "En mazo",
                    modifier = Modifier.weight(1f)
                )
                DeckCountPill(
                    count = discardCount,
                    label = "Descarte",
                    modifier = Modifier.weight(1f)
                )
            }

            TextButton(
                onClick = onDrawBySuit,
                enabled = availableCount > 0,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Robar por palo")
            }

            if (meta.title.contains("tarokka", ignoreCase = true)) {
                TarokkaSpreadPanel(
                    result = spreadResult,
                    cardsInHand = cards,
                    aspectRatio = aspectRatio,
                    backImagePath = backImagePath,
                    onDrawNext = { onDrawSpread?.invoke(CardSpread.TAROKKA_CROSS) },
                    onClear = { confirmNewTarokka = true },
                    onReveal = onRevealCard
                )
            }

            AnimatedVisibility(
                visible = isShuffling,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ShuffleCardsMotion(color = meta.color, modifier = Modifier.fillMaxWidth())
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (availableCount > 0) "Robar carta" else "Agotado", style = MaterialTheme.typography.labelLarge)
                }

                if (cards.isNotEmpty()) {
                    val unrevealedCount = cards.count { !it.isRevealed }
                    if (unrevealedCount > 0) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                cards.filter { !it.isRevealed }.forEach { onRevealCard(it.id) }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Revelar todas las cartas",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDiscardHandConfirm = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Descartar toda la mano",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                onRollTable = {},
                                onRotate = { onRotateCard(card.id) }
                            )

                            TextButton(
                                onClick = { onDiscard(card.id) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text("Descartar", style = MaterialTheme.typography.labelSmall)
                            }
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
                            shuffleAnimationRun++
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
                                shuffleAnimationRun++
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
private fun ShuffleCardsMotion(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shuffle-cards")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shuffle-progress"
    )

    Row(
        modifier = modifier.height(42.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(62.dp).height(38.dp)) {
            val positions = listOf(
                Triple((-16f + progress * 12f), (5f - progress * 9f), (-16f + progress * 30f)),
                Triple(0f, (-3f + progress * 6f), 0f),
                Triple((16f - progress * 12f), (-5f + progress * 9f), (16f - progress * 30f))
            )
            positions.forEachIndexed { index, (x, y, rotation) ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = x.dp, y = y.dp)
                        .rotate(rotation)
                        .size(width = 23.dp, height = 32.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = if (index == 1) color.copy(alpha = 0.9f) else color.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)),
                    shadowElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
        Text(
            text = "Barajando…",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeckCountPill(
    count: Int,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    onDrawBySuit: () -> Unit,
    spreadResult: CardSpreadResult?,
    onDrawSpread: ((CardSpread) -> Unit)?,
    onClearSpread: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    if (cards.isNotEmpty()) {
        val lastCard = cards.last()
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                    .graphicsLayer {
                                        rotationZ = lastCard.currentRotation.toFloat()
                                    },
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            AsyncImage(
                                model = com.deckapp.core.ui.util.toCoilModel(backImagePath),
                                contentDescription = "Dorso",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentScale = ContentScale.Fit
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
                    Text(
                        text = "${cards.size} en mano · $availableCount en mazo · $discardCount desc.",
                        style = MaterialTheme.typography.labelSmall,
                        color = meta.color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!lastCard.isRevealed) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRevealCard(lastCard.id)
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Revelar", style = MaterialTheme.typography.labelMedium)
                    }
                }

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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (availableCount > 0) "Robar" else "Agotado", style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = onDrawBySuit, enabled = availableCount > 0) { Text("Robar por palo") }
            if (meta.title.contains("tarokka", ignoreCase = true)) {
                TarokkaSpreadPanel(spreadResult, cards, aspectRatio, backImagePath,
                    { onDrawSpread?.invoke(CardSpread.TAROKKA_CROSS) }, onClearSpread, onRevealCard)
            }
        }
    } else {
        // La acción queda rotulada también cuando la mano está vacía.
        Column(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column {
                Text("Mano vacía", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = if (availableCount > 0) "$availableCount cartas disponibles" else "Mazo agotado",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (availableCount > 0) meta.color else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }

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
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (availableCount > 0) "Robar" else "Agotado", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onDrawBySuit, enabled = availableCount > 0) { Text("Robar por palo") }
            if (meta.title.contains("tarokka", ignoreCase = true)) {
                TarokkaSpreadPanel(spreadResult, cards, aspectRatio, backImagePath,
                    { onDrawSpread?.invoke(CardSpread.TAROKKA_CROSS) }, onClearSpread, onRevealCard)
            }
        }
    }
}

@Composable
private fun TarokkaSpreadPanel(
    result: CardSpreadResult?,
    cardsInHand: List<Card>,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    onDrawNext: () -> Unit,
    onClear: () -> Unit,
    onReveal: (Long) -> Unit
) {
    val ids = result?.takeIf { it.spread == CardSpread.TAROKKA_CROSS }?.cards.orEmpty().map { it.id }
    val byId = cardsInHand.associateBy { it.id }
    val assigned = ids.mapNotNull(byId::get)
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Tirada Tarokka", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${assigned.size}/5 posiciones", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (assigned.size < TAROKKA_POSITIONS.size) {
                FilledTonalButton(onClick = onDrawNext) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (assigned.isEmpty()) "Iniciar" else "Siguiente")
                }
            } else TextButton(onClick = onClear) { Text("Nueva tirada") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TAROKKA_POSITIONS.forEachIndexed { index, label ->
                val card = assigned.getOrNull(index)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (card == null) {
                        Surface(
                            modifier = Modifier.width(48.dp).aspectRatio(aspectRatio.ratio),
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}") } }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio.ratio)
                                .clip(RoundedCornerShape(7.dp)).clickable {
                                    if (!card.isRevealed) onReveal(card.id)
                                },
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            val imagePath = if (card.isRevealed) card.activeFace.imagePath else backImagePath
                            if (imagePath != null) {
                                AsyncImage(
                                    model = com.deckapp.core.ui.util.toCoilModel(imagePath),
                                    contentDescription = if (card.isRevealed) card.title else "Dorso de carta",
                                    modifier = Modifier.fillMaxSize().graphicsLayer {
                                        rotationZ = if (card.isRevealed) card.currentRotation.toFloat() else 0f
                                    },
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(if (card.isRevealed) card.title.take(1) else "?")
                                }
                            }
                        }
                    }
                }
            }
        }
        if (assigned.any { !it.isRevealed }) {
            Text("Toca una carta boca abajo para revelarla.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
