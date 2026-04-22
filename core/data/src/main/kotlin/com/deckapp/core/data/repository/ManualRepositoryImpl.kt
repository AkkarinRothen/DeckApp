package com.deckapp.core.data.repository

import com.deckapp.core.data.db.ManualDao
import com.deckapp.core.data.db.ManualEntity
import com.deckapp.core.data.db.ManualBookmarkEntity
import com.deckapp.core.domain.repository.ManualRepository
import com.deckapp.core.model.Manual
import com.deckapp.core.model.ManualBookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ManualRepositoryImpl @Inject constructor(
    private val manualDao: ManualDao
) : ManualRepository {

    override fun getAllManuals(): Flow<List<Manual>> =
        manualDao.getAllManuals().map { entities -> entities.map { it.toDomain() } }

    override fun getManualById(id: Long): Flow<Manual?> =
        manualDao.getManualById(id).map { it?.toDomain() }

    override suspend fun saveManual(manual: Manual): Long =
        manualDao.insertManual(manual.toEntity())

    override suspend fun updateLastOpened(id: Long, timestamp: Long) =
        manualDao.updateLastOpened(id, timestamp)

    override suspend fun deleteManual(manual: Manual) =
        manualDao.deleteManual(manual.toEntity())

    override fun getDistinctSystems(): Flow<List<String>> =
        manualDao.getDistinctSystems()

    // Bookmarks
    override fun getBookmarksForManual(manualId: Long): Flow<List<ManualBookmark>> =
        manualDao.getBookmarksForManual(manualId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveBookmark(bookmark: ManualBookmark): Long =
        manualDao.insertBookmark(bookmark.toEntity())

    override suspend fun deleteBookmark(id: Long) =
        manualDao.deleteBookmark(id)

    override suspend fun deleteBookmarkAtPage(manualId: Long, pageIndex: Int) =
        manualDao.deleteBookmarkAtPage(manualId, pageIndex)

    private fun ManualEntity.toDomain() = Manual(
        id = id,
        title = title,
        uri = uri,
        gameSystem = gameSystem,
        fileName = fileName,
        fileSize = fileSize,
        lastOpened = lastOpened,
        createdAt = createdAt
    )

    private fun Manual.toEntity() = ManualEntity(
        id = id,
        title = title,
        uri = uri,
        gameSystem = gameSystem,
        fileName = fileName,
        fileSize = fileSize,
        lastOpened = lastOpened,
        createdAt = createdAt
    )

    private fun ManualBookmarkEntity.toDomain() = ManualBookmark(
        id = id,
        manualId = manualId,
        pageIndex = pageIndex,
        label = label,
        createdAt = createdAt
    )

    private fun ManualBookmark.toEntity() = ManualBookmarkEntity(
        id = id,
        manualId = manualId,
        pageIndex = pageIndex,
        label = label,
        createdAt = createdAt
    )
}
