package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NpcDao {
    @Query("SELECT * FROM npcs ORDER BY name ASC")
    fun getAllNpcs(): Flow<List<NpcEntity>>

    @Query("SELECT * FROM npcs WHERE id = :npcId")
    suspend fun getNpcById(npcId: Long): NpcEntity?

    @Upsert
    suspend fun upsertNpc(npc: NpcEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNpcs(npcs: List<NpcEntity>)

    @Query("DELETE FROM npcs WHERE id = :npcId")
    suspend fun deleteNpc(npcId: Long)

    // --- Tags ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: NpcTagCrossRef)

    @Query("DELETE FROM npc_tags WHERE npcId = :npcId")
    suspend fun deleteCrossRefsForNpc(npcId: Long)

    @Transaction
    @Query("""
        SELECT tags.* FROM tags 
        INNER JOIN npc_tags ON tags.id = npc_tags.tagId 
        WHERE npc_tags.npcId = :npcId
    """)
    fun getTagsForNpc(npcId: Long): Flow<List<TagEntity>>
}
