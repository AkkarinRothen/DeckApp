package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.Npc
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNpcsUseCase @Inject constructor(
    private val npcRepository: NpcRepository
) {
    operator fun invoke(): Flow<List<Npc>> {
        return npcRepository.getAllNpcs()
    }
}
