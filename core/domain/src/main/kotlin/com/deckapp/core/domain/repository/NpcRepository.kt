package com.deckapp.core.domain.repository

import com.deckapp.core.model.Npc
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow

interface NpcRepository {
    fun getAllNpcs(): Flow<List<Npc>>
    suspend fun getNpcById(id: Long): Npc?
    suspend fun saveNpc(npc: Npc): Long
    suspend fun deleteNpc(id: Long)
    
    // Almacenamiento de imagen
    suspend fun saveNpcAvatar(uri: android.net.Uri, npcId: Long): String?
}
