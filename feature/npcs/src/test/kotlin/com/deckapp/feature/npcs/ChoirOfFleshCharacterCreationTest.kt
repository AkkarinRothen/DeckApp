package com.deckapp.feature.npcs

import com.deckapp.core.model.Npc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoirOfFleshCharacterCreationTest {

    private fun calculateModifier(score: Int): Int = when {
        score <= 3 -> -3
        score in 4..5 -> -2
        score in 6..8 -> -1
        score in 9..12 -> 0
        score in 13..15 -> 1
        score in 16..17 -> 2
        else -> 3
    }

    @Test
    fun `attribute modifiers match Choir of Flesh official conversion table`() {
        // 3 o menos = -3
        assertEquals(-3, calculateModifier(1))
        assertEquals(-3, calculateModifier(3))

        // 4-5 = -2
        assertEquals(-2, calculateModifier(4))
        assertEquals(-2, calculateModifier(5))

        // 6-8 = -1
        assertEquals(-1, calculateModifier(6))
        assertEquals(-1, calculateModifier(8))

        // 9-12 = 0
        assertEquals(0, calculateModifier(9))
        assertEquals(0, calculateModifier(10))
        assertEquals(0, calculateModifier(12))

        // 13-15 = +1
        assertEquals(1, calculateModifier(13))
        assertEquals(1, calculateModifier(15))

        // 16-17 = +2
        assertEquals(2, calculateModifier(16))
        assertEquals(2, calculateModifier(17))

        // 18+ = +3
        assertEquals(3, calculateModifier(18))
        assertEquals(3, calculateModifier(19))
    }

    @Test
    fun `derived combat statistics and markdown sheet for Inquebrantable are accurate`() {
        val str = 14 // +1
        val dex = 16 // +2
        val con = 15 // +1
        val intel = 10 // 0
        val wil = 13 // +1
        val pre = 8 // -1

        val conMod = calculateModifier(con)
        val dexMod = calculateModifier(dex)
        val maxHp = (10 + conMod).coerceAtLeast(6)
        val armorClass = 10 + dexMod
        val initiativeBonus = dexMod

        assertEquals("PV Máximos deben ser 10 + ConMod", 11, maxHp)
        assertEquals("CD Defensiva debe ser 10 + DesMod", 12, armorClass)
        assertEquals("Iniciativa debe ser igual a DesMod", 2, initiativeBonus)

        val characterMarkdown = """
# Inquebrantable: Godofredo de Bouillon
**Antigua Ocupación**: Soldado
**Ancla (Pecado Capital)**: Orgullo (Soberbia)
**Fragmento del Viejo Mundo**: Moneda de Plata
**Misión Condenada**: El Cántico Herético

---
### Atributos y Estadísticas
- **Fuerza (FUE)**: $str (+${calculateModifier(str)})
- **Destreza (DES)**: $dex (+${calculateModifier(dex)})
- **Constitución (CON)**: $con (+${calculateModifier(con)})
- **Inteligencia (INT)**: $intel (${calculateModifier(intel)})
- **Voluntad (VOL)**: $wil (+${calculateModifier(wil)})
- **Presencia (PRE)**: $pre (${calculateModifier(pre)})

**Puntos de Golpe (PV)**: $maxHp / $maxHp | **Clase de Dificultad (CD)**: $armorClass | **Iniciativa**: +$initiativeBonus
**Humanidad**: 20 / 20 | **Angustia**: 0 / 10 | **Trauma**: 0 Heridas
        """.trimIndent()

        val npc = Npc(
            id = 0,
            name = "Godofredo de Bouillon",
            description = "Inquebrantable (Soldado). Ancla: Orgullo (Soberbia).",
            maxHp = maxHp,
            currentHp = maxHp,
            armorClass = armorClass,
            initiativeBonus = initiativeBonus,
            notes = characterMarkdown,
            isMonster = false
        )

        assertFalse("El Inquebrantable debe ser un personaje jugable, no un monstruo", npc.isMonster)
        assertEquals("Godofredo de Bouillon", npc.name)
        assertEquals(11, npc.maxHp)
        assertEquals(12, npc.armorClass)
        assertEquals(2, npc.initiativeBonus)
        assertTrue("La ficha debe contener el ancla", npc.notes.contains("Orgullo (Soberbia)"))
        assertTrue("La ficha debe contener la ocupación", npc.notes.contains("Soldado"))
        assertTrue("La ficha debe contener el fragmento", npc.notes.contains("Moneda de Plata"))
    }
}
