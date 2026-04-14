package com.deckapp.core.model

/**
 * Proporciones de carta soportadas.
 * El [ratio] es ancho/alto — el ancho se calcula como `height * ratio`.
 */
enum class CardAspectRatio(val ratio: Float) {
    STANDARD(2.5f / 3.5f),   // 0.714 — naipes estándar, Poker, Magic
    TAROT(2.75f / 4.75f),    // 0.579 — Tarot, Oracle, cartas grandes
    MINI(1.75f / 2.5f),      // 0.700 — miniaturas, cartas de juego rápido
    SQUARE(1f),              // 1.000 — cuadrado
    LANDSCAPE(3.5f / 2.5f)   // 1.400 — apaisado (tokens, fichas)
}
