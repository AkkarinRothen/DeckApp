package com.deckapp.core.data.util

import com.deckapp.core.model.ReferenceImportSource
import com.deckapp.core.model.ImportPreviewData

object CsvTableParser {
    fun parseAllRows(csvText: String): ImportPreviewData {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return ImportPreviewData(emptyList(), emptyList(), ReferenceImportSource.CSV)

        // Detect separator (comma, semicolon or tab)
        val firstLine = lines.first()
        val separator = when {
            firstLine.contains("\t") -> "\t"
            firstLine.contains(";") -> ";"
            else -> ","
        }

        val allRows = lines.map { line ->
            line.split(separator).map { it.trim().removeSurrounding("\"") }
        }

        val headers = allRows.first()
        val dataRows = allRows.drop(1)

        return ImportPreviewData(
            headers = headers,
            rows = dataRows,
            source = ReferenceImportSource.CSV
        )
    }
}
