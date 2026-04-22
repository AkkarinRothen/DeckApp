package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.domain.repository.MythicRepository
import javax.inject.Inject

class DeleteMythicSessionUseCase @Inject constructor(
    private val mythicRepository: MythicRepository
) {
    suspend operator fun invoke(id: Long) = mythicRepository.deleteSession(id)
}
