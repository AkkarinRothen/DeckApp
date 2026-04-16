package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parser para tablas en formato JSON.
 *
 * Soporta:
 * 1. **Formato Foundry VTT (RollTable)**: El más común en la comunidad TTRPG.
 *    {
 *      "name": "Tabla de Encuentros",
 *      "results": [
 *        { "range": [1, 5], "text": "Goblin" },
 *        { "range": [6, 10], "text": "Zombie" }
 *      ]
 *    }
 *
 * 2. **Formato simple (Array)**: Lista de strings o objetos simples.
 *    ["Resultado 1", "Resultado 2", ...]
 *
 * 3. **Formato DeckApp propio**: Si el JSON fue exportado por la propia app.
 *    { "name": "...", "entries": [ {"min": 1, "max": 5, "text": "..."} ] }
 */
class JsonTableParser {

    data class ParsedTable(
        val name: String,
        val description: String = "",
        val entries: List<TableEntry>
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(rawJson: String): ParsedTable {
        val element = try {
            json.parseToJsonElement(rawJson)
        } catch (e: Exception) {
            throw TableParseException.InvalidJson(e.message ?: "error desconocido")
        }

        return when (element) {
            is JsonArray -> parseSimpleArray(element)
            is JsonObject -> parseObject(element)
            else -> throw TableParseException.UnknownFormat("Se esperaba un objeto JSON o un array.")
        }
    }

    // ── Formatos ─────────────────────────────────────────────────────────────

    /** Foundry VTT (RollTable) o formato propio DeckApp */
    private fun parseObject(obj: JsonObject): ParsedTable {
        val name = obj["name"]?.jsonPrimitive?.content ?: "Tabla Importada"
        val description = obj["description"]?.jsonPrimitive?.content ?: ""

        // 1. Foundry VTT: "results" con "range": [min, max]
        val results = obj["results"]?.jsonArray
        if (results != null) {
            return ParsedTable(
                name = name,
                description = description,
                entries = parseFoundryResults(results)
            )
        }

        // 2. Formato DeckApp propio: "entries" con "min", "max", "text"
        val entries = obj["entries"]?.jsonArray
        if (entries != null) {
            return ParsedTable(
                name = name,
                description = description,
                entries = parseDeckAppEntries(entries)
            )
        }

        // 3. Formato Roll20: "items" con "weight" y "name"
        val items = obj["items"]?.jsonArray
        if (items != null) {
            return ParsedTable(
                name = name,
                description = description,
                entries = parseRoll20Items(items)
            )
        }

        throw TableParseException.UnknownFormat("El objeto JSON no contiene 'results', 'entries' ni 'items'.")
    }

    /** Array simple de strings ["Resultado A", "Resultado B", ...] */
    private fun parseSimpleArray(array: JsonArray): ParsedTable {
        val entries = array.mapIndexed { index, element ->
            val text = when {
                element is JsonObject -> element["text"]?.jsonPrimitive?.content
                    ?: element["name"]?.jsonPrimitive?.content
                    ?: element.toString()
                else -> element.jsonPrimitive.content
            }
            TableEntry(
                minRoll = index + 1,
                maxRoll = index + 1,
                text = text,
                sortOrder = index
            )
        }
        return ParsedTable(name = "Tabla Importada", entries = entries)
    }

    /** Parsea resultados en formato Foundry VTT */
    private fun parseFoundryResults(results: JsonArray): List<TableEntry> {
        return results.mapIndexed { index, element ->
            val obj = element.jsonObject
            val range = obj["range"]?.jsonArray
            val min = range?.getOrNull(0)?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)
            val max = range?.getOrNull(1)?.jsonPrimitive?.content?.toIntOrNull() ?: min
            val text = obj["text"]?.jsonPrimitive?.content
                ?: obj["description"]?.jsonPrimitive?.content
                ?: ""
            TableEntry(minRoll = min, maxRoll = max, text = text, sortOrder = index)
        }
    }

    /**
     * Parsea items en formato Roll20 RollableTable.
     * Cada item tiene "weight" (equivale al rango de probabilidad) y "name" (texto).
     * Como Roll20 no usa rangos explícitos, se asignan rangos acumulativos por weight.
     */
    private fun parseRoll20Items(items: JsonArray): List<TableEntry> {
        val entries = mutableListOf<TableEntry>()
        var currentMin = 1
        items.forEachIndexed { index, element ->
            val obj = element.jsonObject
            val weight = obj["weight"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            val text = obj["name"]?.jsonPrimitive?.content
                ?: obj["text"]?.jsonPrimitive?.content
                ?: ""
            val currentMax = currentMin + weight - 1
            entries.add(TableEntry(minRoll = currentMin, maxRoll = currentMax, text = text, sortOrder = index))
            currentMin = currentMax + 1
        }
        return entries
    }

    /** Parsea entradas en formato DeckApp */
    private fun parseDeckAppEntries(entries: JsonArray): List<TableEntry> {
        return entries.mapIndexed { index, element ->
            val obj = element.jsonObject
            val min = obj["min"]?.jsonPrimitive?.content?.toIntOrNull() ?: (index + 1)
            val max = obj["max"]?.jsonPrimitive?.content?.toIntOrNull() ?: min
            val text = obj["text"]?.jsonPrimitive?.content ?: ""
            TableEntry(minRoll = min, maxRoll = max, text = text, sortOrder = index)
        }
    }
}
