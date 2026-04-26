package com.deckapp.core.domain.repository

import com.deckapp.core.model.CardStack
import com.deckapp.core.model.DeckCollection
import com.deckapp.core.model.RandomTable
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun getAllCollections(): Flow<List<DeckCollection>>
    fun getCollectionById(id: Long): Flow<DeckCollection?>
    suspend fun saveCollection(collection: DeckCollection): Long
    suspend fun deleteCollection(id: Long)

    // Gestión de Recursos
    suspend fun addDeckToCollection(collectionId: Long, deckId: Long)
    suspend fun removeDeckFromCollection(collectionId: Long, deckId: Long)
    suspend fun addTableToCollection(collectionId: Long, tableId: Long)
    suspend fun removeTableFromCollection(collectionId: Long, tableId: Long)

    fun getDecksInCollection(collectionId: Long): Flow<List<CardStack>>
    fun getTablesInCollection(collectionId: Long): Flow<List<RandomTable>>
    
    fun searchCollections(query: String): Flow<List<DeckCollection>>
    suspend fun updateCollectionImage(collectionId: Long, imageUrl: String?)
}
