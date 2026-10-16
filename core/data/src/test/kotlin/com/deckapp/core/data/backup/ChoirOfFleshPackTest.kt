package com.deckapp.core.data.backup

import com.deckapp.core.model.backup.FullBackupDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChoirOfFleshPackTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `choir_of_flesh starter pack json is valid and contains full system rules, tables and creatures`() {
        val packFile = File("src/main/assets/starter_packs/choir_of_flesh.json")
        val altPackFile = File("core/data/src/main/assets/starter_packs/choir_of_flesh.json")
        val fileToRead = if (packFile.exists()) packFile else altPackFile

        assertTrue("El archivo choir_of_flesh.json debe existir en assets", fileToRead.exists())

        val jsonString = fileToRead.readText().trimStart('\uFEFF')
        val pack = json.decodeFromString<FullBackupDto>(jsonString)

        // 1. Verificación de Reglas del Sistema
        assertTrue("Debe incluir al menos 10 reglas del sistema", pack.systemRules.size >= 10)
        val escaton = pack.systemRules.find { it.title.contains("Escatón") }
        assertNotNull("Debe incluir la regla El Escatón", escaton)
        assertEquals("Choir of Flesh", escaton?.gameSystem)

        val chequeos = pack.systemRules.find { it.title.contains("Chequeos") }
        assertNotNull("Debe incluir la regla de Chequeos y NO", chequeos)

        // 2. Verificación de Tablas de Referencia
        assertTrue("Debe incluir tablas de referencia de atributos y dificultades", pack.referenceTables.size >= 4)
        assertTrue("Debe incluir filas de referencia", pack.referenceRows.size >= 15)

        // 3. Verificación de Tablas Aleatorias
        assertTrue("Debe incluir al menos 16 tablas aleatorias", pack.randomTables.size >= 16)
        assertTrue("Debe incluir más de 100 entradas aleatorias", pack.tableEntries.size >= 100)

        val anchorTable = pack.randomTables.find { it.name.contains("Ancla") }
        assertNotNull("Debe incluir la tabla El Ancla (Pecado Capital)", anchorTable)

        val fragmentTable = pack.randomTables.find { it.name.contains("Fragmento") }
        assertNotNull("Debe incluir la tabla Fragmento del Viejo Mundo", fragmentTable)

        val whisperTable = pack.randomTables.find { it.name.contains("Susurro") }
        assertNotNull("Debe incluir la tabla Susurro de Condenación", whisperTable)

        // 4. Verificación de Criaturas Oficiales (Bestiario completo: 24 criaturas)
        assertEquals("Debe incluir exactamente 24 criaturas oficiales", 24, pack.npcs.size)
        assertTrue("Todas las criaturas deben estar marcadas como monstruos", pack.npcs.all { it.isMonster })

        // 12 Criaturas de la Carne
        val fleshNames = listOf(
            "Amalgama", "Apóstata", "Tejedor de Huesos", "Bandada de Carroña", "Croador",
            "Gusano de Carne", "Rechinador", "Fauces del Suelo", "Licuador", "Madre de Tumores",
            "Acechador Silencioso", "Sabueso Desollado"
        )
        for (fleshName in fleshNames) {
            val creature = pack.npcs.find { it.name.contains(fleshName, ignoreCase = true) }
            assertNotNull("La criatura de la Carne '$fleshName' debe estar presente en el pack", creature)
            assertTrue("Las notas de '$fleshName' deben contener estadísticas", creature?.notes?.contains("Precisión") == true)
        }

        // 12 Sirvientes del Coro
        val choirNames = listOf(
            "Cantor", "Portador del Incensario", "Querubín", "Confesor", "Flagelante",
            "Heraldo", "Cruzado Cáscara", "Mater Dolorosa", "Pilar de Fe", "Escriba de la Penitencia",
            "Serafín", "Trono"
        )
        for (choirName in choirNames) {
            val creature = pack.npcs.find { it.name.contains(choirName, ignoreCase = true) }
            assertNotNull("El sirviente del Coro '$choirName' debe estar presente en el pack", creature)
            assertTrue("Las notas de '$choirName' deben contener estadísticas", creature?.notes?.contains("Precisión") == true)
        }
    }
}
