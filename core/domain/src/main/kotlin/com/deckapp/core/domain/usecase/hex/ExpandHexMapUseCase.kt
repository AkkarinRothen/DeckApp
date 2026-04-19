package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexTile
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import kotlinx.coroutines.flow.first

class ExpandHexMapUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long) {
        val currentTiles = hexRepository.getHexTiles(mapId).first()

        if (currentTiles.isEmpty()) {
            hexRepository.upsertHexTiles(listOf(HexTile(mapId = mapId, q = 0, r = 0)))
            return
        }

        val currentMaxRadius = currentTiles.maxOf { tile ->
            max(abs(tile.q), max(abs(tile.r), abs(-tile.q - tile.r)))
        }

        val nextRadius = currentMaxRadius + 1
        val nextRing = generateRing(mapId, nextRadius)

        val existingCoords: Set<Pair<Int, Int>> = currentTiles.map { tile -> tile.q to tile.r }.toSet()
        val tilesToAdd = nextRing.filter { tile -> (tile.q to tile.r) !in existingCoords }

        hexRepository.upsertHexTiles(tilesToAdd)
    }

    private fun generateRing(mapId: Long, n: Int): List<HexTile> = buildList {
        var q = n
        var r = -n
        val directions = listOf(0 to 1, -1 to 1, -1 to 0, 0 to -1, 1 to -1, 1 to 0)
        for (dir in directions) {
            for (step in 0 until n) {
                add(HexTile(mapId = mapId, q = q, r = r))
                q += dir.first
                r += dir.second
            }
        }
    }
}
