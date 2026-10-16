package com.deckapp.core.domain.repository

import com.deckapp.core.model.TacticalMap
import com.deckapp.core.model.TacticalMapPin
import com.deckapp.core.model.TacticalToken
import kotlinx.coroutines.flow.Flow

interface TacticalMapRepository {
    fun getAllTacticalMaps(): Flow<List<TacticalMap>>
    fun getTacticalMapById(id: Long): Flow<TacticalMap?>
    fun getTacticalMapsBySessionId(sessionId: Long): Flow<List<TacticalMap>>
    fun getTacticalMapsBySceneId(sceneId: Long): Flow<List<TacticalMap>>

    suspend fun upsertTacticalMap(map: TacticalMap): Long
    suspend fun deleteTacticalMap(id: Long)
    suspend fun updateAreas(mapId: Long, areasJson: String)

    // Tokens
    fun getTokensByMapId(mapId: Long): Flow<List<TacticalToken>>
    fun getTokenById(id: Long): Flow<TacticalToken?>
    suspend fun upsertToken(token: TacticalToken): Long
    suspend fun upsertTokens(tokens: List<TacticalToken>)
    suspend fun updateTokenPosition(tokenId: Long, cellX: Float, cellY: Float)
    suspend fun updateTokenHp(tokenId: Long, currentHp: Int)
    suspend fun updateTokenHpAndTempHp(tokenId: Long, currentHp: Int, tempHp: Int)
    suspend fun deleteToken(id: Long)
    suspend fun deleteTokensByMapId(mapId: Long)

    // Pins
    fun getPinsByMapId(mapId: Long): Flow<List<TacticalMapPin>>
    suspend fun upsertPin(pin: TacticalMapPin): Long
    suspend fun deletePin(id: Long)
}
