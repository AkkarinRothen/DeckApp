package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.ChoirGuideRules
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChoirGuideDelegateTest {
    private val repository = mockk<SessionRepository>()
    private var databaseNotes = "Notas existentes"
    private var persistedMemory = BoardSessionMemory()
    private val startingGuide = ChoirGuideRules.select(ChoirGuideMemory(), ChoirProcedure.EXPLORATION).copy(draftRoll = "6")
    private val ui = MutableStateFlow(SessionUiState(sessionId = 7,
        session = Session(id = 7, name = "Prueba", gameSystems = listOf("Choir of Flesh")),
        dmNotes = databaseNotes, boardMemory = BoardSessionMemory(focusedSceneId = 44, choirGuide = startingGuide)))

    init {
        every { repository.getSessionById(7) } answers { flowOf(Session(id = 7, name = "Prueba", dmNotes = databaseNotes)) }
        coEvery { repository.updateDmNotes(7, any()) } coAnswers { databaseNotes = secondArg() }
    }

    private fun create(scope: kotlinx.coroutines.CoroutineScope) = ChoirGuideDelegate(7, ui, scope, repository, Mutex()) { change ->
        persistedMemory = change(ui.value.boardMemory)
        ui.value = ui.value.copy(boardMemory = persistedMemory)
    }

    @Test fun `double tap confirms one step and one journal entry`() = runTest {
        val delegate = create(this)
        val revision = startingGuide.revision
        delegate.resolve("arrival_roll", revision)
        delegate.resolve("arrival_roll", revision)
        testScheduler.advanceUntilIdle()
        coVerify(exactly = 1) { repository.updateDmNotes(7, any()) }
        assertEquals(1, Regex("Choir · Procedimientos").findAll(databaseNotes).count())
        assertTrue(ui.value.boardMemory.choirGuide.journalOutbox.isEmpty())
        assertEquals(44L, persistedMemory.focusedSceneId)
        assertEquals("action", persistedMemory.choirGuide.progress.steps[ChoirProcedure.EXPLORATION])
    }

    @Test fun `failed write keeps durable outbox and retry does not duplicate optimistic note`() = runTest {
        var attempts = 0
        coEvery { repository.updateDmNotes(7, any()) } coAnswers {
            if (++attempts == 1) throw java.io.IOException("Guardado interrumpido")
            databaseNotes = secondArg()
        }
        val delegate = create(this)
        delegate.resolve("arrival_roll", startingGuide.revision).join()
        assertEquals(1, persistedMemory.choirGuide.journalOutbox.size)
        assertNotNull(ui.value.choirGuideError)
        delegate.retryJournal().join()
        assertEquals(2, attempts)
        assertTrue(persistedMemory.choirGuide.journalOutbox.isEmpty())
        assertEquals(1, Regex("Choir · Procedimientos").findAll(databaseNotes).count())
        assertTrue(databaseNotes.startsWith("Notas existentes"))
    }

    @Test fun `restarting after database commit only acknowledges existing event`() = runTest {
        val entry = ChoirGuideJournalEntry("already-committed", "Resultado", 123)
        databaseNotes += "\nResultado\n<!-- choir-guide:already-committed -->"
        ui.value = ui.value.copy(dmNotes = databaseNotes, boardMemory = ui.value.boardMemory.copy(
            choirGuide = startingGuide.copy(journalOutbox = listOf(entry))))
        create(this).retryJournal().join()
        coVerify(exactly = 0) { repository.updateDmNotes(any(), any()) }
        assertTrue(persistedMemory.choirGuide.journalOutbox.isEmpty())
    }

    @Test fun `table lookup never substitutes a different system or similar name`() {
        val wrong = RandomTable(id = 1, name = "Hitos en Ruinas", sourcePack = "Otro sistema")
        val similar = RandomTable(id = 2, name = "Hitos en Ruinas extra", sourcePack = "Choir of Flesh")
        val correct = RandomTable(id = 3, name = "Hitos en Ruinas", sourcePack = "Choir of Flesh")
        assertNull(ChoirGuideDelegate.findTable(ui.value.copy(allTables = listOf(wrong, similar)), "Hitos en Ruinas"))
        assertEquals(3L, ChoirGuideDelegate.findTable(ui.value.copy(allTables = listOf(wrong, correct)), "Hitos en Ruinas")?.id)
    }
}
