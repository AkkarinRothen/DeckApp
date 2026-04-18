package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.Npc
import javax.inject.Inject

class GetNpcByIdUseCase @Inject constructor(
    private val npcRepository: NpcRepository
) {
    suspend operator fun invoke(id: Long): Npc? {
        return npcRepository.getNpcById(id)
    }
}
