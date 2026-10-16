package com.deckapp.core.domain.usecase

import com.deckapp.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ChoirCombatRulesTest {
    @Test fun `dynamic target uses chapter III table including modifier extremes`() {
        assertEquals(11, ChoirCombatRules.dynamicTarget("Promedio", 0))
        assertEquals(13, ChoirCombatRules.dynamicTarget("Promedio", 2))
        assertEquals(14, ChoirCombatRules.dynamicTarget("Difícil", 1))
        assertEquals(2, ChoirCombatRules.dynamicTarget("Muy Fácil", -5))
        assertEquals(27, ChoirCombatRules.dynamicTarget("Casi Imposible", 10))
        assertNull(ChoirCombatRules.dynamicTarget("—", 0))
        assertNull(ChoirCombatRules.dynamicTarget("Promedio", 11))
    }

    private val profile = ChoirProfile(mapOf("Iniciativa" to "18"), "Estándar", "Especial", "Apoyo", "")
    private val notes = """
        <!-- choir-of-flesh:v2 -->
        ### Estadísticas de Choir of Flesh
        - **Precisión**: 0
        - **Ataque**: Promedio
        - **CD**: Fácil
        - **Iniciativa**: 18
        **Ataque Estándar:** Fragmento (Rango 3, daño Perforante)
        **Ataque Especial:** Barricada: crea cobertura 3 rondas.
        **Acción de Apoyo:** Reforzar: aumenta CD una etapa.
    """.trimIndent()

    @Test fun `all uninjured action boundaries follow chapter V`() {
        assertEquals(List(6) { "Estándar" } + List(4) { "Especial" },
            (1..10).map { ChoirCombatRules.action(profile, 0, it) })
    }

    @Test fun `one major wound unlocks support only on nine and ten`() {
        assertEquals(List(5) { "Estándar" } + List(3) { "Especial" } + List(2) { "Apoyo" },
            (1..10).map { ChoirCombatRules.action(profile, 1, it) })
    }

    @Test fun `two or more wounds use same table including flee`() {
        for (major in listOf(2, 3, 10)) assertEquals(
            List(4) { "Estándar" } + List(2) { "Especial" } + List(2) { "Apoyo" } + List(2) { "Huir" },
            (1..10).map { ChoirCombatRules.action(profile, major, it) })
    }

    @Test fun `absent special and support fall back to standard`() {
        val absent = profile.copy(special = "Ninguno.", support = "Ninguna.")
        assertEquals("Estándar", ChoirCombatRules.action(absent, 0, 10))
        assertEquals("Estándar", ChoirCombatRules.action(absent, 1, 10))
        assertEquals("Huir", ChoirCombatRules.action(absent, 2, 10))
    }

    @Test fun `absent support falls back to available special`() {
        assertEquals("Especial", ChoirCombatRules.action(profile.copy(support = "Ninguna."), 1, 9))
    }

    @Test fun `target table covers all six outcomes`() {
        assertTrue((1..2).all { ChoirCombatRules.target(it).startsWith("Más herido") })
        assertTrue((3..4).all { ChoirCombatRules.target(it).startsWith("Más cercano") })
        assertTrue((5..6).all { ChoirCombatRules.target(it).startsWith("Objetivo aleatorio") })
    }

    @Test fun `parser preserves dynamic grades ranges effects and fixed initiative`() {
        val parsed = ChoirProfile.fromNotes(notes)!!
        assertEquals("Fácil", parsed.statistics["CD"])
        assertEquals(18, parsed.initiative)
        assertTrue(parsed.standard.contains("Rango 3"))
        assertTrue(parsed.special.contains("3 rondas"))
        assertTrue(parsed.support.contains("una etapa"))
    }

    @Test fun `generic or malformed notes never enable Choir controls`() {
        assertNull(ChoirProfile.fromNotes("Iniciativa 18"))
        assertNull(ChoirProfile.fromNotes(ChoirProfile.MARKER + "\n- **Iniciativa**: errónea"))
    }

    @Test fun `Choir initiative ignores old d20 rolls and bonuses`() {
        val creature = EncounterCreature(encounterId = 1, name = "Tejedor", maxHp = 1, currentHp = 1,
            notes = notes, initiativeBonus = 7, initiativeRoll = 20)
        assertEquals(18, creature.initiativeTotal)
        assertEquals(27, creature.copy(notes = "").initiativeTotal)
    }

    @Test fun `Choir queue interleaves players using fixed initiative`() {
        val creature = EncounterCreature(id = 1, encounterId = 1, name = "Tejedor", maxHp = 1, currentHp = 1, notes = notes)
        val players = listOf(PlayerInitiativeParticipant(name = "Lento", initiativeTotal = 3),
            PlayerInitiativeParticipant(name = "Rápido", initiativeTotal = 20))
        assertEquals(listOf("Rápido", "Tejedor", "Lento"), OrderCombatParticipantsUseCase(listOf(creature), players).map { it.name })
    }

    @Test fun `wounds and last rolls round trip without duplicating markers or losing personal notes`() {
        val original = "Mi nota\n$notes"
        val state = ChoirCombatState(2, 3, 8, 4)
        val saved = state.writeTo(original)
        assertEquals(state, ChoirCombatState.fromNotes(saved))
        val changed = state.copy(major = 1).writeTo(saved)
        assertEquals(1, ChoirCombatState.fromNotes(changed).major)
        assertEquals(1, Regex("<!-- choir-state:").findAll(changed).count())
        assertTrue(changed.startsWith("Mi nota"))
        assertEquals(ChoirProfile.fromNotes(original), ChoirProfile.fromNotes(changed))
    }

    @Test fun `vendor upgrade replaces only exact legacy notes`() {
        assertEquals(notes, MergeChoirProfileUseCase.notes("antiguas", notes, "antiguas"))
        val edited = "Mi historia personal"
        val upgraded = MergeChoirProfileUseCase.notes(edited, notes, "antiguas")
        assertTrue(upgraded.contains(edited))
        assertNotNull(ChoirProfile.fromNotes(upgraded))
        assertEquals(upgraded, MergeChoirProfileUseCase.notes(upgraded, notes, "antiguas"))
    }
}
