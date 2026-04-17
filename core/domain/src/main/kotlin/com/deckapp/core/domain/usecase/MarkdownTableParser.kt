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
class MarkdownTableParser {

    /**
     * Parsea un texto que contiene una o más tablas Markdown.
     * Si hay varias tablas, por ahora las une en una sola lista de entradas.
     */
    fun parse(rawText: String): List<TableEntry> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (lines.size < 2) return emptyList()

        val entries = mutableListOf<TableEntry>()
        var sortOrder = 0
        
        // Identificar si la segunda línea es un delimitador de Markdown (|---|)
        val hasMarkdownDelimiter = lines.getOrNull(1)?.contains(Regex("""\|?\s*:?-+:?\s*\|""")) == true
        
        val dataLines = if (hasMarkdownDelimiter) {
            // Saltamos el header (0) y el delimitador (1)
            lines.drop(2)
        } else {
            // No parece una tabla MD estándar competa, probamos a ver si tiene delimitadores |
            lines
        }

        dataLines.forEach { line ->
            // Dividir por | y limpiar vacíos de los extremos
            val parts = line.split("|")
                .map { it.trim() }
                .filterIndexed { index, _ -> 
                    // Si la línea empieza/termina con |, split deja elementos vacíos al inicio/fin
                    // Solo los ignoramos si están vacíos.
                    true 
                }
                .filter { it.isNotEmpty() || line.contains("|") } // Mantener si la línea tiene estructura
            
            // Una línea de tabla MD suele tener al menos 2 columnas reales (si se cuenta el | inicial/final)
            // | 1 | Texto | -> ["", "1", "Texto", ""]
            val actualCols = if (line.startsWith("|")) parts.drop(1) else parts
            val cleanCols = if (line.endsWith("|")) actualCols.dropLast(1) else actualCols

            if (cleanCols.size < 2) return@forEach

            val rangeText = cleanCols[0]
            val entryText = cleanCols.drop(1).joinToString(" — ")

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

    /**
     * Heurística rápida para saber si un texto parece contener una tabla Markdown.
     */
    fun isMarkdownTable(text: String): Boolean {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 2) return false
        // Busca el patrón |---| en la segunda línea
        return lines.getOrNull(1)?.contains(Regex("""\|?\s*:?-+:?\s*\|""")) == true
    }
}
