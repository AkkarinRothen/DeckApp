package com.deckapp.core.domain.repository

import com.deckapp.core.model.*
import kotlinx.coroutines.flow.Flow

interface MythicRepository {
    // Sessions
    fun getSessions(): Flow<List<MythicSession>>
    fun getSessionById(id: Long): Flow<MythicSession?>
    suspend fun saveSession(session: MythicSession): Long
    suspend fun deleteSession(id: Long)
    suspend fun updateChaosFactor(id: Long, chaosFactor: Int)
    suspend fun updateSceneNumber(id: Long, sceneNumber: Int)

    // Characters
    fun getCharacters(sessionId: Long): Flow<List<MythicCharacter>>
    suspend fun addCharacter(character: MythicCharacter): Long
    suspend fun deleteCharacter(id: Long)
    suspend fun updateCharacterNotes(id: Long, notes: String)

    // Threads
    fun getThreads(sessionId: Long): Flow<List<MythicThread>>
    suspend fun addThread(thread: MythicThread): Long
    suspend fun updateThreadStatus(id: Long, isResolved: Boolean)
    suspend fun deleteThread(id: Long)

    // Rolls
    fun getRolls(sessionId: Long): Flow<List<MythicRoll>>
    suspend fun saveRoll(roll: MythicRoll): Long
    suspend fun deleteRolls(sessionId: Long)
}
