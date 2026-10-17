package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChoirProcedure {
    EXPLORATION, INCURSION_SETUP, INCURSION, ANGUISH,
    CHARACTER, SETTLEMENT_SETUP, SETTLEMENT, CHECKS, COMBAT, AFTERMATH, CORRUPTION
}

/** Procedure counters are independent of generic hex exploration and the tactical map. */
@Serializable
data class ChoirGuideProgress(
    val active: ChoirProcedure? = null,
    val steps: Map<ChoirProcedure, String> = emptyMap(),
    val day: Int = 1,
    val turnsSpent: Int = 0,
    val incursionActions: Int = 15,
    val incursionPrepared: Boolean = false,
    val extractionNode: Int? = null,
    val hazardousNodes: List<Int> = emptyList(),
    val pointsPrepared: Int = 0
)

@Serializable
data class ChoirAnguishCheck(
    val id: String,
    val characterId: Long? = null,
    val characterName: String = "Personaje",
    val anguish: Int,
    val roll: Int? = null
)

/** Persisted outbox: retrying after interruption must not duplicate a diary entry. */
@Serializable
data class ChoirGuideJournalEntry(val id: String, val text: String, val timestamp: Long)

@Serializable
data class ChoirGuideDraft(val roll: String = "", val note: String = "", val method: String = "dado físico / resolución manual")

@Serializable
data class ChoirGuideMemory(
    val progress: ChoirGuideProgress = ChoirGuideProgress(),
    val revision: Long = 0,
    val history: List<ChoirGuideProgress> = emptyList(),
    val selectedCharacterId: Long? = null,
    val observedAnguish: Map<Long, Int> = emptyMap(),
    val anguishChecks: List<ChoirAnguishCheck> = emptyList(),
    val draftRoll: String = "",
    val draftNote: String = "",
    val draftMethod: String = "dado físico / resolución manual",
    val suspendedDraft: ChoirGuideDraft? = null,
    val lastResolution: String = "",
    val journalOutbox: List<ChoirGuideJournalEntry> = emptyList()
)
