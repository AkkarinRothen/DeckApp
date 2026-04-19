package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.ReferenceTable
import javax.inject.Inject

class SaveReferenceTableUseCase @Inject constructor(
    private val repository: ReferenceRepository
) {
    suspend operator fun invoke(table: ReferenceTable): Result<Long> = runCatching {
        require(table.name.isNotBlank()) { "El nombre de la tabla no puede estar vacío." }
        require(table.columns.isNotEmpty()) { "La tabla debe tener al menos una columna." }
        repository.saveReferenceTable(table)
    }
}
