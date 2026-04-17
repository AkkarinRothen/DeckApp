package com.deckapp.core.domain.repository

import com.deckapp.core.model.CardStack
import com.deckapp.core.model.Collection
import com.deckapp.core.model.RandomTable
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollections(): Flow<List<Collection>>
    fun getCollectionById(id: Long): Flow<Collection?>
    suspend fun saveCollection(collection: Collection): Long
    suspend fun deleteCollection(id: Long)

    // Gestión de Recursos
    suspend fun addDeckToCollection(collectionId: Long, deckId: Long)
    suspend fun removeDeckFromCollection(collectionId: Long, deckId: Long)
    suspend fun addTableToCollection(collectionId: Long, tableId: Long)
    suspend fun removeTableFromCollection(collectionId: Long, tableId: Long)

    fun getDecksInCollection(collectionId: Long): Flow<List<CardStack>>
    fun getTablesInCollection(collectionId: Long): Flow<List<RandomTable>>
}
