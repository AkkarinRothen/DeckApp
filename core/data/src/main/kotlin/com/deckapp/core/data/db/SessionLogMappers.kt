package com.deckapp.core.data.db

import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionLogEventType
import com.deckapp.core.model.SessionNote

fun SessionLogEntryEntity.toDomain(): SessionLogEntry {
    val type = try {
        SessionLogEventType.valueOf(eventType)
    } catch (_: Exception) {
        SessionLogEventType.MANUAL
    }
    return SessionLogEntry(
        id = id,
        sessionId = sessionId,
        sceneId = sceneId,
        eventType = type,
        title = title,
        details = details,
        payloadJson = payloadJson,
        timestamp = timestamp
    )
}

fun SessionLogEntry.toEntity(): SessionLogEntryEntity {
    return SessionLogEntryEntity(
        id = id,
        sessionId = sessionId,
        sceneId = sceneId,
        eventType = eventType.name,
        title = title,
        details = details,
        payloadJson = payloadJson,
        timestamp = timestamp
    )
}

fun SessionNoteEntity.toDomain(): SessionNote {
    return SessionNote(
        id = id,
        sessionId = sessionId,
        sceneId = sceneId,
        contentMarkdown = contentMarkdown,
        updatedAt = updatedAt
    )
}

fun SessionNote.toEntity(): SessionNoteEntity {
    return SessionNoteEntity(
        id = id,
        sessionId = sessionId,
        sceneId = sceneId,
        contentMarkdown = contentMarkdown,
        updatedAt = updatedAt
    )
}
