package com.deckapp.core.data.db.backup

import androidx.room.Dao
import androidx.room.Query
import com.deckapp.core.data.db.*

@Dao
interface BackupDao {
    @Query("SELECT * FROM tags") suspend fun getAllTags(): List<TagEntity>
    @Query("SELECT * FROM card_stacks") suspend fun getAllDecks(): List<CardStackEntity>
    @Query("SELECT * FROM cards") suspend fun getAllCards(): List<CardEntity>
    @Query("SELECT * FROM card_faces") suspend fun getAllCardFaces(): List<CardFaceEntity>
    
    @Query("SELECT * FROM card_stack_tags") suspend fun getAllCardStackTags(): List<CardStackTagCrossRef>
    @Query("SELECT * FROM card_tags") suspend fun getAllCardTags(): List<CardTagCrossRef>
    @Query("SELECT * FROM random_table_tags") suspend fun getAllRandomTableTags(): List<RandomTableTagCrossRef>

    @Query("SELECT * FROM table_bundles") suspend fun getAllTableBundles(): List<TableBundleEntity>
    @Query("SELECT * FROM random_tables") suspend fun getAllRandomTables(): List<RandomTableEntity>
    @Query("SELECT * FROM table_entries") suspend fun getAllTableEntries(): List<TableEntryEntity>

    @Query("SELECT * FROM sessions") suspend fun getAllSessions(): List<SessionEntity>
    @Query("SELECT * FROM session_deck_refs") suspend fun getAllSessionDeckRefs(): List<SessionDeckRefEntity>
    @Query("SELECT * FROM session_table_refs") suspend fun getAllSessionTableRefs(): List<SessionTableRefEntity>
    @Query("SELECT * FROM draw_events") suspend fun getAllDrawEvents(): List<DrawEventEntity>

    @Query("SELECT * FROM encounters") suspend fun getAllEncounters(): List<EncounterEntity>
    @Query("SELECT * FROM encounter_creatures") suspend fun getAllEncounterCreatures(): List<EncounterCreatureEntity>
    @Query("SELECT * FROM combat_log") suspend fun getAllCombatLogEntries(): List<CombatLogEntryEntity>

    @Query("SELECT * FROM npcs") suspend fun getAllNpcs(): List<NpcEntity>
    @Query("SELECT * FROM npc_tags") suspend fun getAllNpcTags(): List<NpcTagCrossRef>

    @Query("SELECT * FROM wiki_categories") suspend fun getAllWikiCategories(): List<WikiCategoryEntity>
    @Query("SELECT * FROM wiki_entries") suspend fun getAllWikiEntries(): List<WikiEntryEntity>
}
