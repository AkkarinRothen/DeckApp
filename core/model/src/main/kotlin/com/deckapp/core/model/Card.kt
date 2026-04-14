package com.deckapp.core.model

import kotlinx.serialization.Serializable

/**
 * Zona de contenido dentro de una [CardFace].
 * Su posición e interpretación dependen del [CardContentMode] de la cara:
 * - REVERSIBLE: zones[0]=upright, zones[1]=reversed
 * - TOP_BOTTOM_SPLIT: zones[0]=top, zones[1]=bottom
 * - LEFT_RIGHT_SPLIT: zones[0]=left, zones[1]=right
 * - FOUR_EDGE_CUES: zones[0..3] = N/E/S/O
 * - FOUR_QUADRANT: zones[0..3] = NW/NE/SW/SE
 */
@Serializable
data class ContentZone(
    val text: String = "",
    val imagePath: String? = null
)

/**
 * Una cara de una carta. Una carta puede tener múltiples caras
 * (ej: frente, dorso, o caras adicionales para barajas especiales).
 *
 * Las [zones] se interpretan según el [contentMode].
 */
@Serializable
data class CardFace(
    val name: String,                           // "Frente", "Dorso", "Upright", etc.
    val imagePath: String? = null,              // imagen de fondo de la cara completa
    val contentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    val zones: List<ContentZone> = emptyList(),
    val reversedImagePath: String? = null       // imagen alternativa al invertir (raro)
)

/** Modelo de dominio de una carta */
data class Card(
    val id: Long = 0,
    val stackId: Long,
    val originDeckId: Long?,        // mazo de origen — para Reset
    val title: String,
    val suit: String? = null,       // palo (ej: "Espadas", "Fuego", "Bastos")
    val value: Int? = null,         // valor numérico — para ordenamiento e iniciativa
    val faces: List<CardFace>,
    val currentFaceIndex: Int = 0,
    val currentRotation: Int = 0,   // 0/90/180/270 — activo en FOUR_EDGE_CUES y REVERSIBLE
    val isReversed: Boolean = false,
    val isDrawn: Boolean = false,
    val sortOrder: Int = 0,
    val tags: List<Tag> = emptyList()
) {
    val activeFace: CardFace get() = faces.getOrElse(currentFaceIndex) { faces.first() }

    /** Zona activa según el modo de contenido y la rotación actual */
    val activeZone: ContentZone?
        get() = when (activeFace.contentMode) {
            CardContentMode.REVERSIBLE ->
                if (isReversed) activeFace.zones.getOrNull(1) else activeFace.zones.getOrNull(0)
            CardContentMode.FOUR_EDGE_CUES ->
                activeFace.zones.getOrNull(currentRotation / 90)
            else -> activeFace.zones.firstOrNull()
        }
}

/** Modelo de dominio de una pila de cartas (mazo, mano o pila de descarte) */
data class CardStack(
    val id: Long = 0,
    val name: String,
    val type: StackType,
    val description: String = "",
    val coverImagePath: String? = null,
    val sourceFolderPath: String? = null,       // ruta de la carpeta de origen (import)
    val defaultContentMode: CardContentMode = CardContentMode.IMAGE_ONLY,
    val displayCount: Boolean = true,
    val drawMode: DrawMode = DrawMode.RANDOM,
    val aspectRatio: CardAspectRatio = CardAspectRatio.STANDARD,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

/** Tag/etiqueta para organizar mazos y cartas */
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int = 0xFF6200EE.toInt()
)
