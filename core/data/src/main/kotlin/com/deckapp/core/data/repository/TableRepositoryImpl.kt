package com.deckapp.core.data.repository

import com.deckapp.core.data.db.RandomTableDao
import com.deckapp.core.data.db.TableRollResultDao
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TableRepositoryImpl @Inject constructor(
    private val randomTableDao: RandomTableDao,
    private val tableRollResultDao: TableRollResultDao
) : TableRepository {

    override fun getAllTables(): Flow<List<RandomTable>> =
        randomTableDao.getAllTablesWithEntries().map { list -> list.map { it.toDomain() } }

    override fun getTablesByCategory(category: String): Flow<List<RandomTable>> =
        randomTableDao.getTablesByCategory(category).map { list ->
            list.map { it.toDomain() }
        }

    override fun getCategories(): Flow<List<String>> = randomTableDao.getCategories()

    override suspend fun getTableWithEntries(id: Long): RandomTable? =
        randomTableDao.getTableWithEntries(id)?.toDomain()

    override suspend fun getTableByName(name: String): RandomTable? {
        val entity = randomTableDao.getTableByName(name) ?: return null
        val withEntries = randomTableDao.getTableWithEntries(entity.id) ?: return null
        return withEntries.toDomain()
    }

    override suspend fun saveTable(table: RandomTable): Long {
        val tableId = randomTableDao.insertTable(table.toEntity())
        randomTableDao.deleteEntriesForTable(tableId)
        randomTableDao.insertEntries(table.entries.map { it.toEntity(tableId) })
        return tableId
    }

    override suspend fun deleteTable(tableId: Long) {
        randomTableDao.deleteTable(tableId)
    }

    override suspend fun countBuiltInTables(): Int = randomTableDao.countBuiltInTables()

    override fun getRecentResultsForTable(sessionId: Long, tableId: Long): Flow<List<TableRollResult>> =
        tableRollResultDao.getRecentResultsForTable(sessionId, tableId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun saveRollResult(result: TableRollResult): Long =
        tableRollResultDao.insertResult(result.toEntity())
}
