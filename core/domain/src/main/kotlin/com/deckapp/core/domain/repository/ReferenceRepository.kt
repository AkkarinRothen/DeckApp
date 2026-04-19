package com.deckapp.core.domain.repository

import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.SystemRule
import kotlinx.coroutines.flow.Flow

interface ReferenceRepository {
    fun getAllReferenceTables(): Flow<List<ReferenceTable>>
    fun getTablesBySystem(system: String): Flow<List<ReferenceTable>>
    suspend fun getReferenceTableWithRows(id: Long): ReferenceTable?
    fun searchReferenceTables(query: String): Flow<List<ReferenceTable>>
    suspend fun saveReferenceTable(table: ReferenceTable): Long
    suspend fun deleteReferenceTable(tableId: Long)
    suspend fun updateTablePinned(id: Long, pinned: Boolean)

    fun getAllSystemRules(): Flow<List<SystemRule>>
    fun getRulesBySystem(system: String): Flow<List<SystemRule>>
    fun searchSystemRules(query: String): Flow<List<SystemRule>>
    suspend fun getRuleById(id: Long): SystemRule?
    suspend fun saveSystemRule(rule: SystemRule): Long
    suspend fun deleteSystemRule(ruleId: Long)
    suspend fun updateRulePinned(id: Long, pinned: Boolean)

    fun getDistinctSystems(): Flow<List<String>>

    suspend fun addTagToReferenceTable(tableId: Long, tagId: Long)
    suspend fun removeTagFromReferenceTable(tableId: Long, tagId: Long)
    suspend fun addTagToSystemRule(ruleId: Long, tagId: Long)
    suspend fun removeTagFromSystemRule(ruleId: Long, tagId: Long)

    suspend fun importStarterPack(backup: com.deckapp.core.model.backup.FullBackupDto, packName: String)
    suspend fun removeStarterPack(packName: String)
    fun getInstalledPackNames(): Flow<Set<String>>
}
