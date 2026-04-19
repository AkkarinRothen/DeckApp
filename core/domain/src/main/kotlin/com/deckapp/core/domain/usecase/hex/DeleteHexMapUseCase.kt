package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import javax.inject.Inject

class DeleteHexMapUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long) {
        hexRepository.deleteHexMap(mapId)
    }
}
