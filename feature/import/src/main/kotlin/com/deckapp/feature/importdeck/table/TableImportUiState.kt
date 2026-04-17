package com.deckapp.feature.importdeck.table

import android.graphics.Bitmap
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import com.deckapp.core.domain.usecase.ImportResult
import com.deckapp.core.domain.usecase.RangeParser

enum class ImportMode { NONE, OCR, CSV, JSON, PLAIN_TEXT }
enum class ImportStep { SOURCE_SELECTION, FILE_PREVIEW, CROP, RECOGNITION, MAPPING, REVIEW }
enum class StitchingMode { CONTINUE_RANGES, APPEND }

data class TableImportUiState(
    val mode: ImportMode = ImportMode.NONE,
    val step: ImportStep = ImportStep.SOURCE_SELECTION,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    
    // Decks/Tables
    val availableDecks: List<com.deckapp.core.model.RandomTable> = emptyList(),
    val selectedDeckId: String? = null,
    
    // PDF/Images
    val pdfUri: android.net.Uri? = null,
    val selectedUri: android.net.Uri? = null,
    val pdfPages: List<Bitmap> = emptyList(),
    val currentPageIndex: Int = 0,
    val pageBitmap: Bitmap? = null,
    val pdfPageCount: Int = 0,
    val recentPdfs: List<Pair<android.net.Uri, String>> = emptyList(),
    val browsedPdfs: List<Pair<android.net.Uri, String>> = emptyList(),
    val browsedThumbnails: Map<android.net.Uri, Bitmap?> = emptyMap(),
    
    // Results
    val detectedTables: List<ImportResult> = emptyList(),
    val currentTableIndex: Int = 0,
    val editableEntries: List<TableEntry> = emptyList(),
    val tableName: String = "",
    val tableTag: String = "",
    val tableNameDraft: String = "",
    val tableTagDraft: String = "",
    val savedSuccessfully: Boolean = false,
    val validationResult: RangeParser.ValidationResult? = null,
    val confidenceThreshold: Float = 0.6f,
    val lowConfidenceIndices: Set<Int> = emptySet(),
    
    // OCR Advanced
    val isStitchingMode: Boolean = false,
    val stitchingMode: StitchingMode = StitchingMode.CONTINUE_RANGES,
    val expectedTableCount: Int = 0, // 0 = Auto
    val suggestedPoints: List<Pair<Float, Float>>? = null,
    val croppedBitmap: Bitmap? = null,
    val ocrBlocks: List<OcrBlock> = emptyList(),
    val detectedAnchors: List<Float> = emptyList(),
    val currentCluster: List<OcrBlock> = emptyList()
)
