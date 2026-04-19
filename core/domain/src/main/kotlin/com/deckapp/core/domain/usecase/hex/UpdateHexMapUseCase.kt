package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexMap
import javax.inject.Inject

class UpdateHexMapUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(map: HexMap) = hexRepository.upsertHexMap(map)
}
