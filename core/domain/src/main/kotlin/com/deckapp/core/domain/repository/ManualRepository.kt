package com.deckapp.core.domain.repository

import com.deckapp.core.model.Manual
import com.deckapp.core.model.ManualBookmark
import kotlinx.coroutines.flow.Flow

interface ManualRepository {
    fun getAllManuals(): Flow<List<Manual>>
    fun getManualById(id: Long): Flow<Manual?>
    suspend fun saveManual(manual: Manual): Long
    suspend fun updateLastOpened(id: Long, timestamp: Long)
    suspend fun deleteManual(manual: Manual)
    fun getDistinctSystems(): Flow<List<String>>

    // Bookmarks
    fun getBookmarksForManual(manualId: Long): Flow<List<ManualBookmark>>
    suspend fun saveBookmark(bookmark: ManualBookmark): Long
    suspend fun deleteBookmark(id: Long)
    suspend fun deleteBookmarkAtPage(manualId: Long, pageIndex: Int)
}
