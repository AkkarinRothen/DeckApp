package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.OcrRepository
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.TableImportSource
import javax.inject.Inject

/**
 * Parámetros para importaciones de archivo/texto estructurado.
 */
data class TextImportParams(
    val source: TableImportSource,
    val rawText: String,
    // Solo para CSV:
    val csvConfig: CsvTableParser.ParseConfig? = null
)

/**
 * Resultado de una importación — incluye las entradas y metadatos para poblar los campos de RandomTable.
 */
data class ImportResult(
    val suggestedName: String = "",
    val suggestedDescription: String = "",
    val suggestedFormula: String = "1d6",
    val sourceType: String,
    val entries: List<TableEntry>,
    val tags: List<com.deckapp.core.model.Tag> = emptyList(),
    /** Índices de [entries] con confianza OCR baja. Vacío para fuentes no-OCR. */
    val lowConfidenceIndices: Set<Int> = emptySet(),
    val entryBlocks: Map<Int, List<com.deckapp.core.model.OcrBlock>> = emptyMap()
)

/**
 * Orquestador universal de importación de tablas.
 * Recibe la fuente (OCR, CSV, JSON, Texto) y delega al motor correcto.
 */
class ImportTableUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val analyzeTableImageUseCase: AnalyzeTableImageUseCase,
    private val importProvider: TableImportProvider
) {
    /** Importar desde imagen/PDF via OCR. Devuelve todas las tablas detectadas. */
    suspend fun fromBitmap(bitmap: Bitmap, expectedTableCount: Int = 0): List<ImportResult> {
        val blocks = getRawBlocks(bitmap)
        val analyses = analyzeTableImageUseCase(blocks, expectedTableCount)

        return analyses.map { analysis ->
            ImportResult(
                sourceType = "OCR",
                suggestedName = analysis.suggestedName,
                entries = analysis.entries,
                suggestedFormula = inferFormula(analysis.entries),
                lowConfidenceIndices = analysis.lowConfidenceIndices,
                entryBlocks = analysis.entryBlocks
            )
        }
    }

    /** Devuelve los bloques de texto detectados sin procesar. */
    suspend fun getRawBlocks(bitmap: Bitmap): List<com.deckapp.core.model.OcrBlock> {
        return ocrRepository.recognizeText(bitmap).getOrThrow()
    }

    /** Importar desde texto estructurado (CSV, JSON, Texto Plano). */
    fun fromText(params: TextImportParams): ImportResult {
        val parser = importProvider.getParser(params.source)
        
        val content = if (params.source == TableImportSource.CSV_TEXT && params.csvConfig != null) {
            val entries = (parser as CsvTableParser).parse(params.rawText, params.csvConfig)
            ParsedTableContent(entries = entries)
        } else {
            parser.parse(params.rawText)
        }

        if (content.entries.isEmpty()) {
            throw TableParseException.EmptyResult(params.source.name)
        }

        return ImportResult(
            sourceType = params.source.name,
            suggestedName = content.name ?: "",
            suggestedDescription = content.description ?: "",
            entries = content.entries,
            suggestedFormula = inferFormula(content.entries)
        )
    }

    /** Previsualización de CSV sin comprometerse a parsear completamente. */
    fun previewCsv(rawText: String): CsvTableParser.ParsePreview = CsvTableParser().preview(rawText)

    private fun inferFormula(entries: List<TableEntry>): String {
        val min = entries.minOfOrNull { it.minRoll } ?: 1
        val max = entries.maxOfOrNull { it.maxRoll } ?: 6
        return RangeParser.inferRollFormula(min, max)
    }
}
