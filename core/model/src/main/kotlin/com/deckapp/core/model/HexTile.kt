package com.deckapp.core.model

data class HexTile(
    val id: Long = 0,
    val mapId: Long,
    val q: Int,
    val r: Int,
    val terrainCost: Int = 1,   // 1=open, 2=difficult, 3=greater difficult, 0=impassable
    val terrainLabel: String = "",
    val terrainColor: Long = 0xFF7CB87BL,
    val dmNotes: String = "",
    val playerNotes: String = "",
    val isExplored: Boolean = false,
    val isReconnoitered: Boolean = false,
    val isMapped: Boolean = false
)
