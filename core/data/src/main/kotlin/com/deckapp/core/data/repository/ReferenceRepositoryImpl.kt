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
        tableDao.getAllTablesWithRows().map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override fun getTablesBySystem(system: String): Flow<List<ReferenceTable>> =
        tableDao.getTablesWithRowsBySystem(system).map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override suspend fun getReferenceTableWithRows(id: Long): ReferenceTable? {
        val withRows = tableDao.getTableWithRows(id) ?: return null
        val tags = tableDao.getTagsForTable(id).map { it.toDomain() }
        return withRows.table.toDomain(rows = withRows.rows, tags = tags)
    }

    override fun searchReferenceTables(query: String): Flow<List<ReferenceTable>> =
        tableDao.searchTablesWithRows(query).map { entities ->
            entities.map { it.table.toDomain(rows = it.rows) }
        }

    override suspend fun saveReferenceTable(table: ReferenceTable): Long {
        val entity = table.toEntity()
        val id = tableDao.insertTable(entity)
        val tableId = if (id == -1L) {
            tableDao.updateTable(entity)
            entity.id
        } else id

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
        val entity = rule.toEntity()
        val id = ruleDao.insertRule(entity)
        if (id == -1L) {
            ruleDao.updateRule(entity)
            return entity.id
        }
        return id
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
                val entity = ReferenceTableEntity(
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
                val tableId = if (existing == null) {
                    tableDao.insertTable(entity)
                } else {
                    tableDao.updateTable(entity)
                    existing.id
                }
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
                val entity = SystemRuleEntity(
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
                if (existing == null) ruleDao.insertRule(entity) else ruleDao.updateRule(entity)
            }

            // 3. Tablas Aleatorias: preserve database identities when reinstalling.
            importRandomTables(backup, packName, replaceExisting = true)

            // 4. NPCs / Criaturas del paquete
            if (backup.npcs.isNotEmpty()) {
                val tagDao = db.tagDao()
                val npcDao = db.npcDao()
                val existingTag = tagDao.getTagByName(packName)
                val tagId = existingTag?.id ?: tagDao.insertTag(
                    TagEntity(
                        id = 0,
                        name = packName,
                        color = 0xFF8B0000.toInt()
                    )
                )

                backup.npcs.forEach { dto ->
                    val existing = npcDao.getNpcByName(dto.name)
                    val npcId = if (existing != null) {
                        npcDao.upsertNpc(
                            existing.copy(
                                description = dto.description,
                                imagePath = dto.imagePath ?: existing.imagePath,
                                maxHp = dto.maxHp,
                                currentHp = dto.currentHp,
                                armorClass = dto.armorClass,
                                initiativeBonus = dto.initiativeBonus,
                                notes = dto.notes,
                                isMonster = dto.isMonster
                            )
                        )
                        existing.id
                    } else {
                        npcDao.upsertNpc(
                            NpcEntity(
                                id = 0,
                                name = dto.name,
                                description = dto.description,
                                imagePath = dto.imagePath,
                                maxHp = dto.maxHp,
                                currentHp = dto.currentHp,
                                armorClass = dto.armorClass,
                                initiativeBonus = dto.initiativeBonus,
                                notes = dto.notes,
                                isMonster = dto.isMonster,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                    npcDao.insertCrossRef(NpcTagCrossRef(npcId = npcId, tagId = tagId))
                }
            }
        }
    }

    private suspend fun importRandomTables(
        backup: com.deckapp.core.model.backup.FullBackupDto,
        packName: String,
        replaceExisting: Boolean
    ) {
        val randomTableIdMap = mutableMapOf<Long, Long>()
        val writtenTableIds = mutableSetOf<Long>()
        backup.randomTables.forEach { dto ->
            val existing = if (replaceExisting) randomTableDao.getTableByPackAndName(packName, dto.name)
                else randomTableDao.getOwnedTableByPackAndName(packName, dto.name)
            if (existing != null && !replaceExisting) {
                randomTableIdMap[dto.id] = existing.id
                return@forEach
            }
            val entity = RandomTableEntity(
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
            val tableId = if (existing == null) {
                randomTableDao.insertTable(entity)
            } else {
                randomTableDao.updateTable(entity)
                existing.id
            }
            randomTableIdMap[dto.id] = tableId
            writtenTableIds += tableId
            randomTableDao.deleteEntriesForTable(tableId)
        }

        backup.tableEntries.forEach { dto ->
            val newTableId = randomTableIdMap[dto.tableId]
            if (newTableId != null && newTableId in writtenTableIds) {
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

    override suspend fun updateStarterPackArtwork(artwork: com.deckapp.core.model.backup.FullBackupDto, packName: String) {
        db.withTransaction {
            // Recheck ownership in the transaction so an uninstall cannot resurrect the pack.
            val ownedNpcs = db.npcDao().getNpcsByTagName(packName).associateBy { it.name }
            if (ownedNpcs.isEmpty() && ruleDao.getAllRulesSync().none { it.sourcePack == packName } &&
                tableDao.getAllTablesSync().none { it.sourcePack == packName }) return@withTransaction
            artwork.npcs.forEach { dto ->
                val existing = ownedNpcs[dto.name] ?: return@forEach
                val image = dto.imagePath ?: return@forEach
                if (existing.imagePath.isNullOrBlank() ||
                    existing.imagePath?.startsWith("file:///android_asset/starter_packs/choir_of_flesh/") == true) {
                    if (existing.imagePath != image) db.npcDao().upsertNpc(existing.copy(imagePath = image))
                }
            }
            artwork.systemRules.forEach { dto ->
                if (ruleDao.getRuleByPackAndTitle(packName, dto.title) == null) {
                    ruleDao.insertRule(SystemRuleEntity(
                        title = dto.title, content = dto.content, gameSystem = dto.gameSystem,
                        category = dto.category, sortOrder = dto.sortOrder, sourcePack = packName,
                        lastUpdated = System.currentTimeMillis()
                    ))
                }
            }
            // Older installed versions can predate the pack's random tables.
            importRandomTables(artwork, packName, replaceExisting = false)
        }
    }

    override suspend fun removeStarterPack(packName: String) {
        db.withTransaction {
            tableDao.deleteByPack(packName)
            ruleDao.deleteByPack(packName)
            randomTableDao.deleteByPack(packName)
            db.npcDao().deleteNpcsByTagName(packName)
            val tag = db.tagDao().getTagByName(packName)
            if (tag != null) {
                db.tagDao().deleteTag(tag.id)
            }
        }
    }
}
