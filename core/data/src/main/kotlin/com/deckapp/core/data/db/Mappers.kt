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
    drawFaceDown = drawFaceDown,
    backImagePath = backImagePath,
    displayCount = displayCount,
    aspectRatio = runCatching { CardAspectRatio.valueOf(aspectRatio) }.getOrDefault(CardAspectRatio.STANDARD),
    isArchived = isArchived,
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
    drawFaceDown = drawFaceDown,
    backImagePath = backImagePath,
    displayCount = displayCount,
    aspectRatio = aspectRatio.name,
    isArchived = isArchived,
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
    isRevealed = isRevealed,
    sortOrder = sortOrder,
    tags = tags,
    linkedTableId = linkedTableId,
    dmNotes = dmNotes,
    lastDrawnAt = lastDrawnAt
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
    isRevealed = isRevealed,
    sortOrder = sortOrder,
    linkedTableId = linkedTableId,
    dmNotes = dmNotes,
    lastDrawnAt = lastDrawnAt
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

fun RandomTableEntity.toDomain(
    entries: List<TableEntryEntity> = emptyList(),
    tags: List<Tag> = emptyList(),
    bundleName: String? = null
) = com.deckapp.core.model.RandomTable(
        id = id,
        bundleId = bundleId,
        bundleName = bundleName,
        name = name,
        description = description,
        tags = tags,
        rollFormula = rollFormula,
        rollMode = runCatching { com.deckapp.core.model.TableRollMode.valueOf(rollMode) }
            .getOrDefault(com.deckapp.core.model.TableRollMode.RANGE),
        entries = entries.map { it.toDomain() },
        isNoRepeat = isNoRepeat,
        isPinned = isPinned,
        isBuiltIn = isBuiltIn,
        createdAt = createdAt
    )

fun com.deckapp.core.model.RandomTable.toEntity() = RandomTableEntity(
    id = id,
    bundleId = bundleId,
    name = name,
    description = description,
    rollFormula = rollFormula,
    rollMode = rollMode.name,
    isNoRepeat = isNoRepeat,
    isPinned = isPinned,
    isBuiltIn = isBuiltIn,
    createdAt = createdAt
)

// --- TableBundle ---

fun TableBundleEntity.toDomain(tables: List<RandomTable> = emptyList()) = TableBundle(
    id = id,
    name = name,
    description = description,
    sourceUri = sourceUri,
    tables = tables,
    createdAt = createdAt
)

fun TableBundle.toEntity() = TableBundleEntity(
    id = id,
    name = name,
    description = description,
    sourceUri = sourceUri,
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

fun TableWithEntries.toDomain(tags: List<Tag> = emptyList(), bundleName: String? = null) = table.toDomain(
    entries = entries,
    tags = tags,
    bundleName = bundleName
)

// --- DrawEvent ---

fun DrawEventEntity.toDomain() = DrawEvent(
    id = id, sessionId = sessionId, cardId = cardId,
    action = DrawAction.valueOf(action), metadata = metadata, timestamp = timestamp
)

fun DrawEvent.toEntity() = DrawEventEntity(
    id = id, sessionId = sessionId, cardId = cardId,
    action = action.name, metadata = metadata, timestamp = timestamp
)

// --- Encounters ---

fun EncounterEntity.toDomain(creatures: List<EncounterCreature> = emptyList()) = Encounter(
    id = id,
    name = name,
    description = description,
    creatures = creatures,
    linkedSessionId = linkedSessionId,
    isActive = isActive,
    currentRound = currentRound,
    currentTurnIndex = currentTurnIndex,
    createdAt = createdAt
)

fun Encounter.toEntity() = EncounterEntity(
    id = id,
    name = name,
    description = description,
    linkedSessionId = linkedSessionId,
    isActive = isActive,
    currentRound = currentRound,
    currentTurnIndex = currentTurnIndex,
    createdAt = createdAt
)

fun EncounterCreatureEntity.toDomain(): EncounterCreature {
    val conditions: Set<Condition> = try {
        json.decodeFromString(conditionsJson)
    } catch (e: Exception) { emptySet() }
    
    return EncounterCreature(
        id = id,
        encounterId = encounterId,
        name = name,
        maxHp = maxHp,
        currentHp = currentHp,
        armorClass = armorClass,
        initiativeBonus = initiativeBonus,
        initiativeRoll = initiativeRoll,
        conditions = conditions,
        notes = notes,
        sortOrder = sortOrder
    )
}

fun EncounterCreature.toEntity() = EncounterCreatureEntity(
    id = id,
    encounterId = encounterId,
    name = name,
    maxHp = maxHp,
    currentHp = currentHp,
    armorClass = armorClass,
    initiativeBonus = initiativeBonus,
    initiativeRoll = initiativeRoll,
    conditionsJson = json.encodeToString(conditions),
    notes = notes,
    sortOrder = sortOrder
)

// --- Collections ---

fun CollectionEntity.toDomain(resourceCount: Int = 0) = Collection(
    id = id,
    name = name,
    description = description,
    color = color,
    icon = runCatching { CollectionIcon.valueOf(iconName) }.getOrDefault(CollectionIcon.FOLDER),
    resourceCount = resourceCount,
    createdAt = createdAt
)

fun Collection.toEntity() = CollectionEntity(
    id = id,
    name = name,
    description = description,
    color = color,
    iconName = icon.name,
    createdAt = createdAt
)

fun CollectionWithCount.toDomain() = collection.toDomain(resourceCount = resourceCount)
