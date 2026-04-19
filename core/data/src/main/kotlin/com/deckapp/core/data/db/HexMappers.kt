package com.deckapp.core.data.db

import com.deckapp.core.model.HexActivityEntry
import com.deckapp.core.model.HexDay
import com.deckapp.core.model.HexMap
import com.deckapp.core.model.HexPoi
import com.deckapp.core.model.HexStyle
import com.deckapp.core.model.HexTile
import com.deckapp.core.model.PoiType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val hexJson = Json { ignoreUnknownKeys = true }

fun HexMapEntity.toDomain() = HexMap(
    id = id,
    name = name,
    rows = rows,
    cols = cols,
    sessionId = sessionId,
    hexStyle = runCatching { HexStyle.valueOf(hexStyle) }.getOrDefault(HexStyle.FLAT_TOP),
    partyQ = partyQ,
    partyR = partyR,
    isRadial = isRadial,
    maxActivitiesPerDay = maxActivitiesPerDay,
    mapNotes = mapNotes,
    createdAt = createdAt
)

fun HexMap.toEntity() = HexMapEntity(
    id = id,
    name = name,
    rows = rows,
    cols = cols,
    sessionId = sessionId,
    hexStyle = hexStyle.name,
    partyQ = partyQ,
    partyR = partyR,
    isRadial = isRadial,
    maxActivitiesPerDay = maxActivitiesPerDay,
    mapNotes = mapNotes,
    createdAt = createdAt
)

fun HexTileEntity.toDomain() = HexTile(
    mapId = mapId,
    q = q,
    r = r,
    terrainCost = terrainCost,
    terrainLabel = terrainLabel,
    terrainColor = terrainColor,
    dmNotes = dmNotes,
    playerNotes = playerNotes,
    isExplored = isExplored,
    isReconnoitered = isReconnoitered,
    isMapped = isMapped
)

fun HexTile.toEntity() = HexTileEntity(
    mapId = mapId,
    q = q,
    r = r,
    terrainCost = terrainCost,
    terrainLabel = terrainLabel,
    terrainColor = terrainColor,
    dmNotes = dmNotes,
    playerNotes = playerNotes,
    isExplored = isExplored,
    isReconnoitered = isReconnoitered,
    isMapped = isMapped
)

fun HexPoiEntity.toDomain() = HexPoi(
    id = id,
    mapId = mapId,
    tileQ = tileQ,
    tileR = tileR,
    name = name,
    type = runCatching { PoiType.valueOf(type) }.getOrDefault(PoiType.CUSTOM),
    description = description,
    encounterId = encounterId,
    tableId = tableId
)

fun HexPoi.toEntity() = HexPoiEntity(
    id = id,
    mapId = mapId,
    tileQ = tileQ,
    tileR = tileR,
    name = name,
    type = type.name,
    description = description,
    encounterId = encounterId,
    tableId = tableId
)

fun HexDayEntity.toDomain(): HexDay {
    val entries = runCatching {
        hexJson.decodeFromString<List<HexActivityEntry>>(activitiesLog)
    }.getOrDefault(emptyList())
    return HexDay(id = id, mapId = mapId, dayNumber = dayNumber, activitiesLog = entries, notes = notes)
}

fun HexDay.toEntity() = HexDayEntity(
    id = id,
    mapId = mapId,
    dayNumber = dayNumber,
    activitiesLog = hexJson.encodeToString(activitiesLog),
    notes = notes
)
