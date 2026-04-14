package com.deckapp.core.model

data class RandomTable(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val category: String = "",
    val rollFormula: String = "1d6",
    val rollMode: TableRollMode = TableRollMode.RANGE,
    val entries: List<TableEntry> = emptyList(),
    val isBuiltIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class TableEntry(
    val id: Long = 0,
    val tableId: Long = 0,
    val minRoll: Int = 1,
    val maxRoll: Int = 1,
    val weight: Int = 1,
    val text: String,
    val subTableRef: String? = null,
    val subTableId: Long? = null,
    val sortOrder: Int = 0
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

enum class TableRollMode { RANGE, WEIGHTED }
