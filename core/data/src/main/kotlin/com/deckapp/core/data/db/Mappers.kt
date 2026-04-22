package com.deckapp.core.data.db

import com.deckapp.core.model.*
import com.deckapp.core.model.backup.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }

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
    aspectRatio = CardAspectRatio.valueOf(aspectRatio),
    isArchived = isArchived,
    sortOrder = sortOrder,
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
    sortOrder = sortOrder,
    createdAt = createdAt
)

fun CardStackEntity.toBackupDto() = CardStackBackupDto(
    id = id, name = name, type = type, description = description, 
    coverImagePath = coverImagePath, sourceFolderPath = sourceFolderPath, 
    defaultContentMode = defaultContentMode, drawMode = drawMode, 
    drawFaceDown = drawFaceDown, backImagePath = backImagePath, 
    displayCount = displayCount, aspectRatio = aspectRatio, 
    isArchived = isArchived, sortOrder = sortOrder, createdAt = createdAt
)

// --- DeckCollection ---

fun CollectionEntity.toDomain(resourceCount: Int = 0) = DeckCollection(
    id = id,
    name = name,
    description = description,
    color = color,
    icon = runCatching { CollectionIcon.valueOf(iconName) }.getOrDefault(CollectionIcon.CHEST),
    resourceCount = resourceCount,
    createdAt = createdAt
)

fun DeckCollection.toEntity() = CollectionEntity(
    id = id,
    name = name,
    description = description,
    color = color,
    iconName = icon.name,
    createdAt = createdAt
)

fun CollectionWithCount.toDomain() = collection.toDomain(resourceCount)

// --- Card ---

fun CardEntity.toDomain(faces: List<CardFace> = emptyList(), tags: List<Tag> = emptyList()) = Card(
    id = id,
    stackId = stackId,
    originDeckId = originDeckId,
    title = title,
    suit = suit,
    value = value,
    faces = faces,
    tags = tags,
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

fun CardEntity.toBackupDto() = CardBackupDto(
    id = id, stackId = stackId, originDeckId = originDeckId, title = title, 
    suit = suit, value = value, currentFaceIndex = currentFaceIndex, 
    currentRotation = currentRotation, isReversed = isReversed, 
    isDrawn = isDrawn, isRevealed = isRevealed, sortOrder = sortOrder, 
    linkedTableId = linkedTableId, dmNotes = dmNotes, lastDrawnAt = lastDrawnAt
)

// --- CardFace ---

fun CardFaceEntity.toDomain() = CardFace(
    name = name,
    imagePath = imagePath,
    contentMode = CardContentMode.valueOf(contentMode),
    zones = runCatching { json.decodeFromString<List<ContentZone>>(zonesJson) }.getOrDefault(emptyList()),
    reversedImagePath = reversedImagePath
)

fun CardFace.toEntity(cardId: Long, faceIndex: Int) = CardFaceEntity(
    cardId = cardId,
    faceIndex = faceIndex,
    name = name,
    imagePath = imagePath,
    contentMode = contentMode.name,
    zonesJson = json.encodeToString(zones),
    reversedImagePath = reversedImagePath
)

fun CardFaceEntity.toBackupDto() = CardFaceBackupDto(
    id = id, cardId = cardId, faceIndex = faceIndex, name = name, 
    imagePath = imagePath, contentMode = contentMode, 
    zonesJson = zonesJson, reversedImagePath = reversedImagePath
)

// --- Tag ---

fun TagEntity.toDomain() = Tag(
    id = id,
    name = name,
    color = color
)

fun Tag.toEntity() = TagEntity(
    id = id,
    name = name,
    color = color
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
    category = category,
    description = description,
    tags = tags,
    rollFormula = rollFormula,
    rollMode = TableRollMode.valueOf(rollMode),
    entries = entries.map { it.toDomain() },
    isNoRepeat = isNoRepeat,
    isPinned = isPinned,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    createdAt = createdAt
)

fun com.deckapp.core.model.RandomTable.toEntity() = RandomTableEntity(
    id = id,
    bundleId = bundleId,
    name = name,
    category = category,
    description = description,
    rollFormula = rollFormula,
    rollMode = rollMode.name,
    isNoRepeat = isNoRepeat,
    isPinned = isPinned,
    isBuiltIn = isBuiltIn,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    createdAt = createdAt
)

fun RandomTableEntity.toBackupDto() = RandomTableBackupDto(
    id = id, bundleId = bundleId, name = name, category = category, 
    description = description, rollFormula = rollFormula, rollMode = rollMode, 
    isNoRepeat = isNoRepeat, isPinned = isPinned, sourceType = sourceType, 
    sourceName = sourceName, isBuiltIn = isBuiltIn, sortOrder = sortOrder, 
    createdAt = createdAt, sourcePack = sourcePack
)

fun TableBundleEntity.toDomain(tables: List<com.deckapp.core.model.RandomTable> = emptyList()) = TableBundle(
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

// --- TableEntry ---

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

fun TableEntryEntity.toBackupDto() = TableEntryBackupDto(
    id = id, tableId = tableId, minRoll = minRoll, maxRoll = maxRoll, 
    weight = weight, text = text, subTableRef = subTableRef, 
    subTableId = subTableId, sortOrder = sortOrder
)

// --- TableRollResult ---

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

fun DrawEventEntity.toBackupDto() = DrawEventBackupDto(
    id = id, sessionId = sessionId, cardId = cardId, action = action, 
    metadata = metadata, timestamp = timestamp
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

fun EncounterEntity.toBackupDto() = EncounterBackupDto(
    id = id, name = name, description = description, linkedSessionId = linkedSessionId, 
    isActive = isActive, currentRound = currentRound, 
    currentTurnIndex = currentTurnIndex, createdAt = createdAt
)

fun EncounterCreatureEntity.toDomain() = EncounterCreature(
    id = id,
    encounterId = encounterId,
    name = name,
    maxHp = maxHp,
    currentHp = currentHp,
    armorClass = armorClass,
    initiativeBonus = initiativeBonus,
    initiativeRoll = initiativeRoll,
    conditions = runCatching { json.decodeFromString<Set<Condition>>(conditionsJson) }.getOrDefault(emptySet()),
    notes = notes,
    sortOrder = sortOrder,
    imagePath = imagePath,
    npcId = npcId
)

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
    sortOrder = sortOrder,
    imagePath = imagePath,
    npcId = npcId
)

fun EncounterCreatureEntity.toBackupDto() = EncounterCreatureBackupDto(
    id = id, encounterId = encounterId, name = name, maxHp = maxHp, currentHp = currentHp, 
    armorClass = armorClass, initiativeBonus = initiativeBonus, 
    initiativeRoll = initiativeRoll, conditionsJson = conditionsJson, 
    notes = notes, sortOrder = sortOrder, npcId = npcId, imagePath = imagePath
)

fun CombatLogEntryEntity.toDomain() = CombatLogEntry(
    id = id,
    encounterId = encounterId,
    message = message,
    type = CombatLogType.valueOf(type),
    timestamp = timestamp
)

fun CombatLogEntry.toEntity() = CombatLogEntryEntity(
    id = id,
    encounterId = encounterId,
    message = message,
    type = type.name,
    timestamp = timestamp
)

fun CombatLogEntryEntity.toBackupDto() = CombatLogEntryBackupDto(
    id = id, encounterId = encounterId, message = message, type = type, timestamp = timestamp
)

// --- NPCs ---

fun NpcEntity.toDomain(tags: List<Tag> = emptyList()) = Npc(
    id = id, name = name, description = description, imagePath = imagePath, 
    voiceSamplePath = voiceSamplePath,
    maxHp = maxHp, currentHp = currentHp, armorClass = armorClass, 
    initiativeBonus = initiativeBonus, notes = notes, isMonster = isMonster, 
    tags = tags, createdAt = createdAt
)

fun Npc.toEntity() = NpcEntity(
    id = id, name = name, description = description, imagePath = imagePath, 
    voiceSamplePath = voiceSamplePath,
    maxHp = maxHp, currentHp = currentHp, armorClass = armorClass, 
    initiativeBonus = initiativeBonus, notes = notes, isMonster = isMonster, 
    createdAt = createdAt
)

fun NpcEntity.toBackupDto() = NpcBackupDto(
    id = id, name = name, description = description, imagePath = imagePath, 
    maxHp = maxHp, currentHp = currentHp, armorClass = armorClass, 
    initiativeBonus = initiativeBonus, notes = notes, isMonster = isMonster, 
    createdAt = createdAt
)

// --- Sessions ---

fun SessionEntity.toDomain() = Session(
    id = id,
    name = name,
    status = SessionStatus.valueOf(status),
    scheduledDate = scheduledDate,
    summary = summary,
    showCardTitles = showCardTitles,
    dmNotes = dmNotes,
    createdAt = createdAt,
    endedAt = endedAt,
    gameSystems = runCatching { json.decodeFromString<List<String>>(gameSystemsJson) }.getOrDefault(listOf("General")),
    linkedMythicSessionId = linkedMythicSessionId
)

fun Session.toEntity() = SessionEntity(
    id = id,
    name = name,
    status = status.name,
    scheduledDate = scheduledDate,
    summary = summary,
    showCardTitles = showCardTitles,
    dmNotes = dmNotes,
    createdAt = createdAt,
    endedAt = endedAt,
    gameSystemsJson = json.encodeToString(gameSystems),
    linkedMythicSessionId = linkedMythicSessionId
)

fun SessionEntity.toBackupDto() = SessionBackupDto(
    id = id, name = name, status = status, scheduledDate = scheduledDate, 
    summary = summary, createdAt = createdAt, endedAt = endedAt, 
    showCardTitles = showCardTitles, dmNotes = dmNotes, gameSystemsJson = gameSystemsJson
)

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

fun SessionTableRefEntity.toDomain() = SessionTableRef(
    sessionId = sessionId,
    tableId = tableId,
    sortOrder = sortOrder
)

fun SessionTableRef.toEntity() = SessionTableRefEntity(
    sessionId = sessionId,
    tableId = tableId,
    sortOrder = sortOrder
)

// --- Wiki ---

fun WikiCategoryEntity.toDomain(entryCount: Int = 0) = WikiCategory(
    id = id,
    name = name,
    iconName = iconName,
    entryCount = entryCount
)

fun WikiCategory.toEntity() = WikiCategoryEntity(
    id = id,
    name = name,
    iconName = iconName
)

fun WikiEntryEntity.toDomain() = WikiEntry(
    id = id,
    title = title,
    content = content,
    categoryId = categoryId,
    imagePath = imagePath,
    isPinned = isPinned,
    lastUpdated = lastUpdated
)

fun WikiEntry.toEntity() = WikiEntryEntity(
    id = id,
    title = title,
    content = content,
    categoryId = categoryId,
    imagePath = imagePath,
    isPinned = isPinned,
    lastUpdated = lastUpdated
)

fun WikiEntryEntity.toBackupDto() = WikiEntryBackupDto(
    id = id, title = title, content = content, categoryId = categoryId, 
    imagePath = imagePath, isPinned = isPinned, lastUpdated = lastUpdated
)

// --- Reference ---

fun ReferenceTableEntity.toDomain(
    rows: List<ReferenceRowEntity> = emptyList(),
    tags: List<Tag> = emptyList()
) = ReferenceTable(
    id = id,
    name = name,
    description = description,
    gameSystem = gameSystem,
    category = category,
    columns = runCatching { json.decodeFromString<List<ReferenceColumn>>(columnsJson) }.getOrDefault(emptyList()),
    rows = rows.map { it.toDomain() },
    tags = tags,
    isPinned = isPinned,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    createdAt = createdAt
)

fun ReferenceTable.toEntity() = ReferenceTableEntity(
    id = id,
    name = name,
    description = description,
    gameSystem = gameSystem,
    category = category,
    columnsJson = json.encodeToString(columns),
    isPinned = isPinned,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    createdAt = createdAt
)

fun ReferenceTableEntity.toBackupDto() = ReferenceTableBackupDto(
    id = id, name = name, description = description, gameSystem = gameSystem, 
    category = category, columnsJson = columnsJson, isPinned = isPinned, 
    sortOrder = sortOrder, createdAt = createdAt, sourcePack = sourcePack
)

fun ReferenceRowEntity.toDomain() = ReferenceRow(
    id = id,
    tableId = tableId,
    cells = runCatching { json.decodeFromString<List<String>>(cellsJson) }.getOrDefault(emptyList()),
    sortOrder = sortOrder
)

fun ReferenceRow.toEntity(tableId: Long) = ReferenceRowEntity(
    id = id,
    tableId = tableId,
    cellsJson = json.encodeToString(cells),
    sortOrder = sortOrder
)

fun ReferenceRowEntity.toBackupDto() = ReferenceRowBackupDto(
    id = id, tableId = tableId, cellsJson = cellsJson, sortOrder = sortOrder
)

fun SystemRuleEntity.toDomain(tags: List<Tag> = emptyList()) = SystemRule(
    id = id,
    title = title,
    content = content,
    gameSystem = gameSystem,
    category = category,
    tags = tags,
    isPinned = isPinned,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    lastUpdated = lastUpdated
)

fun SystemRule.toEntity() = SystemRuleEntity(
    id = id,
    title = title,
    content = content,
    gameSystem = gameSystem,
    category = category,
    isPinned = isPinned,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    lastUpdated = lastUpdated
)

fun SystemRuleEntity.toBackupDto() = SystemRuleBackupDto(
    id = id,
    title = title,
    content = content,
    gameSystem = gameSystem,
    category = category,
    isPinned = isPinned,
    sortOrder = sortOrder,
    sourcePack = sourcePack,
    lastUpdated = lastUpdated
)

// ── Session Planning ────────────────────────────────────────────────────────

fun SceneEntity.toDomain() = Scene(
    id = id,
    sessionId = sessionId,
    title = title,
    content = content,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    linkedTableId = linkedTableId,
    linkedDeckId = linkedDeckId,
    imagePath = imagePath,
    isAlternative = isAlternative
)

fun Scene.toEntity() = SceneEntity(
    id = id,
    sessionId = sessionId,
    title = title,
    content = content,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    linkedTableId = linkedTableId,
    linkedDeckId = linkedDeckId,
    imagePath = imagePath,
    isAlternative = isAlternative
)
