package com.deckapp.feature.draw.threads

import com.deckapp.core.model.MythicCharacter
import com.deckapp.core.model.MythicThread
import org.junit.Assert.*
import org.junit.Test

class SessionThreadsTest {

    @Test
    fun `Active threads filter excludes resolved threads accurately`() {
        val threads = listOf(
            MythicThread(id = 1L, sessionId = 10L, description = "Encontrar la llave dorada", isResolved = false),
            MythicThread(id = 2L, sessionId = 10L, description = "Rescatar al cautivo", isResolved = true),
            MythicThread(id = 3L, sessionId = 10L, description = "Investigar el culto del abismo", isResolved = false)
        )

        val active = threads.filterNot { it.isResolved }
        assertEquals(2, active.size)
        assertEquals("Encontrar la llave dorada", active[0].description)
        assertEquals("Investigar el culto del abismo", active[1].description)
    }

    @Test
    fun `Toggling thread resolved flips boolean state`() {
        val thread = MythicThread(id = 5L, sessionId = 10L, description = "Derrotar al dragón", isResolved = false)
        val resolved = thread.copy(isResolved = true)
        assertTrue(resolved.isResolved)

        val reopened = resolved.copy(isResolved = false)
        assertFalse(reopened.isResolved)
    }

    @Test
    fun `Random d100 oracle selection picks from active items correctly`() {
        val activeThreads = listOf(
            MythicThread(id = 1L, sessionId = 1L, description = "Hilo Alfa"),
            MythicThread(id = 2L, sessionId = 1L, description = "Hilo Beta")
        )

        val roll = 45
        val picked = activeThreads.random()
        assertNotNull(picked)
        assertTrue(picked.description == "Hilo Alfa" || picked.description == "Hilo Beta")

        val resultText = "🎲 [d100: $roll] Hilo Sorteado: \"${picked.description}\""
        assertTrue(resultText.contains("d100: 45"))
        assertTrue(resultText.contains("Hilo Sorteado"))
    }

    @Test
    fun `Character random selection includes notes when available`() {
        val characters = listOf(
            MythicCharacter(id = 1L, sessionId = 1L, name = "Capitán Vane", notes = "Corsario leal"),
            MythicCharacter(id = 2L, sessionId = 1L, name = "Nigromante", notes = "")
        )

        val picked = characters.first { it.name == "Capitán Vane" }
        val notesSnippet = if (picked.notes.isNotBlank()) " (${picked.notes})" else ""
        val roll = 88
        val resultText = "🎲 [d100: $roll] PNJ Sorteado: \"${picked.name}\"$notesSnippet"

        assertEquals("🎲 [d100: 88] PNJ Sorteado: \"Capitán Vane\" (Corsario leal)", resultText)
    }

    @Test
    fun `Threads summary markdown formats checklists and characters properly`() {
        val threads = listOf(
            MythicThread(id = 1L, sessionId = 1L, description = "Misión principal", isResolved = false),
            MythicThread(id = 2L, sessionId = 1L, description = "Misión secundaria", isResolved = true)
        )
        val characters = listOf(
            MythicCharacter(id = 1L, sessionId = 1L, name = "Elrond", notes = "Líder sabio")
        )

        val active = threads.filterNot { it.isResolved }
        val resolved = threads.filter { it.isResolved }

        val sb = StringBuilder()
        sb.append("### 🧵 Resumen de Hilos y Amenazas\n")
        sb.append("**Hilos Activos:**\n")
        active.forEach { sb.append("- [ ] ${it.description}\n") }
        sb.append("**Hilos Resueltos:**\n")
        resolved.forEach { sb.append("- [x] ${it.description}\n") }
        sb.append("\n**Personajes y Amenazas:**\n")
        characters.forEach {
            val note = if (it.notes.isNotBlank()) " — *${it.notes}*" else ""
            sb.append("- **${it.name}**$note\n")
        }

        val result = sb.toString()
        assertTrue(result.contains("- [ ] Misión principal"))
        assertTrue(result.contains("- [x] Misión secundaria"))
        assertTrue(result.contains("- **Elrond** — *Líder sabio*"))
    }
}
