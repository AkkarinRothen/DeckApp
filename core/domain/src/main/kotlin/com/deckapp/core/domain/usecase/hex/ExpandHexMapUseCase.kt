package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexTile
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

class ExpandHexMapUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long) {
        val mapWithTiles = hexRepository.getHexMapWithTiles(mapId) ?: return
        val currentTiles = mapWithTiles.tiles
        
        if (currentTiles.isEmpty()) {
            hexRepository.upsertHexTiles(listOf(HexTile(mapId = mapId, q = 0, r = 0)))
            return
        }

        // Calculate current max radius
        val currentMaxRadius = currentTiles.maxOf { tile ->
            max(abs(tile.q), max(abs(tile.r), abs(-tile.q - tile.r)))
        }

        val nextRadius = currentMaxRadius + 1
        val nextRing = generateRing(mapId, nextRadius)
        
        // Only add tiles that don't already exist (to avoid collisions)
        val existingCoords = currentTiles.map { it.q to it.r }.toSet()
        val tilesToAdd = nextRing.filter { (it.q to it.r) !in existingCoords }
        
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
