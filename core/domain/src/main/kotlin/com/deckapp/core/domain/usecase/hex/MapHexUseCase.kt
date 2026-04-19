package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexActivityEntry
import com.deckapp.core.model.HexActivityType
import com.deckapp.core.model.HexTile
import javax.inject.Inject

class MapHexUseCase @Inject constructor(
    private val hexRepository: HexRepository,
    private val logHexActivityUseCase: LogHexActivityUseCase
) {
    suspend operator fun invoke(tile: HexTile, dayId: Long) {
        hexRepository.upsertHexTile(tile.copy(isMapped = true))
        logHexActivityUseCase(
            dayId,
            HexActivityEntry(
                type = HexActivityType.MAP_AREA,
                description = "Mapeado (${tile.q}, ${tile.r})",
                tileQ = tile.q,
                tileR = tile.r
            )
        )
    }
}
