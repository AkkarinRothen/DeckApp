package com.deckapp.feature.importdeck.table

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
import com.deckapp.core.domain.usecase.ImportSource
import com.deckapp.feature.importdeck.table.components.SourceSelectionView
import com.deckapp.feature.importdeck.table.components.PdfPreviewView
import com.deckapp.feature.importdeck.table.components.TableCropView
import com.deckapp.feature.importdeck.table.components.TableMappingView
import com.deckapp.feature.importdeck.table.components.TableReviewView
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import androidx.compose.ui.platform.LocalContext
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.RecentFileRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.usecase.RenderPdfPageUseCase
import com.deckapp.core.domain.usecase.ImportTableUseCase
import com.deckapp.core.domain.usecase.ReadTextFromUriUseCase

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TableImportEntryPoint {
    fun renderPdfPageUseCase(): RenderPdfPageUseCase
    fun importTableUseCase(): ImportTableUseCase
    fun readTextFromUriUseCase(): ReadTextFromUriUseCase
    fun tableRepository(): TableRepository
    fun recentFileRepository(): RecentFileRepository
    fun fileRepository(): FileRepository
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
            entryPoint.recentFileRepository(),
            entryPoint.fileRepository()
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

    var showSourceOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getStepTitle(uiState.step)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Crossfade(targetState = uiState.step, label = "TableImportStep") { step ->
                when (step) {
                    ImportStep.SOURCE_SELECTION -> {
                        SourceSelectionView(
                            onSelect = { source ->
                                if (source == ImportSource.OCR_IMAGE) {
                                    showSourceOptions = true
                                } else {
                                    viewModel.setSource(source)
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
                            onConfirm = { viewModel.confirmRecognition() }
                        )
                    }
                    ImportStep.MAPPING -> {
                        TableReviewView(
                            entries = uiState.editableEntries,
                            tableName = uiState.tableNameDraft,
                            tableTag = uiState.tableTagDraft,
                            onEntryChange = { idx, entry -> viewModel.updateEntry(idx, entry) },
                            onNameChange = { viewModel.setDraftName(it) },
                            onTagChange = { viewModel.setDraftTag(it) },
                            onConfirm = { viewModel.nextTable() },
                            validationResult = uiState.validationResult,
                            lowConfidenceIndices = uiState.lowConfidenceIndices,
                            tableProgress = "${uiState.currentTableIndex + 1} de ${uiState.detectedTables.size}"
                        )
                    }
                    ImportStep.REVIEW -> {
                        // Resumen final opcional
                        LaunchedEffect(Unit) { viewModel.saveAll() }
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

            uiState.errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text("Error") },
                    text = { Text(error) },
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
