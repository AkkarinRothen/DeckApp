package com.deckapp.feature.draw.components

import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemType
import org.junit.Assert.*
import org.junit.Test

class BoardWorkspacePolicyTest {
    private fun block(id: Long, type: SessionBoardItemType, pinned: Boolean = false) =
        SessionBoardItem(id = id, sessionId = 1, type = type, isPinned = pinned)

    private val notes = block(1, SessionBoardItemType.NOTES)
    private val combat = block(2, SessionBoardItemType.COMBAT)
    private val oracle = block(3, SessionBoardItemType.MYTHIC)
    private val dice = block(4, SessionBoardItemType.DICE)
    private val items = listOf(oracle, notes, dice, combat)

    @Test fun `las columnas responden al ancho util tras abrir paneles`() {
        assertEquals(1, boardWorkspaceColumns(360))
        assertEquals(2, boardWorkspaceColumns(600))
        assertEquals(3, boardWorkspaceColumns(900))
        assertEquals(4, boardWorkspaceColumns(1200))
        assertEquals(4, boardWorkspaceColumns(3000))
        assertEquals(1, boardWorkspaceColumns(0))
    }

    @Test fun `solo prioriza diario y dm prioriza combate sin depender del orden`() {
        assertEquals(notes, boardWorkspacePrimary(items, BoardWorkspaceMode.SOLO, null))
        assertEquals(combat, boardWorkspacePrimary(items, BoardWorkspaceMode.DM, null))
    }

    @Test fun `la seleccion manual tiene prioridad y se recupera al borrar ese bloque`() {
        assertEquals(dice, boardWorkspacePrimary(items, BoardWorkspaceMode.SOLO, dice.id))
        assertEquals(notes, boardWorkspacePrimary(items - dice, BoardWorkspaceMode.SOLO, dice.id))
    }

    @Test fun `la grilla conserva todos los bloques sin protagonista ni herramientas extraidas`() {
        assertNull(boardWorkspacePrimary(items, BoardWorkspaceMode.GRID, notes.id))
        assertTrue(boardWorkspaceTools(items, BoardWorkspaceMode.GRID, null).isEmpty())
    }

    @Test fun `las herramientas solo complementan el diario sin duplicarlo`() {
        assertEquals(listOf(oracle, dice), boardWorkspaceTools(items, BoardWorkspaceMode.SOLO, notes.id))
    }

    @Test fun `los bloques fijados reemplazan sugerencias y conservan su orden`() {
        val table = block(5, SessionBoardItemType.TABLE, pinned = true)
        val clock = block(6, SessionBoardItemType.CLOCK, pinned = true)
        val pinnedPrimary = notes.copy(isPinned = true)
        assertEquals(
            listOf(clock, table),
            boardWorkspaceTools(listOf(pinnedPrimary, oracle, clock, dice, table), BoardWorkspaceMode.SOLO, notes.id)
        )
    }

    @Test fun `una vista vacia o sin tipos preferidos sigue siendo utilizable`() {
        assertNull(boardWorkspacePrimary(emptyList(), BoardWorkspaceMode.DM, null))
        assertTrue(boardWorkspaceTools(emptyList(), BoardWorkspaceMode.SOLO, null).isEmpty())
        assertEquals(dice, boardWorkspacePrimary(listOf(dice), BoardWorkspaceMode.DM, null))
        assertTrue(boardWorkspaceTools(listOf(dice), BoardWorkspaceMode.DM, dice.id).isEmpty())
    }
}
