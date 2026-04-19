package com.deckapp.core.domain.usecase.reference

import com.deckapp.core.domain.repository.ReferenceRepository
import javax.inject.Inject

class RemoveStarterPackUseCase @Inject constructor(
    private val repository: ReferenceRepository
) {
    suspend operator fun invoke(packName: String) {
        repository.removeStarterPack(packName)
    }
}
