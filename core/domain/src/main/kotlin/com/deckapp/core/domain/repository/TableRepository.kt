package com.deckapp.core.domain.repository

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableBundle
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollResult
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun getAllTables(): Flow<List<RandomTable>>
    suspend fun getTableWithEntries(id: Long): RandomTable?
    suspend fun getTableByName(name: String): RandomTable?
    suspend fun saveTable(table: RandomTable): Long
    suspend fun deleteTable(tableId: Long)
    /** Persiste el orden de las tablas actualizando el campo sortOrder. */
    suspend fun updateTablesSortOrder(orderedIds: List<Long>)
    suspend fun countBuiltInTables(): Int
    fun getRecentResultsForTable(sessionId: Long, tableId: Long): Flow<List<TableRollResult>>
    suspend fun getRecentResultsForTable(sessionId: Long, tableId: Long, limit: Int): List<TableRollResult>

    suspend fun saveRollResult(result: TableRollResult): Long
    suspend fun updatePinnedState(tableId: Long, isPinned: Boolean)

    // --- Bundles ---
    fun getAllBundles(): Flow<List<TableBundle>>
    fun getBundleWithTables(bundleId: Long): Flow<TableBundle?>
    suspend fun saveBundle(bundle: TableBundle): Long
    suspend fun deleteBundle(bundleId: Long)

    // --- Tags ---
    suspend fun addTagToTable(tableId: Long, tagId: Long)
    suspend fun removeTagFromTable(tableId: Long, tagId: Long)

    // --- Bulk Operations ---
    suspend fun bulkDeleteTables(ids: List<Long>)
    suspend fun bulkUpdatePinnedState(ids: List<Long>, isPinned: Boolean)
    suspend fun bulkAddTagToTables(tableIds: List<Long>, tagId: Long)
    suspend fun bulkRemoveTagFromTables(tableIds: List<Long>, tagId: Long)

    // --- Search ---
    fun searchTables(query: String): Flow<List<RandomTable>>
    fun searchEntries(query: String): Flow<List<Pair<Long, String>>> // tableId to entryText
}
