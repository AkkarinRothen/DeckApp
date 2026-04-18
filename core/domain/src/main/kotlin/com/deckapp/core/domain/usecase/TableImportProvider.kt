package com.deckapp.core.domain.usecase

import com.deckapp.core.model.TableImportSource
import javax.inject.Inject

/**
 * Proveedor centralizado que detecta y aplica el parser adecuado para un texto dado.
 * Implementa el patrón Strategy para la importación de tablas.
 */
class TableImportProvider @Inject constructor() {
    
    private val jsonParser = JsonTableParser()
    private val markdownParser = MarkdownTableParser()
    private val csvParser = CsvTableParser()
    private val plainTextParser = PlainTextTableParser()

    private val parsers = listOf(
        jsonParser,
        markdownParser,
        csvParser,
        plainTextParser // Fallback
    )

    /**
     * Autodetecta el formato y parsea el texto.
     */
    fun parse(rawText: String): ParsedTableContent {
        val parser = parsers.find { it.canParse(rawText) } ?: plainTextParser
        return parser.parse(rawText)
    }

    /**
     * Devuelve el parser específico para una fuente dada.
     */
    fun getParser(source: TableImportSource): TableParser {
        return when (source) {
            TableImportSource.JSON_TEXT -> jsonParser
            TableImportSource.MARKDOWN_TABLE -> markdownParser
            TableImportSource.CSV_TEXT -> csvParser
            TableImportSource.PLAIN_TEXT -> plainTextParser
            TableImportSource.OCR_IMAGE -> throw IllegalArgumentException("OCR no es un parser de texto plano.")
        }
    }
}
