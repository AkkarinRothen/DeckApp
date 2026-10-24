package com.deckapp.core.data.repository

import com.deckapp.core.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardMemoryCodecTest {
    @Test
    fun `old board memory decodes with new board fields defaulted`() {
        val memory = BoardMemoryCodec.decode("{\"focusedSceneId\":123}")
        assertEquals(123L, memory.focusedSceneId)
        assertTrue(memory.boardItems.isEmpty())
        assertTrue(memory.boardGroups.isEmpty())
        assertTrue(memory.boardUndoHistory.isEmpty())
        assertEquals(BoardPlayMode.PREPARATION, memory.boardMode)
    }

    @Test
    fun `board snapshot and activity survive a round trip`() {
        val item = SessionBoardItem(
            id = 7,
            sessionId = 2,
            type = SessionBoardItemType.TABLE,
            resourceId = 99,
            boardView = SessionBoardView.EXPLORATION,
            groupId = 5
        )
        val group = SessionBoardGroup(id = 5, sessionId = 2, boardView = SessionBoardView.EXPLORATION, name = "Viaje")
        val memory = BoardSessionMemory(
            boardItems = listOf(item),
            boardGroups = listOf(group),
            boardUndoHistory = listOf(BoardUndoSnapshot("mover bloque", listOf(item), listOf(group))),
            blockLastUsedAt = mapOf(7L to 1234L),
            boardMode = BoardPlayMode.PLAY,
            activityTrackingStartedAt = 1000L
        )

        assertEquals(memory, BoardMemoryCodec.decode(BoardMemoryCodec.encode(memory)))
    }

    @Test
    fun `saved board templates can be serialized independently`() {
        val template = SavedBoardTemplate(
            id = "template-1",
            name = "Exploración",
            groups = listOf(BoardTemplateGroup(name = "Viaje", position = 0)),
            items = listOf(BoardTemplateItem(SessionBoardItemType.TABLE, resourceId = 91, groupIndex = 0))
        )
        assertEquals(listOf(template), BoardMemoryCodec.decodeTemplates(BoardMemoryCodec.encodeTemplates(listOf(template))))
    }

    @Test
    fun `Tarokka card positions survive a board memory round trip`() {
        val memory = BoardSessionMemory(tarokkaCardsByDeck = mapOf(42L to listOf(101L, 103L, 107L)))
        assertEquals(memory, BoardMemoryCodec.decode(BoardMemoryCodec.encode(memory)))
    }

    @Test
    fun `playmat groups and mixed members survive a board memory round trip`() {
        val group = PlaymatElementGroup(
            id = "group-1",
            name = "Investigación",
            memberIds = setOf("card:9", "token:ally", "discard:4"),
            isLocked = true
        )
        val surface = PlaymatSurface(
            groups = listOf(group),
            cards = listOf(PlaymatCardPlacement(cardId = 9, x = 10f, y = 20f, clusterId = group.id)),
            tokens = listOf(PlaymatTokenPlacement(id = "ally", name = "Aliada", x = 30f, y = 40f, groupId = group.id)),
            decks = listOf(PlaymatDeckPlacement(stackId = 4, x = 50f, y = 60f, discardX = 200f, discardY = 60f, discardGroupId = group.id))
        )
        val memory = BoardSessionMemory(playmatSurfaces = listOf(surface), activePlaymatSurfaceId = surface.id)

        assertEquals(memory, BoardMemoryCodec.decode(BoardMemoryCodec.encode(memory)))
        val legacySurface = BoardMemoryCodec.decode("""{"playmatSurfaces":[{"id":"legacy","name":"Mesa antigua"}]}""")
            .playmatSurfaces.single()
        assertTrue(legacySurface.groups.isEmpty())
    }
}
