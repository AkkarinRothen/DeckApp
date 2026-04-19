package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceTableDao {

    @Query("SELECT * FROM reference_tables ORDER BY isPinned DESC, sortOrder ASC, createdAt DESC")
    fun getAllTables(): Flow<List<ReferenceTableEntity>>

    @Query("SELECT * FROM reference_tables WHERE gameSystem = :system ORDER BY isPinned DESC, sortOrder ASC")
    fun getTablesBySystem(system: String): Flow<List<ReferenceTableEntity>>

    @Transaction
    @Query("SELECT * FROM reference_tables WHERE id = :id")
    suspend fun getTableWithRows(id: Long): ReferenceTableWithRows?

    @Query("""
        SELECT t.* FROM reference_tables t
        INNER JOIN reference_tables_fts fts ON t.id = fts.rowid
        WHERE reference_tables_fts MATCH :query
        ORDER BY t.isPinned DESC, t.sortOrder ASC
    """)
    fun searchTables(query: String): Flow<List<ReferenceTableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: ReferenceTableEntity): Long

    @Query("DELETE FROM reference_rows WHERE tableId = :tableId")
    suspend fun deleteRowsForTable(tableId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRows(rows: List<ReferenceRowEntity>)

    @Query("DELETE FROM reference_tables WHERE id = :id")
    suspend fun deleteTable(id: Long)

    @Query("UPDATE reference_tables SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("SELECT DISTINCT gameSystem FROM reference_tables")
    fun getDistinctSystems(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToTable(ref: ReferenceTableTagCrossRef)

    @Delete
    suspend fun removeTagFromTable(ref: ReferenceTableTagCrossRef)

    @Query("SELECT t.* FROM tags t INNER JOIN reference_table_tags r ON t.id = r.tagId WHERE r.tableId = :tableId")
    suspend fun getTagsForTable(tableId: Long): List<TagEntity>

    // --- Backup sync queries (no Flow) ---

    @Query("SELECT * FROM reference_tables")
    suspend fun getAllTablesSync(): List<ReferenceTableEntity>

    @Query("SELECT * FROM reference_rows")
    suspend fun getAllRowsSync(): List<ReferenceRowEntity>

    @Query("SELECT * FROM reference_table_tags")
    suspend fun getAllTagRefsSync(): List<ReferenceTableTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<ReferenceTableEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRefs(refs: List<ReferenceTableTagCrossRef>)

    @Query("DELETE FROM reference_tables WHERE sourcePack = :packName")
    suspend fun deleteByPack(packName: String)

    @Query("SELECT * FROM reference_tables WHERE name = :name AND (sourcePack = :packName OR sourcePack IS NULL) LIMIT 1")
    suspend fun getTableByPackAndName(packName: String, name: String): ReferenceTableEntity?

    @Query("DELETE FROM reference_tables WHERE gameSystem = :gameSystem AND (sourcePack IS NULL OR sourcePack = '')")
    suspend fun deleteOrphansByGameSystem(gameSystem: String)

    @Query("SELECT DISTINCT sourcePack FROM reference_tables WHERE sourcePack IS NOT NULL AND sourcePack != ''")
    fun getDistinctSourcePacks(): Flow<List<String>>
}

@Dao
interface SystemRuleDao {

    @Query("SELECT * FROM system_rules ORDER BY isPinned DESC, sortOrder ASC")
    fun getAllRules(): Flow<List<SystemRuleEntity>>

    @Query("SELECT * FROM system_rules WHERE gameSystem = :system ORDER BY isPinned DESC, sortOrder ASC")
    fun getRulesBySystem(system: String): Flow<List<SystemRuleEntity>>

    @Query("""
        SELECT r.* FROM system_rules r
        INNER JOIN system_rules_fts fts ON r.id = fts.rowid
        WHERE system_rules_fts MATCH :query
        ORDER BY r.isPinned DESC, r.sortOrder ASC
    """)
    fun searchRules(query: String): Flow<List<SystemRuleEntity>>

    @Query("SELECT * FROM system_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): SystemRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SystemRuleEntity): Long

    @Query("DELETE FROM system_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)

    @Query("UPDATE system_rules SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("SELECT DISTINCT gameSystem FROM system_rules")
    fun getDistinctSystems(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToRule(ref: SystemRuleTagCrossRef)

    @Delete
    suspend fun removeTagFromRule(ref: SystemRuleTagCrossRef)

    @Query("SELECT t.* FROM tags t INNER JOIN system_rule_tags s ON t.id = s.tagId WHERE s.ruleId = :ruleId")
    suspend fun getTagsForRule(ruleId: Long): List<TagEntity>

    // --- Backup sync queries (no Flow) ---

    @Query("SELECT * FROM system_rules")
    suspend fun getAllRulesSync(): List<SystemRuleEntity>

    @Query("SELECT * FROM system_rule_tags")
    suspend fun getAllTagRefsSync(): List<SystemRuleTagCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<SystemRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRefs(refs: List<SystemRuleTagCrossRef>)

    @Query("DELETE FROM system_rules WHERE sourcePack = :packName")
    suspend fun deleteByPack(packName: String)

    @Query("SELECT * FROM system_rules WHERE title = :title AND (sourcePack = :packName OR sourcePack IS NULL) LIMIT 1")
    suspend fun getRuleByPackAndTitle(packName: String, title: String): SystemRuleEntity?

    @Query("DELETE FROM system_rules WHERE gameSystem = :gameSystem AND (sourcePack IS NULL OR sourcePack = '')")
    suspend fun deleteOrphansByGameSystem(gameSystem: String)

    @Query("SELECT DISTINCT sourcePack FROM system_rules WHERE sourcePack IS NOT NULL AND sourcePack != ''")
    fun getDistinctSourcePacks(): Flow<List<String>>
}
