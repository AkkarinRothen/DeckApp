package com.deckapp.core.domain.repository

import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexTile
import kotlinx.coroutines.flow.Flow

interface HexRepository {
    fun getHexMaps(): Flow<List<HexMap>>
    fun getHexMap(id: Long): Flow<HexMap?>
    fun getHexTiles(mapId: Long): Flow<List<HexTile>>
    fun getHexPois(mapId: Long): Flow<List<HexPoi>>
    fun getHexDays(mapId: Long): Flow<List<HexDay>>

    suspend fun upsertHexMap(map: HexMap): Long
    suspend fun upsertHexTile(tile: HexTile)
    suspend fun upsertHexTiles(tiles: List<HexTile>)
    suspend fun upsertHexPoi(poi: HexPoi): Long
    suspend fun upsertHexDay(day: HexDay): Long

    suspend fun updatePartyLocation(mapId: Long, q: Int, r: Int)
    suspend fun updateLinkedMythicSession(mapId: Long, mythicSessionId: Long?)

    suspend fun deleteHexMap(id: Long)
    suspend fun deleteHexTile(mapId: Long, q: Int, r: Int)
    suspend fun deleteHexPoi(id: Long)
}
