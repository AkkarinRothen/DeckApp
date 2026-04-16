package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStackDao {
    @Query("SELECT * FROM card_stacks ORDER BY createdAt DESC")
    fun getAllStacks(): Flow<List<CardStackEntity>>

    @Query("SELECT * FROM card_stacks WHERE id = :id")
    fun getStackById(id: Long): Flow<CardStackEntity?>

    @Query("SELECT * FROM card_stacks WHERE type = 'DECK' AND isArchived = 0 ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<CardStackEntity>>

    @Query("SELECT * FROM card_stacks WHERE type = 'DECK' AND isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedDecks(): Flow<List<CardStackEntity>>

    @Query("UPDATE card_stacks SET isArchived = :archived WHERE id = :deckId")
    suspend fun setArchived(deckId: Long, archived: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStack(stack: CardStackEntity): Long

    @Update
    suspend fun updateStack(stack: CardStackEntity)

    @Query("DELETE FROM card_stacks WHERE id = :id")
    suspend fun deleteStack(id: Long)

    @Query("UPDATE card_stacks SET isArchived = :archived WHERE id IN (:deckIds)")
    suspend fun bulkSetArchived(deckIds: List<Long>, archived: Boolean)

    @Query("DELETE FROM card_stacks WHERE id IN (:deckIds)")
    suspend fun bulkDeleteStacks(deckIds: List<Long>)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE stackId = :stackId ORDER BY sortOrder ASC")
    fun getCardsForStack(stackId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardById(id: Long): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE stackId = :stackId AND isDrawn = 0 ORDER BY sortOrder ASC")
    suspend fun getAvailableCards(stackId: Long): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCard(id: Long)

    @Query("UPDATE cards SET isDrawn = :isDrawn, last_drawn_at = :lastDrawnAt WHERE id = :cardId")
    suspend fun updateDrawnState(cardId: Long, isDrawn: Boolean, lastDrawnAt: Long?)

    @Query("UPDATE cards SET currentRotation = :rotation WHERE id = :cardId")
    suspend fun updateRotation(cardId: Long, rotation: Int)

    @Query("UPDATE cards SET isReversed = :isReversed WHERE id = :cardId")
    suspend fun updateReversed(cardId: Long, isReversed: Boolean)

    @Query("UPDATE cards SET currentFaceIndex = :faceIndex WHERE id = :cardId")
    suspend fun updateFaceIndex(cardId: Long, faceIndex: Int)

    @Query("UPDATE cards SET isRevealed = :isRevealed WHERE id = :cardId")
    suspend fun updateRevealed(cardId: Long, isRevealed: Boolean)

    @Query("UPDATE cards SET dm_notes = :notes WHERE id = :cardId")
    suspend fun updateDmNotes(cardId: Long, notes: String?)

    @Query("UPDATE cards SET isDrawn = 0, last_drawn_at = NULL WHERE stackId = :deckId")
    suspend fun resetDeck(deckId: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE stackId = :stackId AND isDrawn = 0")
    fun getAvailableCount(stackId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE stackId = :stackId")
    fun getTotalCardCount(stackId: Long): Flow<Int>

    /** Devuelve la primera carta disponible del mazo (sin modificar su estado). Para Peek. */
    @Query("SELECT * FROM cards WHERE stackId = :stackId AND isDrawn = 0 ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getTopCard(stackId: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE isDrawn = 1 ORDER BY last_drawn_at ASC")
    fun getDrawnCards(): Flow<List<CardEntity>>

    /**
     * Cartas en la pila de descarte de la sesión.
     * = cartas con un evento DISCARD en esta sesión, después del último RESET,
     *   que actualmente no están en mano (isDrawn = 0).
     */
    @Query("""
        SELECT DISTINCT c.* FROM cards c
        WHERE c.id IN (
            SELECT de.cardId FROM draw_events de
            WHERE de.sessionId = :sessionId
              AND de.action = 'DISCARD'
              AND de.timestamp > COALESCE(
                  (SELECT MAX(de2.timestamp) FROM draw_events de2
                   WHERE de2.sessionId = :sessionId 
                     AND de2.action IN ('RESET', 'SHUFFLE_BACK')),
                  0
              )
        )
        AND c.isDrawn = 0
        ORDER BY c.id ASC
    """)
    fun getPiledCards(sessionId: Long): Flow<List<CardEntity>>
}

@Dao
interface CardFaceDao {
    @Query("SELECT * FROM card_faces WHERE cardId = :cardId ORDER BY faceIndex ASC")
    fun getFacesForCard(cardId: Long): Flow<List<CardFaceEntity>>

    @Query("SELECT * FROM card_faces WHERE cardId = :cardId ORDER BY faceIndex ASC")
    suspend fun getFacesForCardSync(cardId: Long): List<CardFaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFace(face: CardFaceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaces(faces: List<CardFaceEntity>)

    @Query("DELETE FROM card_faces WHERE cardId = :cardId")
    suspend fun deleteFacesForCard(cardId: Long)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN card_stack_tags st ON t.id = st.tagId WHERE st.stackId = :stackId")
    suspend fun getTagsForStack(stackId: Long): List<TagEntity>

    @Query("SELECT t.* FROM tags t INNER JOIN card_tags ct ON t.id = ct.tagId WHERE ct.cardId = :cardId")
    suspend fun getTagsForCard(cardId: Long): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStackTagRef(ref: CardStackTagCrossRef)

    @Query("DELETE FROM card_stack_tags WHERE stackId = :stackId")
    suspend fun deleteTagsForStack(stackId: Long)

    @Query("DELETE FROM card_stack_tags WHERE stackId = :stackId AND tagId = :tagId")
    suspend fun removeStackTagRef(stackId: Long, tagId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTagRef(ref: CardTagCrossRef)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun deleteTagsForCard(cardId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableTagRef(ref: RandomTableTagCrossRef)

    @Query("DELETE FROM random_table_tags WHERE tableId = :tableId")
    suspend fun deleteTagsForTable(tableId: Long)

    @Query("DELETE FROM random_table_tags WHERE tableId = :tableId AND tagId = :tagId")
    suspend fun removeTableTagRef(tableId: Long, tagId: Long)

    @Query("SELECT t.* FROM tags t INNER JOIN random_table_tags rt ON t.id = rt.tagId WHERE rt.tableId = :tableId")
    suspend fun getTagsForTable(tableId: Long): List<TagEntity>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<SessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("UPDATE sessions SET isActive = 0, endedAt = :endedAt WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endedAt: Long)

    @Query("SELECT * FROM session_deck_refs WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    fun getDecksForSession(sessionId: Long): Flow<List<SessionDeckRefEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionDeckRef(ref: SessionDeckRefEntity)

    @Query("DELETE FROM session_deck_refs WHERE sessionId = :sessionId AND stackId = :stackId")
    suspend fun removeSessionDeckRef(sessionId: Long, stackId: Long)

    @Query("SELECT * FROM session_table_refs WHERE sessionId = :sessionId ORDER BY sortOrder ASC")
    fun getTablesForSession(sessionId: Long): Flow<List<SessionTableRefEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionTableRef(ref: SessionTableRefEntity)

    @Query("DELETE FROM session_table_refs WHERE sessionId = :sessionId AND tableId = :tableId")
    suspend fun removeSessionTableRef(sessionId: Long, tableId: Long)

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("UPDATE sessions SET name = :name WHERE id = :sessionId")
    suspend fun updateSessionName(sessionId: Long, name: String)

    @Query("UPDATE sessions SET showCardTitles = :show WHERE id = :sessionId")
    suspend fun updateCardTitlesVisibility(sessionId: Long, show: Boolean)

    @Query("UPDATE sessions SET dm_notes = :notes WHERE id = :sessionId")
    suspend fun updateDmNotes(sessionId: Long, notes: String)
}

@Dao
interface RandomTableDao {
    @Query("SELECT * FROM random_tables ORDER BY name ASC")
    fun getAllTables(): Flow<List<RandomTableEntity>>

    @Transaction
    @Query("SELECT * FROM random_tables WHERE id = :id")
    suspend fun getTableWithEntries(id: Long): TableWithEntries?

    @Transaction
    @Query("SELECT * FROM random_tables ORDER BY name ASC")
    fun getAllTablesWithEntries(): Flow<List<TableWithEntries>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RandomTableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<TableEntryEntity>)

    @Update
    suspend fun updateTable(table: RandomTableEntity)

    @Query("DELETE FROM random_tables WHERE id = :id")
    suspend fun deleteTable(id: Long)

    @Query("UPDATE random_tables SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinnedState(id: Long, isPinned: Boolean)

    @Query("UPDATE random_tables SET isPinned = :isPinned WHERE id IN (:ids)")
    suspend fun bulkUpdatePinnedState(ids: List<Long>, isPinned: Boolean)

    @Query("DELETE FROM random_tables WHERE id IN (:ids)")
    suspend fun bulkDeleteTables(ids: List<Long>)

    @Query("DELETE FROM table_entries WHERE tableId = :tableId")
    suspend fun deleteEntriesForTable(tableId: Long)

    @Query("SELECT COUNT(*) FROM random_tables WHERE isBuiltIn = 1")
    suspend fun countBuiltInTables(): Int

    @Query("SELECT * FROM random_tables WHERE name = :name LIMIT 1")
    suspend fun getTableByName(name: String): RandomTableEntity?
}

@Dao
interface TableRollResultDao {
    @Query("SELECT * FROM table_roll_results WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getResultsForSession(sessionId: Long): Flow<List<TableRollResultEntity>>

    @Query("SELECT * FROM table_roll_results WHERE sessionId = :sessionId AND tableId = :tableId ORDER BY timestamp DESC LIMIT 5")
    fun getRecentResultsForTable(sessionId: Long, tableId: Long): Flow<List<TableRollResultEntity>>

    @Insert
    suspend fun insertResult(result: TableRollResultEntity): Long

    @Query("DELETE FROM table_roll_results WHERE sessionId = :sessionId")
    suspend fun clearSessionHistory(sessionId: Long)
}

@Dao
interface DrawEventDao {
    @Insert
    suspend fun insertEvent(event: DrawEventEntity): Long

    @Query("SELECT * FROM draw_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<DrawEventEntity>>

    @Query("SELECT * FROM draw_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastEvent(sessionId: Long): DrawEventEntity?

    @Query("DELETE FROM draw_events WHERE id = (SELECT id FROM draw_events WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLastEvent(sessionId: Long)
}

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastAccessed DESC LIMIT :limit")
    fun getRecentFiles(limit: Int): Flow<List<RecentFileRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecentFileRecord)

    @Query("UPDATE recent_files SET lastAccessed = :timestamp WHERE uri = :uri")
    suspend fun updateLastAccessed(uri: String, timestamp: Long)

    @Query("SELECT * FROM recent_files WHERE uri = :uri LIMIT 1")
    suspend fun getRecordByUri(uri: String): RecentFileRecord?

    @Query("DELETE FROM recent_files WHERE uri = :uri")
    suspend fun deleteRecord(uri: String)

    /** Mantiene limpia la tabla eliminando los registros más antiguos que excedan el límite. */
    @Query("""
        DELETE FROM recent_files WHERE id NOT IN (
            SELECT id FROM recent_files ORDER BY lastAccessed DESC LIMIT :limit
        )
    """)
    suspend fun pruneOldRecords(limit: Int)
}
