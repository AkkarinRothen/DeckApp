package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import javax.inject.Inject

class DeleteHexPoiUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(poiId: Long) {
        hexRepository.deleteHexPoi(poiId)
    }
}
