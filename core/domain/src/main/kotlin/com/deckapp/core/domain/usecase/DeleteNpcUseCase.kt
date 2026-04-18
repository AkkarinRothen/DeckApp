package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.NpcRepository
import javax.inject.Inject

class DeleteNpcUseCase @Inject constructor(
    private val npcRepository: NpcRepository
) {
    suspend operator fun invoke(npcId: Long) {
        npcRepository.deleteNpc(npcId)
    }
}
