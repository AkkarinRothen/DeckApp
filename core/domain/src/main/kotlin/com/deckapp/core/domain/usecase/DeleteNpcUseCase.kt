package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.NpcRepository
import javax.inject.Inject

class DeleteNpcUseCase @Inject constructor(
    private val npcRepository: NpcRepository,
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(npcId: Long) {
        val npc = npcRepository.getNpcById(npcId)
        npc?.imagePath?.let { path ->
            fileRepository.deleteFile(path)
        }
        npcRepository.deleteNpc(npcId)
    }
}
