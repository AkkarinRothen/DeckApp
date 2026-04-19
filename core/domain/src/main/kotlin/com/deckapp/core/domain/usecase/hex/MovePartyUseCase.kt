package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import javax.inject.Inject

class MovePartyUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long, q: Int, r: Int) =
        hexRepository.updatePartyLocation(mapId, q, r)
}
