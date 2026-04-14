package com.deckapp.core.data.db

import com.deckapp.core.model.*
import com.deckapp.core.model.CardAspectRatio
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

// --- CardStack ---

fun CardStackEntity.toDomain(tags: List<Tag> = emptyList()) = CardStack(
    id = id,
    name = name,
    type = StackType.valueOf(type),
    description = description,
    coverImagePath = coverImagePath,
    sourceFolderPath = sourceFolderPath,
    defaultContentMode = CardContentMode.valueOf(defaultContentMode),
    drawMode = DrawMode.valueOf(drawMode),
    displayCount = displayCount,
    aspectRatio = runCatching { CardAspectRatio.valueOf(aspectRatio) }.getOrDefault(CardAspectRatio.STANDARD),
    tags = tags,
    createdAt = createdAt
)

fun CardStack.toEntity() = CardStackEntity(
    id = id,
    name = name,
    type = type.name,
    description = description,
    coverImagePath = coverImagePath,
    sourceFolderPath = sourceFolderPath,
    defaultContentMode = defaultContentMode.name,
    drawMode = drawMode.name,
    displayCount = displayCount,
    aspectRatio = aspectRatio.name,
    createdAt = createdAt
)

// --- Card ---

fun CardEntity.toDomain(faces: List<CardFace> = emptyList(), tags: List<Tag> = emptyList()) = Card(
    id = id,
    stackId = stackId,
    originDeckId = originDeckId,
    title = title,
    suit = suit,
    value = value,
    faces = faces,
    currentFaceIndex = currentFaceIndex,
    currentRotation = currentRotation,
    isReversed = isReversed,
    isDrawn = isDrawn,
    sortOrder = sortOrder,
    tags = tags
)

fun Card.toEntity() = CardEntity(
    id = id,
    stackId = stackId,
    originDeckId = originDeckId,
    title = title,
    suit = suit,
    value = value,
    currentFaceIndex = currentFaceIndex,
    currentRotation = currentRotation,
    isReversed = isReversed,
    isDrawn = isDrawn,
    sortOrder = sortOrder
)

// --- CardFace ---

fun CardFaceEntity.toDomain(): CardFace {
    val zones: List<ContentZone> = try {
        json.decodeFromString(zonesJson)
    } catch (e: Exception) { emptyList() }
    return CardFace(
        name = name,
        imagePath = imagePath,
        contentMode = CardContentMode.valueOf(contentMode),
        zones = zones,
        reversedImagePath = reversedImagePath
    )
}

fun CardFace.toEntity(cardId: Long, faceIndex: Int) = CardFaceEntity(
    cardId = cardId,
    faceIndex = faceIndex,
    name = name,
    imagePath = imagePath,
    contentMode = contentMode.name,
    zonesJson = json.encodeToString(zones),
    reversedImagePath = reversedImagePath
)

// --- Tag ---

fun TagEntity.toDomain() = Tag(id = id, name = name, color = color)
fun Tag.toEntity() = TagEntity(id = id, name = name, color = color)

// --- Session ---

fun SessionEntity.toDomain() = Session(
    id = id,
    name = name,
    isActive = isActive,
    showCardTitles = showCardTitles,
    dmNotes = dmNotes,
    createdAt = createdAt,
    endedAt = endedAt
)

fun Session.toEntity() = SessionEntity(
    id = id,
    name = name,
    isActive = isActive,
    showCardTitles = showCardTitles,
    dmNotes = dmNotes,
    createdAt = createdAt,
    endedAt = endedAt
)


// --- SessionDeckRef ---

fun SessionDeckRefEntity.toDomain() = SessionDeckRef(
    sessionId = sessionId,
    stackId = stackId,
    drawModeOverride = drawModeOverride?.let { DrawMode.valueOf(it) },
    sortOrder = sortOrder
)

fun SessionDeckRef.toEntity() = SessionDeckRefEntity(
    sessionId = sessionId,
    stackId = stackId,
    drawModeOverride = drawModeOverride?.name,
    sortOrder = sortOrder
)

// --- RandomTable ---

fun RandomTableEntity.toDomain(entries: List<TableEntryEntity> = emptyList()) =
    com.deckapp.core.model.RandomTable(
        id = id,
        name = name,
        description = description,
        category = category,
        rollFormula = rollFormula,
        rollMode = runCatching { com.deckapp.core.model.TableRollMode.valueOf(rollMode) }
            .getOrDefault(com.deckapp.core.model.TableRollMode.RANGE),
        entries = entries.map { it.toDomain() },
        isBuiltIn = isBuiltIn,
        createdAt = createdAt
    )

fun com.deckapp.core.model.RandomTable.toEntity() = RandomTableEntity(
    id = id,
    name = name,
    description = description,
    category = category,
    rollFormula = rollFormula,
    rollMode = rollMode.name,
    isBuiltIn = isBuiltIn,
    createdAt = createdAt
)

fun TableEntryEntity.toDomain() = com.deckapp.core.model.TableEntry(
    id = id,
    tableId = tableId,
    minRoll = minRoll,
    maxRoll = maxRoll,
    weight = weight,
    text = text,
    subTableRef = subTableRef,
    subTableId = subTableId,
    sortOrder = sortOrder
)

fun com.deckapp.core.model.TableEntry.toEntity(tableId: Long) = TableEntryEntity(
    id = id,
    tableId = tableId,
    minRoll = minRoll,
    maxRoll = maxRoll,
    weight = weight,
    text = text,
    subTableRef = subTableRef,
    subTableId = subTableId,
    sortOrder = sortOrder
)

fun TableRollResultEntity.toDomain() = com.deckapp.core.model.TableRollResult(
    id = id,
    tableId = tableId,
    tableName = tableName,
    sessionId = sessionId,
    rollValue = rollValue,
    resolvedText = resolvedText,
    timestamp = timestamp
)

fun com.deckapp.core.model.TableRollResult.toEntity() = TableRollResultEntity(
    id = id,
    tableId = tableId,
    tableName = tableName,
    sessionId = sessionId,
    rollValue = rollValue,
    resolvedText = resolvedText,
    timestamp = timestamp
)

fun TableWithEntries.toDomain() = table.toDomain(entries)

// --- DrawEvent ---

fun DrawEventEntity.toDomain() = DrawEvent(
    id = id, sessionId = sessionId, cardId = cardId,
    action = DrawAction.valueOf(action), metadata = metadata, timestamp = timestamp
)

fun DrawEvent.toEntity() = DrawEventEntity(
    id = id, sessionId = sessionId, cardId = cardId,
    action = action.name, metadata = metadata, timestamp = timestamp
)
