package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableRollResult
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RollTableManualSelectionTest {
    @Test fun `manual die selects the matching row and persists resolved text`() = runTest {
        val repository = mockk<TableRepository>()
        val table = RandomTable(id = 7, name = "Hallazgos", rollFormula = "1d10", entries = listOf(
            TableEntry(minRoll = 1, maxRoll = 3, text = "Nada"),
            TableEntry(minRoll = 4, maxRoll = 6, text = "[1d1] reliquia")
        ))
        coEvery { repository.getTableWithEntries(7) } returns table
        val saved = slot<TableRollResult>()
        coEvery { repository.saveRollResult(capture(saved)) } returns 1L

        val result = RollTableUseCase(repository).selectEntry(7, 1, 42, 5)

        assertEquals(5, result?.rollValue)
        assertEquals("1 reliquia", result?.resolvedText)
        assertEquals(42L, saved.captured.sessionId)
        assertEquals(result, saved.captured)
    }

    @Test fun `manual die outside selected range changes nothing`() = runTest {
        val repository = mockk<TableRepository>()
        coEvery { repository.getTableWithEntries(7) } returns RandomTable(id = 7, name = "Hallazgos",
            entries = listOf(TableEntry(minRoll = 4, maxRoll = 6, text = "Reliquia")))

        assertNull(RollTableUseCase(repository).selectEntry(7, 0, 42, 2))
        coVerify(exactly = 0) { repository.saveRollResult(any()) }
    }
}
