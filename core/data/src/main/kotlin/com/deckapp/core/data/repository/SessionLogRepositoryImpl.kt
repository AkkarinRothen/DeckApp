package com.deckapp.core.data.repository

import com.deckapp.core.data.db.SessionLogDao
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.SessionLogRepository
import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionNote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionLogRepositoryImpl @Inject constructor(
    private val sessionLogDao: SessionLogDao
) : SessionLogRepository {

    override fun getLogsForSession(sessionId: Long): Flow<List<SessionLogEntry>> =
        sessionLogDao.getLogsForSession(sessionId).map { list -> list.map { it.toDomain() } }

    override fun getLogsForScene(sessionId: Long, sceneId: Long): Flow<List<SessionLogEntry>> =
        sessionLogDao.getLogsForScene(sessionId, sceneId).map { list -> list.map { it.toDomain() } }

    override suspend fun logEvent(entry: SessionLogEntry): Long =
        sessionLogDao.insertLog(entry.toEntity())

    override suspend fun deleteLog(logId: Long) =
        sessionLogDao.deleteLog(logId)

    override suspend fun clearLogs(sessionId: Long) =
        sessionLogDao.clearLogsForSession(sessionId)

    override fun getAllNotesForSession(sessionId: Long): Flow<List<SessionNote>> =
        sessionLogDao.getAllNotesForSession(sessionId).map { list -> list.map { it.toDomain() } }

    override fun getNoteForScene(sessionId: Long, sceneId: Long?): Flow<SessionNote?> =
        sessionLogDao.getNoteForScene(sessionId, sceneId).map { it?.toDomain() }

    override suspend fun getNoteForSceneOnce(sessionId: Long, sceneId: Long?): SessionNote? =
        sessionLogDao.getNoteForSceneOnce(sessionId, sceneId)?.toDomain()

    override suspend fun saveNote(note: SessionNote) =
        sessionLogDao.upsertNote(note.toEntity())

    override suspend fun deleteNote(sessionId: Long, sceneId: Long?) =
        sessionLogDao.deleteNoteForScene(sessionId, sceneId)
}
