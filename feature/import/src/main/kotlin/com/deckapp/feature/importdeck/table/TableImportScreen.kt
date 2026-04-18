package com.deckapp.feature.importdeck.table

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.TableImportSource
import com.deckapp.feature.importdeck.table.components.SourceSelectionView
import com.deckapp.feature.importdeck.table.components.PdfPreviewView
import com.deckapp.feature.importdeck.table.components.TableCropView
import com.deckapp.feature.importdeck.table.components.TableMappingView
import com.deckapp.feature.importdeck.table.components.TableReviewView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.StrokeCap
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.usecase.RenderPdfPageUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase
import com.deckapp.core.domain.usecase.ReadTextFromUriUseCase
import com.deckapp.core.domain.usecase.TranscribeTableWithAiUseCase
import com.deckapp.core.domain.usecase.RecognizeTableStreamingUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TableImportEntryPoint {
    fun renderPdfPageUseCase(): RenderPdfPageUseCase
    fun importTableUseCase(): ImportTableUseCase
    fun readTextFromUriUseCase(): ReadTextFromUriUseCase
    fun transcribeTableWithAiUseCase(): TranscribeTableWithAiUseCase
    fun recognizeTableStreamingUseCase(): RecognizeTableStreamingUseCase
    fun tableRepository(): TableRepository
    fun recentFileRepository(): RecentFileRepository
    fun fileRepository(): FileRepository
    fun settingsRepository(): SettingsRepository
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableImportScreen(
    onBack: () -> Unit,
    onNavigateToTable: (Long) -> Unit
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, TableImportEntryPoint::class.java)
    
    val viewModel: TableImportViewModel = viewModel(
        factory = TableImportViewModel.Factory(
            entryPoint.renderPdfPageUseCase(),
            entryPoint.importTableUseCase(),
            entryPoint.readTextFromUriUseCase(),
            entryPoint.tableRepository(),
            entryPoint.transcribeTableWithAiUseCase(),
            entryPoint.recognizeTableStreamingUseCase(),
            entryPoint.recentFileRepository(),
            entryPoint.fileRepository(),
            entryPoint.settingsRepository()
        )
    )
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            snackbarHostState.showSnackbar("Tablas guardadas con éxito")
            onBack()
        }
    }

    LaunchedEffect(uiState.autoVisionMessage) {
        val msg = uiState.autoVisionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearAutoVisionMessage()
    }

    val browsedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.browsePdfs(it) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectFile(it) }
    }

    val textFilePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importTextFile(it) }
    }

    var showSourceOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(getStepTitle(uiState.step), style = MaterialTheme.typography.titleMedium)
                            if (uiState.step != ImportStep.SOURCE_SELECTION) {
                                Text(
                                    "Paso ${getStepNumber(uiState.step)} de 5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        if (uiState.step == ImportStep.MAPPING) {
                            TextButton(onClick = { viewModel.reset() }) {
                                Text("Reiniciar")
                            }
                        }
                    }
                )
                if (uiState.step != ImportStep.SOURCE_SELECTION) {
                    val progress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = getStepProgress(uiState.step),
                        label = "StepProgress"
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = uiState.step, label = "TableImportStep") { currentStep ->
                when (currentStep) {
                    ImportStep.SOURCE_SELECTION -> {
                        SourceSelectionView(
                            onSelect = { source ->
                                if (source == TableImportSource.OCR_IMAGE) {
                                    showSourceOptions = true
                                } else {
                                    viewModel.setSource(source)
                                    when (source) {
                                        TableImportSource.CSV_TEXT -> textFilePickerLauncher.launch(arrayOf("text/comma-separated-values", "text/csv"))
                                        TableImportSource.JSON_TEXT -> textFilePickerLauncher.launch(arrayOf("application/json"))
                                        TableImportSource.PLAIN_TEXT -> textFilePickerLauncher.launch(arrayOf("text/plain", "text/markdown"))
                                        TableImportSource.MARKDOWN_TABLE -> textFilePickerLauncher.launch(arrayOf("text/markdown", "text/x-markdown"))
                                        else -> {}
                                    }
                                }
                            },
                            onFileSelect = { viewModel.selectFile(it) },
                            recentPdfs = uiState.recentPdfs,
                            browsedPdfs = uiState.browsedPdfs,
                            browsedThumbnails = uiState.browsedThumbnails,
                            isSearching = uiState.isProcessing,
                            onSearchExternal = { browsedLauncher.launch(null) }
                        )
                    }
                    ImportStep.FILE_PREVIEW -> {
                        PdfPreviewView(
                            bitmap = uiState.pageBitmap,
                            pageIndex = uiState.currentPageIndex,
                            pageCount = uiState.pdfPageCount,
                            onPageChange = { viewModel.loadPage(it) },
                            onConfirm = { viewModel.confirmPageSelection() }
                        )
                    }
                    ImportStep.CROP -> {
                        TableCropView(
                            bitmap = uiState.pageBitmap!!,
                            expectedTableCount = uiState.expectedTableCount,
                            isStitching = uiState.isStitchingMode,
                            onSetExpectedTableCount = { viewModel.setExpectedTableCount(it) },
                            onToggleStitching = { viewModel.toggleStitchingMode(it) },
                            onCropConfirmed = { viewModel.processCrop(it) }
                        )
                    }
                    ImportStep.RECOGNITION -> {
                        TableMappingView(
                            bitmap = uiState.croppedBitmap!!,
                            anchors = uiState.detectedAnchors,
                            onAddAnchor = { viewModel.addAnchor(it) },
                            onRemoveAnchor = { viewModel.removeAnchor(it) },
                            onConfirm = { viewModel.confirmRecognition() },
                            isVisionProcessing = uiState.isVisionProcessing,
                            onVisionRecognize = { viewModel.recognizeWithVision() }
                        )
                    }
                    ImportStep.MAPPING -> {
                        TableReviewView(
                            entries = uiState.editableEntries,
                            tableName = uiState.tableNameDraft,
                            tableTag = uiState.tableTagDraft,
                            tableFormula = uiState.tableFormulaDraft,
                            onEntryChange = { idx, entry -> viewModel.updateEntry(idx, entry) },
                            onNameChange = { viewModel.setDraftName(it) },
                            onTagChange = { viewModel.setDraftTag(it) },
                            onFormulaChange = { viewModel.setDraftFormula(it) },
                            onConfirm = { viewModel.nextTable() },
                            validationResult = uiState.validationResult,
                            lowConfidenceIndices = uiState.lowConfidenceIndices,
                            confidenceThreshold = uiState.confidenceThreshold,
                            onThresholdChange = { viewModel.setConfidenceThreshold(it) },
                            tableProgress = "${uiState.currentTableIndex + 1} de ${uiState.detectedTables.size}",
                            isAiProcessing = uiState.isAiProcessing,
                            isVisionProcessing = uiState.isVisionProcessing,
                            onAiOptimize = { viewModel.optimizeWithAi() },
                            croppedBitmap = uiState.croppedBitmap,
                            ocrBlocks = uiState.ocrBlocks,
                            detectedAnchors = uiState.detectedAnchors
                        )
                    }
                    ImportStep.REVIEW -> {
                        // Resumen final opcional
                        LaunchedEffect(Unit) { viewModel.saveAll() }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            if (uiState.isProcessing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (showSourceOptions) {
                ModalBottomSheet(
                    onDismissRequest = { showSourceOptions = false },
                    dragHandle = { BottomSheetDefaults.DragHandle() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 48.dp)
                    ) {
                        Text(
                            "Importar Imagen / PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Selecciona cómo quieres buscar el archivo para OCR.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        
                        ListItem(
                            headlineContent = { Text("Seleccionar Archivo") },
                            supportingContent = { Text("Elegir un PDF o imagen directamente") },
                            leadingContent = { Icon(Icons.Default.FileOpen, null) },
                            modifier = Modifier.clickable {
                                showSourceOptions = false
                                filePickerLauncher.launch(arrayOf("application/pdf", "image/*"))
                            }
                        )
                        
                        ListItem(
                            headlineContent = { Text("Explorar Carpeta") },
                            supportingContent = { Text("Ver miniaturas de todos los PDFs en una carpeta") },
                            leadingContent = { Icon(Icons.Default.FolderOpen, null) },
                            modifier = Modifier.clickable {
                                showSourceOptions = false
                                browsedLauncher.launch(null)
                            }
                        )
                    }
                }
            }

            if (uiState.retryCountdown != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Límite de cuota alcanzado. Reintentando en ${uiState.retryCountdown}s...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                val isQuotaError = "cuota" in errorMessage.lowercase() || "quota" in errorMessage.lowercase()
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text(if (isQuotaError) "Límite de API alcanzado" else "Error de IA") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Aceptar")
                        }
                    }
                )
            }
        }
    }
}

private fun getStepTitle(step: ImportStep): String = when (step) {
    ImportStep.SOURCE_SELECTION -> "Importar Tabla"
    ImportStep.FILE_PREVIEW -> "Vista previa"
    ImportStep.CROP -> "Recortar Tabla"
    ImportStep.RECOGNITION -> "Ajustar Columnas"
    ImportStep.MAPPING -> "Revisar Datos"
    ImportStep.REVIEW -> "Guardando..."
}

private fun getStepNumber(step: ImportStep): Int = when (step) {
    ImportStep.SOURCE_SELECTION -> 1
    ImportStep.FILE_PREVIEW -> 2
    ImportStep.CROP -> 3
    ImportStep.RECOGNITION -> 4
    ImportStep.MAPPING -> 5
    ImportStep.REVIEW -> 5
}

private fun getStepProgress(step: ImportStep): Float = when (step) {
    ImportStep.SOURCE_SELECTION -> 0.2f
    ImportStep.FILE_PREVIEW -> 0.4f
    ImportStep.CROP -> 0.6f
    ImportStep.RECOGNITION -> 0.8f
    ImportStep.MAPPING -> 0.95f
    ImportStep.REVIEW -> 1f
}
