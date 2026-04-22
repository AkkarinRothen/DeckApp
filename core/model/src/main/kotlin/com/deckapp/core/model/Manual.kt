package com.deckapp.core.model

import kotlinx.serialization.Serializable

/** Referencia a un manual en PDF */
@Serializable
data class Manual(
    val id: Long = 0,
    val title: String,
    val uri: String,
    val gameSystem: String = "General",
    val fileName: String = "",
    val fileSize: Long = 0,
    val lastOpened: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Marcador en una página específica de un manual */
@Serializable
data class ManualBookmark(
    val id: Long = 0,
    val manualId: Long,
    val pageIndex: Int,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)
