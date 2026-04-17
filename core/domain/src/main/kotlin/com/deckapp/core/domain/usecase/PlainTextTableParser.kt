package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableEntry

/**
 * Parser para texto plano (portapapeles, notas, etc.).
 *
 * Soporta los formatos más habituales en texto copiado de manuales TTRPG:
 *
 * 1. Markdown pipetable:
 *    | 1-5  | Goblin |
 *    | 6-10 | Zombie |
 *
 * 2. Lista enumerada con rango:
 *    1-5. Goblin
 *    6-10. Zombie
 *
 *    1-5: Goblin
 *    6-10: Zombie
 *
 * 3. Lista con número suelto:
 *    1. Goblin
 *    2. Zombie
 *
 * 4. Lista sin número (cada línea = una entrada con peso igual):
 *    Goblin
 *    Zombie
 *    Esqueleto
 */
class PlainTextTableParser {

    data class ParseResult(
        val entries: List<TableEntry>,
        /**
         * Fórmula de dado sugerida para la tabla, inferida a partir del rango de las entradas.
         * Null si el texto ya contenía rangos explícitos (el parser los usa directamente).
         * Ejemplo: lista de 6 ítems sin rangos → "1d6".
         */
        val suggestedFormula: String?
    )

    /**
     * Parsea texto plano y devuelve las entradas detectadas junto con la fórmula sugerida.
     * Usar [parse] si solo se necesitan las entradas.
     */
    fun parseWithMeta(rawText: String): ParseResult {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !isMarkdownTableSeparator(it) }

        if (lines.isEmpty()) return ParseResult(emptyList(), null)

        return when {
            looksLikeMarkdownTable(lines) ->
                ParseResult(parseMarkdownTable(lines), null)
            looksLikeEnumeratedList(lines) ->
                ParseResult(parseEnumeratedList(lines), null)
            else -> {
                val entries = parseSimpleList(lines)
                val formula = if (entries.isNotEmpty())
                    RangeParser.inferRollFormula(1, entries.size)
                else null
                ParseResult(entries, formula)
            }
        }
    }

    /**
     * Parsea texto plano y devuelve las entradas detectadas.
     * Para obtener también la fórmula sugerida, usar [parseWithMeta].
     */
    fun parse(rawText: String): List<TableEntry> = parseWithMeta(rawText).entries

    // ── Markdown Table ────────────────────────────────────────────────────────

    private fun looksLikeMarkdownTable(lines: List<String>) =
        lines.count { it.startsWith("|") } >= 2

    private fun parseMarkdownTable(lines: List<String>): List<TableEntry> {
        val entries = mutableListOf<TableEntry>()
        var sortOrder = 0

        for (line in lines) {
            if (!line.startsWith("|")) continue
            val cells = line.split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (cells.size < 2) continue

            val rangeCell = cells[0]
            val textCell = cells.drop(1).joinToString(" — ")

            val range = RangeParser.parse(rangeCell)
            entries.add(TableEntry(
                minRoll = range?.range?.min ?: (sortOrder + 1),
                maxRoll = range?.range?.max ?: (sortOrder + 1),
                text = textCell,
                sortOrder = sortOrder++
            ))
        }
        return entries
    }

    // ── Enumerated List ───────────────────────────────────────────────────────

    // Patrón: "1-5. texto", "1-5: texto", "1-5 texto", "[1-5] texto", "1-5\ttexto"
    // Cubre además corchetes ([1-5]), tabulador como separador, y espacio simple sin símbolo.
    private val ENUM_PATTERN = Regex(
        """^\[?(\d{1,3}(?:\s*[-–—]\s*\d{1,3})?)\]?\s*[.:)\t]?\s{1,4}(.+)$"""
    )

    private fun looksLikeEnumeratedList(lines: List<String>) =
        lines.take(3).count { ENUM_PATTERN.containsMatchIn(it) } >= 2

    private fun parseEnumeratedList(lines: List<String>): List<TableEntry> {
        val entries = mutableListOf<TableEntry>()
        var sortOrder = 0
        var lastEntry: TableEntry? = null

        for (line in lines) {
            val match = ENUM_PATTERN.find(line)
            if (match != null) {
                lastEntry?.let { entries.add(it) }
                val rangeText = match.groupValues[1]
                val text = match.groupValues[2].trim()
                val range = RangeParser.parse(rangeText)
                lastEntry = TableEntry(
                    minRoll = range?.range?.min ?: (sortOrder + 1),
                    maxRoll = range?.range?.max ?: (sortOrder + 1),
                    text = text,
                    sortOrder = sortOrder++
                )
            } else if (lastEntry != null) {
                // Continuación de la entrada anterior
                lastEntry = lastEntry!!.copy(text = "${lastEntry!!.text} $line")
            }
        }
        lastEntry?.let { entries.add(it) }
        return entries
    }

    // ── Simple List ───────────────────────────────────────────────────────────

    private fun parseSimpleList(lines: List<String>): List<TableEntry> {
        return lines.mapIndexed { index, text ->
            TableEntry(
                minRoll = index + 1,
                maxRoll = index + 1,
                text = text,
                sortOrder = index
            )
        }
    }

    private fun isMarkdownTableSeparator(line: String) =
        line.matches(Regex("""^[\s|:-]+$"""))
}
