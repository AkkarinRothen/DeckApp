package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Test

class UpdateChoirCombatStateUseCaseTest {
    @Test fun `rapid repeated commands accumulate against latest saved state`() = runTest {
        val repository = mockk<EncounterRepository>(relaxed = true)
        var current = EncounterCreature(id = 7, encounterId = 4, name = "Amalgama", maxHp = 1, currentHp = 1,
            notes = "${ChoirProfile.MARKER}\n- **Iniciativa**: 5")
        coEvery { repository.getCreatureById(7) } answers { current }
        coEvery { repository.updateCreature(any()) } answers { current = firstArg() }
        val update = UpdateChoirCombatStateUseCase(repository)
        val jobs = List(10) { launch { update(7, ChoirCombatCommand.ADD_MINOR) } }
        jobs.forEach { it.join() }
        assertEquals(10, ChoirCombatState.fromNotes(current.notes).minor)
    }

    @Test fun `updates encounter notes preserves identity hp portrait and logs wounds`() = runTest {
        val repository = mockk<EncounterRepository>(relaxed = true)
        val creature = EncounterCreature(id = 7, encounterId = 4, name = "Amalgama", maxHp = 18, currentHp = 12,
            imagePath = "portrait.webp", notes = "${ChoirProfile.MARKER}\n- **Iniciativa**: 5\n**Ataque Estándar:** Golpe\n**Ataque Especial:** Agarrar\n**Acción de Apoyo:** Regenerar")
        coEvery { repository.getCreatureById(7) } returns creature
        val captured = slot<EncounterCreature>()
        coEvery { repository.updateCreature(capture(captured)) } just Runs
        val state = ChoirCombatState(1, 0, 0, 0)
        UpdateChoirCombatStateUseCase(repository)(7, ChoirCombatCommand.ADD_MAJOR)
        assertEquals(creature.copy(notes = state.writeTo(creature.notes)), captured.captured)
        coVerify { repository.recordLog(match { it.encounterId == 4L && it.message.contains("1 mayores, 0 menores") }) }
    }

    @Test fun `never changes a generic encounter creature`() = runTest {
        val repository = mockk<EncounterRepository>(relaxed = true)
        coEvery { repository.getCreatureById(7) } returns EncounterCreature(id = 7, encounterId = 4,
            name = "Goblin", maxHp = 7, currentHp = 7)
        UpdateChoirCombatStateUseCase(repository)(7, ChoirCombatCommand.ADD_MAJOR)
        coVerify(exactly = 0) { repository.updateCreature(any()) }
    }
}
