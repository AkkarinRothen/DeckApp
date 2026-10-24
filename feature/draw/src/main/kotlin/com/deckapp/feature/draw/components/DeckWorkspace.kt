package com.deckapp.feature.draw.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.ui.components.DeckTextButton
import com.deckapp.core.ui.components.ContextualEmptyState
import com.deckapp.feature.draw.SessionUiState

@Composable
fun DeckWorkspace(
    uiState: SessionUiState,
    onToggleCollapse: (Long) -> Unit,
    onDraw: (Long) -> Unit,
    onRecoverDiscards: (Long) -> Unit,
    onCardClick: (Long, Long) -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onRollTable: (Long) -> Unit,
    onOpenDeckPicker: () -> Unit = {},
    onRotateCard: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (uiState.deckRefs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ContextualEmptyState(
                title = "No hay mazos en esta sesión",
                description = "Añade mazos de tu biblioteca para comenzar a robar cartas en la mesa.",
                actionLabel = "Añadir mazos",
                onAction = onOpenDeckPicker,
                icon = Icons.Default.Style,
                modifier = Modifier.padding(24.dp)
            )
        }
    } else {
        val discardCountByDeck = uiState.pile.groupingBy { it.stackId }.eachCount()
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mazos en juego (${uiState.deckRefs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DeckTextButton(onClick = onOpenDeckPicker) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Gestionar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            items(uiState.deckRefs, key = { it.stackId }) { ref ->
                val cardsInCluster = uiState.handByDeck[ref.stackId] ?: emptyList()
                DeckClusterItem(
                    deckName = uiState.deckNames[ref.stackId] ?: "Mazo",
                    cards = cardsInCluster,
                    availableCount = uiState.deckCardCounts[ref.stackId] ?: 0,
                    discardCount = discardCountByDeck[ref.stackId] ?: 0,
                    isShuffling = uiState.isShuffling,
                    isCollapsed = ref.stackId in uiState.collapsedDeckIds,
                    showTitle = uiState.session?.showCardTitles ?: true,
                    aspectRatio = uiState.deckAspectRatios[ref.stackId] ?: CardAspectRatio.STANDARD,
                    backImagePath = uiState.deckBackImages[ref.stackId],
                    sessionId = uiState.session?.id ?: 0L,
                    onToggleCollapse = { onToggleCollapse(ref.stackId) },
                    onDraw = { onDraw(ref.stackId) },
                    onRecoverDiscards = { onRecoverDiscards(ref.stackId) },
                    onCardClick = onCardClick,
                    onDiscard = onDiscard,
                    onRevealCard = onRevealCard,
                    onRollTable = onRollTable,
                    onRotateCard = onRotateCard
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DeckClusterItem(
    deckName: String,
    cards: List<Card>,
    availableCount: Int,
    discardCount: Int,
    isShuffling: Boolean,
    isCollapsed: Boolean,
    showTitle: Boolean,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    sessionId: Long,
    onToggleCollapse: () -> Unit,
    onDraw: () -> Unit,
    onRecoverDiscards: () -> Unit,
    onCardClick: (Long, Long) -> Unit,
    onDiscard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    onRollTable: (Long) -> Unit,
    onRotateCard: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleCollapse() }
                ) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = if (isCollapsed) 2 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${cards.size} en mano · $availableCount en mazo · $discardCount en descarte",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expandir $deckName" else "Colapsar $deckName"
                    )
                }
            }

            FilledTonalButton(
                onClick = onDraw,
                enabled = availableCount > 0 && !isShuffling,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(40.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (availableCount > 0) "Robar carta" else "Mazo agotado", style = MaterialTheme.typography.labelMedium)
            }

            if (discardCount > 0 && (availableCount == 0 || !isCollapsed)) {
                OutlinedButton(
                    onClick = onRecoverDiscards,
                    enabled = !isShuffling,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(40.dp)
                ) {
                    Text(
                        "Recuperar descartes ($discardCount)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (!isCollapsed) {
                if (cards.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Style,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = when {
                                availableCount > 0 -> "Mano vacía. Robá una carta para empezar."
                                discardCount > 0 -> "Mazo agotado. Recuperá los descartes para seguir."
                                else -> "No quedan cartas en este mazo."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(cards, key = { it.id }) { card ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CardItem(
                                    card = card,
                                    aspectRatio = aspectRatio,
                                    showTitle = showTitle,
                                    backImagePath = backImagePath,
                                    onClick = { onCardClick(card.id, sessionId) },
                                    onDiscard = { onDiscard(card.id) },
                                    onReveal = { onRevealCard(card.id) },
                                    onRollTable = onRollTable,
                                    onRotate = { onRotateCard(card.id) }
                                )
                                TextButton(
                                    onClick = { onDiscard(card.id) },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Descartar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
