package com.deckapp.core.model

data class WikiEntry(
    val id: Long = 0,
    val title: String,
    val content: String,
    val categoryId: Long,
    val imagePath: String? = null,
    val isPinned: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class WikiCategory(
    val id: Long = 0,
    val name: String,
    val iconName: String = "Description",
    val entryCount: Int = 0
)
