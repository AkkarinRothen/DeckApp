package com.deckapp.feature.tables.tableimport

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.magnifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.domain.usecase.CsvTableParser
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.model.TableEntry
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TableImportScreen(
    onBack: () -> Unit,
    onImportFinished: (List<TableEntry>) -> Unit,
    viewModel: TableImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onImportFinished(uiState.editableEntries)
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onOcrSelected(it, isPdf = false) }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.onOcrSelected(it, isPdf = true) }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = { TextButton(onClick = { viewModel.clearError() }) { Text("Cerrar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stepTitle(uiState.step)) },
                navigationIcon = {
                    IconButton(onClick = { if (uiState.step == ImportStep.SOURCE_SELECTION) onBack() else viewModel.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.step == ImportStep.REVIEW && uiState.editableEntries.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.saveTable() },
                            enabled = !uiState.isProcessing
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (uiState.step) {
                ImportStep.SOURCE_SELECTION -> SourceSelectionStep(
                    onPickImage = { photoPicker.launch("image/*") },
                    onPickPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
                    onPickFile = { filePicker.launch(arrayOf("text/csv", "application/json", "text/plain")) },
                    onPasteText = {
                        val text = clipboard.getText()?.toString() ?: ""
                        if (text.isNotBlank()) viewModel.onTextPasted(text)
                    }
                )
                ImportStep.CROP -> CropStep(
                    bitmap = uiState.pageBitmap,
                    isPdf = uiState.isPdf,
                    currentPage = uiState.currentPageIndex,
                    totalPages = uiState.pdfPageCount,
                    suggestedPoints = uiState.suggestedPoints,
                    expectedTableCount = uiState.expectedTableCount,
                    onPageChange = { viewModel.loadPdfPage(it) },
                    onTableCountChange = { viewModel.updateExpectedTableCount(it) },
                    onCropFinished = { viewModel.onCropFinished(it) }
                )
                ImportStep.FILE_PREVIEW -> FilePreviewStep(
                    mode = uiState.mode,
                    rawText = uiState.rawText,
                    onTextChange = { viewModel.onRawTextChanged(it) },
                    onPreviewCsv = { viewModel.previewCsv() },
                    onParsePlainText = { viewModel.onTextPasted(uiState.rawText) }
                )
                ImportStep.MAPPING -> MappingStep(
                    preview = uiState.csvPreview,
                    onConfirmMapping = { config -> viewModel.applyMappingAndParse(config) }
                )
                ImportStep.REVIEW -> ReviewStep(
                    tableName = uiState.tableNameDraft,
                    tag = uiState.tableTagDraft,
                    entries = uiState.editableEntries,
                    validation = uiState.validationResult,
                    suggestedFormula = uiState.detectedTables.getOrNull(uiState.currentTableIndex)?.suggestedFormula ?: "1d6",
                    detectedTables = uiState.detectedTables,
                    currentTableIndex = uiState.currentTableIndex,
                    croppedBitmap = uiState.croppedBitmap,
                    onSelectTable = { viewModel.selectDetectedTable(it) },
                    onNameChange = { viewModel.updateTableName(it) },
                    onTagChange = { viewModel.updateTableTag(it) },
                    onUpdateEntry = { index, entry -> viewModel.updateEntry(index, entry) },
                    onDeleteEntry = { viewModel.deleteEntry(it) },
                    onMoveEntry = { from, to -> viewModel.moveEntry(from, to) },
                    onAddEntry = { viewModel.addEntry() },
                    onAddMoreContent = { viewModel.startStitching() }
                )
            }

            if (uiState.isProcessing) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// ── Step: Source Selection ────────────────────────────────────────────────────

@Composable
private fun SourceSelectionStep(
    onPickImage: () -> Unit, onPickPdf: () -> Unit,
    onPickFile: () -> Unit, onPasteText: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Importar tabla", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Elige de dónde quieres obtener la tabla",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        SourceCard(icon = { Icon(Icons.Default.Camera, null, tint = MaterialTheme.colorScheme.primary) },
            title = "Imagen / Captura de pantalla", subtitle = "OCR sobre imagen de un manual", onClick = onPickImage)
        Spacer(Modifier.height(12.dp))
        SourceCard(icon = { Icon(Icons.Default.PictureAsPdf, null, tint = MaterialTheme.colorScheme.error) },
            title = "PDF", subtitle = "Seleccionar página de un PDF", onClick = onPickPdf)
        Spacer(Modifier.height(12.dp))
        SourceCard(icon = { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary) },
            title = "Archivo CSV / JSON", subtitle = "Exportación de Foundry VTT u hoja de cálculo", onClick = onPickFile)
        Spacer(Modifier.height(12.dp))
        SourceCard(icon = { Icon(Icons.Default.ContentPaste, null, tint = MaterialTheme.colorScheme.tertiary) },
            title = "Pegar texto", subtitle = "Tabla en Markdown, lista o texto plano del portapapeles", onClick = onPasteText)
    }
}

@Composable
private fun SourceCard(icon: @Composable () -> Unit, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            icon(); Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Step: Crop (OCR) ──────────────────────────────────────────────────────────

private sealed class CropHandle {
    data class Corner(val index: Int) : CropHandle()
    data class Edge(val index: Int) : CropHandle()
}

@Composable
private fun CropStep(
    bitmap: Bitmap?, isPdf: Boolean, currentPage: Int, totalPages: Int,
    suggestedPoints: List<Offset>?,
    expectedTableCount: Int,
    onPageChange: (Int) -> Unit,
    onTableCountChange: (Int) -> Unit,
    onCropFinished: (Bitmap) -> Unit
) {
    if (bitmap == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // 4 puntos: TopLeft, TopRight, BottomRight, BottomLeft
    var points by remember {
        mutableStateOf(
            listOf(
                Offset(100f, 100f),
                Offset(600f, 100f),
                Offset(600f, 400f),
                Offset(100f, 400f)
            )
        )
    }
    
    // Rastrear qué estamos moviendo: Corner (0-3) o Edge (0-3: Top, Right, Bottom, Left)
    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }

    val density = androidx.compose.ui.platform.LocalDensity.current
    val touchSlop = with(density) { 32.dp.toPx() }
    val magnifierOffset = with(density) { 120.dp.toPx() }

    // Sincronizar con sugerencia de bordes
    LaunchedEffect(suggestedPoints) {
        suggestedPoints?.let { normalized ->
            if (containerSize != IntSize.Zero) {
                points = normalized.map { 
                    Offset(it.x * containerSize.width, it.y * containerSize.height)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (isPdf) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = currentPage > 0, onClick = { onPageChange(currentPage - 1) }) { Text("← Anterior") }
                Text("Pág ${currentPage + 1} / $totalPages", style = MaterialTheme.typography.labelLarge)
                TextButton(enabled = currentPage < totalPages - 1, onClick = { onPageChange(currentPage + 1) }) { Text("Siguiente →") }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { containerSize = it.size }
        ) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // 1. Buscar esquinas primero (prioridad)
                                val cornerIndex = points.indexOfFirst { (it - offset).getDistance() < touchSlop }
                                if (cornerIndex != -1) {
                                    activeHandle = CropHandle.Corner(cornerIndex)
                                    return@detectDragGestures
                                }

                                // 2. Buscar centros de aristas
                                val edgeMidpoints = listOf(
                                    (points[0] + points[1]) / 2f, // Top
                                    (points[1] + points[2]) / 2f, // Right
                                    (points[2] + points[3]) / 2f, // Bottom
                                    (points[3] + points[0]) / 2f  // Left
                                )
                                val edgeIndex = edgeMidpoints.indexOfFirst { (it - offset).getDistance() < touchSlop * 1.5f }
                                if (edgeIndex != -1) {
                                    activeHandle = CropHandle.Edge(edgeIndex)
                                    return@detectDragGestures
                                }
                                
                                activeHandle = null
                            },
                            onDrag = { change, delta ->
                                change.consume()
                                val newPoints = points.toMutableList()
                                when (val handle = activeHandle) {
                                    is CropHandle.Corner -> {
                                        newPoints[handle.index] = Offset(
                                            (newPoints[handle.index].x + delta.x).coerceIn(0f, containerSize.width.toFloat()),
                                            (newPoints[handle.index].y + delta.y).coerceIn(0f, containerSize.height.toFloat())
                                        )
                                    }
                                    is CropHandle.Edge -> {
                                        // Mover los dos puntos que definen la arista
                                        val idx1 = when (handle.index) { 0 -> 0; 1 -> 1; 2 -> 2; else -> 3 }
                                        val idx2 = when (handle.index) { 0 -> 1; 1 -> 2; 2 -> 3; else -> 0 }
                                        
                                        newPoints[idx1] = Offset(
                                            (newPoints[idx1].x + delta.x).coerceIn(0f, containerSize.width.toFloat()),
                                            (newPoints[idx1].y + delta.y).coerceIn(0f, containerSize.height.toFloat())
                                        )
                                        newPoints[idx2] = Offset(
                                            (newPoints[idx2].x + delta.x).coerceIn(0f, containerSize.width.toFloat()),
                                            (newPoints[idx2].y + delta.y).coerceIn(0f, containerSize.height.toFloat())
                                        )
                                    }
                                    null -> {
                                        // Mover todo el cuadro
                                        points = points.map {
                                            Offset(
                                                (it.x + delta.x).coerceIn(0f, containerSize.width.toFloat()),
                                                (it.y + delta.y).coerceIn(0f, containerSize.height.toFloat())
                                            )
                                        }
                                        return@detectDragGestures
                                    }
                                }
                                points = newPoints
                            },
                            onDragEnd = { activeHandle = null }
                        )
                    }
                    .then(
                        if (activeHandle is CropHandle.Corner) {
                            Modifier.magnifier(
                                sourceCenter = { 
                                    (activeHandle as? CropHandle.Corner)?.let { 
                                        points.getOrNull(it.index) ?: Offset.Unspecified 
                                    } ?: Offset.Unspecified 
                                },
                                magnifierCenter = { 
                                    val pt = (activeHandle as? CropHandle.Corner)?.let { points.getOrNull(it.index) } ?: return@magnifier Offset.Unspecified
                                    Offset(pt.x, (pt.y - magnifierOffset).coerceAtLeast(0f))
                                },
                                zoom = 2.5f,
                                size = DpSize(120.dp, 120.dp),
                                cornerRadius = 60.dp,
                                elevation = 8.dp
                            )
                        } else Modifier
                    )
            ) {
                val primaryColor = Color(0xFF42A5F5) // Blue 400
                val handleRadius = 14.dp.toPx()
                val strokeWidth = 2.5.dp.toPx()

                // 1. Polígono principal
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    lineTo(points[1].x, points[1].y)
                    lineTo(points[2].x, points[2].y)
                    lineTo(points[3].x, points[3].y)
                    close()
                }
                drawPath(path = path, color = primaryColor, style = Stroke(width = strokeWidth))

                // 2. Tiradores de Arista (Bars)
                val edgeMidpoints = listOf(
                    (points[0] + points[1]) / 2f, (points[1] + points[2]) / 2f,
                    (points[2] + points[3]) / 2f, (points[3] + points[0]) / 2f
                )
                edgeMidpoints.forEachIndexed { idx, point ->
                    val isVertical = idx % 2 != 0
                    val barW = if (isVertical) 6.dp.toPx() else 32.dp.toPx()
                    val barH = if (isVertical) 32.dp.toPx() else 6.dp.toPx()
                    
                    drawRoundRect(
                        color = if (activeHandle == CropHandle.Edge(idx)) Color.White else primaryColor,
                        topLeft = Offset(point.x - barW/2, point.y - barH/2),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(point.x - barW/2, point.y - barH/2),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // 3. Esquinas y Crosshairs
                points.forEachIndexed { idx, point ->
                    val isDragging = activeHandle == CropHandle.Corner(idx)
                    
                    if (isDragging) {
                        // CROSSHAIR de precisión (Mira telescópica)
                        // Línea negra exterior para contraste
                        drawLine(Color.Black.copy(0.5f), Offset(0f, point.y), Offset(size.width, point.y), 3.dp.toPx())
                        drawLine(Color.Black.copy(0.5f), Offset(point.x, 0f), Offset(point.x, size.height), 3.dp.toPx())
                        // Línea blanca central ultra-fina
                        drawLine(Color.White, Offset(0f, point.y), Offset(size.width, point.y), 1.dp.toPx())
                        drawLine(Color.White, Offset(point.x, 0f), Offset(point.x, size.height), 1.dp.toPx())
                        
                        // Círculo calado para no tapar el centro exacto
                        drawCircle(Color.White, radius = handleRadius, center = point, style = Stroke(width = 2.dp.toPx()))
                    } else {
                        // Círculo normal en reposo
                        drawCircle(color = primaryColor, radius = handleRadius, center = point)
                        drawCircle(color = Color.White, radius = handleRadius, center = point, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tablas en página:", style = MaterialTheme.typography.labelMedium)
            listOf(0 to "Auto", 1 to "1", 2 to "2", 3 to "3+").forEach { (valCount, label) ->
                FilterChip(
                    selected = expectedTableCount == valCount,
                    onClick = { onTableCountChange(valCount) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Button(
            onClick = {
                val warped = warpBitmap(bitmap, points, containerSize)
                onCropFinished(warped)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Analizar área seleccionada")
        }
    }
}

/** Corrige la perspectiva (Homografía) basándose en los 4 puntos. */
private fun warpBitmap(bitmap: Bitmap, uiPoints: List<Offset>, container: IntSize): Bitmap {
    val scale = min(
        container.width.toFloat() / bitmap.width,
        container.height.toFloat() / bitmap.height
    )
    val offsetX = (container.width - (bitmap.width * scale)) / 2
    val offsetY = (container.height - (bitmap.height * scale)) / 2

    // Mapear puntos de UI a píxeles reales del bitmap
    val srcPoints = FloatArray(8)
    uiPoints.forEachIndexed { i, pt ->
        srcPoints[i * 2] = (pt.x - offsetX) / scale
        srcPoints[i * 2 + 1] = (pt.y - offsetY) / scale
    }

    // Calcular dimensiones de destino (promedio de anchos y altos para preservar aspecto)
    val width1 = sqrt(((srcPoints[2] - srcPoints[0]) * (srcPoints[2] - srcPoints[0]) + (srcPoints[3] - srcPoints[1]) * (srcPoints[3] - srcPoints[1])))
    val width2 = sqrt(((srcPoints[6] - srcPoints[4]) * (srcPoints[6] - srcPoints[4]) + (srcPoints[7] - srcPoints[5]) * (srcPoints[7] - srcPoints[5])))
    val height1 = sqrt(((srcPoints[4] - srcPoints[2]) * (srcPoints[4] - srcPoints[2]) + (srcPoints[5] - srcPoints[3]) * (srcPoints[5] - srcPoints[3])))
    val height2 = sqrt(((srcPoints[0] - srcPoints[6]) * (srcPoints[0] - srcPoints[6]) + (srcPoints[1] - srcPoints[7]) * (srcPoints[1] - srcPoints[7])))
    
    val targetW = ((width1 + width2) / 2).toInt().coerceAtLeast(100)
    val targetH = ((height1 + height2) / 2).toInt().coerceAtLeast(100)

    val dstPoints = floatArrayOf(
        0f, 0f,
        targetW.toFloat(), 0f,
        targetW.toFloat(), targetH.toFloat(),
        0f, targetH.toFloat()
    )

    val matrix = android.graphics.Matrix()
    matrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

    val warpedBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(warpedBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.drawBitmap(bitmap, matrix, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))

    return warpedBitmap
}

// ── Step: File Preview ────────────────────────────────────────────────────────

@Composable
private fun FilePreviewStep(
    mode: ImportMode, rawText: String,
    onTextChange: (String) -> Unit, onPreviewCsv: () -> Unit, onParsePlainText: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Vista previa del texto", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = rawText, onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth().weight(1f),
            textStyle = MaterialTheme.typography.bodySmall,
            placeholder = { Text("Pega o edita el contenido aquí…") })
        Spacer(Modifier.height(12.dp))
        Button(onClick = if (mode == ImportMode.CSV) onPreviewCsv else onParsePlainText, modifier = Modifier.fillMaxWidth()) {
            Text(if (mode == ImportMode.CSV) "Siguiente: Configurar columnas" else "Analizar texto")
        }
    }
}

// ── Step: Mapping (CSV) ───────────────────────────────────────────────────────

@Composable
private fun MappingStep(preview: CsvTableParser.ParsePreview?, onConfirmMapping: (CsvTableParser.ParseConfig) -> Unit) {
    if (preview == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    var rangeColIndex by remember { mutableIntStateOf(preview.config.rangeColumnIndex) }
    var textColIndex by remember { mutableIntStateOf(preview.config.textColumnIndex) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Configurar columnas", style = MaterialTheme.typography.titleMedium)
        Text("Indica cuál columna es el rango y cuál es el texto del resultado.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            preview.headers.forEachIndexed { idx, header ->
                Column(modifier = Modifier.weight(1f).padding(4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).padding(8.dp)) {
                    Text(header, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = rangeColIndex == idx, onClick = { rangeColIndex = idx })
                        Text("Rango", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = textColIndex == idx, onClick = { textColIndex = idx })
                        Text("Texto", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Muestra (primeras filas):", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        preview.sampleRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                row.forEachIndexed { idx, cell ->
                    Surface(modifier = Modifier.weight(1f).padding(2.dp),
                        color = when (idx) {
                            rangeColIndex -> MaterialTheme.colorScheme.primaryContainer.copy(0.5f)
                            textColIndex -> MaterialTheme.colorScheme.secondaryContainer.copy(0.5f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                        }, shape = RoundedCornerShape(4.dp)) {
                        Text(cell, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onConfirmMapping(preview.config.copy(rangeColumnIndex = rangeColIndex, textColumnIndex = textColIndex)) },
            modifier = Modifier.fillMaxWidth()) {
            Text("Importar con esta configuración")
        }
    }
}

// ── Step: Review ──────────────────────────────────────────────────────────────

@Composable
private fun ReviewStep(
    tableName: String, tag: String, entries: List<TableEntry>,
    validation: RangeParser.ValidationResult?, suggestedFormula: String,
    detectedTables: List<com.deckapp.core.domain.usecase.ImportResult>,
    currentTableIndex: Int,
    croppedBitmap: Bitmap?,
    onSelectTable: (Int) -> Unit,
    onNameChange: (String) -> Unit, onTagChange: (String) -> Unit,
    onUpdateEntry: (Int, TableEntry) -> Unit, onDeleteEntry: (Int) -> Unit, 
    onMoveEntry: (Int, Int) -> Unit,
    onAddEntry: () -> Unit, onAddMoreContent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (croppedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                ZoomableImage(bitmap = croppedBitmap)
                
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(topEnd = 8.dp),
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Text(
                        "Referencia original",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            HorizontalDivider()
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            if (detectedTables.size > 1) {
                Text("Se detectaron ${detectedTables.size} tablas", 
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                ScrollableTabRow(
                    selectedTabIndex = currentTableIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTableIndex])
                        )
                    }
                ) {
                    detectedTables.forEachIndexed { index, table ->
                        Tab(
                            selected = currentTableIndex == index,
                            onClick = { onSelectTable(index) },
                            text = { 
                                Text(
                                    table.suggestedName.ifBlank { "Tabla ${index + 1}" },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(value = tableName, onValueChange = onNameChange,
                label = { Text("Nombre de la tabla") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = tag, onValueChange = onTagChange,
                label = { Text("Etiqueta (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Fórmula detectada: ", style = MaterialTheme.typography.bodySmall)
                    Text(suggestedFormula, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (validation != null && !validation.isValid) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            if (validation.gaps.isNotEmpty())
                                Text("Huecos en: ${validation.gaps.take(5).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (validation.overlaps.isNotEmpty())
                                Text("Solapamientos en: ${validation.overlaps.take(5).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${entries.size} entradas detectadas", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
        }
        itemsIndexed(entries) { index, entry ->
            EntryRow(
                entry = entry, 
                onUpdate = { onUpdateEntry(index, it) }, 
                onDelete = { onDeleteEntry(index) },
                onMoveUp = if (index > 0) { { onMoveEntry(index, index - 1) } } else null,
                onMoveDown = if (index < entries.size - 1) { { onMoveEntry(index, index + 1) } } else null
            )
        }
        item {
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddEntry, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Fila")
                }
                OutlinedButton(onClick = onAddMoreContent, modifier = Modifier.weight(1.2f)) {
                    Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp)); Text("Añadir página")
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}
}

@Composable
private fun EntryRow(
    entry: TableEntry, 
    onUpdate: (TableEntry) -> Unit, 
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Reorder handles / buttons
            Column(verticalArrangement = Arrangement.Center) {
                IconButton(onClick = onMoveUp ?: {}, enabled = onMoveUp != null, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ExpandLess, null, tint = if (onMoveUp != null) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
                }
                IconButton(onClick = onMoveDown ?: {}, enabled = onMoveDown != null, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ExpandMore, null, tint = if (onMoveDown != null) MaterialTheme.colorScheme.primary else Color.Gray.copy(0.3f))
                }
            }
            Spacer(Modifier.width(4.dp))
            // Rango editable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                    .padding(horizontal = 4.dp)
            ) {
                BasicTextField(
                    value = entry.minRoll.toString(),
                    onValueChange = { newVal: String -> val value = newVal.toIntOrNull() ?: entry.minRoll; onUpdate(entry.copy(minRoll = value)) },
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.width(intrinsicSize = IntrinsicSize.Min).widthIn(min = 24.dp)
                )
                Text("–", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = entry.maxRoll.toString(),
                    onValueChange = { newVal: String -> val value = newVal.toIntOrNull() ?: entry.maxRoll; onUpdate(entry.copy(maxRoll = value)) },
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.width(intrinsicSize = IntrinsicSize.Min).widthIn(min = 24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = entry.text, onValueChange = { onUpdate(entry.copy(text = it)) },
                modifier = Modifier.weight(1f), maxLines = 2, textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent))
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ZoomableImage(bitmap: Bitmap, modifier: Modifier = Modifier) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .transformable(state = state)
            .clipToBounds()
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.coerceIn(0.8f, 5f),
                    scaleY = scale.coerceIn(0.8f, 5f),
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

private fun stepTitle(step: ImportStep) = when (step) {
    ImportStep.SOURCE_SELECTION -> "Importar tabla"
    ImportStep.CROP -> "Seleccionar área"
    ImportStep.FILE_PREVIEW -> "Vista previa del texto"
    ImportStep.MAPPING -> "Configurar columnas"
    ImportStep.REVIEW -> "Revisar y guardar"
}
