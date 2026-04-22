package com.deckapp.core.model

data class HexMap(
    val id: Long = 0,
    val name: String,
    val rows: Int,
    val cols: Int,
    val sessionId: Long? = null,
    val hexStyle: HexStyle = HexStyle.FLAT_TOP,
    val partyQ: Int? = null,
    val partyR: Int? = null,
    val isRadial: Boolean = false,
    val maxActivitiesPerDay: Int = 8,
    val mapNotes: String = "",
    val weatherTableId: Long? = null,
    val travelEventTableId: Long? = null,
    val terrainTableConfig: String = "{}",
    val sessionResources: String = "{}",
    val linkedMythicSessionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class HexStyle { FLAT_TOP, POINTY_TOP }

data class TerrainBrush(
    val cost: Int,         // 1, 2, 3, or 0 (impassable)
    val label: String,
    val color: Long
)

val defaultBrushes = listOf(
    TerrainBrush(1, "Abierto", 0xFF7CB87BL),
    TerrainBrush(2, "Difícil", 0xFF8B7355L),
    TerrainBrush(3, "Muy difícil", 0xFF6B6B6BL),
    TerrainBrush(0, "Infranqueable", 0xFF2D2D2DL),
    TerrainBrush(1, "Agua", 0xFF4A90D9L),
    TerrainBrush(1, "Llanura", 0xFFD4C875L),
    TerrainBrush(2, "Bosque", 0xFF3A7D44L),
    TerrainBrush(3, "Montaña", 0xFF9E9E9EL),
    TerrainBrush(2, "Pantano", 0xFF4B5320L),
    TerrainBrush(1, "Desierto", 0xFFEDC9AFL),
    TerrainBrush(2, "Jungla", 0xFF228B22L),
    TerrainBrush(2, "Colina", 0xFF7C8D7CL),
    TerrainBrush(2, "Nieve/Hielo", 0xFFB0E0E6L),
    TerrainBrush(1, "Ruinas", 0xFF5D5D5DL),
    TerrainBrush(-1, "Borrar", 0x00000000L)
)
