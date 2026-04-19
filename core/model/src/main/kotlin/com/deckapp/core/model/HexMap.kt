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
    val createdAt: Long = System.currentTimeMillis()
)

enum class HexStyle { FLAT_TOP, POINTY_TOP }
