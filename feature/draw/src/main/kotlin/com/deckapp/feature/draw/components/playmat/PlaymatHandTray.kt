package com.deckapp.feature.draw.components.playmat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import com.deckapp.feature.draw.components.CardItem

/**
 * Bandeja inferior acoplable de mano de cartas para el tapete 2D.
 */
@Composable
internal fun PlaymatHandTray(
    visible: Boolean,
    hand: List<Card>,
    placedCardIds: Set<Long>,
    deckNames: Map<Long, String>,
    deckAspectRatios: Map<Long, CardAspectRatio>,
    deckBackImages: Map<Long, String?>,
    sessionId: Long,
    onToggleVisible: () -> Unit,
    onPlaceOnPlaymat: (Long) -> Unit,
    onCardClick: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onRotateCard: (Long) -> Unit,
    draggedCardId: Long?,
    onHandCardDragChanged: (PlaymatHandCardDrag?) -> Unit,
    onDropHandCard: (PlaymatHandCardDrag) -> Unit,
    onTrayBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    // Solo mostrar en la bandeja las cartas que aún no han sido bajadas a la mesa
    val unplacedCards = hand.filterNot { it.id in placedCardIds }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onTrayBoundsChanged(Rect(coordinates.positionInWindow(), coordinates.size.toSize()))
            }
    ) {
        // Pestaña tirador superior
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onToggleVisible() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Mano (${unplacedCards.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (visible) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                if (unplacedCards.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hand.isEmpty()) "No hay cartas en mano. Roba un mazo para comenzar." else "Todas las cartas de tu mano están desplegadas en el tapete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(unplacedCards, key = { it.id }) { card ->
                            var cardWindowOrigin by remember(card.id) { mutableStateOf(Offset.Zero) }
                            var activeDrag by remember(card.id) { mutableStateOf<PlaymatHandCardDrag?>(null) }
                            val stackId = card.originDeckId ?: card.stackId
                            val aspectRatio = deckAspectRatios[stackId] ?: CardAspectRatio.STANDARD
                            val backImage = deckBackImages[stackId]
                            val dragModifier = Modifier
                                .onGloballyPositioned { cardWindowOrigin = it.positionInWindow() }
                                .playmatHandCardDrag(
                                    cardId = card.id,
                                    onStart = { cardId, localPointer, cardSize ->
                                        val drag = PlaymatHandCardDrag(
                                            cardId = cardId,
                                            pointerInWindow = cardWindowOrigin + localPointer,
                                            grabFraction = Offset(
                                                (localPointer.x / cardSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                (localPointer.y / cardSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                            )
                                        )
                                        activeDrag = drag
                                        onHandCardDragChanged(drag)
                                    },
                                    onMove = { cardId, localPointer ->
                                        activeDrag?.takeIf { it.cardId == cardId }?.let { drag ->
                                            val updatedDrag = drag.copy(pointerInWindow = cardWindowOrigin + localPointer)
                                            activeDrag = updatedDrag
                                            onHandCardDragChanged(updatedDrag)
                                        }
                                    },
                                    onEnd = { cardId, localPointer ->
                                        activeDrag?.takeIf { it.cardId == cardId }?.let { drag ->
                                            val finalDrag = drag.copy(pointerInWindow = cardWindowOrigin + localPointer)
                                            onDropHandCard(finalDrag)
                                            activeDrag = null
                                            onHandCardDragChanged(null)
                                        }
                                    },
                                    onCancel = { cardId ->
                                        if (activeDrag?.cardId == cardId) {
                                            activeDrag = null
                                            onHandCardDragChanged(null)
                                        }
                                    }
                                )
                                .graphicsLayer { alpha = if (draggedCardId == card.id) 0.42f else 1f }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CardItem(
                                    card = card,
                                    aspectRatio = aspectRatio,
                                    showTitle = true,
                                    backImagePath = backImage,
                                    onClick = { onCardClick(card.id) },
                                    onDiscard = { onDiscardCard(card.id) },
                                    onReveal = {},
                                    onRollTable = {},
                                    onRotate = { onRotateCard(card.id) },
                                    modifier = dragModifier
                                )

                                Button(
                                    onClick = { onPlaceOnPlaymat(card.id) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerticalAlignTop,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("A la Mesa", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
