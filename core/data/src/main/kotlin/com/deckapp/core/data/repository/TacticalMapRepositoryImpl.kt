package com.deckapp.core.data.repository

import com.deckapp.core.data.db.TacticalMapDao
import com.deckapp.core.data.db.toDomain
import com.deckapp.core.data.db.toEntity
import com.deckapp.core.domain.repository.TacticalMapRepository
import com.deckapp.core.model.TacticalMap
import com.deckapp.core.model.TacticalMapPin
import com.deckapp.core.model.TacticalToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TacticalMapRepositoryImpl @Inject constructor(
    private val dao: TacticalMapDao
) : TacticalMapRepository {

    override fun getAllTacticalMaps(): Flow<List<TacticalMap>> =
        dao.getAllMaps().map { entities -> entities.map { it.toDomain() } }

    override fun getTacticalMapById(id: Long): Flow<TacticalMap?> =
        dao.getMapById(id).map { it?.toDomain() }

    override fun getTacticalMapsBySessionId(sessionId: Long): Flow<List<TacticalMap>> =
        dao.getMapsBySessionId(sessionId).map { entities -> entities.map { it.toDomain() } }

    override fun getTacticalMapsBySceneId(sceneId: Long): Flow<List<TacticalMap>> =
        dao.getMapsBySceneId(sceneId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertTacticalMap(map: TacticalMap): Long =
        dao.upsertMap(map.toEntity())

    override suspend fun deleteTacticalMap(id: Long) =
        dao.deleteMap(id)

    override suspend fun updateAreas(mapId: Long, areasJson: String) =
        dao.updateAreas(mapId, areasJson)

    // Tokens
    override fun getTokensByMapId(mapId: Long): Flow<List<TacticalToken>> =
        dao.getTokensByMapId(mapId).map { entities -> entities.map { it.toDomain() } }

    override fun getTokenById(id: Long): Flow<TacticalToken?> =
        dao.getTokenById(id).map { it?.toDomain() }

    override suspend fun upsertToken(token: TacticalToken): Long =
        dao.upsertToken(token.toEntity())

    override suspend fun upsertTokens(tokens: List<TacticalToken>) =
        dao.upsertTokens(tokens.map { it.toEntity() })

    override suspend fun updateTokenPosition(tokenId: Long, cellX: Float, cellY: Float) =
        dao.updateTokenPosition(tokenId, cellX, cellY)

    override suspend fun updateTokenHp(tokenId: Long, currentHp: Int) =
        dao.updateTokenHp(tokenId, currentHp)

    override suspend fun updateTokenHpAndTempHp(tokenId: Long, currentHp: Int, tempHp: Int) =
        dao.updateTokenHpAndTempHp(tokenId, currentHp, tempHp)

    override suspend fun deleteToken(id: Long) =
        dao.deleteToken(id)

    override suspend fun deleteTokensByMapId(mapId: Long) =
        dao.deleteTokensByMapId(mapId)

    // Pins
    override fun getPinsByMapId(mapId: Long): Flow<List<TacticalMapPin>> =
        dao.getPinsByMapId(mapId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertPin(pin: TacticalMapPin): Long =
        dao.upsertPin(pin.toEntity())

    override suspend fun deletePin(id: Long) =
        dao.deletePin(id)
}
