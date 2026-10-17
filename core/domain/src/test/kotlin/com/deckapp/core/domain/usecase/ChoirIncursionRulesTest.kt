package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ChoirIncursionRulesTest {

    @Test
    fun `grid offers only printed passages and moves to chosen node`() {
        val inc = ChoirIncursion()
        assertEquals(0, inc.currentNodeIndex)
        assertEquals(0, inc.songLevel)
        assertEquals(ChoirNodeState.CURRENT, inc.nodes[0].state)
        assertEquals(ChoirNodeState.UNEXPLORED, inc.nodes[1].state)

        assertEquals(listOf(1, 3), ChoirIncursionRules.destinations(inc))
        assertEquals(0, ChoirIncursionRules.advance(inc, 8).currentNodeIndex)
        val advanced = ChoirIncursionRules.advance(inc, 3)
        assertEquals(3, advanced.currentNodeIndex)
        assertEquals(0, advanced.songLevel)
        assertEquals(ChoirNodeState.CLEARED, advanced.nodes[0].state)
        assertEquals(ChoirNodeState.CURRENT, advanced.nodes[3].state)
        assertEquals(listOf(0, 4, 6), ChoirIncursionRules.destinations(advanced))
    }

    @Test
    fun `clear current node stores loot and handles terminal node`() {
        val inc = ChoirIncursionRules.advance(ChoirIncursion())
        val cleared = ChoirIncursionRules.clearCurrentNode(inc, "Amuleto de Diente")

        assertEquals(ChoirNodeState.CLEARED, cleared.nodes[1].state)
        assertTrue(cleared.lootCollected.contains("Amuleto de Diente"))
        assertEquals(ChoirIncursionStatus.ACTIVE, cleared.status)

        // El nodo 8 no determina automáticamente el final de la incursión.
        var bossInc = inc.copy(currentNodeIndex = 8)
        bossInc = ChoirIncursionRules.clearCurrentNode(bossInc, "Reliquia de la Carne")
        assertEquals(ChoirIncursionStatus.ACTIVE, bossInc.status)
        assertTrue(bossInc.lootCollected.contains("Reliquia de la Carne"))
    }

    @Test
    fun `extract to settlement logs journey without granting invented morale`() {
        val inc = ChoirIncursionRules.clearCurrentNode(ChoirIncursionRules.advance(ChoirIncursion()))
        val settlement = ChoirSettlement(morale = 5)

        val (extractedInc, updatedSettlement) = ChoirIncursionRules.extractToSettlement(inc, settlement)
        assertEquals(ChoirIncursionStatus.EXTRACTED_SUCCESS, extractedInc.status)
        assertEquals(5, updatedSettlement.morale)
        assertTrue(updatedSettlement.log.isNotEmpty())
    }

    @Test
    fun `codec persists incursion and settlement across markdown roundtrip`() {
        val notes = "# Notas de la Campaña\nTexto inicial del DM."
        val incursion = ChoirIncursion(title = "Criptas Escarlata", currentNodeIndex = 2, songLevel = 3)
        val settlement = ChoirSettlement(name = "Bastión de Ceniza", stockRations = 10)

        val withIncursion = ChoirIncursionCodec.writeIncursion(notes, incursion)
        val withBoth = ChoirIncursionCodec.writeSettlement(withIncursion, settlement)

        val restoredIncursion = ChoirIncursionCodec.readIncursion(withBoth)
        val restoredSettlement = ChoirIncursionCodec.readSettlement(withBoth)

        assertNotNull(restoredIncursion)
        assertEquals("Criptas Escarlata", restoredIncursion?.title)
        assertEquals(2, restoredIncursion?.currentNodeIndex)
        assertEquals(3, restoredIncursion?.songLevel)

        assertNotNull(restoredSettlement)
        assertEquals("Bastión de Ceniza", restoredSettlement?.name)
        assertEquals(10, restoredSettlement?.stockRations)

        // Verify initial text remains intact
        assertTrue(withBoth.startsWith("# Notas de la Campaña\nTexto inicial del DM."))
    }
}
