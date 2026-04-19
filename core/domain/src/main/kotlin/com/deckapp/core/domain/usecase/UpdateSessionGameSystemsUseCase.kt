package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.SessionRepository
import javax.inject.Inject

class UpdateSessionGameSystemsUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, gameSystems: List<String>) {
        val systems = gameSystems.ifEmpty { listOf("General") }
        sessionRepository.updateGameSystems(sessionId, systems)
    }
}
