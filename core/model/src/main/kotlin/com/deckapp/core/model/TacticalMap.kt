package com.deckapp.core.model

import kotlinx.serialization.Serializable

/**
 * Representa un mapa táctico de batalla o dungeon para el VTT táctico de DeckApp.
 */
@Serializable
data class TacticalMap(
    val id: Long = 0,
    val name: String,
    val imageUri: String,
    val gridType: GridType = GridType.SQUARE,
    val cellSizePx: Float = 70f,
    val gridOffsetX: Float = 0f,
    val gridOffsetY: Float = 0f,
    val gridColorHex: Long = 0x40FFFFFFL, // Blanco semitransparente por defecto
    val gridOpacity: Float = 0.5f,
    val scaleFtPerCell: Int = 5,
    val diagonalRule: DiagonalRule = DiagonalRule.ALTERNATING_5_10_5,
    val sessionId: Long? = null,
    val sceneId: Long? = null,
    val fogOfWarMaskJson: String = "",
    val terrainMaskJson: String = "",
    val areasJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Tipos de terreno táctico sobre la grilla (terreno difícil, muros, peligros).
 */
@Serializable
enum class TacticalTerrainType {
    NORMAL,
    DIFFICULT,
    WALL,
    HAZARD
}

/**
 * Tipos de grilla soportados en el mapa táctico.
 */
@Serializable
enum class GridType {
    SQUARE,
    HEX_FLAT,
    HEX_POINTY,
    NONE
}

/**
 * Reglas de cálculo de distancia diagonal en grillas cuadradas (D&D / PF / Genérico).
 */
@Serializable
enum class DiagonalRule {
    /** 5 pies la primera diagonal, 10 la segunda, 5 la tercera... (D&D 3.5 / PF / D&D 5e variante) */
    ALTERNATING_5_10_5,
    /** Distancia euclidiana geométrica continua */
    EUCLIDEAN,
    /** Cada diagonal cuesta 1 casilla (5 pies), regla estándar D&D 5e */
    EQUIDISTANT
}

/**
 * Facciones a las que puede pertenecer un token táctico.
 */
@Serializable
enum class TokenFaction {
    PLAYER,
    ALLY,
    ENEMY,
    NEUTRAL
}

/**
 * Representa un token (criatura, personaje o marcador) situado sobre la grilla del mapa táctico.
 */
@Serializable
data class TacticalToken(
    val id: Long = 0,
    val mapId: Long,
    val name: String,
    val avatarUri: String? = null,
    val faction: TokenFaction = TokenFaction.ENEMY,
    val sizeInCells: Float = 1f, // 1 = Medium/Small, 2 = Large, 3 = Huge, 4 = Gargantuan
    val cellX: Float = 0f,       // Posición en coordenadas de celda (permite snap a centros o bordes)
    val cellY: Float = 0f,
    val currentHp: Int = 10,
    val maxHp: Int = 10,
    val tempHp: Int = 0,
    val armorClass: Int = 10,
    val speedFt: Int = 30,
    val conditions: Set<Condition> = emptySet(),
    val isHidden: Boolean = false,
    val elevationFt: Int = 0,
    val encounterCreatureId: Long? = null,
    val npcId: Long? = null,
    val notes: String = ""
)

fun TacticalToken.applyDamage(damage: Int): TacticalToken {
    val dmgToTemp = minOf(tempHp, damage)
    val remainingDmg = damage - dmgToTemp
    val newTemp = tempHp - dmgToTemp
    val newHp = (currentHp - remainingDmg).coerceAtLeast(0)
    return copy(currentHp = newHp, tempHp = newTemp)
}

fun TacticalToken.applyHealing(healAmount: Int): TacticalToken {
    val newHp = (currentHp + healAmount).coerceAtMost(maxHp)
    return copy(currentHp = newHp)
}

fun TacticalToken.applyTempHp(newTempHp: Int): TacticalToken {
    return copy(tempHp = maxOf(tempHp, newTempHp))
}

@Serializable
enum class MapPinExplorationState {
    UNKNOWN,
    DISCOVERED,
    EXPLORED
}

typealias PinExplorationState = MapPinExplorationState
typealias PinDiscovery = TableRollResult
typealias MapPin = TacticalMapPin

/**
 * Pin de información o secretos de dungeon fijado a una coordenada del mapa (Room Keying).
 */
@Serializable
data class TacticalMapPin(
    val id: Long = 0,
    val mapId: Long,
    val cellX: Float,
    val cellY: Float,
    val title: String,
    val description: String = "",
    val iconName: String = "info", // "info", "chest", "trap", "door", "skull"
    val colorHex: Long = 0xFFFFA000L,
    val isSecret: Boolean = true, // Por defecto secreto para uso del DM / Solo RPG
    val explorationState: MapPinExplorationState = MapPinExplorationState.UNKNOWN,
    val targetTableId: Long? = null,
    val targetEncounterId: Long? = null,
    val targetWikiEntryId: Long? = null,
    val targetMapId: Long? = null,
    val discoveries: List<TableRollResult> = emptyList()
)

/**
 * Formas geométricas para plantillas de Área de Efecto (AoE) y medición de distancia.
 */
@Serializable
enum class TacticalMeasureShape {
    LINE,
    CONE,
    CIRCLE,
    BOX
}
