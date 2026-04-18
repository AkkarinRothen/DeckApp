package com.deckapp.core.domain.repository

import com.deckapp.core.model.WikiCategory
import com.deckapp.core.model.WikiEntry
import kotlinx.coroutines.flow.Flow

interface WikiRepository {
    fun getCategories(): Flow<List<WikiCategory>>
    suspend fun saveCategory(category: WikiCategory): Long
    
    fun getEntriesByCategory(categoryId: Long): Flow<List<WikiEntry>>
    suspend fun getEntryById(id: Long): WikiEntry?
    fun searchEntries(query: String): Flow<List<WikiEntry>>
    
    suspend fun saveEntry(entry: WikiEntry): Long
    suspend fun deleteEntry(entry: WikiEntry)
    suspend fun saveEntryImage(uri: android.net.Uri, entryId: Long): String?
}
