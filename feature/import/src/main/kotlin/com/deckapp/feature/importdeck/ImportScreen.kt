package com.deckapp.feature.importdeck

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.CardContentMode
import com.deckapp.core.ui.components.ErrorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImportSuccess: (deckId: Long) -> Unit,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.importedDeckId) {
        uiState.importedDeckId?.let { onImportSuccess(it) }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? -> uri?.let { viewModel.onFolderSelected(it) } }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.onPdfSelected(it) } }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.onZipSelected(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar mazo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState.phase) {
                ImportPhase.SELECT_SOURCE -> SourceSelectionPhase(
                    onSelectFolder = {
                        viewModel.selectSource(ImportSource.FOLDER)
                        folderPickerLauncher.launch(null)
                    },
                    onSelectPdf = {
                        viewModel.selectSource(ImportSource.PDF)
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    onSelectZip = {
                        viewModel.selectSource(ImportSource.ZIP)
                        zipPickerLauncher.launch(arrayOf("application/zip"))
                    }
                )

                ImportPhase.CONFIGURE -> ConfigurePhase(
                    uiState = uiState,
                    onDeckNameChange = { viewModel.updateDeckName(it) },
                    onContentModeChange = { viewModel.updateDefaultContentMode(it) },
                    onPdfLayoutModeChange = { viewModel.updatePdfLayoutMode(it) },
                    onPdfGridColsChange = { viewModel.updatePdfGridCols(it) },
                    onPdfGridRowsChange = { viewModel.updatePdfGridRows(it) },
                    onPdfAutoTrimCellsChange = { viewModel.updatePdfAutoTrimCells(it) },
                    // Para PDF → vista previa antes de importar
                    // Para carpeta → importar directo
                    onPreview = { viewModel.generatePreview() },
                    onStartImport = { viewModel.startImport() }
                )

                ImportPhase.PREVIEW -> PreviewPhase(
                    bitmaps = uiState.previewCardBitmaps,
                    isLoading = uiState.isGeneratingPreview,
                    pageCount = uiState.pdfPageCount,
                    gridCols = uiState.pdfGridCols,
                    gridRows = uiState.pdfGridRows,
                    layoutMode = uiState.pdfLayoutMode,
                    onConfirm = { viewModel.startImport() },
                    onBack = { viewModel.backToConfigure() }
                )

                ImportPhase.IMPORTING -> ImportingPhase(
                    progress = uiState.importProgress,
                    cardCount = uiState.importedCardCount
                )

                ImportPhase.SUCCESS -> SuccessPhase(
                    cardCount = uiState.importedCardCount,
                    deckName = uiState.deckName,
                    failedFiles = uiState.failedFiles
                )
            }

            // Tarjeta de error con botón copiar
            uiState.errorMessage?.let { msg ->
                ErrorCard(
                    message = msg,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
    }
}

@Composable
private fun SourceSelectionPhase(
    onSelectFolder: () -> Unit,
    onSelectPdf: () -> Unit,
    onSelectZip: () -> Unit
) {
    Text("¿Desde dónde importás el mazo?", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    OutlinedCard(
        onClick = onSelectFolder,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text("Carpeta de imágenes", style = MaterialTheme.typography.titleSmall)
                Text(
                    "JPG, PNG, WEBP — cada imagen es una carta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    OutlinedCard(
        onClick = onSelectPdf,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Column {
                Text("PDF de mazo", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Print-and-play, Nord Games, etc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    OutlinedCard(
        onClick = onSelectZip,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Inventory2, // Icono de ZIP/Archivo
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Column {
                Text("Archivo ZIP", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Pack de imágenes comprimido (.zip)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfigurePhase(
    uiState: ImportUiState,
    onDeckNameChange: (String) -> Unit,
    onContentModeChange: (CardContentMode) -> Unit,
    onPdfLayoutModeChange: (PdfLayoutMode) -> Unit,
    onPdfGridColsChange: (Int) -> Unit,
    onPdfGridRowsChange: (Int) -> Unit,
    onPdfAutoTrimCellsChange: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onStartImport: () -> Unit
) {
    Text("Configurar importación", style = MaterialTheme.typography.titleMedium)

    OutlinedTextField(
        value = uiState.deckName,
        onValueChange = onDeckNameChange,
        label = { Text("Nombre del mazo") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    // PDF layout config
    if (uiState.source == ImportSource.PDF) {
        HorizontalDivider()
        Text("Layout del PDF", style = MaterialTheme.typography.labelLarge)

        uiState.pdfPreviewBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Preview PDF",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline),
                contentScale = ContentScale.Fit
            )
        }

        val pdfLayouts = listOf(
            PdfLayoutMode.ALTERNATING_PAGES to Triple(
                "Páginas alternadas",
                "Pág 1=frente, pág 2=dorso, pág 3=frente…",
                "2 caras por carta"
            ),
            PdfLayoutMode.SIDE_BY_SIDE to Triple(
                "Imagen | Descripción (mismo lado)",
                "Izquierda=imagen, derecha=texto/stats",
                "2 caras — ideal para NPCs, bestiarios"
            ),
            PdfLayoutMode.GRID to Triple(
                "Grilla N×M por página",
                "Print-and-play estándar, varias cartas por página",
                "1 cara por carta"
            ),
            PdfLayoutMode.FIRST_HALF_FRONTS to Triple(
                "Primera mitad frentes, segunda dorsos",
                "Págs 1..N=frentes, págs N+1..2N=dorsos",
                "2 caras por carta"
            )
        )
        pdfLayouts.forEach { (mode, info) ->
            val (title, subtitle, badge) = info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                RadioButton(
                    selected = uiState.pdfLayoutMode == mode,
                    onClick = { onPdfLayoutModeChange(mode) }
                )
                Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.pdfLayoutMode == mode) {
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Grilla disponible en todos los modos — el significado varía por modo
        Spacer(Modifier.height(4.dp))
        val gridHint = when (uiState.pdfLayoutMode) {
            PdfLayoutMode.GRID ->
                "Cartas por página: ${uiState.pdfGridCols} × ${uiState.pdfGridRows} = ${uiState.pdfGridCols * uiState.pdfGridRows} cartas/pág"
            PdfLayoutMode.SIDE_BY_SIDE ->
                "Pares por página: ${uiState.pdfGridCols} × ${uiState.pdfGridRows} = ${uiState.pdfGridCols * uiState.pdfGridRows} cartas/pág"
            PdfLayoutMode.ALTERNATING_PAGES ->
                "Cartas por página-par: ${uiState.pdfGridCols} × ${uiState.pdfGridRows} = ${uiState.pdfGridCols * uiState.pdfGridRows} cartas/pág"
            PdfLayoutMode.FIRST_HALF_FRONTS ->
                "Cartas por página: ${uiState.pdfGridCols} × ${uiState.pdfGridRows} = ${uiState.pdfGridCols * uiState.pdfGridRows} cartas/pág"
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(
                value = uiState.pdfGridCols,
                onValueChange = onPdfGridColsChange,
                label = "Columnas",
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = uiState.pdfGridRows,
                onValueChange = onPdfGridRowsChange,
                label = "Filas",
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            gridHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Recortar bordes blancos", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Elimina el espacio sobrante alrededor de cada carta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.pdfAutoTrimCells,
                onCheckedChange = onPdfAutoTrimCellsChange
            )
        }
    }

    // Content mode for all cards in deck
    HorizontalDivider()
    Text("Tipo de contenido", style = MaterialTheme.typography.labelLarge)

    // Para layouts de 2 caras, ofrecer opciones de cara doble; para grilla, solo de 1 cara.
    val isTwoFaceLayout = uiState.source == ImportSource.PDF &&
            uiState.pdfLayoutMode != PdfLayoutMode.GRID

    val contentModes = if (isTwoFaceLayout) {
        listOf(
            CardContentMode.DOUBLE_SIDED_FULL to "Dos caras completas" to
                "Tocá la carta para girarla y ver la otra cara",
            CardContentMode.IMAGE_ONLY to "Solo cara delantera" to
                "Ignora el dorso, solo importa la imagen frontal",
            CardContentMode.REVERSIBLE to "Reversible (tarot / oracle)" to
                "Misma imagen, texto derecho e invertido"
        )
    } else {
        listOf(
            CardContentMode.IMAGE_ONLY to "Solo imagen" to
                "Imagen full-screen con pinch-zoom",
            CardContentMode.IMAGE_WITH_TEXT to "Imagen + texto" to
                "Imagen con panel de descripción Markdown",
            CardContentMode.REVERSIBLE to "Reversible (tarot / oracle)" to
                "Misma imagen, texto derecho e invertido"
        )
    }
    contentModes.forEach { (modeAndLabel, description) ->
        val (mode, label) = modeAndLabel
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = uiState.defaultContentMode == mode,
                onClick = { onContentModeChange(mode) }
            )
            Column(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (uiState.source == ImportSource.PDF) {
        // PDF: primero previsualizar, luego confirmar
        Button(
            onClick = onPreview,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.deckName.isNotBlank()
        ) {
            Text("Vista previa")
        }
    } else {
        // Carpeta: importar directamente
        Button(
            onClick = onStartImport,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.deckName.isNotBlank()
        ) {
            Text("Importar")
        }
    }
}

@Composable
private fun PreviewPhase(
    bitmaps: List<android.graphics.Bitmap>,
    isLoading: Boolean,
    pageCount: Int,
    gridCols: Int,
    gridRows: Int,
    layoutMode: PdfLayoutMode,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Vista previa de cartas", style = MaterialTheme.typography.titleMedium)

        // Estimado de cartas totales (todas las grillas usan gridCols×gridRows)
        val cardsPerPage = gridCols * gridRows
        val estimatedTotal = when (layoutMode) {
            PdfLayoutMode.ALTERNATING_PAGES -> ((pageCount + 1) / 2) * cardsPerPage
            PdfLayoutMode.SIDE_BY_SIDE      -> pageCount * cardsPerPage
            PdfLayoutMode.GRID              -> pageCount * cardsPerPage
            PdfLayoutMode.FIRST_HALF_FRONTS -> (pageCount / 2) * cardsPerPage
        }
        val previewNote = when {
            layoutMode == PdfLayoutMode.SIDE_BY_SIDE ->
                "Pares: imagen (frente) + descripción (dorso). ~$estimatedTotal cartas estimadas."
            else ->
                "Mostrando las primeras ${bitmaps.size} de ~$estimatedTotal cartas estimadas."
        }
        Text(
            previewNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator()
                    Text("Generando previsualización…", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (bitmaps.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("No se pudieron generar previsualizaciones.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(2.dp)
            ) {
                itemsIndexed(bitmaps) { _, bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Acciones
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text("Importar ($estimatedTotal cartas aprox.)")
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ajustar configuración")
        }
    }
}

/**
 * Campo numérico sin interferencia durante la escritura.
 *
 * Problema que resuelve: si al borrar el campo queda "0" y el cursor está antes
 * del cero, escribir "2" produce "20". La solución es permitir el campo vacío
 * mientras el usuario escribe y validar solo al perder el foco.
 *
 * Comportamiento:
 * - Mientras el campo tiene foco: acepta cualquier secuencia de dígitos (incluso vacío)
 * - Llama [onValueChange] en cada cambio válido (≥1) para actualizar el hint en tiempo real
 * - Al perder foco: si está vacío o < 1, lo normaliza a 1
 * - [value] externo solo sincroniza el display si difiere del texto actual parseado
 *   (evita resetear mientras el usuario está escribiendo)
 */
@Composable
private fun NumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(value.toString()) }
    var hasFocus by remember { mutableStateOf(false) }

    // Sincronizar con cambios externos solo cuando el campo no tiene foco
    LaunchedEffect(value) {
        if (!hasFocus && text.toIntOrNull() != value) {
            text = value.toString()
        }
    }

    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            // Solo dígitos; campo puede quedar vacío mientras se escribe
            val digits = raw.filter { it.isDigit() }
            text = digits
            val parsed = digits.toIntOrNull()
            if (parsed != null && parsed >= 1) onValueChange(parsed)
        },
        label = { Text(label) },
        modifier = modifier.onFocusChanged { focusState ->
            hasFocus = focusState.isFocused
            if (!focusState.isFocused) {
                // Al salir: normalizar a mínimo 1 y limpiar ceros iniciales
                val parsed = text.toIntOrNull()?.coerceAtLeast(1) ?: 1
                text = parsed.toString()
                onValueChange(parsed)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ImportingPhase(progress: Float, cardCount: Int) {
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator(progress = { progress })
            Text("Importando cartas...", style = MaterialTheme.typography.bodyLarge)
            if (cardCount > 0) {
                Text("$cardCount cartas procesadas", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SuccessPhase(cardCount: Int, deckName: String, failedFiles: List<String> = emptyList()) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Mazo importado", style = MaterialTheme.typography.headlineSmall)
        Text(
            "\"$deckName\" — $cardCount cartas",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
        if (failedFiles.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${failedFiles.size} archivo${if (failedFiles.size != 1) "s" else ""} no se pudo importar:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    failedFiles.forEach { name ->
                        Text(
                            "• $name",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
