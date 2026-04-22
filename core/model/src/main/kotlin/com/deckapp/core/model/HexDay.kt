package com.deckapp.core.model

import kotlinx.serialization.Serializable

data class HexDay(
    val id: Long = 0,
    val mapId: Long,
    val dayNumber: Int,
    val activitiesLog: List<HexActivityEntry> = emptyList(),
    val notes: String = ""
)

@Serializable
data class HexActivityEntry(
    val type: HexActivityType,
    val description: String,
    val tileQ: Int? = null,
    val tileR: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class HexActivityType {
    TRAVEL, EXPLORE, RECONNOITER, FORTIFY_CAMP, MAP_AREA, CUSTOM
}
