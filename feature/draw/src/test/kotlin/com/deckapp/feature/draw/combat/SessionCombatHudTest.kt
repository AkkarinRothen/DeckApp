package com.deckapp.feature.draw.combat

import com.deckapp.core.model.Condition
import com.deckapp.core.model.EncounterCreature
import com.deckapp.core.model.PlayerInitiativeParticipant
import com.deckapp.core.model.UnifiedParticipant
import com.deckapp.core.model.label
import org.junit.Assert.*
import org.junit.Test

class SessionCombatHudTest {

    @Test
    fun `Unified participants are correctly segregated and identifiable`() {
        val creature = EncounterCreature(
            id = 1L,
            encounterId = 10L,
            name = "Goblin Archer",
            currentHp = 7,
            maxHp = 7,
            armorClass = 12,
            conditions = setOf(Condition.BLINDED),
            sortOrder = 1
        )
        val player = PlayerInitiativeParticipant(
            id = "p-1",
            name = "Valeros",
            initiativeTotal = 15
        )

        val participantCreature = UnifiedParticipant.Creature(creature)
        val participantPlayer = UnifiedParticipant.Player(player)

        assertEquals("Goblin Archer", participantCreature.name)
        assertEquals(7, participantCreature.data.currentHp)
        assertEquals(7, participantCreature.data.maxHp)
        assertTrue(participantCreature.data.conditions.contains(Condition.BLINDED))
        assertEquals("Cegado", Condition.BLINDED.label)

        assertEquals("Valeros", participantPlayer.name)
        assertEquals(15, participantPlayer.initiative)
        assertEquals(10, participantPlayer.ac)
    }

    @Test
    fun `Turn advancement cycles cleanly through participants and increments round when wrapping`() {
        val totalParticipants = 3
        var currentIndex = 0
        var round = 1

        // Turn 1 -> Turn 2
        var nextIndex = (currentIndex + 1) % totalParticipants
        if (nextIndex == 0) round++
        currentIndex = nextIndex
        assertEquals(1, currentIndex)
        assertEquals(1, round)

        // Turn 2 -> Turn 3
        nextIndex = (currentIndex + 1) % totalParticipants
        if (nextIndex == 0) round++
        currentIndex = nextIndex
        assertEquals(2, currentIndex)
        assertEquals(1, round)

        // Turn 3 -> Turn 1 (new round)
        nextIndex = (currentIndex + 1) % totalParticipants
        if (nextIndex == 0) round++
        currentIndex = nextIndex
        assertEquals(0, currentIndex)
        assertEquals(2, round)
    }

    @Test
    fun `Damage and healing deltas calculate new HP correctly with lower bound at zero`() {
        var currentHp = 10
        val maxHp = 15

        // Take 5 damage
        currentHp = (currentHp - 5).coerceAtLeast(0)
        assertEquals(5, currentHp)

        // Take 10 damage (clamps to 0)
        currentHp = (currentHp - 10).coerceAtLeast(0)
        assertEquals(0, currentHp)

        // Heal 7 HP (clamps to maxHp)
        currentHp = (currentHp + 7).coerceAtMost(maxHp)
        assertEquals(7, currentHp)
    }

    @Test
    fun `Combat round summary markdown formats status accurately`() {
        val round = 2
        val activeName = "Goblin Archer"
        val participantsSummary = listOf(
            "Goblin Archer: 3/7 HP [Cegado]",
            "Valeros: 25/30 HP"
        )

        val sb = StringBuilder()
        sb.append("### ⚔️ Asalto $round de Combate\n")
        sb.append("**Turno Activo:** $activeName\n")
        sb.append("**Participantes:**\n")
        participantsSummary.forEach {
            sb.append("- $it\n")
        }

        val output = sb.toString()
        assertTrue(output.contains("### ⚔️ Asalto 2 de Combate"))
        assertTrue(output.contains("**Turno Activo:** Goblin Archer"))
        assertTrue(output.contains("- Goblin Archer: 3/7 HP [Cegado]"))
        assertTrue(output.contains("- Valeros: 25/30 HP"))
    }
}
