package com.deckapp.core.domain.repository

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollResult
import kotlinx.coroutines.flow.Flow

interface TableRepository {

    fun getAllTables(): Flow<List<RandomTable>>

    fun getTablesByCategory(category: String): Flow<List<RandomTable>>

    fun getCategories(): Flow<List<String>>

    suspend fun getTableWithEntries(id: Long): RandomTable?

    suspend fun getTableByName(name: String): RandomTable?

    suspend fun saveTable(table: RandomTable): Long

    suspend fun deleteTable(tableId: Long)

    suspend fun countBuiltInTables(): Int

    fun getRecentResultsForTable(sessionId: Long, tableId: Long): Flow<List<TableRollResult>>

    suspend fun saveRollResult(result: TableRollResult): Long
}
