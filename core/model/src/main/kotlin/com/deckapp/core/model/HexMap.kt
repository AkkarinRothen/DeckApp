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
    val createdAt: Long = System.currentTimeMillis()
)

enum class HexStyle { FLAT_TOP, POINTY_TOP }
