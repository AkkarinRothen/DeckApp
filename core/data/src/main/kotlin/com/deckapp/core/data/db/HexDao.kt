package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HexDao {

    // --- Maps ---

    @Query("SELECT * FROM hex_maps ORDER BY createdAt DESC")
    fun getAllMaps(): Flow<List<HexMapEntity>>

    @Query("SELECT * FROM hex_maps WHERE id = :id")
    fun getMapById(id: Long): Flow<HexMapEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMap(map: HexMapEntity): Long

    @Query("DELETE FROM hex_maps WHERE id = :id")
    suspend fun deleteMap(id: Long)

    // --- Tiles ---

    @Query("SELECT * FROM hex_tiles WHERE mapId = :mapId")
    fun getTilesForMap(mapId: Long): Flow<List<HexTileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: HexTileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTiles(tiles: List<HexTileEntity>)

    // --- POIs ---

    @Query("SELECT * FROM hex_pois WHERE mapId = :mapId")
    fun getPoisForMap(mapId: Long): Flow<List<HexPoiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoi(poi: HexPoiEntity): Long

    @Query("DELETE FROM hex_pois WHERE id = :id")
    suspend fun deletePoi(id: Long)

    @Query("UPDATE hex_maps SET partyQ = :q, partyR = :r WHERE id = :mapId")
    suspend fun updatePartyLocation(mapId: Long, q: Int, r: Int)

    // --- Days ---

    @Query("SELECT * FROM hex_days WHERE mapId = :mapId ORDER BY dayNumber ASC")
    fun getDaysForMap(mapId: Long): Flow<List<HexDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: HexDayEntity): Long
}
