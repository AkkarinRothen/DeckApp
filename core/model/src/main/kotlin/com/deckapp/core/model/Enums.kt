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
    PEEK,      // Vió el tope del mazo sin robar
    SHUFFLE_BACK // La pila de descarte vuelve al mazo
}

/** Fuentes de importación para mazos completos */
enum class DeckImportSource { FOLDER, PDF, ZIP }

/** Fuentes de importación para tablas individuales */
enum class TableImportSource {
    OCR_IMAGE,      // Imagen/PDF con análisis óptico
    CSV_TEXT,       // Texto en formato CSV / TSV / DSV
    JSON_TEXT,      // JSON (Foundry VTT, DeckApp Export, o array simple)
    PLAIN_TEXT,     // Texto plano (portapapeles, listas)
    MARKDOWN_TABLE, // Tablas estándar Markdown (|---|)
    AI_GENERATE     // Generación desde cero con IA
}

/** Modos de distribución de cartas dentro de un PDF */
enum class PdfLayoutMode {
    /** Página 1=frente, página 2=dorso, página 3=frente... */
    ALTERNATING_PAGES,
    /** Frente | Dorso en la misma página (corte vertical) */
    SIDE_BY_SIDE,
    /** N×M cartas por página */
    GRID,
    /** Primera mitad = frentes, segunda mitad = dorsos */
    FIRST_HALF_FRONTS
}
