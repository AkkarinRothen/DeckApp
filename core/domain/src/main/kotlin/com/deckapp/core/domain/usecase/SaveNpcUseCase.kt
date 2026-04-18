package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.Npc
import javax.inject.Inject

class SaveNpcUseCase @Inject constructor(
    private val npcRepository: NpcRepository
) {
    suspend operator fun invoke(npc: Npc, imageUri: Uri? = null): Long {
        var npcId = npcRepository.saveNpc(npc)
        
        // Si hay una nueva imagen, guardarla y actualizar el NPC
        if (imageUri != null) {
            val internalPath = npcRepository.saveNpcAvatar(imageUri, npcId)
            if (internalPath != null) {
                npcId = npcRepository.saveNpc(npc.copy(id = npcId, imagePath = internalPath))
            }
        }
        
        return npcId
    }
}
