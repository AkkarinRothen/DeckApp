package com.deckapp.core.data.repository

import com.deckapp.core.data.db.*
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Collection
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val tagDao: TagDao,
    private val faceDao: CardFaceDao
) : CollectionRepository {

    override fun getAllCollections(): Flow<List<Collection>> =
        collectionDao.getAllCollectionsWithCount().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getCollectionById(id: Long): Flow<Collection?> =
        collectionDao.getCollectionById(id).map { it?.toDomain() }

    override suspend fun saveCollection(collection: Collection): Long {
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
}
