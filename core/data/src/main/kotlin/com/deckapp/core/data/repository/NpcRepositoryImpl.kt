package com.deckapp.core.data.repository

import android.net.Uri
import com.deckapp.core.data.db.NpcDao
import com.deckapp.core.data.db.NpcTagCrossRef
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.Npc
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NpcRepositoryImpl @Inject constructor(
    private val npcDao: NpcDao,
    private val fileRepository: FileRepository
) : NpcRepository {

    override fun getAllNpcs(): Flow<List<Npc>> {
        return npcDao.getAllNpcs().flatMapLatest { entities ->
            // En una app real, podríamos querer optimizar esto con un JOIN en el DAO para evitar N Flows
            // Pero por ahora, para mantener consistencia con el patrón de tags:
            kotlinx.coroutines.flow.flow {
                val results = entities.map { entity ->
                    // Por simplicidad en esta fase, no cargamos los tags en el listado masivo 
                    // a menos que sea necesario. Pero lo haremos para consistencia.
                    entity.toDomain() 
                }
                emit(results)
            }
        }
    }

    override suspend fun getNpcById(id: Long): Npc? {
        return npcDao.getNpcById(id)?.toDomain()
    }

    override suspend fun saveNpc(npc: Npc): Long {
        val npcId = npcDao.upsertNpc(npc.toEntity())
        
        // Actualizar Tags
        npcDao.deleteCrossRefsForNpc(npcId)
        npc.tags.forEach { tag ->
            npcDao.insertCrossRef(NpcTagCrossRef(npcId, tag.id))
        }
        
        return npcId
    }

    override suspend fun deleteNpc(id: Long) {
        npcDao.deleteNpc(id)
    }

    override suspend fun saveNpcAvatar(uri: Uri, npcId: Long): String? {
        return try {
            val fileName = "avatar_$npcId.jpg"
            fileRepository.copyImageToInternalByCategory(uri, "npcs", fileName)
        } catch (e: Exception) {
            null
        }
    }
}
