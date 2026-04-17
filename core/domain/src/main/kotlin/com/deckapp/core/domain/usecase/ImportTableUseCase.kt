package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.OcrRepository
import com.deckapp.core.model.TableEntry
import javax.inject.Inject

/**
 * Enumeración de fuentes de importación soportadas.
 */
enum class ImportSource {
    OCR_IMAGE,   // Imagen/PDF con análisis óptico
    CSV_TEXT,    // Texto en formato CSV / TSV / DSV
    JSON_TEXT,   // JSON (Foundry VTT, DeckApp Export, o array simple)
    PLAIN_TEXT,  // Texto plano (portapapeles, listas)
    MARKDOWN_TABLE // Tablas estándar Markdown (|---|)
}

/**
 * Parámetros para importaciones de archivo/texto estructurado.
 */
data class TextImportParams(
    val source: ImportSource,
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
    /** Índices de [entries] con confianza OCR baja. Vacío para fuentes no-OCR. */
    val lowConfidenceIndices: Set<Int> = emptySet()
)

/**
 * Orquestador universal de importación de tablas.
 * Recibe la fuente (OCR, CSV, JSON, Texto) y delega al motor correcto.
 */
class ImportTableUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val analyzeTableImageUseCase: AnalyzeTableImageUseCase
) {
    // Parsers de texto sin estado — instancias compartidas
    private val csvParser = CsvTableParser()
    private val jsonParser = JsonTableParser()
    private val plainTextParser = PlainTextTableParser()
    private val markdownParser = MarkdownTableParser()

    /** Importar desde imagen/PDF via OCR. Devuelve todas las tablas detectadas.
     *  Lanza [com.deckapp.core.domain.repository.OcrException] si ML Kit falla. */
    suspend fun fromBitmap(bitmap: Bitmap, expectedTableCount: Int = 0): List<ImportResult> {
        val blocks = getRawBlocks(bitmap)
        val analyses = analyzeTableImageUseCase(blocks, expectedTableCount)

        return analyses.map { analysis ->
            ImportResult(
                sourceType = "OCR",
                suggestedName = analysis.suggestedName,
                entries = analysis.entries,
                suggestedFormula = inferFormula(analysis.entries),
                lowConfidenceIndices = analysis.lowConfidenceIndices
            )
        }
    }

    /** Devuelve los bloques de texto detectados sin procesar.
     *  Propaga [com.deckapp.core.domain.repository.OcrException] ante fallo de ML Kit. */
    suspend fun getRawBlocks(bitmap: Bitmap): List<com.deckapp.core.model.OcrBlock> {
        return ocrRepository.recognizeText(bitmap).getOrThrow()
    }

    /** Importar desde texto estructurado (CSV, JSON, Texto Plano). */
    fun fromText(params: TextImportParams): ImportResult {
        return when (params.source) {
            ImportSource.CSV_TEXT -> {
                val config = params.csvConfig ?: csvParser.preview(params.rawText).config
                val entries = csvParser.parse(params.rawText, config)
                ImportResult(
                    sourceType = "CSV",
                    entries = entries,
                    suggestedFormula = inferFormula(entries)
                )
            }
            ImportSource.JSON_TEXT -> {
                val parsed = jsonParser.parse(params.rawText)
                if (parsed.entries.isEmpty()) throw TableParseException.EmptyResult("el archivo JSON")
                ImportResult(
                    sourceType = "JSON",
                    suggestedName = parsed.name,
                    suggestedDescription = parsed.description,
                    entries = parsed.entries,
                    suggestedFormula = inferFormula(parsed.entries)
                )
            }
            ImportSource.PLAIN_TEXT -> {
                val entries = plainTextParser.parse(params.rawText)
                if (entries.isEmpty()) throw TableParseException.EmptyResult("el texto pegado")
                ImportResult(
                    sourceType = "TEXT",
                    entries = entries,
                    suggestedFormula = inferFormula(entries)
                )
            }
            ImportSource.MARKDOWN_TABLE -> {
                val entries = markdownParser.parse(params.rawText)
                if (entries.isEmpty()) throw TableParseException.EmptyResult("la tabla Markdown")
                ImportResult(
                    sourceType = "MARKDOWN",
                    entries = entries,
                    suggestedFormula = inferFormula(entries)
                )
            }
            ImportSource.OCR_IMAGE -> throw IllegalArgumentException("Use fromBitmap() para OCR.")
        }
    }

    /** Previsualización de CSV sin comprometerse a parsear completamente. */
    fun previewCsv(rawText: String): CsvTableParser.ParsePreview = csvParser.preview(rawText)

    private fun inferFormula(entries: List<TableEntry>): String {
        val min = entries.minOfOrNull { it.minRoll } ?: 1
        val max = entries.maxOfOrNull { it.maxRoll } ?: 6
        return RangeParser.inferRollFormula(min, max)
    }
}
