package com.deckapp.core.data.repository

import com.deckapp.core.data.db.WikiDao
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.WikiRepository
import com.deckapp.core.model.WikiCategory
import com.deckapp.core.model.WikiEntry
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WikiRepositoryImpl @Inject constructor(
    private val wikiDao: WikiDao,
    private val fileRepository: FileRepository
) : WikiRepository {

    override fun getCategories(): Flow<List<WikiCategory>> {
        return combine(
            wikiDao.getAllCategories(),
            wikiDao.getCategoryCounts()
        ) { categories, counts ->
            val countMap = counts.associate { it.categoryId to it.count }
            categories.map { it.toDomain(countMap[it.id] ?: 0) }
        }
    }

    override suspend fun saveCategory(category: WikiCategory): Long {
        return wikiDao.insertCategory(category.toEntity())
    }

    override fun getEntriesByCategory(categoryId: Long): Flow<List<WikiEntry>> {
        return wikiDao.getEntriesByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getEntryById(id: Long): WikiEntry? {
        return wikiDao.getEntryById(id)?.toDomain()
    }

    override fun searchEntries(query: String): Flow<List<WikiEntry>> {
        return wikiDao.searchEntries(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveEntry(entry: WikiEntry): Long {
        return wikiDao.insertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: WikiEntry) {
        wikiDao.deleteEntry(entry.toEntity())
    }

    override suspend fun saveEntryImage(uri: Uri, entryId: Long): String? {
        return try {
            val fileName = "wiki_$entryId.jpg"
            fileRepository.copyImageToInternalByCategory(uri, "wiki", fileName)
        } catch (e: Exception) {
            null
        }
    }
}
