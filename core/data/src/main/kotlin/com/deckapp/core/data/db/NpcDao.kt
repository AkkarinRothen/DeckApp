package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NpcDao {
    @Query("SELECT * FROM npcs ORDER BY name ASC")
    fun getAllNpcs(): Flow<List<NpcEntity>>

    @Query("SELECT * FROM npcs WHERE id = :npcId")
    suspend fun getNpcById(npcId: Long): NpcEntity?

    @Query("SELECT * FROM npcs WHERE name = :name LIMIT 1")
    suspend fun getNpcByName(name: String): NpcEntity?

    @Query("""
        SELECT npcs.* FROM npcs 
        INNER JOIN npc_tags ON npcs.id = npc_tags.npcId 
        INNER JOIN tags ON tags.id = npc_tags.tagId 
        WHERE tags.name = :tagName
    """)
    suspend fun getNpcsByTagName(tagName: String): List<NpcEntity>

    @Query("""
        DELETE FROM npcs WHERE id IN (
            SELECT npcs.id FROM npcs 
            INNER JOIN npc_tags ON npcs.id = npc_tags.npcId 
            INNER JOIN tags ON tags.id = npc_tags.tagId 
            WHERE tags.name = :tagName
        )
    """)
    suspend fun deleteNpcsByTagName(tagName: String)

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
