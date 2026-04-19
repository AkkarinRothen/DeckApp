package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexTile
import javax.inject.Inject

class CreateHexMapUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(name: String, rows: Int, cols: Int, radius: Int? = null): Long {
        val mapId = hexRepository.upsertHexMap(HexMap(name = name, rows = rows, cols = cols))
        val tiles = if (radius != null && radius > 0) {
            generateRadialTiles(mapId, radius)
        } else {
            generateRectangularTiles(mapId, rows, cols)
        }
        hexRepository.upsertHexTiles(tiles)
        return mapId
    }

    private fun generateRectangularTiles(mapId: Long, rows: Int, cols: Int): List<HexTile> = buildList {
        for (r in 0 until rows) {
            for (q in 0 until cols) {
                add(HexTile(mapId = mapId, q = q, r = r))
            }
        }
    }

    private fun generateRadialTiles(mapId: Long, radius: Int): List<HexTile> = buildList {
        // Center
        add(HexTile(mapId = mapId, q = 0, r = 0))
        
        // Rings from 1 to radius
        for (n in 1..radius) {
            addAll(generateRing(mapId, n))
        }
    }

    private fun generateRing(mapId: Long, n: Int): List<HexTile> = buildList {
        // A ring of radius N has 6 segments, each of length N
        // Starting point: (q=n, r=-n)
        var q = n
        var r = -n
        
        // Directions for moving along the 6 sides of the ring
        val directions = listOf(
            0 to 1, -1 to 1, -1 to 0, 0 to -1, 1 to -1, 1 to 0
        )
        
        for (dir in directions) {
            for (step in 0 until n) {
                add(HexTile(mapId = mapId, q = q, r = r))
                q += dir.first
                r += dir.second
            }
        }
    }
}
