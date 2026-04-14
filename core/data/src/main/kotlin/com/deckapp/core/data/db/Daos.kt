package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStackDao {
    @Query("SELECT * FROM card_stacks ORDER BY createdAt DESC")
    fun getAllStacks(): Flow<List<CardStackEntity>>

    @Query("SELECT * FROM card_stacks WHERE id = :id")
    fun getStackById(id: Long): Flow<CardStackEntity?>

    @Query("SELECT * FROM card_stacks WHERE type = 'DECK' ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<CardStackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStack(stack: CardStackEntity): Long

    @Update
    suspend fun updateStack(stack: CardStackEntity)

    @Query("DELETE FROM card_stacks WHERE id = :id")
    suspend fun deleteStack(id: Long)
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

    @Query("UPDATE cards SET isDrawn = :isDrawn WHERE id = :cardId")
    suspend fun updateDrawnState(cardId: Long, isDrawn: Boolean)

    @Query("UPDATE cards SET currentRotation = :rotation WHERE id = :cardId")
    suspend fun updateRotation(cardId: Long, rotation: Int)

    @Query("UPDATE cards SET isReversed = :isReversed WHERE id = :cardId")
    suspend fun updateReversed(cardId: Long, isReversed: Boolean)

    @Query("UPDATE cards SET currentFaceIndex = :faceIndex WHERE id = :cardId")
    suspend fun updateFaceIndex(cardId: Long, faceIndex: Int)

    @Query("UPDATE cards SET isDrawn = 0 WHERE stackId = :deckId")
    suspend fun resetDeck(deckId: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE stackId = :stackId AND isDrawn = 0")
    fun getAvailableCount(stackId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM cards WHERE stackId = :stackId")
    fun getTotalCardCount(stackId: Long): Flow<Int>

    /** Devuelve la primera carta disponible del mazo (sin modificar su estado). Para Peek. */
    @Query("SELECT * FROM cards WHERE stackId = :stackId AND isDrawn = 0 ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getTopCard(stackId: Long): CardEntity?

    @Query("SELECT * FROM cards WHERE isDrawn = 1 ORDER BY id ASC")
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
                   WHERE de2.sessionId = :sessionId AND de2.action = 'RESET'),
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTagRef(ref: CardTagCrossRef)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun deleteTagsForCard(cardId: Long)
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

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("UPDATE sessions SET name = :name WHERE id = :sessionId")
    suspend fun updateSessionName(sessionId: Long, name: String)

    @Query("UPDATE sessions SET showCardTitles = :show WHERE id = :sessionId")
    suspend fun updateCardTitlesVisibility(sessionId: Long, show: Boolean)

    @Query("UPDATE sessions SET dmNotes = :notes WHERE id = :sessionId")
    suspend fun updateDmNotes(sessionId: Long, notes: String)
}

@Dao
interface RandomTableDao {
    @Query("SELECT * FROM random_tables ORDER BY name ASC")
    fun getAllTables(): Flow<List<RandomTableEntity>>

    @Query("SELECT * FROM random_tables WHERE category = :category ORDER BY name ASC")
    fun getTablesByCategory(category: String): Flow<List<RandomTableEntity>>

    @Query("SELECT DISTINCT category FROM random_tables WHERE category != '' ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

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
