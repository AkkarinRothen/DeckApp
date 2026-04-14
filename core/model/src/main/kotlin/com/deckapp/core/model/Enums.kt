package com.deckapp.core.model

/** Tipo de pila de cartas — basado en el modelo de Foundry VTT */
enum class StackType {
    /** Fuente principal de cartas. Se baraja, reparte y resetea. */
    DECK,
    /** Cartas en mano durante la sesión activa. */
    HAND,
    /** Descarte / cartas jugadas. */
    PILE
}

/** Modo de robo de cartas */
enum class DrawMode {
    /** Toma la carta del tope del mazo (orden fijo) */
    TOP,
    /** Toma la carta del fondo del mazo */
    BOTTOM,
    /** Selecciona una carta al azar */
    RANDOM
}

/**
 * Define cómo se estructura el contenido dentro de una [CardFace].
 *
 * Basado en el análisis de productos del mercado TTRPG:
 * - [IMAGE_ONLY]: Deck of Illusions, arte puro
 * - [IMAGE_WITH_TEXT]: Nord Games, Paul Weber
 * - [REVERSIBLE]: Tarot, Oracle decks (upright / reversed)
 * - [TOP_BOTTOM_SPLIT]: Gloomhaven ability cards (2 acciones por carta)
 * - [LEFT_RIGHT_SPLIT]: Variantes de flip books
 * - [FOUR_EDGE_CUES]: Story Engine Deck (4 pistas, una por orientación)
 * - [FOUR_QUADRANT]: Compases narrativos (2×2 zonas)
 * - [DOUBLE_SIDED_FULL]: Arkham Horror LCG (ambas caras completas e independientes)
 */
enum class CardContentMode {
    IMAGE_ONLY,
    IMAGE_WITH_TEXT,
    REVERSIBLE,
    TOP_BOTTOM_SPLIT,
    LEFT_RIGHT_SPLIT,
    FOUR_EDGE_CUES,
    FOUR_QUADRANT,
    DOUBLE_SIDED_FULL
}

/** Acciones registradas en el event log de sesión */
enum class DrawAction {
    DRAW,      // Carta robada del mazo a la mano
    DISCARD,   // Carta movida de la mano a la pila
    PASS,      // Carta pasada a un jugador nombrado
    FLIP,      // Cambio de cara activa
    ROTATE,    // Cambio de orientación (para FOUR_EDGE_CUES)
    REVERSE,   // Toggle upright/reversed (para REVERSIBLE)
    RESET,     // Todas las cartas vuelven al mazo de origen
    PEEK       // Vió el tope del mazo sin robar
}
