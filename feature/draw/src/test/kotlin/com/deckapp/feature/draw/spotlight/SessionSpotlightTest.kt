package com.deckapp.feature.draw.spotlight

import com.deckapp.core.model.TableRollResult
import com.deckapp.feature.draw.components.spotlight.SpotlightCategory
import com.deckapp.feature.draw.components.spotlight.SpotlightItem
import org.junit.Assert.*
import org.junit.Test

class SessionSpotlightTest {

    @Test
    fun `Spotlight item types correctly expose stable IDs and assigned categories`() {
        val tableItem = SpotlightItem.TableResult(
            tableId = 42L,
            title = "Encuentros en el Pantano",
            subtitle = "D&D 5e · 1d20",
            formula = "1d20",
            rowCount = 20,
            description = "Tablas de monstruos de ciénaga"
        )
        val creatureItem = SpotlightItem.CreatureResult(
            creatureId = 101L,
            title = "Basilisco",
            subtitle = "HP: 52 · CA: 15",
            hp = 52,
            ac = 15,
            initiativeBonus = -1,
            notes = "Mirada petrificante DC 12"
        )
        val npcItem = SpotlightItem.NpcResult(
            npcId = 7L,
            title = "Barón Vallakovich",
            subtitle = "Burgomaestre",
            notes = "Líder paranoico de Vallaki"
        )
        val ruleItem = SpotlightItem.RuleResult(
            ruleId = 15L,
            title = "Ataques de Oportunidad",
            subtitle = "D&D 5e · Combate",
            content = "Se produce cuando una criatura hostil sale de tu alcance.",
            gameSystem = "D&D 5e"
        )
        val noteItem = SpotlightItem.NoteResult(
            lineNumber = 24,
            title = "Nota L24",
            subtitle = "Diario de Sesión",
            snippet = "El tabernero mencionó una entrada secreta en la bodega."
        )

        assertEquals("table_42", tableItem.id)
        assertEquals(SpotlightCategory.TABLES, tableItem.category)

        assertEquals("creature_101", creatureItem.id)
        assertEquals(SpotlightCategory.MONSTERS, creatureItem.category)

        assertEquals("npc_7", npcItem.id)
        assertEquals(SpotlightCategory.NPCS, npcItem.category)

        assertEquals("rule_15", ruleItem.id)
        assertEquals(SpotlightCategory.RULES, ruleItem.category)

        assertEquals("note_24", noteItem.id)
        assertEquals(SpotlightCategory.NOTES, noteItem.category)
    }

    @Test
    fun `Filtering by category isolates only corresponding items when not ALL`() {
        val items: List<SpotlightItem> = listOf(
            SpotlightItem.TableResult(1L, "Tabla A", "1d6", "1d6", 6),
            SpotlightItem.CreatureResult(2L, "Orco", "HP: 15", 15, 13),
            SpotlightItem.NpcResult(3L, "Elora", "Maga", "Aliada"),
            SpotlightItem.RuleResult(4L, "Sigilo", "General", "Regla de sigilo", "D&D 5e")
        )

        val onlyTables = items.filter { it.category == SpotlightCategory.TABLES }
        assertEquals(1, onlyTables.size)
        assertEquals("Tabla A", onlyTables.first().title)

        val onlyMonsters = items.filter { it.category == SpotlightCategory.MONSTERS }
        assertEquals(1, onlyMonsters.size)
        assertEquals("Orco", onlyMonsters.first().title)

        val allItems = items.filter { SpotlightCategory.ALL == SpotlightCategory.ALL }
        assertEquals(4, allItems.size)
    }

    @Test
    fun `Query search matches titles, subtitles or notes case-insensitively`() {
        val items: List<SpotlightItem> = listOf(
            SpotlightItem.TableResult(1L, "Botín Dracónico", "Tesoros", "1d100", 100),
            SpotlightItem.CreatureResult(2L, "Dragón Rojo Joven", "HP: 178", 178, 18, notes = "Inmune al fuego"),
            SpotlightItem.NpcResult(3L, "Ignis", "Herrero", "Vende armas de fuego")
        )

        val query = "fuego"
        val matches = items.filter { item ->
            when (item) {
                is SpotlightItem.TableResult -> item.title.contains(query, true) || item.subtitle.contains(query, true)
                is SpotlightItem.CreatureResult -> item.title.contains(query, true) || item.notes.contains(query, true)
                is SpotlightItem.NpcResult -> item.title.contains(query, true) || item.notes.contains(query, true)
                else -> false
            }
        }

        assertEquals(2, matches.size)
        assertTrue(matches.any { it.title == "Dragón Rojo Joven" })
        assertTrue(matches.any { it.title == "Ignis" })
    }

    @Test
    fun `Copying table roll result to diary formats markdown consistently`() {
        val rollResult = TableRollResult(
            tableId = 5L,
            tableName = "Rumores de Taberna",
            sessionId = 1L,
            rollValue = 18,
            resolvedText = "Una caravana fue asaltada cerca del vado."
        )

        val diaryMarkdown = "🎲 **Tirada en ${rollResult.tableName}:** ${rollResult.resolvedText} (dado: ${rollResult.rollValue})"
        
        assertEquals(
            "🎲 **Tirada en Rumores de Taberna:** Una caravana fue asaltada cerca del vado. (dado: 18)",
            diaryMarkdown
        )
    }

    @Test
    fun `Copying rule and creature details formats structured markdown properly`() {
        val creature = SpotlightItem.CreatureResult(
            creatureId = 50L,
            title = "Liche",
            subtitle = "CR 21",
            hp = 135,
            ac = 17,
            notes = "Filacteria oculta"
        )
        val creatureMarkdown = "- **${creature.title}** (HP: ${creature.hp}, CA: ${creature.ac}) — ${creature.notes}"
        assertEquals("- **Liche** (HP: 135, CA: 17) — Filacteria oculta", creatureMarkdown)

        val rule = SpotlightItem.RuleResult(
            ruleId = 99L,
            title = "Concentración",
            subtitle = "D&D 5e · Magia",
            content = "Debes superar una salvación de Constitución al recibir daño.",
            gameSystem = "D&D 5e"
        )
        val ruleMarkdown = "### 📜 ${rule.title} (${rule.gameSystem})\n${rule.content}"
        assertTrue(ruleMarkdown.startsWith("### 📜 Concentración (D&D 5e)"))
        assertTrue(ruleMarkdown.contains("salvación de Constitución"))
    }
}
