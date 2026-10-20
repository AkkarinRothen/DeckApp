package com.deckapp.feature.draw.model.state

import com.deckapp.core.model.*

/**
 * Estado UI desacoplado y cohesivo para la gestión de mazos, mano privada y pilas de descarte en la sesión.
 */
data class DecksUiState(
    val deckRefs: List<SessionDeckRef> = emptyList(),
    val deckNames: Map<Long, String> = emptyMap(),       // stackId → nombre del mazo
    val deckCardCounts: Map<Long, Int> = emptyMap(),     // stackId → cartas disponibles
    val selectedDeckId: Long? = null,                    // mazo activo para ROBAR
    val hand: List<Card> = emptyList(),
    val pile: List<Card> = emptyList(),
    val peekCard: Card? = null,                          // carta visible en modo Peek (no robada)
    val deckAspectRatios: Map<Long, CardAspectRatio> = emptyMap(), // stackId → aspectRatio
    val deckBackImages: Map<Long, String?> = emptyMap(),           // stackId → backImagePath
    val collapsedDeckIds: Set<Long> = emptySet(),        // clusters colapsados manualmente
    val handByDeck: Map<Long, List<Card>> = emptyMap(),  // hand agrupada por stackId
    val allDecks: List<CardStack> = emptyList(),
    val isShuffling: Boolean = false,
    val showDrawBySuitSheet: Boolean = false,
    val availableSuitsForDraw: List<String> = emptyList()
)
