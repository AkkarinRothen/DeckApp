package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.model.MythicSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMythicSessionsUseCase @Inject constructor(
    private val mythicRepository: MythicRepository
) {
    operator fun invoke(): Flow<List<MythicSession>> = mythicRepository.getSessions()
}
