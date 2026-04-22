package com.deckapp.feature.reference

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.ManualBookmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualViewerScreen(
    manualId: Long,
    onBack: () -> Unit,
    viewModel: ManualViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val pagerState = rememberPagerState(pageCount = { uiState.pageCount })
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var currentScale by remember { mutableFloatStateOf(1f) }

    // Cargar manual al iniciar
    LaunchedEffect(manualId) {
        viewModel.loadManual(manualId)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                BookmarksDrawerContent(
                    bookmarks = uiState.bookmarks,
                    onJump = { pageIndex ->
                        scope.launch {
                            pagerState.scrollToPage(pageIndex)
                            drawerState.close()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(uiState.manualName, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Bookmarks, "Marcadores")
                        }
                        IconButton(onClick = { viewModel.toggleOcrMode() }) {
                            Icon(
                                Icons.Default.DocumentScanner, 
                                "OCR",
                                tint = if (uiState.isOcrMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    },
                    colors = topAppBarColors(
                        containerColor = Color(0xFF1A1A1A),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize().background(Color(0xFF0A0A0A))) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (uiState.pdfRenderer != null) {
                    Column(Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = !uiState.isOcrMode && currentScale == 1f, // Deshabilitar swipe si hay zoom
                            contentPadding = PaddingValues(horizontal = 32.dp),
                            pageSpacing = 16.dp,
                            verticalAlignment = Alignment.CenterVertically
                        ) { index ->
                            PdfPageItem(
                                renderer = uiState.pdfRenderer!!,
                                pageIndex = index,
                                isOcrMode = uiState.isOcrMode,
                                onAreaSelected = { rect -> viewModel.performOcr(index, rect) },
                                onScaleChanged = { currentScale = it }
                            )
                        }

                        // Barra de navegación inferior premium
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            tonalElevation = 12.dp,
                            color = Color(0xFF1A1A1A),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                                    .navigationBarsPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    TextButton(
                                        onClick = { 
                                            jumpPageInput = (pagerState.currentPage + 1).toString()
                                            showJumpDialog = true 
                                        }
                                    ) {
                                        Text(
                                            text = "Página ${pagerState.currentPage + 1} de ${uiState.pageCount}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                                            enabled = pagerState.currentPage > 0
                                        ) {
                                            Icon(Icons.Default.FirstPage, "Inicio", tint = Color.White)
                                        }

                                        IconButton(
                                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                                            enabled = pagerState.currentPage > 0
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Anterior", tint = Color.White)
                                        }
                                        
                                        IconButton(
                                            onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                                            enabled = pagerState.currentPage < uiState.pageCount - 1
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente", tint = Color.White)
                                        }

                                        IconButton(
                                            onClick = { scope.launch { pagerState.animateScrollToPage(uiState.pageCount - 1) } },
                                            enabled = pagerState.currentPage < uiState.pageCount - 1
                                        ) {
                                            Icon(Icons.Default.LastPage, "Final", tint = Color.White)
                                        }
                                    }
                                }
                                
                                Slider(
                                    value = pagerState.currentPage.toFloat(),
                                    onValueChange = { page -> 
                                        scope.launch { pagerState.scrollToPage(page.toInt()) }
                                    },
                                    valueRange = 0f..(uiState.pageCount - 1).coerceAtLeast(0).toFloat(),
                                    steps = if (uiState.pageCount > 1) uiState.pageCount - 2 else 0,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Overlay de resultado OCR
                uiState.ocrResult?.let { result ->
                    AlertDialog(
                        onDismissRequest = viewModel::clearOcrResult,
                        title = { Text("Texto Extraído") },
                        text = { 
                            Column {
                                Text(result.text)
                                if (result.confidence < 0.7f) {
                                    Text(
                                        "Aviso: Baja confianza en la lectura", 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = viewModel::saveOcrToWiki) {
                                Text("Guardar en Wiki")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = viewModel::clearOcrResult) {
                                Text("Cerrar")
                            }
                        }
                    )
                }

                if (showJumpDialog) {
                    AlertDialog(
                        onDismissRequest = { showJumpDialog = false },
                        title = { Text("Ir a la Página") },
                        text = {
                            OutlinedTextField(
                                value = jumpPageInput,
                                onValueChange = { if (it.all { char -> char.isDigit() }) jumpPageInput = it },
                                label = { Text("Número de página (1 - ${uiState.pageCount})") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val page = jumpPageInput.toIntOrNull()?.minus(1)
                                    if (page != null && page in 0 until uiState.pageCount) {
                                        scope.launch {
                                            pagerState.scrollToPage(page)
                                            showJumpDialog = false
                                        }
                                    }
                                }
                            ) {
                                Text("Ir")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showJumpDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksDrawerContent(
    bookmarks: List<ManualBookmark>,
    onJump: (Int) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Marcadores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        if (bookmarks.isEmpty()) {
            Text("No hay marcadores en este manual", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn {
                items(bookmarks) { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.label) },
                        supportingContent = { Text("Página ${bookmark.pageIndex + 1}") },
                        leadingContent = { 
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    (bookmark.pageIndex + 1).toString(),
                                    modifier = Modifier.padding(4.dp),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        },
                        modifier = Modifier.clickable { onJump(bookmark.pageIndex) }
                    )
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    isOcrMode: Boolean,
    onAreaSelected: (Rect) -> Unit,
    onScaleChanged: (Float) -> Unit = {}
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    // Para calcular límites dinámicos
    var containerSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale
        onScaleChanged(newScale)
        
        if (newScale > 1f) {
            // Calcular límites máximos (cuánto se puede mover la imagen escalada)
            val maxX = (containerSize.width * (newScale - 1)) / (2 * newScale)
            val maxY = (containerSize.height * (newScale - 1)) / (2 * newScale)
            
            val potentialOffset = offset + offsetChange
            offset = Offset(
                potentialOffset.x.coerceIn(-maxX, maxX),
                potentialOffset.y.coerceIn(-maxY, maxY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    // Selección de área para OCR
    var selectionStart by remember { mutableStateOf<Offset?>(null) }
    var selectionEnd by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            val page = renderer.openPage(pageIndex)
            // Renderizamos a una resolución razonable para pantalla
            val b = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(b)
            canvas.drawColor(android.graphics.Color.WHITE) // Fondo blanco para PDF transparentes
            page.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap = b
            page.close()
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.707f)
            .padding(16.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            containerSize = androidx.compose.ui.unit.IntSize(constraints.maxWidth, constraints.maxHeight)
            
            bitmap?.let {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x * scale
                            translationY = offset.y * scale
                        }
                        .transformable(state = transformState)
                        .pointerInput(isOcrMode, scale) {
                            if (isOcrMode) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { selectionStart = it; selectionEnd = it },
                                    onDrag = { change, dragAmount -> 
                                        change.consume()
                                        selectionEnd = (selectionEnd ?: Offset.Zero) + dragAmount 
                                    },
                                    onDragEnd = {
                                        if (selectionStart != null && selectionEnd != null) {
                                            val rect = Rect(
                                                selectionStart!!.x.toInt(), 
                                                selectionStart!!.y.toInt(),
                                                selectionEnd!!.x.toInt(),
                                                selectionEnd!!.y.toInt()
                                            )
                                            onAreaSelected(rect)
                                        }
                                        selectionStart = null
                                        selectionEnd = null
                                    }
                                )
                            } else {
                                // Doble toque para zoom rápido / reset
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                            onScaleChanged(1f)
                                        } else {
                                            scale = 3f
                                            onScaleChanged(3f)
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    drawImage(it.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()))
                    
                    // Dibujar rectángulo de selección
                    if (selectionStart != null && selectionEnd != null) {
                        drawRect(
                            color = Color.Primary.copy(alpha = 0.3f),
                            topLeft = selectionStart!!,
                            size = androidx.compose.ui.geometry.Size(
                                selectionEnd!!.x - selectionStart!!.x,
                                selectionEnd!!.y - selectionStart!!.y
                            )
                        )
                        drawRect(
                            color = Color.Primary,
                            topLeft = selectionStart!!,
                            size = androidx.compose.ui.geometry.Size(
                                selectionEnd!!.x - selectionStart!!.x,
                                selectionEnd!!.y - selectionStart!!.y
                            ),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            } ?: CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

val Color.Companion.Primary get() = Color(0xFF6200EE)
