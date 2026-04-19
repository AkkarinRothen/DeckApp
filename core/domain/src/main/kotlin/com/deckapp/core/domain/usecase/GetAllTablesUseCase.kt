package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.RandomTable
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTablesUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    operator fun invoke(): Flow<List<RandomTable>> = tableRepository.getAllTables()
}
