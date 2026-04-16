package com.deckapp.core.data.repository

import com.deckapp.core.data.db.RandomTableDao
import com.deckapp.core.data.db.TableRollResultDao
import com.deckapp.core.data.db.TagDao
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
    private val tableRollResultDao: TableRollResultDao,
    private val tagDao: TagDao
) : TableRepository {

    override fun getAllTables(): Flow<List<RandomTable>> =
        randomTableDao.getAllTablesWithEntries().map { list -> 
            list.map { item ->
                val tags = tagDao.getTagsForTable(item.table.id).map { it.toDomain() }
                item.toDomain(tags)
            }
        }


    override suspend fun getTableWithEntries(id: Long): RandomTable? {
        val withEntries = randomTableDao.getTableWithEntries(id) ?: return null
        val tags = tagDao.getTagsForTable(id).map { it.toDomain() }
        return withEntries.toDomain(tags)
    }

    override suspend fun getTableByName(name: String): RandomTable? {
        val entity = randomTableDao.getTableByName(name) ?: return null
        return getTableWithEntries(entity.id)
    }

    override suspend fun saveTable(table: RandomTable): Long {
        val tableId = randomTableDao.insertTable(table.toEntity())
        randomTableDao.deleteEntriesForTable(tableId)
        randomTableDao.insertEntries(table.entries.map { it.toEntity(tableId) })
        
        // Tags
        tagDao.deleteTagsForTable(tableId)
        table.tags.forEach { tag ->
            val tagId = tagDao.insertTag(tag.toEntity())
            tagDao.insertTableTagRef(com.deckapp.core.data.db.RandomTableTagCrossRef(tableId, tagId))
        }
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

    override suspend fun updatePinnedState(tableId: Long, isPinned: Boolean) =
        randomTableDao.updatePinnedState(tableId, isPinned)

    override suspend fun addTagToTable(tableId: Long, tagId: Long) {
        tagDao.insertTableTagRef(com.deckapp.core.data.db.RandomTableTagCrossRef(tableId, tagId))
    }

    override suspend fun removeTagFromTable(tableId: Long, tagId: Long) {
        tagDao.removeTableTagRef(tableId, tagId)
    }

    override suspend fun bulkDeleteTables(ids: List<Long>) =
        randomTableDao.bulkDeleteTables(ids)

    override suspend fun bulkUpdatePinnedState(ids: List<Long>, isPinned: Boolean) =
        randomTableDao.bulkUpdatePinnedState(ids, isPinned)

    override suspend fun bulkAddTagToTables(tableIds: List<Long>, tagId: Long) {
        tableIds.forEach { tableId ->
            tagDao.insertTableTagRef(com.deckapp.core.data.db.RandomTableTagCrossRef(tableId, tagId))
        }
    }

    override suspend fun bulkRemoveTagFromTables(tableIds: List<Long>, tagId: Long) {
        tableIds.forEach { tableId ->
            tagDao.removeTableTagRef(tableId, tagId)
        }
    }
}
