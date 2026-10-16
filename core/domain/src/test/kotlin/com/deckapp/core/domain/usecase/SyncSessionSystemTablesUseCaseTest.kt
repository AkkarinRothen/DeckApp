package com.deckapp.core.domain.usecase

import app.cash.turbine.test
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.Session
import com.deckapp.core.model.SystemRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncSessionSystemTablesUseCaseTest {
    private val sessions = mockk<SessionRepository>()
    private val tables = mockk<TableRepository>()
    private val references = mockk<ReferenceRepository>()
    private val session = MutableStateFlow<Session?>(Session(id = 7, name = "Partida", gameSystems = listOf("Choir of Flesh")))
    private val available = MutableStateFlow(listOf(RandomTable(id = 11, name = "Ancla", sourcePack = "choir_of_flesh.json")))
    private val rules = MutableStateFlow(listOf(SystemRule(title = "Chequeos", gameSystem = "Choir of Flesh", sourcePack = "choir_of_flesh.json")))
    private val referenceTables = MutableStateFlow(emptyList<ReferenceTable>())
    private val assigned = MutableStateFlow(emptyList<RandomTable>())

    private fun useCase(): SyncSessionSystemTablesUseCase {
        every { sessions.getSessionById(7) } returns session
        every { sessions.getTablesForSession(7) } returns assigned
        every { tables.getAllTables() } returns available
        every { references.getAllSystemRules() } returns rules
        every { references.getAllReferenceTables() } returns referenceTables
        coEvery { sessions.addTableToSession(7, any()) } answers {
            assigned.value += available.value.first { it.id == secondArg<Long>() }
            Unit
        }
        return SyncSessionSystemTablesUseCase(sessions, tables, references)
    }

    @Test fun `existing session gets the selected system tables using asset pack identifiers`() = runTest {
        available.value += RandomTable(id = 12, name = "Otro sistema", sourcePack = "dnd_5e_2024.json")
        useCase()(7).test {
            assertEquals(listOf(11L), awaitItem())
            assertEquals(listOf(11L), assigned.value.map { it.id })
            coVerify(exactly = 0) { sessions.addTableToSession(7, 12) }
        }
    }

    @Test fun `reference-only system packs can also supply random tables`() = runTest {
        rules.value = emptyList()
        referenceTables.value = listOf(ReferenceTable(name = "Dificultades", gameSystem = "Choir of Flesh", sourcePack = "choir_of_flesh.json"))
        useCase()(7).test {
            assertEquals(listOf(11L), awaitItem())
            coVerify(exactly = 1) { sessions.addTableToSession(7, 11) }
        }
    }

    @Test fun `manual tables and existing system assignments are preserved without duplicates`() = runTest {
        assigned.value = listOf(RandomTable(id = 90, name = "Mi tabla"), available.value.first())
        useCase()(7).test {
            awaitItem()
            coVerify(exactly = 0) { sessions.addTableToSession(any(), any()) }
            assertEquals(listOf(90L, 11L), assigned.value.map { it.id })
        }
    }

    @Test fun `installing the pack while the session is open adds its newly available tables`() = runTest {
        available.value = emptyList()
        useCase()(7).test {
            assertEquals(emptyList<Long>(), awaitItem())
            available.value = listOf(RandomTable(id = 11, name = "Ancla", sourcePack = "choir_of_flesh.json"))
            assertEquals(listOf(11L), awaitItem())
            coVerify(exactly = 1) { sessions.addTableToSession(7, 11) }
        }
    }

    @Test fun `changing the selected system loads its resources without deleting manual choices`() = runTest {
        session.value = session.value!!.copy(gameSystems = listOf("General"))
        assigned.value = listOf(RandomTable(id = 90, name = "Mi tabla"))
        useCase()(7).test {
            assertEquals(emptyList<Long>(), awaitItem())
            session.value = session.value!!.copy(gameSystems = listOf("Choir of Flesh"))
            assertEquals(listOf(11L), awaitItem())
            assertEquals(listOf(90L, 11L), assigned.value.map { it.id })
        }
    }

    @Test fun `new tables in an installed pack do not duplicate earlier session resources`() = runTest {
        useCase()(7).test {
            awaitItem()
            available.value += RandomTable(id = 13, name = "Ocupaciones", sourcePack = "choir_of_flesh.json")
            assertEquals(listOf(11L, 13L), awaitItem())
            coVerify(exactly = 1) { sessions.addTableToSession(7, 11) }
            coVerify(exactly = 1) { sessions.addTableToSession(7, 13) }
        }
    }

    @Test fun `general sessions and unowned tables are not assigned every installed pack`() = runTest {
        session.value = session.value!!.copy(gameSystems = listOf("General"))
        rules.value += SystemRule(title = "General", gameSystem = "General", sourcePack = "choir_of_flesh.json")
        available.value += RandomTable(id = 14, name = "Sin paquete")
        useCase()(7).test {
            assertEquals(emptyList<Long>(), awaitItem())
            coVerify(exactly = 0) { sessions.addTableToSession(any(), any()) }
        }
    }
}
