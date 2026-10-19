package com.deckapp.feature.draw.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardStack
import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckOutlinedButton
import com.deckapp.core.ui.components.DeckTextButton
import com.deckapp.core.ui.components.PaperCard
import com.deckapp.core.ui.util.toCoilModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDeckPickerSheet(
    allDecks: List<CardStack>,
    activeDeckIds: Set<Long>,
    deckCardCounts: Map<Long, Int>,
    cardsInHandByDeck: Map<Long, List<Card>>,
    onToggleDeck: (Long) -> Unit,
    onOpenStarterPacks: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var deckPendingRemoval by remember { mutableStateOf<CardStack?>(null) }

    val filteredDecks = remember(allDecks, searchQuery, activeDeckIds) {
        val query = searchQuery.trim().lowercase()
        val list = if (query.isEmpty()) {
            allDecks
        } else {
            allDecks.filter {
                it.name.lowercase().contains(query) ||
                it.description.lowercase().contains(query) ||
                it.tags.any { tag -> tag.name.lowercase().contains(query) }
            }
        }
        // Active decks first, then alphabetical
        list.sortedWith(
            compareByDescending<CardStack> { it.id in activeDeckIds }
                .thenBy { it.name }
        )
    }

    // Modal Bottom Sheet
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mazos de la Sesión",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activeDeckIds.size} de ${allDecks.size} mazos activos en mesa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DeckOutlinedButton(
                    onClick = onOpenStarterPacks,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Packs de Inicio", style = MaterialTheme.typography.labelMedium)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar mazo por nombre o sistema...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Limpiar búsqueda"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Content List
            if (filteredDecks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "No hay mazos registrados" else "Sin resultados para \"$searchQuery\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) {
                                "Puedes instalar paquetes de cartas oficiales como Deck of Worlds o Choir of Flesh usando el botón 'Packs de Inicio'."
                            } else {
                                "Prueba con otro término de búsqueda."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            DeckButton(onClick = onOpenStarterPacks) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Explorar Packs de Inicio")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredDecks, key = { it.id }) { deck ->
                        val isSelected = deck.id in activeDeckIds
                        val cardsInHand = cardsInHandByDeck[deck.id]?.size ?: 0
                        val cardCount = deckCardCounts[deck.id] ?: 0

                        PaperCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    if (isSelected && cardsInHand > 0) {
                                        deckPendingRemoval = deck
                                    } else {
                                        onToggleDeck(deck.id)
                                    }
                                },
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail / Cover
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val coverModel = deck.coverImagePath?.toCoilModel()
                                    if (coverModel != null) {
                                        AsyncImage(
                                            model = coverModel,
                                            contentDescription = deck.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Style,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                // Info Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = deck.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (cardsInHand > 0) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "🃏 $cardsInHand en mesa",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "$cardCount cartas",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        val primaryTag = deck.tags.firstOrNull()?.name
                                        if (!primaryTag.isNullOrBlank()) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = primaryTag,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Toggle Switch
                                Switch(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (!checked && cardsInHand > 0) {
                                            deckPendingRemoval = deck
                                        } else {
                                            onToggleDeck(deck.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before removing a deck with active cards
    deckPendingRemoval?.let { deck ->
        val count = cardsInHandByDeck[deck.id]?.size ?: 0
        AlertDialog(
            onDismissRequest = { deckPendingRemoval = null },
            title = { Text("¿Desvincular mazo de la sesión?") },
            text = {
                Text(
                    "El mazo \"${deck.name}\" tiene $count carta(s) actualmente en juego sobre la mesa o mano. " +
                    "Si lo quitas, las cartas activas se recogerán al descarte de la sesión."
                )
            },
            confirmButton = {
                DeckButton(
                    onClick = {
                        onToggleDeck(deck.id)
                        deckPendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Quitar y recoger")
                }
            },
            dismissButton = {
                DeckTextButton(onClick = { deckPendingRemoval = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
