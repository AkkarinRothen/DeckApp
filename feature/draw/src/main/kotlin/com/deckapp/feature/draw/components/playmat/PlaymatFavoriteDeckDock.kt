package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.PlaymatDeckPlacement
import com.deckapp.core.model.SessionDeckRef

@Composable
internal fun PlaymatFavoriteDeckDock(
    deckRefs: List<SessionDeckRef>,
    favoriteDeckIds: Set<Long>,
    deckNames: Map<Long, String>,
    deckCardCounts: Map<Long, Int>,
    placements: List<PlaymatDeckPlacement>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onShowDeck: (Long, Float, Float) -> Unit,
    onDrawCard: (Long, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites = deckRefs
        .distinctBy { it.stackId }
        .filter { it.stackId in favoriteDeckIds }
    if (favorites.isEmpty()) return

    Surface(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(
                if (expanded) "Favoritos" else "Mazos · ${favorites.size}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    favorites.forEachIndexed { index, deck ->
                        val placement = placements.firstOrNull { it.stackId == deck.stackId }
                        val missingPlacementsBefore = favorites.take(index).count { favorite ->
                            placements.none { it.stackId == favorite.stackId }
                        }
                        val x = placement?.x ?: -160f
                        val y = placement?.y ?: (80f + (placements.size + missingPlacementsBefore) * 240f)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = { onShowDeck(deck.stackId, x, y) },
                                    color = androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, top = 6.dp, bottom = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            deckNames[deck.stackId] ?: "Mazo",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            " · ${deckCardCounts[deck.stackId] ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDrawCard(deck.stackId, x, y) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.AddCard, contentDescription = "Robar de ${deckNames[deck.stackId] ?: "mazo"}")
                                }
                            }
                        }
                    }
                }
            }
            IconButton(onClick = onToggleExpanded, modifier = Modifier.size(38.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Plegar mazos favoritos" else "Mostrar mazos favoritos"
                )
            }
        }
    }
}
