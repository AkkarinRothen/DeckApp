package com.deckapp.core.data.db

import com.deckapp.core.model.*

fun MythicSessionEntity.toDomain() = MythicSession(
    id = id,
    name = name,
    chaosFactor = chaosFactor,
    sceneNumber = sceneNumber,
    actionTableId = actionTableId,
    subjectTableId = subjectTableId,
    createdAt = createdAt
)

fun MythicSession.toEntity() = MythicSessionEntity(
    id = id,
    name = name,
    chaosFactor = chaosFactor,
    sceneNumber = sceneNumber,
    actionTableId = actionTableId,
    subjectTableId = subjectTableId,
    createdAt = createdAt
)

fun MythicCharacterEntity.toDomain() = MythicCharacter(
    id = id,
    sessionId = sessionId,
    name = name,
    notes = notes,
    sortOrder = sortOrder
)

fun MythicCharacter.toEntity() = MythicCharacterEntity(
    id = id,
    sessionId = sessionId,
    name = name,
    notes = notes,
    sortOrder = sortOrder
)

fun MythicThreadEntity.toDomain() = MythicThread(
    id = id,
    sessionId = sessionId,
    description = description,
    isResolved = isResolved,
    sortOrder = sortOrder
)

fun MythicThread.toEntity() = MythicThreadEntity(
    id = id,
    sessionId = sessionId,
    description = description,
    isResolved = isResolved,
    sortOrder = sortOrder
)

fun MythicRollEntity.toDomain() = MythicRoll(
    id = id,
    sessionId = sessionId,
    question = question,
    probability = ProbabilityLevel.valueOf(probability),
    chaosFactor = chaosFactor,
    roll = roll,
    result = FateResult.valueOf(result),
    isRandomEvent = isRandomEvent,
    eventAction = eventAction,
    eventSubject = eventSubject,
    sceneNumber = sceneNumber,
    timestamp = timestamp
)

fun MythicRoll.toEntity() = MythicRollEntity(
    id = id,
    sessionId = sessionId,
    question = question,
    probability = probability.name,
    chaosFactor = chaosFactor,
    roll = roll,
    result = result.name,
    isRandomEvent = isRandomEvent,
    eventAction = eventAction,
    eventSubject = eventSubject,
    sceneNumber = sceneNumber,
    timestamp = timestamp
)
