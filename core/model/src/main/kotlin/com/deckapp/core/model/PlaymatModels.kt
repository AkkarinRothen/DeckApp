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
    val tableChips: List<PlaymatTableChip> = emptyList(),
    val decks: List<PlaymatDeckPlacement> = emptyList(),
    /** Session-local quick access list; independent from whether decks are placed on the board. */
    val favoriteDeckIds: Set<Long> = emptySet(),
    val connections: List<PlaymatConnection> = emptyList(),
    val tokens: List<PlaymatTokenPlacement> = emptyList(),
    val clocks: List<PlaymatClockPlacement> = emptyList(),
    val groups: List<PlaymatElementGroup> = emptyList(),
    val backgroundPreset: PlaymatAtmosphericPreset = PlaymatAtmosphericPreset.NONE,
    val backgroundOpacity: Float = 1.0f,
    val backgroundScale: Float = 1.0f,
    val backgroundX: Float = 0f,
    val backgroundY: Float = 0f,
    val isBackgroundLocked: Boolean = true,
    val gridType: PlaymatGridType = PlaymatGridType.NONE,
    val gridStyle: PlaymatGridStyle = PlaymatGridStyle.LINES,
    val cellSizeDp: Float = 48f,
    val snapToGrid: Boolean = false
)

/** Named, persistent grouping of movable objects on one playmat surface. */
@Serializable
data class PlaymatElementGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val memberIds: Set<String> = emptySet(),
    val isLocked: Boolean = false,
    val isHidden: Boolean = false
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
 * Presets atmosféricos de escenas y mapas para el tapete.
 */
@Serializable
enum class PlaymatAtmosphericPreset {
    NONE,
    DUNGEON_STONE,
    MEDIEVAL_TAVERN,
    MYSTIC_FOREST,
    PARCHMENT_MAP
}

/**
 * Tipo o forma geométrica de la cuadrícula táctica.
 */
@Serializable
enum class PlaymatGridType {
    NONE,
    SQUARE,
    HEXAGONAL
}

/**
 * Estilo visual de la cuadrícula sobre la mesa.
 */
@Serializable
enum class PlaymatGridStyle {
    LINES,
    DOTS
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
    val rotationDegrees: Float = 0f,
    val groupId: String? = null
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
    val y: Float,
    val groupId: String? = null
)

/**
 * Posición espacial 2D de un mazo físico y su pila de descarte asociada en el tapete.
 */
@Serializable
data class PlaymatDeckPlacement(
    val stackId: Long,
    val x: Float,
    val y: Float,
    val discardX: Float? = null,
    val discardY: Float? = null,
    val isLocked: Boolean = false,
    val groupId: String? = null,
    val discardGroupId: String? = null,
    /** Whether this session deck and its discard pile are drawn on this playmat surface. */
    val isVisibleOnPlaymat: Boolean = true
)

/**
 * Tipo o facción del token en el tapete.
 */
@Serializable
enum class PlaymatTokenType {
    ALLY,
    NEUTRAL,
    ENEMY
}

/**
 * Posición espacial 2D, estadísticas y apariencia de un token de personaje (PJ/PNJ) sobre el tapete.
 */
@Serializable
data class PlaymatTokenPlacement(
    val id: String = UUID.randomUUID().toString(),
    val npcId: Long? = null,
    val name: String,
    val imagePath: String? = null,
    val x: Float,
    val y: Float,
    val currentHp: Int = 10,
    val maxHp: Int = 10,
    val tokenType: PlaymatTokenType = PlaymatTokenType.ALLY,
    val ringColorHex: String = "#4CAF50",
    val notes: String = "",
    val groupId: String? = null
)

/**
 * Reloj de progreso interactivo (estilo Blades in the Dark / PBTA / TTRPG).
 * Permite seguir tensión, sigilo, cuenta regresiva o proyectos en 4, 6 u 8 segmentos.
 */
@Serializable
data class PlaymatClockPlacement(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Reloj de Tensión",
    val totalSegments: Int = 4, // 4, 6 u 8
    val filledSegments: Int = 0,
    val x: Float,
    val y: Float,
    val colorHex: String = "#E53935",
    val groupId: String? = null
)

/**
 * Conexión o hilo narrativo entre elementos del tapete (cartas y/o tokens de personajes).
 */
@Serializable
data class PlaymatConnection(
    val id: String = UUID.randomUUID().toString(),
    val fromCardId: Long = 0L,
    val toCardId: Long = 0L,
    val fromEndpointId: String = "",
    val toEndpointId: String = "",
    val label: String = "",
    val colorHex: String = "#E53935", // Rojo investigación por defecto
    val isBidirectional: Boolean = false
) {
    val effectiveFromId: String
        get() = fromEndpointId.ifBlank { if (fromCardId != 0L) "card_$fromCardId" else "" }

    val effectiveToId: String
        get() = toEndpointId.ifBlank { if (toCardId != 0L) "card_$toCardId" else "" }
}

