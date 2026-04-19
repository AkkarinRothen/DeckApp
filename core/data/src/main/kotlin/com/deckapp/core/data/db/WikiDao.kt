package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WikiDao {
    @Query("SELECT * FROM wiki_categories")
    fun getAllCategories(): Flow<List<WikiCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: WikiCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<WikiCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WikiEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<WikiEntryEntity>)

    @Query("SELECT * FROM wiki_entries WHERE categoryId = :categoryId ORDER BY lastUpdated DESC")
    fun getEntriesByCategory(categoryId: Long): Flow<List<WikiEntryEntity>>

    @Query("SELECT * FROM wiki_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): WikiEntryEntity?

    @Query("SELECT * FROM wiki_entries WHERE title LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<WikiEntryEntity>>

    @Delete
    suspend fun deleteEntry(entry: WikiEntryEntity)

    @Query("SELECT categoryId, COUNT(*) as count FROM wiki_entries GROUP BY categoryId")
    fun getCategoryCounts(): Flow<List<WikiCategoryCount>>
}

data class WikiCategoryCount(
    val categoryId: Long,
    val count: Int
)
