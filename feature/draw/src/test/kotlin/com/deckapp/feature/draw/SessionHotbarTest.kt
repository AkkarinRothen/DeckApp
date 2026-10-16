package com.deckapp.feature.draw

import com.deckapp.core.model.HotbarActionType
import com.deckapp.core.model.SessionHotbarAction
import com.deckapp.core.model.defaultHotbarActions
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SessionHotbarTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `defaultHotbarActions provides standard solo rpg starter kit`() {
        val actions = defaultHotbarActions()
        assertEquals(4, actions.size)

        val d20 = actions.find { it.payload == "1d20" }
        assertNotNull(d20)
        assertEquals(HotbarActionType.DICE_ROLL, d20?.type)
        assertEquals("d20", d20?.label)

        val d100 = actions.find { it.payload == "1d100" }
        assertNotNull(d100)
        assertEquals(HotbarActionType.DICE_ROLL, d100?.type)
        assertEquals("d100", d100?.label)

        val oracle = actions.find { it.payload == "50_50" }
        assertNotNull(oracle)
        assertEquals(HotbarActionType.ORACLE_QUERY, oracle?.type)
        assertEquals("Oráculo 50/50", oracle?.label)

        val event = actions.find { it.payload == "RANDOM_EVENT" }
        assertNotNull(event)
        assertEquals(HotbarActionType.ORACLE_QUERY, event?.type)
        assertEquals("Evento Aleatorio", event?.label)
    }

    @Test
    fun `SessionHotbarAction serializes and deserializes correctly to JSON`() {
        val customAction = SessionHotbarAction(
            id = "custom_macro_1",
            label = "Ataque Espada",
            type = HotbarActionType.DICE_ROLL,
            payload = "1d20+5",
            colorHex = 0xFFFF5722L
        )

        val encoded = json.encodeToString(listOf(customAction))
        val decoded = json.decodeFromString<List<SessionHotbarAction>>(encoded)

        assertEquals(1, decoded.size)
        val action = decoded.first()
        assertEquals("custom_macro_1", action.id)
        assertEquals("Ataque Espada", action.label)
        assertEquals(HotbarActionType.DICE_ROLL, action.type)
        assertEquals("1d20+5", action.payload)
        assertEquals(0xFFFF5722L, action.colorHex)
    }

    @Test
    fun `table and deck actions preserve payload in JSON serialization`() {
        val tableAction = SessionHotbarAction(
            id = "table_action_1",
            label = "Encuentros Bosque",
            type = HotbarActionType.RANDOM_TABLE,
            payload = "42",
            colorHex = 0xFF4CAF50L
        )

        val deckAction = SessionHotbarAction(
            id = "deck_action_1",
            label = "Robar Mazo Clave",
            type = HotbarActionType.DRAW_CARD,
            payload = "101",
            colorHex = 0xFF9C27B0L
        )

        val encoded = json.encodeToString(listOf(tableAction, deckAction))
        val decoded = json.decodeFromString<List<SessionHotbarAction>>(encoded)

        assertEquals(2, decoded.size)
        assertEquals("42", decoded[0].payload)
        assertEquals(42L, decoded[0].payload.toLongOrNull())
        assertEquals(HotbarActionType.RANDOM_TABLE, decoded[0].type)
        assertEquals("101", decoded[1].payload)
        assertEquals(101L, decoded[1].payload.toLongOrNull())
        assertEquals(HotbarActionType.DRAW_CARD, decoded[1].type)
    }

    @Test
    fun `hotbar action list modifications support upsert and deletion`() {
        val actions = defaultHotbarActions().toMutableList()

        // Add
        val newAction = SessionHotbarAction(
            id = "action_sneak",
            label = "Sigilo",
            type = HotbarActionType.DICE_ROLL,
            payload = "1d20+3"
        )
        actions.add(newAction)
        assertEquals(5, actions.size)

        // Edit
        val updatedAction = newAction.copy(label = "Sigilo Experto", payload = "1d20+6")
        val index = actions.indexOfFirst { it.id == updatedAction.id }
        assertTrue(index >= 0)
        actions[index] = updatedAction
        assertEquals("Sigilo Experto", actions[index].label)
        assertEquals("1d20+6", actions[index].payload)

        // Delete
        actions.removeAll { it.payload == "1d100" }
        assertEquals(4, actions.size)
        assertNull(actions.find { it.payload == "1d100" })
    }
}
