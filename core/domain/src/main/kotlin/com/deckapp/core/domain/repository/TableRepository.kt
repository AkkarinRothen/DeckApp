package com.deckapp.core.domain.repository

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollResult
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun getAllTables(): Flow<List<RandomTable>>
    suspend fun getTableWithEntries(id: Long): RandomTable?
    suspend fun getTableByName(name: String): RandomTable?
    suspend fun saveTable(table: RandomTable): Long
    suspend fun deleteTable(tableId: Long)
    suspend fun countBuiltInTables(): Int
    fun getRecentResultsForTable(sessionId: Long, tableId: Long): Flow<List<TableRollResult>>

    suspend fun saveRollResult(result: TableRollResult): Long
    suspend fun updatePinnedState(tableId: Long, isPinned: Boolean)

    // --- Tags ---
    suspend fun addTagToTable(tableId: Long, tagId: Long)
    suspend fun removeTagFromTable(tableId: Long, tagId: Long)

    // --- Bulk Operations ---
    suspend fun bulkDeleteTables(ids: List<Long>)
    suspend fun bulkUpdatePinnedState(ids: List<Long>, isPinned: Boolean)
    suspend fun bulkAddTagToTables(tableIds: List<Long>, tagId: Long)
    suspend fun bulkRemoveTagFromTables(tableIds: List<Long>, tagId: Long)
}
