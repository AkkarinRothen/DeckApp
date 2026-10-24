package com.deckapp.feature.draw.components

import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemType

internal enum class BoardWorkspaceMode(val label: String) {
    GRID("Grilla"), DM("DM"), SOLO("Solo"), BENTO("Bloques compactos")
}

internal fun boardWorkspaceColumns(widthDp: Int): Int = ((widthDp + 10) / 250).coerceIn(1, 4)

internal fun boardWorkspaceBentoColumns(widthDp: Int): Int = ((widthDp + 12) / 188).coerceIn(2, 4)

internal fun boardWorkspacePrimary(
    items: List<SessionBoardItem>,
    mode: BoardWorkspaceMode,
    selectedId: Long?
): SessionBoardItem? {
    if (mode == BoardWorkspaceMode.GRID || mode == BoardWorkspaceMode.BENTO) return null
    items.firstOrNull { it.id == selectedId }?.let { return it }
    val priorities = when (mode) {
        BoardWorkspaceMode.SOLO -> listOf(SessionBoardItemType.NOTES, SessionBoardItemType.MYTHIC)
        else -> listOf(SessionBoardItemType.COMBAT, SessionBoardItemType.WIKI_MAP, SessionBoardItemType.NOTES)
    }
    return priorities.firstNotNullOfOrNull { type -> items.firstOrNull { it.type == type } }
        ?: items.firstOrNull()
}

internal fun boardWorkspaceTools(
    items: List<SessionBoardItem>,
    mode: BoardWorkspaceMode,
    primaryId: Long?
): List<SessionBoardItem> {
    if (mode == BoardWorkspaceMode.GRID || mode == BoardWorkspaceMode.BENTO) return emptyList()
    val candidates = items.filter { it.id != primaryId }
    val pinned = candidates.filter { it.isPinned }
    if (pinned.isNotEmpty()) return pinned
    val priorities = when (mode) {
        BoardWorkspaceMode.SOLO -> listOf(SessionBoardItemType.MYTHIC, SessionBoardItemType.DICE, SessionBoardItemType.TABLE)
        BoardWorkspaceMode.BENTO -> emptyList()
        else -> listOf(SessionBoardItemType.COMBAT, SessionBoardItemType.CLOCK, SessionBoardItemType.DICE, SessionBoardItemType.NPCS)
    }
    return priorities.mapNotNull { type -> candidates.firstOrNull { it.type == type } }.take(3)
}
