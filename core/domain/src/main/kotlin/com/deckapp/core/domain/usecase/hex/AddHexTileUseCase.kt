package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexTile
import javax.inject.Inject

class AddHexTileUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long, q: Int, r: Int, terrainCost: Int = 1, terrainLabel: String = "", terrainColor: Long = 0xFF7CB87BL) {
        val tile = HexTile(
            mapId = mapId,
            q = q,
            r = r,
            terrainCost = terrainCost,
            terrainLabel = terrainLabel,
            terrainColor = terrainColor
        )
        hexRepository.upsertHexTiles(listOf(tile))
    }
}
