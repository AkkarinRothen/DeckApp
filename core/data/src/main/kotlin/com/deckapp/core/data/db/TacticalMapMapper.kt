package com.deckapp.core.data.db

import com.deckapp.core.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val mapperJson = Json { ignoreUnknownKeys = true }

fun TacticalMapEntity.toDomain() = TacticalMap(
    id = id,
    name = name,
    imageUri = imageUri,
    gridType = runCatching { GridType.valueOf(gridType) }.getOrDefault(GridType.SQUARE),
    cellSizePx = cellSizePx,
    gridOffsetX = gridOffsetX,
    gridOffsetY = gridOffsetY,
    gridColorHex = gridColorHex,
    gridOpacity = gridOpacity,
    scaleFtPerCell = scaleFtPerCell,
    diagonalRule = runCatching { DiagonalRule.valueOf(diagonalRule) }.getOrDefault(DiagonalRule.ALTERNATING_5_10_5),
    sessionId = sessionId,
    sceneId = sceneId,
    fogOfWarMaskJson = fogOfWarMaskJson,
    terrainMaskJson = terrainMaskJson,
    areasJson = areasJson,
    createdAt = createdAt
)

fun TacticalMap.toEntity() = TacticalMapEntity(
    id = id,
    name = name,
    imageUri = imageUri,
    gridType = gridType.name,
    cellSizePx = cellSizePx,
    gridOffsetX = gridOffsetX,
    gridOffsetY = gridOffsetY,
    gridColorHex = gridColorHex,
    gridOpacity = gridOpacity,
    scaleFtPerCell = scaleFtPerCell,
    diagonalRule = diagonalRule.name,
    sessionId = sessionId,
    sceneId = sceneId,
    fogOfWarMaskJson = fogOfWarMaskJson,
    terrainMaskJson = terrainMaskJson,
    areasJson = areasJson,
    createdAt = createdAt
)

fun TacticalTokenEntity.toDomain(): TacticalToken {
    val decodedConditions: Set<Condition> = runCatching {
        mapperJson.decodeFromString<Set<Condition>>(conditionsJson)
    }.getOrDefault(emptySet())

    return TacticalToken(
        id = id,
        mapId = mapId,
        name = name,
        avatarUri = avatarUri,
        faction = runCatching { TokenFaction.valueOf(faction) }.getOrDefault(TokenFaction.ENEMY),
        sizeInCells = sizeInCells,
        cellX = cellX,
        cellY = cellY,
        currentHp = currentHp,
        maxHp = maxHp,
        armorClass = armorClass,
        speedFt = speedFt,
        conditions = decodedConditions,
        isHidden = isHidden == 1,
        encounterCreatureId = encounterCreatureId,
        npcId = npcId,
        notes = notes
    )
}

fun TacticalToken.toEntity(): TacticalTokenEntity {
    val conditionsEncoded = runCatching {
        mapperJson.encodeToString(conditions)
    }.getOrDefault("[]")

    return TacticalTokenEntity(
        id = id,
        mapId = mapId,
        name = name,
        avatarUri = avatarUri,
        faction = faction.name,
        sizeInCells = sizeInCells,
        cellX = cellX,
        cellY = cellY,
        currentHp = currentHp,
        maxHp = maxHp,
        armorClass = armorClass,
        speedFt = speedFt,
        conditionsJson = conditionsEncoded,
        isHidden = if (isHidden) 1 else 0,
        encounterCreatureId = encounterCreatureId,
        npcId = npcId,
        notes = notes
    )
}

fun TacticalMapPinEntity.toDomain(): TacticalMapPin {
    val decodedDiscoveries: List<PinDiscovery> = runCatching {
        mapperJson.decodeFromString<List<PinDiscovery>>(discoveriesJson)
    }.getOrDefault(emptyList())

    return TacticalMapPin(
        id = id,
        mapId = mapId,
        cellX = cellX,
        cellY = cellY,
        title = title,
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        isSecret = isSecret == 1,
        explorationState = runCatching { PinExplorationState.valueOf(explorationState) }.getOrDefault(PinExplorationState.UNKNOWN),
        targetTableId = targetTableId,
        targetEncounterId = targetEncounterId,
        targetWikiEntryId = targetWikiEntryId,
        discoveries = decodedDiscoveries
    )
}

fun TacticalMapPin.toEntity(): TacticalMapPinEntity {
    val encodedDiscoveries = runCatching {
        mapperJson.encodeToString(discoveries)
    }.getOrDefault("[]")

    return TacticalMapPinEntity(
        id = id,
        mapId = mapId,
        cellX = cellX,
        cellY = cellY,
        title = title,
        description = description,
        iconName = iconName,
        colorHex = colorHex,
        isSecret = if (isSecret) 1 else 0,
        explorationState = explorationState.name,
        targetTableId = targetTableId,
        targetEncounterId = targetEncounterId,
        targetWikiEntryId = targetWikiEntryId,
        discoveriesJson = encodedDiscoveries
    )
}
