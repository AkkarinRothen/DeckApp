package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.model.MythicSession
import javax.inject.Inject

class CreateMythicSessionUseCase @Inject constructor(
    private val mythicRepository: MythicRepository
) {
    suspend operator fun invoke(name: String): Long {
        val session = MythicSession(name = name)
        return mythicRepository.saveSession(session)
    }
}
