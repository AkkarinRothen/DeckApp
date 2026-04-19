package com.deckapp.core.model

data class HexPoi(
    val id: Long = 0,
    val mapId: Long,
    val tileQ: Int,
    val tileR: Int,
    val name: String,
    val type: PoiType,
    val description: String = "",
    val encounterId: Long? = null,
    val tableId: Long? = null
)

enum class PoiType {
    DUNGEON, SETTLEMENT, LANDMARK, HAZARD, RESOURCE, SECRET, CUSTOM
}
