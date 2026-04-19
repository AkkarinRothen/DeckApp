package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexActivityEntry
import com.deckapp.core.model.HexActivityType
import com.deckapp.core.model.HexTile
import javax.inject.Inject

class ReconnoiterHexUseCase @Inject constructor(
    private val hexRepository: HexRepository,
    private val logHexActivityUseCase: LogHexActivityUseCase
) {
    // Reconnoiter costs terrainCost activities (same as travel cost per PF2e convention)
    suspend operator fun invoke(tile: HexTile, dayId: Long) {
        hexRepository.upsertHexTile(tile.copy(isReconnoitered = true))
        repeat(tile.terrainCost.coerceAtLeast(1)) {
            logHexActivityUseCase(
                dayId,
                HexActivityEntry(
                    type = HexActivityType.RECONNOITER,
                    description = "Reconocido (${tile.q}, ${tile.r})",
                    tileQ = tile.q,
                    tileR = tile.r
                )
            )
        }
    }
}
