package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class HexMapWithTiles(
    val map: HexMap,
    val tiles: List<HexTile>,
    val pois: List<HexPoi>
)

class GetHexMapWithTilesUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    operator fun invoke(mapId: Long): Flow<HexMapWithTiles?> =
        combine(
            hexRepository.getHexMap(mapId),
            hexRepository.getHexTiles(mapId),
            hexRepository.getHexPois(mapId)
        ) { map, tiles, pois ->
            map?.let { HexMapWithTiles(it, tiles, pois) }
        }
}
