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
    val activePlaymatSurfaceId: String? = null,
    val boardItems: List<SessionBoardItem> = emptyList(),
    val boardGroups: List<SessionBoardGroup> = emptyList(),
    val boardUndoHistory: List<BoardUndoSnapshot> = emptyList(),
    val blockLastUsedAt: Map<Long, Long> = emptyMap(),
    val boardMode: BoardPlayMode = BoardPlayMode.PREPARATION,
    val activityTrackingStartedAt: Long = 0L,
    /** Cartas asignadas a las cinco posiciones de Tarokka, agrupadas por mazo. */
    val tarokkaCardsByDeck: Map<Long, List<Long>> = emptyMap()
)

@Serializable
data class BoardUndoSnapshot(
    val label: String,
    val items: List<SessionBoardItem>,
    val groups: List<SessionBoardGroup>,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class BoardPlayMode { PREPARATION, PLAY }

@Serializable
data class SavedBoardTemplate(
    val id: String,
    val name: String,
    val items: List<BoardTemplateItem>,
    val groups: List<BoardTemplateGroup>,
    val savedAt: Long = System.currentTimeMillis()
)

@Serializable
data class BoardTemplateItem(
    val type: SessionBoardItemType,
    val resourceId: Long? = null,
    val titleOverride: String? = null,
    val position: Int = 0,
    val size: SessionBoardItemSize = SessionBoardItemSize.M,
    val displayMode: String? = null,
    val groupIndex: Int? = null
)

@Serializable
data class BoardTemplateGroup(
    val name: String,
    val accentColorHex: String? = null,
    val iconName: String? = null,
    val preferredLayoutMode: SessionBoardLayoutMode = SessionBoardLayoutMode.GRID,
    val position: Int = 0,
    val autoFilterType: String? = null
)
