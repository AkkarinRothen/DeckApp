package com.deckapp.core.domain.repository

import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionNote
import kotlinx.coroutines.flow.Flow

interface SessionLogRepository {
    fun getLogsForSession(sessionId: Long): Flow<List<SessionLogEntry>>
    fun getLogsForScene(sessionId: Long, sceneId: Long): Flow<List<SessionLogEntry>>
    suspend fun logEvent(entry: SessionLogEntry): Long
    suspend fun deleteLog(logId: Long)
    suspend fun clearLogs(sessionId: Long)

    fun getAllNotesForSession(sessionId: Long): Flow<List<SessionNote>>
    fun getNoteForScene(sessionId: Long, sceneId: Long?): Flow<SessionNote?>
    suspend fun getNoteForSceneOnce(sessionId: Long, sceneId: Long?): SessionNote?
    suspend fun saveNote(note: SessionNote)
    suspend fun deleteNote(sessionId: Long, sceneId: Long?)
}
