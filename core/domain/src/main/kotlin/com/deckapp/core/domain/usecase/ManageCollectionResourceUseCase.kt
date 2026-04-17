package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.model.SearchResultType
import javax.inject.Inject

class ManageCollectionResourceUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    suspend fun add(collectionId: Long, resourceId: Long, type: SearchResultType) {
        when (type) {
            SearchResultType.CARD -> repository.addDeckToCollection(collectionId, resourceId) // En colecciones guardamos el mazo
            SearchResultType.DECK -> repository.addDeckToCollection(collectionId, resourceId)
            SearchResultType.TABLE -> repository.addTableToCollection(collectionId, resourceId)
            else -> {}
        }
    }

    suspend fun remove(collectionId: Long, resourceId: Long, type: SearchResultType) {
        when (type) {
            SearchResultType.CARD -> repository.removeDeckFromCollection(collectionId, resourceId)
            SearchResultType.DECK -> repository.removeDeckFromCollection(collectionId, resourceId)
            SearchResultType.TABLE -> repository.removeTableFromCollection(collectionId, resourceId)
            else -> {}
        }
    }
}
