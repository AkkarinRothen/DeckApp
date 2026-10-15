package com.deckapp.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TacticalMapDao {

    @Query("SELECT * FROM tactical_maps ORDER BY createdAt DESC")
    fun getAllMaps(): Flow<List<TacticalMapEntity>>

    @Query("SELECT * FROM tactical_maps WHERE id = :id LIMIT 1")
    fun getMapById(id: Long): Flow<TacticalMapEntity?>

    @Query("SELECT * FROM tactical_maps WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getMapsBySessionId(sessionId: Long): Flow<List<TacticalMapEntity>>

    @Query("SELECT * FROM tactical_maps WHERE sceneId = :sceneId ORDER BY createdAt DESC")
    fun getMapsBySceneId(sceneId: Long): Flow<List<TacticalMapEntity>>

    @Upsert
    suspend fun upsertMap(map: TacticalMapEntity): Long

    @Query("UPDATE tactical_maps SET areasJson = :areasJson WHERE id = :id")
    suspend fun updateAreas(id: Long, areasJson: String)

    @Query("DELETE FROM tactical_maps WHERE id = :id")
    suspend fun deleteMap(id: Long)

    // Tokens
    @Query("SELECT * FROM tactical_tokens WHERE mapId = :mapId")
    fun getTokensByMapId(mapId: Long): Flow<List<TacticalTokenEntity>>

    @Query("SELECT * FROM tactical_tokens WHERE id = :id LIMIT 1")
    fun getTokenById(id: Long): Flow<TacticalTokenEntity?>

    @Upsert
    suspend fun upsertToken(token: TacticalTokenEntity): Long

    @Upsert
    suspend fun upsertTokens(tokens: List<TacticalTokenEntity>)

    @Query("UPDATE tactical_tokens SET cellX = :cellX, cellY = :cellY WHERE id = :tokenId")
    suspend fun updateTokenPosition(tokenId: Long, cellX: Float, cellY: Float)

    @Query("UPDATE tactical_tokens SET currentHp = :currentHp WHERE id = :tokenId")
    suspend fun updateTokenHp(tokenId: Long, currentHp: Int)

    @Query("DELETE FROM tactical_tokens WHERE id = :id")
    suspend fun deleteToken(id: Long)

    @Query("DELETE FROM tactical_tokens WHERE mapId = :mapId")
    suspend fun deleteTokensByMapId(mapId: Long)

    // Pins
    @Query("SELECT * FROM tactical_map_pins WHERE mapId = :mapId")
    fun getPinsByMapId(mapId: Long): Flow<List<TacticalMapPinEntity>>

    @Upsert
    suspend fun upsertPin(pin: TacticalMapPinEntity): Long

    @Query("DELETE FROM tactical_map_pins WHERE id = :id")
    suspend fun deletePin(id: Long)
}
