package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.DeckCollection
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val tagDao: TagDao,
    private val searchDao: SearchDao
) : CollectionRepository {

    override fun getAllCollections(): Flow<List<DeckCollection>> =
        collectionDao.getAllCollectionsWithCount().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCollectionById(id: Long): Flow<DeckCollection?> =
        collectionDao.getCollectionById(id).map { it?.toDomain() }

    override suspend fun saveCollection(collection: DeckCollection): Long {
        return collectionDao.insertCollection(collection.toEntity())
    }

    override suspend fun deleteCollection(id: Long) =
        collectionDao.deleteCollection(id)

    override suspend fun addDeckToCollection(collectionId: Long, deckId: Long) {
        collectionDao.insertResourceRef(CollectionResourceCrossRef(collectionId, deckId, "DECK"))
    }

    override suspend fun removeDeckFromCollection(collectionId: Long, deckId: Long) {
        collectionDao.removeResourceFromCollection(collectionId, deckId, "DECK")
    }

    override suspend fun addTableToCollection(collectionId: Long, tableId: Long) {
        collectionDao.insertResourceRef(CollectionResourceCrossRef(collectionId, tableId, "TABLE"))
    }

    override suspend fun removeTableFromCollection(collectionId: Long, tableId: Long) {
        collectionDao.removeResourceFromCollection(collectionId, tableId, "TABLE")
    }

    override fun getDecksInCollection(collectionId: Long): Flow<List<CardStack>> =
        collectionDao.getDecksInCollection(collectionId).map { entities ->
            entities.map { entity ->
                val tags = tagDao.getTagsForStack(entity.id).map { it.toDomain() }
                entity.toDomain(tags)
            }
        }

    override fun getTablesInCollection(collectionId: Long): Flow<List<RandomTable>> =
        collectionDao.getTablesInCollection(collectionId).map { entities ->
            entities.map { entity ->
                val tags = tagDao.getTagsForTable(entity.id).map { it.toDomain() }
                entity.toDomain(tags = tags)
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun searchCollections(query: String): Flow<List<DeckCollection>> {
        if (query.isBlank()) return flowOf(emptyList())
        val ftsQuery = "$query*"
        
        return searchDao.searchCollectionIds(ftsQuery).flatMapLatest { ids ->
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    ids.map { getCollectionById(it.rowid) }
                ) { collections: Array<DeckCollection?> -> 
                    collections.filterNotNull()
                }
            }
        }
    }

    override suspend fun updateCollectionImage(collectionId: Long, imageUrl: String?) {
        collectionDao.updateCollectionImage(collectionId, imageUrl)
    }
}
