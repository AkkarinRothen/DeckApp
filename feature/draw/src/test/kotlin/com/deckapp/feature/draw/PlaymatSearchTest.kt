package com.deckapp.feature.draw

import com.deckapp.core.model.Card
import com.deckapp.core.model.CardFace
import com.deckapp.core.model.PlaymatCardPlacement
import com.deckapp.core.model.PlaymatElementGroup
import com.deckapp.core.model.PlaymatSurface
import com.deckapp.core.model.PlaymatStickyNote
import com.deckapp.feature.draw.components.playmat.PlaymatSearchKind
import com.deckapp.feature.draw.components.playmat.buildPlaymatSearchResults
import com.deckapp.feature.draw.components.playmat.filterPlaymatSearchResults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaymatSearchTest {
    @Test
    fun `search matches partial text without case sensitivity and filters by type`() {
        val surface = PlaymatSurface(
            cards = listOf(PlaymatCardPlacement(cardId = 1L, x = 100f, y = 200f)),
            stickyNotes = listOf(PlaymatStickyNote(id = "note-1", text = "Find the hidden door", x = 0f, y = 0f))
        )
        val card = Card(
            id = 1L,
            stackId = 7L,
            originDeckId = 7L,
            title = "Goblin Scout",
            faces = listOf(CardFace(name = "Front"))
        )

        val results = buildPlaymatSearchResults(surface, mapOf(card.id to card), mapOf(7L to "Bestiary"))
        val partialCardMatch = filterPlaymatSearchResults(results, "GOB", PlaymatSearchKind.ALL)
        val contextMatch = filterPlaymatSearchResults(results, "BESTIARY", PlaymatSearchKind.CARD)
        val typeOnly = filterPlaymatSearchResults(results, "", PlaymatSearchKind.NOTE)

        assertEquals(listOf("card:1"), partialCardMatch.map { it.id })
        assertEquals(listOf("card:1"), contextMatch.map { it.id })
        assertEquals(listOf("note:note-1"), typeOnly.map { it.id })
    }

    @Test
    fun `hidden pieces are identified without changing group visibility`() {
        val hiddenGroup = PlaymatElementGroup(id = "hidden-room", name = "Cuarto secreto", isHidden = true)
        val surface = PlaymatSurface(
            cards = listOf(PlaymatCardPlacement(cardId = 1L, x = 100f, y = 200f, clusterId = hiddenGroup.id)),
            stickyNotes = listOf(PlaymatStickyNote(id = "note-1", text = "Pista antigua", x = 0f, y = 0f)),
            groups = listOf(hiddenGroup)
        )
        val card = Card(1L, 7L, 7L, "Llave", faces = listOf(CardFace(name = "Frente")))

        val results = buildPlaymatSearchResults(surface, mapOf(card.id to card), emptyMap())

        assertEquals(setOf(hiddenGroup.id), results.single { it.id == "card:1" }.hiddenGroupIds)
        assertEquals(setOf(hiddenGroup.id), results.single { it.id == "group:hidden-room" }.hiddenGroupIds)
        assertTrue(surface.groups.single().isHidden)
    }
}
