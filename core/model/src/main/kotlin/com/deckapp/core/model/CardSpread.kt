package com.deckapp.core.model

enum class CardSpread(val title: String, val cardCount: Int) {
    SINGLE_CARD("Una Carta", 1),
    PAST_PRESENT_FUTURE("Pasado, Presente, Futuro", 3),
    CELTIC_CROSS("Cruz Celta", 10)
}

data class CardSpreadResult(
    val spread: CardSpread,
    val cards: List<Card> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
