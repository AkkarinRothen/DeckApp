package com.deckapp.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Representa una superficie o mesa de juego dentro de la sesión (Playmat).
 */
@Serializable
data class PlaymatSurface(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Mesa Principal",
    val backgroundTheme: PlaymatTheme = PlaymatTheme.FELT_GREEN,
    val customImageUri: String? = null,
    val template: PlaymatTemplateType = PlaymatTemplateType.FREEFORM,
    val cameraX: Float = 0f,
    val cameraY: Float = 0f,
    val cameraZoom: Float = 1f,
    val cards: List<PlaymatCardPlacement> = emptyList(),
    val stickyNotes: List<PlaymatStickyNote> = emptyList(),
    val tableChips: List<PlaymatTableChip> = emptyList()
)

/**
 * Temas visuales para el fondo del tapete.
 */
@Serializable
enum class PlaymatTheme {
    FELT_GREEN,
    FELT_RED,
    RUSTIC_WOOD,
    PARCHMENT_LEATHER,
    DARK_MINIMAL
}

/**
 * Moldes y plantillas temáticas con zonas magnéticas preconfiguradas.
 */
@Serializable
enum class PlaymatTemplateType {
    FREEFORM,
    DECK_OF_WORLDS_MICROSETTING,
    STORY_ENGINE_PREMISE,
    TAROT_THREE_CARDS,
    ORACLE_CROSS
}

/**
 * Posición, rotación y estado de una carta sobre el tapete.
 */
@Serializable
data class PlaymatCardPlacement(
    val cardId: Long,
    val x: Float,
    val y: Float,
    val rotationDegrees: Int = 0, // 0, 90, 180, 270
    val zIndex: Int = 0,
    val isFaceUp: Boolean = true,
    val isPinned: Boolean = false,
    val clusterId: String? = null,
    val slotTag: String? = null // Ej: "REGION", "LANDMARK", "ORIGIN", etc.
)

/**
 * Nota adhesiva colocada sobre el tapete.
 */
@Serializable
data class PlaymatStickyNote(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val colorHex: String = "#FFF59D", // Amarillo pastel
    val x: Float,
    val y: Float,
    val rotationDegrees: Float = 0f
)

/**
 * Ficha que representa una tirada de tabla aleatoria sobre el tapete.
 */
@Serializable
data class PlaymatTableChip(
    val id: String = UUID.randomUUID().toString(),
    val tableId: Long,
    val tableName: String,
    val rollResultText: String,
    val x: Float,
    val y: Float
)
