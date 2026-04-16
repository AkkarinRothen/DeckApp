package com.deckapp.core.data.repository

import android.net.Uri
import com.deckapp.core.data.db.RecentFileDao
import com.deckapp.core.data.db.RecentFileRecord
import com.deckapp.core.domain.repository.RecentFile
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.RecentFileType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecentFileRepositoryImpl @Inject constructor(
    private val recentFileDao: RecentFileDao
) : RecentFileRepository {

    override fun getRecentFiles(limit: Int): Flow<List<RecentFile>> {
        return recentFileDao.getRecentFiles(limit).map { records ->
            records.map { it.toDomain() }
        }
    }

    override suspend fun addRecentFile(uri: Uri, name: String, type: RecentFileType) {
        val uriString = uri.toString()
        val existing = recentFileDao.getRecordByUri(uriString)
        
        if (existing != null) {
            recentFileDao.updateLastAccessed(uriString, System.currentTimeMillis())
        } else {
            recentFileDao.insertRecord(
                RecentFileRecord(
                    uri = uriString,
                    name = name,
                    type = type.name,
                    lastAccessed = System.currentTimeMillis()
                )
            )
        }
        
        // Mantener el historial limpio (limite de 20 por seguridad técnica, aunque pidamos 10)
        recentFileDao.pruneOldRecords(20)
    }

    override suspend fun removeRecentFile(uri: Uri) {
        recentFileDao.deleteRecord(uri.toString())
    }

    private fun RecentFileRecord.toDomain(): RecentFile {
        return RecentFile(
            uri = Uri.parse(uri),
            name = name,
            type = RecentFileType.valueOf(type),
            lastAccessed = lastAccessed
        )
    }
}
