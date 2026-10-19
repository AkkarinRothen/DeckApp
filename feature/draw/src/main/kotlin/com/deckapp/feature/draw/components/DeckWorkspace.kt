package com.deckapp.feature.draw.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckTextButton
import com.deckapp.feature.draw.SessionUiState

@Composable
fun DeckWorkspace(
    uiState: SessionUiState,
    onToggleCollapse: (Long) -> Unit,
    onDraw: (Long) -> Unit,
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "No hay mazos en esta sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Añade mazos de tu biblioteca o activa packs de inicio para comenzar a robar cartas en la mesa.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                DeckButton(onClick = onOpenDeckPicker) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Añadir Mazos a la Mesa")
                }
            }
        }
    } else {
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
                    isCollapsed = ref.stackId in uiState.collapsedDeckIds,
                    showTitle = uiState.session?.showCardTitles ?: true,
                    aspectRatio = uiState.deckAspectRatios[ref.stackId] ?: CardAspectRatio.STANDARD,
                    backImagePath = uiState.deckBackImages[ref.stackId],
                    sessionId = uiState.session?.id ?: 0L,
                    onToggleCollapse = { onToggleCollapse(ref.stackId) },
                    onDraw = { onDraw(ref.stackId) },
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
    isCollapsed: Boolean,
    showTitle: Boolean,
    aspectRatio: CardAspectRatio,
    backImagePath: String?,
    sessionId: Long,
    onToggleCollapse: () -> Unit,
    onDraw: () -> Unit,
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
                    .clickable { onToggleCollapse() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deckName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${cards.size} en mano · $availableCount disponibles",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onDraw,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Robar", style = MaterialTheme.typography.labelMedium)
                }

                Spacer(Modifier.width(6.dp))

                IconButton(
                    onClick = onToggleCollapse,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (isCollapsed) "Expandir mazo" else "Colapsar mazo"
                    )
                }
            }

            if (!isCollapsed && cards.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
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
                    }
                }
            }
        }
    }
}
