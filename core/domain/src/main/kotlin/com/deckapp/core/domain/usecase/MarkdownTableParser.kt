package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableEntry

/**
 * Motor de parsing para tablas en formato Markdown.
 * 
 * Formato esperado:
 * | Rango | Resultado |
 * |-------|-----------|
 * | 1-5   | Encuentro |
 * | 6     | Nada      |
 */
class MarkdownTableParser : TableParser {

    override fun canParse(rawText: String): Boolean = isMarkdownTable(rawText)

    /**
     * Parsea un texto que contiene una o más tablas Markdown.
     * Si hay varias tablas, por ahora las une en una sola lista de entradas.
     */
    override fun parse(rawText: String): ParsedTableContent {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (lines.size < 2) return ParsedTableContent()

        val entries = mutableListOf<TableEntry>()
        var sortOrder = 0
        
        // Regex para detectar el delimitador de Markdown (|---| | :--- | etc)
        // Buscamos al menos una secuencia de guiones rodeada opcionalmente de pipes o espacios.
        val markdownDelimiterRegex = Regex("""^\|?\s*:?-+:?\s*(\|?\s*:?-+:?\s*)+\|?$""")
        
        val headerLine = lines.getOrNull(0) ?: ""
        val hasMarkdownDelimiter = lines.getOrNull(1)?.let { markdownDelimiterRegex.matches(it) } ?: false
        
        val dataLines = if (hasMarkdownDelimiter) {
            // Saltamos el header (0) y el delimitador (1)
            lines.drop(2)
        } else {
            // Si no tiene delimitador estándar, solo procesamos si las líneas parecen tener estructura de pipes
            lines.filter { it.contains("|") }
        }

        dataLines.forEach { line ->
            // Dividir por |
            val rawParts = line.split("|").map { it.trim() }
            
            // Si la línea empieza con |, el primer elemento es vacío. Si termina con |, el último es vacío.
            val parts = when {
                line.startsWith("|") && line.endsWith("|") -> rawParts.drop(1).dropLast(1)
                line.startsWith("|") -> rawParts.drop(1)
                line.endsWith("|") -> rawParts.dropLast(1)
                else -> rawParts
            }

            if (parts.size < 2) return@forEach

            val rangeText = parts[0]
            val entryText = parts.drop(1).filter { it.isNotBlank() }.joinToString(" — ")

            if (entryText.isBlank()) return@forEach

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

        return ParsedTableContent(
            name = if (hasMarkdownDelimiter) headerLine.replace("|", "").trim() else null,
            entries = entries
        )
    }

    /**
     * Heurística rápida para saber si un texto parece contener una tabla Markdown.
     */
    fun isMarkdownTable(text: String): Boolean {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return false
        val markdownDelimiterRegex = Regex("""^\|?\s*:?-+:?\s*(\|?\s*:?-+:?\s*)+\|?$""")
        return lines.getOrNull(1)?.let { markdownDelimiterRegex.matches(it) } ?: false
    }
}
