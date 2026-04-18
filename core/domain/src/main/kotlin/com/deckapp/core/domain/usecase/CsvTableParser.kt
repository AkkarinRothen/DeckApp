package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableEntry

/**
 * Motor de parsing para archivos CSV/TSV.
 *
 * Soporta:
 * - Delimitadores: `,` `;` `|` o tabulador
 * - Comillas escapadas: `"texto con, coma"` cuenta como un solo campo
 * - Auto-detección de delimitador a partir de las primeras 3 líneas
 * - Columna de rango opcional (la primera columna con formato numérico)
 */
class CsvTableParser : TableParser {

    override fun canParse(rawText: String): Boolean {
        val lines = rawText.trim().lines().take(3)
        if (lines.isEmpty()) return false
        val delimiters = listOf(',', ';', '|', '\t')
        return delimiters.any { d -> lines.all { it.contains(d) } }
    }

    override fun parse(rawText: String): ParsedTableContent {
        val preview = preview(rawText)
        val entries = parse(rawText, preview.config)
        return ParsedTableContent(entries = entries)
    }

    data class ParseConfig(
        val delimiter: Char,
        val hasHeader: Boolean,
        val rangeColumnIndex: Int,
        val textColumnIndex: Int,
        /**
         * Columnas adicionales que se concatenan al texto principal con " — ".
         * Útil cuando una tabla TTRPG tiene varias columnas de contenido relevante
         * (ej. Rango | Nombre | Descripción → textColumnIndex=1, additionalColumns=[2]).
         */
        val additionalColumns: List<Int> = emptyList()
    )

    data class ParsePreview(
        val config: ParseConfig,
        val headers: List<String>,
        val sampleRows: List<List<String>>,  // Primeras 3 filas para que el usuario revise
        val columnCount: Int = 0
    )

    /**
     * Analiza el texto CSV y propone una configuración de parseo.
     */
    fun preview(rawText: String): ParsePreview {
        val lines = rawText.trimStart('\uFEFF').lines().filter { it.isNotBlank() }
        val delimiter = detectDelimiter(lines.take(3))
        val splitLines = lines.map { splitCsvLine(it, delimiter) }

        val firstRow = splitLines.firstOrNull() ?: emptyList()
        val hasHeader = looksLikeHeader(firstRow)

        val (rangeCol, textCol) = detectColumns(splitLines, hasHeader)

        val headers = if (hasHeader) firstRow else List(firstRow.size) { "Columna ${it + 1}" }
        val sampleRows = splitLines.drop(if (hasHeader) 1 else 0).take(3)
        val columnCount = headers.size

        return ParsePreview(
            config = ParseConfig(
                delimiter = delimiter,
                hasHeader = hasHeader,
                rangeColumnIndex = rangeCol,
                textColumnIndex = textCol
            ),
            headers = headers,
            sampleRows = sampleRows,
            columnCount = columnCount
        )
    }

    /**
     * Parsea el texto CSV usando la configuración provista (posiblemente ajustada por el usuario).
     */
    fun parse(rawText: String, config: ParseConfig): List<TableEntry> {
        val lines = rawText.trimStart('\uFEFF').lines().filter { it.isNotBlank() }
        val skipFirst = config.hasHeader
        val entries = mutableListOf<TableEntry>()
        var sortOrder = 0

        lines.drop(if (skipFirst) 1 else 0).forEach { line ->
            val cols = splitCsvLine(line, config.delimiter)
            if (cols.size <= maxOf(config.rangeColumnIndex, config.textColumnIndex)) return@forEach

            val rangeText = cols[config.rangeColumnIndex].trim()
            val primaryText = cols[config.textColumnIndex].trim()
            if (primaryText.isBlank()) return@forEach

            // Concatenar columnas adicionales configuradas por el usuario
            val extraText = config.additionalColumns
                .mapNotNull { idx -> cols.getOrNull(idx)?.trim()?.takeIf { it.isNotBlank() } }
                .joinToString(" — ")
            val entryText = if (extraText.isNotBlank()) "$primaryText — $extraText" else primaryText

            val range = RangeParser.parse(rangeText)
            entries.add(
                TableEntry(
                    minRoll = range?.range?.min ?: (sortOrder + 1),
                    maxRoll = range?.range?.max ?: (sortOrder + 1),
                    text = entryText,
                    sortOrder = sortOrder++
                )
            )
        }
        return entries
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun detectDelimiter(lines: List<String>): Char {
        val candidates = listOf('|', '\t', ';', ',')
        return candidates.maxByOrNull { delim ->
            lines.sumOf { line -> line.count { it == delim } }
        } ?: ','
    }

    private fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    /**
     * Determina si la primera fila es una cabecera.
     *
     * Positivo (es header) si alguna celda contiene palabras clave típicas de cabecera TTRPG.
     * Negativo (son datos) si todas las celdas numéricas son rangos válidos según [RangeParser].
     * En caso de duda, aplica la heurística original: ninguna celda empieza por dígito.
     */
    private fun looksLikeHeader(firstRow: List<String>): Boolean {
        val headerKeywords = setOf(
            "roll", "result", "encounter", "name", "description",
            "range", "rango", "resultado", "nombre", "descripción", "descripcion",
            "table", "tabla", "item", "effect", "efecto", "type", "tipo"
        )
        // Si alguna celda coincide con una keyword de header → es header
        if (firstRow.any { cell -> cell.trim().lowercase() in headerKeywords }) return true
        // Si todas las celdas numéricas son rangos válidos → son datos, no header
        val numericCells = firstRow.filter { it.trim().matches(Regex("""\d.*""")) }
        if (numericCells.isNotEmpty() && numericCells.all { RangeParser.parse(it.trim()) != null }) return false
        // Heurística original: ninguna celda empieza por dígito
        return firstRow.none { cell -> cell.trim().matches(Regex("""\d+.*""")) }
    }

    /** Detecta qué columna es el rango (contiene números/rangos) y cuál el texto descriptivo. */
    private fun detectColumns(splitLines: List<List<String>>, hasHeader: Boolean): Pair<Int, Int> {
        val dataRows = splitLines.drop(if (hasHeader) 1 else 0).take(5)
        if (dataRows.isEmpty()) return 0 to 1

        val columnCount = dataRows.maxOf { it.size }
        var rangeCol = -1
        var textCol = -1

        for (colIndex in 0 until columnCount) {
            val values = dataRows.map { it.getOrNull(colIndex)?.trim() ?: "" }
            val rangeScore = values.count { RangeParser.parse(it) != null }
            val textScore = values.count { it.length > 5 }  // Texto largo → columna descriptiva

            if (rangeCol == -1 && rangeScore >= (dataRows.size / 2)) {
                rangeCol = colIndex
            } else if (textCol == -1 && textScore > rangeScore) {
                textCol = colIndex
            }
        }

        // Fallback
        if (rangeCol == -1) rangeCol = 0
        if (textCol == -1) textCol = if (rangeCol == 0) 1 else 0
        return rangeCol to textCol
    }
}
