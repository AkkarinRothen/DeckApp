package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.Npc
import javax.inject.Inject

class SaveNpcUseCase @Inject constructor(
    private val npcRepository: NpcRepository
) {
    suspend operator fun invoke(
        npc: Npc,
        imageUri: Uri? = null,
        voiceSampleFile: java.io.File? = null,
        voiceSampleUri: android.net.Uri? = null
    ): Long {
        var npcId = npcRepository.saveNpc(npc)
        
        var updatedNpc = npc.copy(id = npcId)
        var needsResave = false

        // Si hay una nueva imagen, guardarla
        if (imageUri != null) {
            val internalPath = npcRepository.saveNpcAvatar(imageUri, npcId)
            if (internalPath != null) {
                updatedNpc = updatedNpc.copy(imagePath = internalPath)
                needsResave = true
            }
        }

        // Si hay una nueva muestra de voz (Grabación)
        if (voiceSampleFile != null) {
            val internalPath = npcRepository.saveNpcVoiceSample(voiceSampleFile, npcId)
            if (internalPath != null) {
                updatedNpc = updatedNpc.copy(voiceSamplePath = internalPath)
                needsResave = true
            }
        }
        
        // Si hay una nueva muestra de voz (Archivo seleccionado)
        if (voiceSampleUri != null) {
            val internalPath = npcRepository.saveNpcVoiceSampleFromUri(voiceSampleUri, npcId)
            if (internalPath != null) {
                updatedNpc = updatedNpc.copy(voiceSamplePath = internalPath)
                needsResave = true
            }
        }
        
        if (needsResave) {
            npcId = npcRepository.saveNpc(updatedNpc)
        }
        
        return npcId
    }
}
