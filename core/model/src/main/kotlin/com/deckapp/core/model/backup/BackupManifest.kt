package com.deckapp.core.model.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupManifest(
    val version: Int = 1,
    val appVersion: String,
    val createdAt: Long = System.currentTimeMillis(),
    val deviceModel: String,
    val dbVersion: Int,
    val counts: BackupCounts,
    val checksumSha256: String? = null
)

@Serializable
data class BackupCounts(
    val decks: Int,
    val cards: Int,
    val randomTables: Int,
    val npcs: Int,
    val wikiEntries: Int,
    val sessions: Int
)
