package com.deckapp.core.domain.usecase

import com.deckapp.core.model.RandomTable

/**
 * Convierte una [RandomTable] al formato JSON de exportación de DeckApp.
 *
 * Formato resultante compatible con el import de [JsonTableParser]:
 * ```json
 * { "version": 1, "tables": [ { "name": "...", "entries": [...] } ] }
 * ```
 */
object ExportTableUseCase {

    fun buildJson(table: RandomTable): String = buildString {
        appendLine("{")
        appendLine("  \"version\": 1,")
        appendLine("  \"tables\": [")
        appendLine("    {")
        appendLine("      \"name\": ${table.name.json()},")
        appendLine("      \"description\": ${table.description.json()},")
        appendLine("      \"category\": ${table.tags.joinToString(", ") { it.name }.json()},")
        appendLine("      \"rollFormula\": ${table.rollFormula.json()},")
        appendLine("      \"rollMode\": \"${table.rollMode.name}\",")
        appendLine("      \"entries\": [")
        table.entries.forEachIndexed { idx, entry ->
            val comma = if (idx < table.entries.lastIndex) "," else ""
            appendLine("        { \"minRoll\": ${entry.minRoll}, \"maxRoll\": ${entry.maxRoll}, \"weight\": ${entry.weight}, \"text\": ${entry.text.json()} }$comma")
        }
        appendLine("      ]")
        appendLine("    }")
        appendLine("  ]")
        append("}")
    }

    /** Envuelve la cadena en comillas dobles escapando caracteres especiales JSON. */
    private fun String.json(): String =
        "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")}\""
}
