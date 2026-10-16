package com.deckapp.feature.draw

import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import com.deckapp.feature.draw.scene.SceneFramingDraft
import com.deckapp.feature.draw.scene.buildFramedSceneJournalEntry
import com.deckapp.feature.draw.scene.evaluateSceneCheck
import org.junit.Assert.*
import org.junit.Test

class SceneFramingAssistantTest {

    @Test
    fun `evaluateSceneCheck produces NORMAL when roll is strictly greater than chaos factor`() {
        // Chaos 5, roll 6 -> Normal
        val outcome = evaluateSceneCheck(chaosFactor = 5, forcedRoll = 6)
        assertEquals(SceneCheckResult.NORMAL, outcome.result)
        assertEquals("NORMAL", outcome.displayResult)
        assertEquals(6, outcome.roll)
        assertEquals(5, outcome.chaosFactor)
        assertNull(outcome.alterationSuggestion)
        assertNull(outcome.eventFocus)
        assertNull(outcome.eventMeaning)
    }

    @Test
    fun `evaluateSceneCheck produces ALTERED when roll is less than or equal to chaos and is even`() {
        // Chaos 6, roll 4 (even <= 6) -> Altered
        val outcome = evaluateSceneCheck(chaosFactor = 6, forcedRoll = 4)
        assertEquals(SceneCheckResult.ALTERED, outcome.result)
        assertEquals("ALTERADA", outcome.displayResult)
        assertEquals(4, outcome.roll)
        assertEquals(6, outcome.chaosFactor)
        assertNotNull(outcome.alterationSuggestion)
        assertNull(outcome.eventFocus)
        assertNull(outcome.eventMeaning)
    }

    @Test
    fun `evaluateSceneCheck produces INTERRUPTED when roll is less than or equal to chaos and is odd`() {
        // Chaos 6, roll 3 (odd <= 6) -> Interrupted
        val outcome = evaluateSceneCheck(chaosFactor = 6, forcedRoll = 3)
        assertEquals(SceneCheckResult.INTERRUPTED, outcome.result)
        assertEquals("INTERRUMPIDA", outcome.displayResult)
        assertEquals(3, outcome.roll)
        assertEquals(6, outcome.chaosFactor)
        assertNotNull("Interrupted scene must include event focus", outcome.eventFocus)
        assertNotNull("Interrupted scene must include event meaning", outcome.eventMeaning)
    }

    @Test
    fun `buildFramedSceneJournalEntry generates clean structured Markdown for session diary`() {
        val check = evaluateSceneCheck(chaosFactor = 5, forcedRoll = 8) // Normal
        val draft = SceneFramingDraft(
            title = "Infiltración en las Catacumbas",
            location = "Pasillo inundado bajo la catedral",
            objective = "Abrir el portón de hierro sin alertar a los cultistas",
            expectation = "El pasillo está desierto salvo por el eco de agua goteando",
            checkOutcome = check
        )

        val entry = buildFramedSceneJournalEntry(sceneNumber = 3, draft = draft)

        assertTrue("Must include scene title with number", entry.contains("## 🎬 Escena 3: Infiltración en las Catacumbas"))
        assertTrue("Must include chaos check details", entry.contains("> **Chequeo de Caos:** Caos 5 · d10: 8 → **NORMAL**"))
        assertTrue("Must include location header", entry.contains("### Lugar\nPasillo inundado bajo la catedral"))
        assertTrue("Must include objective header", entry.contains("### Objetivo\nAbrir el portón de hierro sin alertar a los cultistas"))
        assertTrue("Must include situation", entry.contains("### Situación Inicial\nEl pasillo está desierto salvo por el eco de agua goteando"))
    }

    @Test
    fun `buildFramedSceneJournalEntry documents interrupted scenes with event focus and meaning`() {
        val check = evaluateSceneCheck(chaosFactor = 7, forcedRoll = 5) // Interrupted
        val draft = SceneFramingDraft(
            title = "Negociación con el Mercader",
            location = "Taberna El Jabalí Plateado",
            objective = "Comprar el mapa antiguo",
            expectation = "El mercader nos espera en la mesa del rincón",
            adjustedSituation = "Al entrar, la guardia de la ciudad tiene al mercader arrestado",
            checkOutcome = check
        )

        val entry = buildFramedSceneJournalEntry(sceneNumber = 5, draft = draft)

        assertTrue(entry.contains("## 🎬 Escena 5: Negociación con el Mercader"))
        assertTrue(entry.contains("→ **INTERRUMPIDA**"))
        assertTrue("Must display event details in header quote", entry.contains("> **Evento:** ${check.eventFocus}"))
        assertTrue(entry.contains("### Situación Inicial\nAl entrar, la guardia de la ciudad tiene al mercader arrestado"))
    }
}
