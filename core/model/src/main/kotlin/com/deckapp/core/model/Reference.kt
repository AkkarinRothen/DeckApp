package com.deckapp.core.model

import kotlinx.serialization.Serializable

data class ReferenceTable(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val columns: List<ReferenceColumn> = emptyList(),
    val rows: List<ReferenceRow> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sourcePack: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ReferenceColumn(
    val index: Int,
    val header: String,
    val widthWeight: Float = 1f
)

data class ReferenceRow(
    val id: Long = 0,
    val tableId: Long = 0,
    val cells: List<String>,
    val sortOrder: Int = 0
)

data class SystemRule(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val tags: List<Tag> = emptyList(),
    val isPinned: Boolean = false,
    val sortOrder: Int = 0,
    val sourcePack: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class ImportPreviewData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val source: ReferenceImportSource
)

enum class ReferenceImportSource { CSV, MARKDOWN, OCR_IMAGE }
