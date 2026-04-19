package com.deckapp.core.data.repository

import com.deckapp.core.data.db.HexDao
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HexRepositoryImpl @Inject constructor(
    private val hexDao: HexDao
) : HexRepository {

    override fun getHexMaps(): Flow<List<HexMap>> =
        hexDao.getAllMaps().map { it.map { e -> e.toDomain() } }

    override fun getHexMap(id: Long): Flow<HexMap?> =
        hexDao.getMapById(id).map { it?.toDomain() }

    override fun getHexTiles(mapId: Long): Flow<List<HexTile>> =
        hexDao.getTilesForMap(mapId).map { it.map { e -> e.toDomain() } }

    override fun getHexPois(mapId: Long): Flow<List<HexPoi>> =
        hexDao.getPoisForMap(mapId).map { it.map { e -> e.toDomain() } }

    override fun getHexDays(mapId: Long): Flow<List<HexDay>> =
        hexDao.getDaysForMap(mapId).map { it.map { e -> e.toDomain() } }

    override suspend fun upsertHexMap(map: HexMap): Long {
        val entity = map.toEntity()
        val id = hexDao.insertMap(entity)
        if (id == -1L) {
            hexDao.updateMap(entity)
            return entity.id
        }
        return id
    }

    override suspend fun upsertHexTile(tile: HexTile) =
        hexDao.insertTile(tile.toEntity())

    override suspend fun upsertHexTiles(tiles: List<HexTile>) =
        hexDao.insertTiles(tiles.map { it.toEntity() })

    override suspend fun upsertHexPoi(poi: HexPoi): Long =
        hexDao.insertPoi(poi.toEntity())

    override suspend fun upsertHexDay(day: HexDay): Long =
        hexDao.insertDay(day.toEntity())

    override suspend fun updatePartyLocation(mapId: Long, q: Int, r: Int) =
        hexDao.updatePartyLocation(mapId, q, r)

    override suspend fun deleteHexMap(id: Long) =
        hexDao.deleteMap(id)

    override suspend fun deleteHexPoi(id: Long) =
        hexDao.deletePoi(id)
}
