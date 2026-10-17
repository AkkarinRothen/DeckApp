package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.NpcRepository
import com.deckapp.core.model.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class UpdateChoirCharacterUseCaseTest {
    @Test fun `rest saves the library sheet and every active encounter copy`() = runTest {
        val npcs = mockk<NpcRepository>()
        val encounters = mockk<EncounterRepository>()
        val initial = ChoirCharacter(attributes = ChoirAttributes(constitution = 16, willpower = 16), vigor = 0)
        var npc = Npc(id = 9, name = "Iria", notes = ChoirCharacterCodec.write("Mi nota", initial))
        val copies = mutableMapOf(
            21L to EncounterCreature(id = 21, encounterId = 5, npcId = 9, name = "Iria", maxHp = 1,
                currentHp = 1, notes = ChoirCharacterCodec.write("A", initial)),
            22L to EncounterCreature(id = 22, encounterId = 6, npcId = 9, name = "Iria", maxHp = 1,
                currentHp = 1, notes = ChoirCharacterCodec.write("B", initial))
        )
        coEvery { npcs.getNpcById(9) } answers { npc }
        coEvery { npcs.saveNpc(any()) } answers { npc = firstArg(); 9L }
        coEvery { encounters.getCreatureById(any()) } answers { copies[firstArg()] }
        every { encounters.getAllEncounters() } returns flowOf(listOf(
            Encounter(id = 5, name = "Activo", isActive = true),
            Encounter(id = 6, name = "Preparado", isActive = false)))
        every { encounters.getEncounterById(5) } answers { flowOf(Encounter(id = 5, name = "Activo",
            isActive = true, creatures = listOfNotNull(copies[21]))) }
        coEvery { encounters.updateCreature(any()) } answers { val c = firstArg<EncounterCreature>(); copies[c.id] = c }

        assertTrue(UpdateChoirCharacterUseCase(npcs, encounters).saveSheet(22, initial.copy(vigor = 4)))

        assertEquals(4, ChoirCharacterCodec.read(npc.notes)?.vigor)
        assertEquals(4, ChoirCharacterCodec.read(copies[21]!!.notes)?.vigor)
        assertEquals(4, ChoirCharacterCodec.read(copies[22]!!.notes)?.vigor)
        assertTrue(copies[22]!!.notes.startsWith("B"))
    }
}
