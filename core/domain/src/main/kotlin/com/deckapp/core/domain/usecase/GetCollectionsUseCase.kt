package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CollectionRepository
import com.deckapp.core.model.Collection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCollectionsUseCase @Inject constructor(
    private val repository: CollectionRepository
) {
    operator fun invoke(): Flow<List<Collection>> = repository.getAllCollections()
}
