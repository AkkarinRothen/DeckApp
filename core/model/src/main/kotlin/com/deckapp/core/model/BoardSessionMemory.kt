package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class BoardDistribution { GRID, DM, SOLO }

@Serializable
data class BoardViewMemory(
    val distribution: BoardDistribution = BoardDistribution.GRID,
    val primaryBlockId: Long? = null
)

@Serializable
enum class InspirationSource { TABLE, ORACLE, CARD }

@Serializable
enum class InspirationStatus { PENDING, USED, DISCARDED }

@Serializable
data class BoardInspiration(
    val id: String,
    val source: InspirationSource,
    val title: String,
    val text: String,
    val sceneTitle: String = "",
    val imagePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: InspirationStatus = InspirationStatus.PENDING
)

@Serializable
data class BoardSessionMemory(
    val views: Map<String, BoardViewMemory> = emptyMap(),
    val focusedSceneId: Long? = null,
    val inspiration: List<BoardInspiration> = emptyList(),
    val pinnedTableResults: Map<Long, TableRollResult> = emptyMap(),
    val choirGuide: ChoirGuideMemory = ChoirGuideMemory(),
    val playmatSurfaces: List<PlaymatSurface> = emptyList(),
    val activePlaymatSurfaceId: String? = null
)
