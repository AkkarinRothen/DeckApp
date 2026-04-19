package com.deckapp.core.domain.repository

import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Session
import com.deckapp.core.model.SessionDeckRef
import com.deckapp.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getAllSessions(): Flow<List<Session>>
    fun getActiveSession(): Flow<Session?>
    fun getSessionsByStatus(status: SessionStatus): Flow<List<Session>>
    fun getSessionById(id: Long): Flow<Session?>
    suspend fun createSession(session: Session): Long
    suspend fun updateSessionStatus(sessionId: Long, status: SessionStatus)
    suspend fun endSession(sessionId: Long)
    suspend fun deleteSession(sessionId: Long)
    suspend fun renameSession(sessionId: Long, newName: String)
    suspend fun updateDmNotes(sessionId: Long, notes: String)

    /** Cambia el ajuste de visibilidad de títulos de cartas en la sesión. */
    suspend fun toggleCardTitles(sessionId: Long, show: Boolean)

    fun getDecksForSession(sessionId: Long): Flow<List<SessionDeckRef>>
    suspend fun addDeckToSession(ref: SessionDeckRef)
    suspend fun removeDeckFromSession(sessionId: Long, stackId: Long)

    fun getTablesForSession(sessionId: Long): Flow<List<RandomTable>>
    suspend fun addTableToSession(sessionId: Long, tableId: Long)
    suspend fun removeTableFromSession(sessionId: Long, tableId: Long)

    // Event log — append only
    suspend fun logEvent(event: DrawEvent)
    fun getEventsForSession(sessionId: Long): Flow<List<DrawEvent>>
    suspend fun getLastEventForSession(sessionId: Long): DrawEvent?
    suspend fun deleteLastEvent(sessionId: Long)  // para Undo

    suspend fun updateGameSystems(sessionId: Long, gameSystems: List<String>)
}
