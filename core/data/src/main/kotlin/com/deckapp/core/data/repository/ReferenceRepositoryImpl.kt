package com.deckapp.core.data.repository

import androidx.room.withTransaction
import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferenceRepositoryImpl @Inject constructor(
    private val db: DeckAppDatabase,
    private val tableDao: ReferenceTableDao,
    private val ruleDao: SystemRuleDao,
    private val randomTableDao: RandomTableDao
) : ReferenceRepository {

    override fun getAllReferenceTables(): Flow<List<ReferenceTable>> =
        tableDao.getAllTables().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getTablesBySystem(system: String): Flow<List<ReferenceTable>> =
        tableDao.getTablesBySystem(system).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getReferenceTableWithRows(id: Long): ReferenceTable? {
        val withRows = tableDao.getTableWithRows(id) ?: return null
        val tags = tableDao.getTagsForTable(id).map { it.toDomain() }
        return withRows.table.toDomain(rows = withRows.rows, tags = tags)
    }

    override fun searchReferenceTables(query: String): Flow<List<ReferenceTable>> =
        tableDao.searchTables(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveReferenceTable(table: ReferenceTable): Long {
        val tableId = tableDao.insertTable(table.toEntity())
        tableDao.deleteRowsForTable(tableId)
        tableDao.insertRows(table.rows.map { it.toEntity(tableId) })
        return tableId
    }

    override suspend fun deleteReferenceTable(tableId: Long) {
        tableDao.deleteTable(tableId)
    }

    override suspend fun updateTablePinned(id: Long, pinned: Boolean) {
        tableDao.updatePinned(id, pinned)
    }

    override fun getAllSystemRules(): Flow<List<SystemRule>> =
        ruleDao.getAllRules().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getRulesBySystem(system: String): Flow<List<SystemRule>> =
        ruleDao.getRulesBySystem(system).map { entities ->
            entities.map { it.toDomain() }
        }

    override fun searchSystemRules(query: String): Flow<List<SystemRule>> =
        ruleDao.searchRules(query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getRuleById(id: Long): SystemRule? {
        val entity = ruleDao.getRuleById(id) ?: return null
        val tags = ruleDao.getTagsForRule(id).map { it.toDomain() }
        return entity.toDomain(tags = tags)
    }

    override suspend fun saveSystemRule(rule: SystemRule): Long {
        return ruleDao.insertRule(rule.toEntity())
    }

    override suspend fun deleteSystemRule(ruleId: Long) {
        ruleDao.deleteRule(ruleId)
    }

    override suspend fun updateRulePinned(id: Long, pinned: Boolean) {
        ruleDao.updatePinned(id, pinned)
    }

    override fun getDistinctSystems(): Flow<List<String>> =
        combine(tableDao.getDistinctSystems(), ruleDao.getDistinctSystems()) { tableSystems, ruleSystems ->
            (tableSystems + ruleSystems).distinct().sorted()
        }

    override suspend fun addTagToReferenceTable(tableId: Long, tagId: Long) {
        tableDao.addTagToTable(ReferenceTableTagCrossRef(tableId, tagId))
    }

    override suspend fun removeTagFromReferenceTable(tableId: Long, tagId: Long) {
        tableDao.removeTagFromTable(ReferenceTableTagCrossRef(tableId, tagId))
    }

    override suspend fun addTagToSystemRule(ruleId: Long, tagId: Long) {
        ruleDao.addTagToRule(SystemRuleTagCrossRef(ruleId, tagId))
    }

    override suspend fun removeTagFromSystemRule(ruleId: Long, tagId: Long) {
        ruleDao.removeTagFromRule(SystemRuleTagCrossRef(ruleId, tagId))
    }

    override fun getInstalledPackNames(): Flow<Set<String>> = combine(
        tableDao.getDistinctSourcePacks(),
        ruleDao.getDistinctSourcePacks(),
        randomTableDao.getDistinctSourcePacks()
    ) { t, r, rt -> (t + r + rt).toSet() }

    override suspend fun importStarterPack(backup: com.deckapp.core.model.backup.FullBackupDto, packName: String) {
        db.withTransaction {
            val gameSystems = (backup.referenceTables.map { it.gameSystem } +
                               backup.systemRules.map { it.gameSystem }).toSet()
            gameSystems.forEach { system ->
                tableDao.deleteOrphansByGameSystem(system)
                ruleDao.deleteOrphansByGameSystem(system)
            }

            val tableIdMap = mutableMapOf<Long, Long>()

            backup.referenceTables.forEach { dto ->
                val existing = tableDao.getTableByPackAndName(packName, dto.name)
                val tableId = tableDao.insertTable(
                    ReferenceTableEntity(
                        id = existing?.id ?: 0,
                        name = dto.name,
                        description = dto.description,
                        gameSystem = dto.gameSystem,
                        category = dto.category,
                        columnsJson = dto.columnsJson,
                        isPinned = dto.isPinned,
                        sortOrder = dto.sortOrder,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        sourcePack = packName
                    )
                )
                tableIdMap[dto.id] = tableId
                tableDao.deleteRowsForTable(tableId)
            }

            backup.referenceRows.forEach { dto ->
                val newTableId = tableIdMap[dto.tableId]
                if (newTableId != null) {
                    tableDao.insertRows(listOf(
                        ReferenceRowEntity(
                            id = 0,
                            tableId = newTableId,
                            cellsJson = dto.cellsJson,
                            sortOrder = dto.sortOrder
                        )
                    ))
                }
            }

            backup.systemRules.forEach { dto ->
                val existing = ruleDao.getRuleByPackAndTitle(packName, dto.title)
                ruleDao.insertRule(
                    SystemRuleEntity(
                        id = existing?.id ?: 0,
                        title = dto.title,
                        content = dto.content,
                        gameSystem = dto.gameSystem,
                        category = dto.category,
                        isPinned = dto.isPinned,
                        sortOrder = dto.sortOrder,
                        lastUpdated = System.currentTimeMillis(),
                        sourcePack = packName
                    )
                )
            }

            // 3. Tablas Aleatorias
            val randomTableIdMap = mutableMapOf<Long, Long>()
            backup.randomTables.forEach { dto ->
                val existing = randomTableDao.getTableByPackAndName(packName, dto.name)
                val tableId = randomTableDao.insertTable(
                    RandomTableEntity(
                        id = existing?.id ?: 0,
                        bundleId = null,
                        name = dto.name,
                        category = dto.category,
                        description = dto.description,
                        rollFormula = dto.rollFormula,
                        rollMode = dto.rollMode,
                        isNoRepeat = dto.isNoRepeat,
                        isPinned = dto.isPinned,
                        sourceType = "JSON",
                        sourceName = packName,
                        isBuiltIn = true,
                        sortOrder = dto.sortOrder,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                        sourcePack = packName
                    )
                )
                randomTableIdMap[dto.id] = tableId
                randomTableDao.deleteEntriesForTable(tableId)
            }

            backup.tableEntries.forEach { dto ->
                val newTableId = randomTableIdMap[dto.tableId]
                if (newTableId != null) {
                    randomTableDao.insertEntries(listOf(
                        TableEntryEntity(
                            id = 0,
                            tableId = newTableId,
                            minRoll = dto.minRoll,
                            maxRoll = dto.maxRoll,
                            weight = dto.weight,
                            text = dto.text,
                            subTableRef = dto.subTableRef,
                            subTableId = dto.subTableId?.let { randomTableIdMap[it] },
                            sortOrder = dto.sortOrder
                        )
                    ))
                }
            }
        }
    }

    override suspend fun removeStarterPack(packName: String) {
        db.withTransaction {
            tableDao.deleteByPack(packName)
            ruleDao.deleteByPack(packName)
            randomTableDao.deleteByPack(packName)
        }
    }
}
