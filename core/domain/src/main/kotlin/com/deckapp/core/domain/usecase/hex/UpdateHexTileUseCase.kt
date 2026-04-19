package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexTile
import javax.inject.Inject

class UpdateHexTileUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(tile: HexTile) {
        hexRepository.upsertHexTile(tile)
    }
}
