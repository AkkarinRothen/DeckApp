package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ImportPreviewData
import javax.inject.Inject

class ImportReferenceTableFromCsvUseCase @Inject constructor(
    private val csvParser: CsvTableParser
) {
    operator fun invoke(rawText: String): Result<ImportPreviewData> = runCatching {
        require(rawText.isNotBlank()) { "El contenido CSV está vacío." }
        csvParser.parseAllRows(rawText)
    }
}
