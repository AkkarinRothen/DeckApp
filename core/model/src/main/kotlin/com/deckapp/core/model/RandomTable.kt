package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class RandomTable(
    val id: Long = 0,
    val bundleId: Long? = null,
    val bundleName: String? = null,
    val name: String,
    val category: String = "General",
    val description: String = "",
    val tags: List<Tag> = emptyList(),
    val rollFormula: String = "1d6",
    val rollMode: TableRollMode = TableRollMode.RANGE,
    val entries: List<TableEntry> = emptyList(),
    val isNoRepeat: Boolean = false,
    val isPinned: Boolean = false,
    val isBuiltIn: Boolean = false,
    val sortOrder: Int = 0,
    val sourcePack: String? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class TableEntry(
    val id: Long = 0,
    val tableId: Long = 0,
    val minRoll: Int = 1,
    val maxRoll: Int = 1,
    val weight: Int = 1,
    val text: String,
    val subTableRef: String? = null,
    val subTableId: Long? = null,
    val sortOrder: Int = 0,
    val confidence: Float = 1.0f
)

data class TableRollResult(
    val id: Long = 0,
    val tableId: Long,
    val tableName: String,
    val sessionId: Long?,
    val rollValue: Int,
    val resolvedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class TableRollMode { RANGE, WEIGHTED, SEQUENTIAL, MACRO }

data class TableBundle(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val sourceUri: String? = null,
    val imageUrl: String? = null,
    val tables: List<RandomTable> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
