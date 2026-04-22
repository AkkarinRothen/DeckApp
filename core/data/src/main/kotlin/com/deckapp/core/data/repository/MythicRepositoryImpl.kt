package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MythicRepositoryImpl @Inject constructor(
    private val mythicDao: MythicDao
) : MythicRepository {

    override fun getSessions(): Flow<List<MythicSession>> =
        mythicDao.getSessions().map { entities -> entities.map { it.toDomain() } }

    override fun getSessionById(id: Long): Flow<MythicSession?> =
        mythicDao.getSessionById(id).map { it?.toDomain() }

    override suspend fun saveSession(session: MythicSession): Long =
        mythicDao.insertSession(session.toEntity())

    override suspend fun deleteSession(id: Long) = mythicDao.deleteSession(id)

    override suspend fun updateChaosFactor(id: Long, chaosFactor: Int) =
        mythicDao.updateChaosFactor(id, chaosFactor)

    override suspend fun updateSceneNumber(id: Long, sceneNumber: Int) =
        mythicDao.updateSceneNumber(id, sceneNumber)

    override fun getCharacters(sessionId: Long): Flow<List<MythicCharacter>> =
        mythicDao.getCharacters(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addCharacter(character: MythicCharacter): Long =
        mythicDao.insertCharacter(character.toEntity())

    override suspend fun deleteCharacter(id: Long) = mythicDao.deleteCharacter(id)

    override suspend fun updateCharacterNotes(id: Long, notes: String) =
        mythicDao.updateCharacterNotes(id, notes)

    override fun getThreads(sessionId: Long): Flow<List<MythicThread>> =
        mythicDao.getThreads(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun addThread(thread: MythicThread): Long =
        mythicDao.insertThread(thread.toEntity())

    override suspend fun updateThreadStatus(id: Long, isResolved: Boolean) =
        mythicDao.updateThreadStatus(id, isResolved)

    override suspend fun deleteThread(id: Long) = mythicDao.deleteThread(id)

    override fun getRolls(sessionId: Long): Flow<List<MythicRoll>> =
        mythicDao.getRolls(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveRoll(roll: MythicRoll): Long =
        mythicDao.insertRoll(roll.toEntity())

    override suspend fun deleteRolls(sessionId: Long) = mythicDao.deleteRolls(sessionId)
}
