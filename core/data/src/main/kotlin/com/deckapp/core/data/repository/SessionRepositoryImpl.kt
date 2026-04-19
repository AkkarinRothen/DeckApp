package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val drawEventDao: DrawEventDao,
    private val randomTableDao: RandomTableDao
) : SessionRepository {

    override fun getAllSessions(): Flow<List<Session>> =
        sessionDao.getAllSessions().map { it.map { e -> e.toDomain() } }

    override fun getActiveSession(): Flow<Session?> =
        sessionDao.getActiveSession().map { it?.toDomain() }

    override fun getSessionsByStatus(status: SessionStatus): Flow<List<Session>> =
        sessionDao.getSessionsByStatus(status.name).map { it.map { e -> e.toDomain() } }

    override fun getSessionById(id: Long): Flow<Session?> =
        sessionDao.getSessionById(id).map { it?.toDomain() }

    override suspend fun createSession(session: Session): Long =
        sessionDao.insertSession(session.toEntity())

    override suspend fun updateSessionStatus(sessionId: Long, status: SessionStatus) =
        sessionDao.updateSessionStatus(sessionId, status.name)

    override suspend fun endSession(sessionId: Long) =
        sessionDao.endSession(sessionId, System.currentTimeMillis())

    override suspend fun deleteSession(sessionId: Long) =
        sessionDao.deleteSession(sessionId)

    override suspend fun renameSession(sessionId: Long, newName: String) =
        sessionDao.updateSessionName(sessionId, newName)

    override suspend fun updateDmNotes(sessionId: Long, notes: String) =
        sessionDao.updateDmNotes(sessionId, notes)

    override suspend fun toggleCardTitles(sessionId: Long, show: Boolean) =
        sessionDao.updateCardTitlesVisibility(sessionId, show)

    override fun getDecksForSession(sessionId: Long): Flow<List<SessionDeckRef>> =
        sessionDao.getDecksForSession(sessionId).map { it.map { e -> e.toDomain() } }

    override suspend fun addDeckToSession(ref: SessionDeckRef) =
        sessionDao.insertSessionDeckRef(ref.toEntity())

    override suspend fun removeDeckFromSession(sessionId: Long, stackId: Long) =
        sessionDao.removeSessionDeckRef(sessionId, stackId)

    override fun getTablesForSession(sessionId: Long): Flow<List<RandomTable>> =
        sessionDao.getTablesForSession(sessionId).map { refs ->
            refs.mapNotNull { ref ->
                randomTableDao.getTableWithEntries(ref.tableId)?.toDomain()
            }
        }

    override suspend fun addTableToSession(sessionId: Long, tableId: Long) =
        sessionDao.insertSessionTableRef(SessionTableRefEntity(sessionId, tableId))

    override suspend fun removeTableFromSession(sessionId: Long, tableId: Long) =
        sessionDao.removeSessionTableRef(sessionId, tableId)

    override suspend fun logEvent(event: DrawEvent) {
        drawEventDao.insertEvent(event.toEntity())
    }

    override fun getEventsForSession(sessionId: Long): Flow<List<DrawEvent>> =
        drawEventDao.getEventsForSession(sessionId).map { it.map { e -> e.toDomain() } }

    override suspend fun getLastEventForSession(sessionId: Long): DrawEvent? =
        drawEventDao.getLastEvent(sessionId)?.toDomain()

    override suspend fun deleteLastEvent(sessionId: Long) =
        drawEventDao.deleteLastEvent(sessionId)

    override suspend fun updateGameSystems(sessionId: Long, systems: List<String>) {
        sessionDao.updateGameSystems(sessionId, json.encodeToString(systems))
    }
}
