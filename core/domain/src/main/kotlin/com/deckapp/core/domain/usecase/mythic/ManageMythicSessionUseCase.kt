package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread
import javax.inject.Inject

class ManageMythicSessionUseCase @Inject constructor(
    private val mythicRepository: MythicRepository
) {
    suspend fun updateChaosFactor(id: Long, chaosFactor: Int) =
        mythicRepository.updateChaosFactor(id, chaosFactor)

    suspend fun updateSceneNumber(id: Long, sceneNumber: Int) =
        mythicRepository.updateSceneNumber(id, sceneNumber)

    suspend fun saveSession(session: com.deckapp.core.model.MythicSession) =
        mythicRepository.saveSession(session)

    // Characters
    suspend fun addCharacter(sessionId: Long, name: String): Long =
        mythicRepository.addCharacter(MythicCharacter(sessionId = sessionId, name = name))

    suspend fun deleteCharacter(id: Long) = mythicRepository.deleteCharacter(id)

    suspend fun updateCharacterNotes(id: Long, notes: String) =
        mythicRepository.updateCharacterNotes(id, notes)

    // Threads
    suspend fun addThread(sessionId: Long, description: String): Long =
        mythicRepository.addThread(MythicThread(sessionId = sessionId, description = description))

    suspend fun updateThreadStatus(id: Long, isResolved: Boolean) =
        mythicRepository.updateThreadStatus(id, isResolved)

    suspend fun deleteThread(id: Long) = mythicRepository.deleteThread(id)
}
