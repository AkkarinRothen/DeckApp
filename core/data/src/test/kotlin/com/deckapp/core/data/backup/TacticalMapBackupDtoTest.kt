package com.deckapp.core.data.backup

import com.deckapp.core.model.backup.FullBackupDto
import com.deckapp.core.model.backup.TacticalMapBackupDto
import com.deckapp.core.model.backup.TacticalMapPinBackupDto
import com.deckapp.core.model.backup.TacticalTokenBackupDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalMapBackupDtoTest {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @Test
    fun `FullBackupDto serializes and deserializes tactical maps, tokens and pins accurately`() {
        val original = FullBackupDto(
            tacticalMaps = listOf(
                TacticalMapBackupDto(
                    id = 101L,
                    name = "Catacumbas Antiguas",
                    imageUri = "content://maps/catacombs.jpg",
                    gridType = "SQUARE",
                    cellSizePx = 70f,
                    scaleFtPerCell = 5,
                    fogOfWarMaskJson = "{\"revealed\":[1,2,3]}",
                    areasJson = "[{\"id\":\"area1\",\"name\":\"Entrada\"}]"
                )
            ),
            tacticalTokens = listOf(
                TacticalTokenBackupDto(
                    id = 201L,
                    mapId = 101L,
                    name = "Guardián Esqueleto",
                    faction = "ENEMY",
                    currentHp = 15,
                    maxHp = 20,
                    elevationFt = 10,
                    conditionsJson = "[\"PRONE\"]"
                )
            ),
            tacticalMapPins = listOf(
                TacticalMapPinBackupDto(
                    id = 301L,
                    mapId = 101L,
                    cellX = 4f,
                    cellY = 6f,
                    title = "Escalera hacia el Nivel Inferior",
                    iconName = "stairs",
                    targetMapId = 102L,
                    isSecret = 0
                )
            )
        )

        val serialized = json.encodeToString(original)
        assertTrue(serialized.contains("Catacumbas Antiguas"))
        assertTrue(serialized.contains("Guardián Esqueleto"))
        assertTrue(serialized.contains("targetMapId\":102"))

        val restored = json.decodeFromString<FullBackupDto>(serialized)
        assertEquals(1, restored.tacticalMaps.size)
        assertEquals("Catacumbas Antiguas", restored.tacticalMaps[0].name)
        assertEquals(1, restored.tacticalTokens.size)
        assertEquals(10, restored.tacticalTokens[0].elevationFt)
        assertEquals(1, restored.tacticalMapPins.size)
        assertEquals(102L, restored.tacticalMapPins[0].targetMapId)
    }

    @Test
    fun `FullBackupDto safely deserializes legacy json without tactical map fields`() {
        val legacyJson = """
            {
                "schemaVersion": 1,
                "tags": [],
                "decks": [],
                "cards": []
            }
        """.trimIndent()

        val restored = json.decodeFromString<FullBackupDto>(legacyJson)
        assertTrue("Tactical maps must default to emptyList()", restored.tacticalMaps.isEmpty())
        assertTrue("Tactical tokens must default to emptyList()", restored.tacticalTokens.isEmpty())
        assertTrue("Tactical pins must default to emptyList()", restored.tacticalMapPins.isEmpty())
    }
}
