package com.deckapp.feature.importdeck.table.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.deckapp.core.ui.components.MarkdownText
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.deckapp.core.domain.usecase.RangeParser
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import com.deckapp.core.model.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TableReviewView(
    entries: List<TableEntry>,
    tableName: String,
    tableTags: List<Tag>,
    allTags: List<Tag>,
    tableFormula: String = "1d6",
    onEntryChange: (Int, TableEntry) -> Unit,
    onNameChange: (String) -> Unit,
    onToggleTag: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    onFormulaChange: (String) -> Unit = {},
    onConfirm: () -> Unit,
    onPrevious: () -> Unit = {},
    validationResult: RangeParser.ValidationResult?,
    lowConfidenceIndices: Set<Int>,
    confidenceThreshold: Float,
    onThresholdChange: (Float) -> Unit,
    tableProgress: String,
    isAiProcessing: Boolean = false,
    isVisionProcessing: Boolean = false,
    onAiOptimize: () -> Unit = {},
    onHealRanges: () -> Unit = {},
    onMergePrevious: (Int) -> Unit = {},
    onInsertAfter: (Int) -> Unit = {},
    onCleanNoise: () -> Unit = {},
    entryBlocks: Map<Int, List<OcrBlock>> = emptyMap(),
    isStitchingMode: Boolean = false,
    onContinueStitching: () -> Unit = {},
    allBundles: List<com.deckapp.core.model.TableBundle> = emptyList(),
    selectedBundleId: Long? = null,
    bundleNameDraft: String = "",
    onBundleSelect: (Long?) -> Unit = {},
    onBundleNameChange: (String) -> Unit = {},
    isPreviewMode: Boolean = false,
    onTogglePreview: () -> Unit = {},
    onShareQr: () -> Unit = {},
    // Overlay params
    croppedBitmap: Bitmap? = null,
    ocrBlocks: List<OcrBlock> = emptyList(),
    detectedAnchors: List<Float> = emptyList()
) {
    var showOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Anterior")
                }
                Text("Revisando tabla: $tableProgress", style = MaterialTheme.typography.labelMedium)
            }
            
            Row {
                IconButton(onClick = onTogglePreview) {
                    Icon(
                        if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                        if (isPreviewMode) "Modo Edición" else "Modo Previsualización"
                    )
                }

                IconButton(onClick = onShareQr) {
                    Icon(Icons.Default.QrCode, "Compartir vía QR")
                }
                
                IconButton(onClick = onCleanNoise) {
                    Icon(Icons.Default.Search, "Limpiar ruido OCR", tint = MaterialTheme.colorScheme.secondary)
                }

                if (isAiProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onAiOptimize) {
                        Icon(Icons.Default.AutoFixHigh, "Optimizar con Gemini IA", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (croppedBitmap != null) {
                    IconButton(onClick = { showOverlay = !showOverlay }) {
                        Icon(
                            if (showOverlay) Icons.AutoMirrored.Filled.List else Icons.Default.Image,
                            if (showOverlay) "Ver lista" else "Ver original"
                        )
                    }
                }
                
                if (isStitchingMode) {
                    IconButton(onClick = onContinueStitching) {
                        Icon(Icons.Default.AddPhotoAlternate, "Añadir otra página/sección", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                if (validationResult?.isValid == true) {
                    Icon(Icons.Default.Check, "Válido", tint = Color.Green, modifier = Modifier.padding(8.dp))
                } else {
                    IconButton(onClick = onHealRanges) {
                        Icon(Icons.Default.AutoFixHigh, "Reparar rangos", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { /* TODO: Scroll to error logic if needed, or just highlight */ }) {
                        Icon(Icons.Default.Warning, "Errores en rangos", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))

        if (showOverlay && croppedBitmap != null) {
            Text("Inspección visual de OCR", style = MaterialTheme.typography.titleSmall)
            Text("Rojo = Baja confianza, Verde = Alta confianza", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            TableOcrOverlay(
                bitmap = croppedBitmap,
                blocks = ocrBlocks,
                anchors = detectedAnchors,
                lowConfidenceIndices = lowConfidenceIndices,
                modifier = Modifier.weight(1f)
            )
        } else {
            // Slider de confianza
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Umbral de confianza: ${(confidenceThreshold * 100).toInt()}%", 
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f))
                    if (lowConfidenceIndices.isNotEmpty()) {
                        Text("${lowConfidenceIndices.size} avisos", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Slider(
                    value = confidenceThreshold,
                    onValueChange = onThresholdChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            
            if (isVisionProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text(
                    "Vision AI extrayendo entradas...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = tableName,
                onValueChange = onNameChange,
                label = { Text("Nombre de la tabla") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            var showBundleDialog by remember { mutableStateOf(false) }
            val selectedBundleName = allBundles.find { it.id == selectedBundleId }?.name ?: bundleNameDraft

            OutlinedTextField(
                value = selectedBundleName,
                onValueChange = onBundleNameChange,
                label = { Text("Paquete / Libro") },
                placeholder = { Text("Ej: Manual de Monstruos") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showBundleDialog = true }) {
                        Icon(Icons.Default.LibraryBooks, "Seleccionar paquete")
                    }
                }
            )

            if (showBundleDialog) {
                AlertDialog(
                    onDismissRequest = { showBundleDialog = false },
                    title = { Text("Seleccionar Paquete") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            item {
                                ListItem(
                                    headlineContent = { Text("Ninguno (Individual)") },
                                    modifier = Modifier.clickable {
                                        onBundleSelect(null)
                                        showBundleDialog = false
                                    },
                                    trailingContent = { if (selectedBundleId == null && bundleNameDraft.isBlank()) Icon(Icons.Default.Check, null) }
                                )
                            }
                            itemsIndexed(allBundles) { _, bundle ->
                                ListItem(
                                    headlineContent = { Text(bundle.name) },
                                    modifier = Modifier.clickable {
                                        onBundleSelect(bundle.id)
                                        showBundleDialog = false
                                    },
                                    trailingContent = { if (selectedBundleId == bundle.id) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showBundleDialog = false }) { Text("Cerrar") }
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tableFormula,
                    onValueChange = onFormulaChange,
                    label = { Text("Fórmula") },
                    placeholder = { Text("1d6") },
                    modifier = Modifier.width(100.dp)
                )
                
                // Selector de Etiquetas
                Column(modifier = Modifier.weight(1f)) {
                    Text("Etiquetas", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tableTags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { onToggleTag(tag) },
                                label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp)) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = Color(tag.color).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(tag.color)
                                )
                            )
                        }
                        
                        var showTagDialog by remember { mutableStateOf(false) }
                        var tagSearchQuery by remember { mutableStateOf("") }

                        IconButton(onClick = { showTagDialog = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Añadir etiqueta", modifier = Modifier.size(16.dp))
                        }

                        if (showTagDialog) {
                            AlertDialog(
                                onDismissRequest = { 
                                    showTagDialog = false
                                    tagSearchQuery = ""
                                },
                                title = { Text("Añadir Etiqueta") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(
                                            value = tagSearchQuery,
                                            onValueChange = { tagSearchQuery = it },
                                            placeholder = { Text("Buscar o crear etiqueta...") },
                                            leadingIcon = { Icon(Icons.Default.Search, null) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        val filteredTags = allTags.filter { 
                                            it.name.contains(tagSearchQuery, ignoreCase = true) 
                                        }

                                        if (filteredTags.isNotEmpty() || tagSearchQuery.isNotBlank()) {
                                            HorizontalDivider()
                                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                                if (tagSearchQuery.isNotBlank() && filteredTags.none { it.name.equals(tagSearchQuery, ignoreCase = true) }) {
                                                    item {
                                                        ListItem(
                                                            headlineContent = { Text("Crear \"$tagSearchQuery\"") },
                                                            leadingContent = { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) },
                                                            modifier = Modifier.clickable {
                                                                onCreateTag(tagSearchQuery)
                                                                showTagDialog = false
                                                                tagSearchQuery = ""
                                                            }
                                                        )
                                                    }
                                                }

                                                itemsIndexed(filteredTags) { _, tag ->
                                                    val isSelected = tag in tableTags
                                                    ListItem(
                                                        headlineContent = { Text(tag.name) },
                                                        modifier = Modifier.clickable {
                                                            onToggleTag(tag)
                                                            showTagDialog = false
                                                            tagSearchQuery = ""
                                                        },
                                                        trailingContent = {
                                                            if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { 
                                        showTagDialog = false
                                        tagSearchQuery = ""
                                    }) { Text("Cerrar") }
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.sortOrder }) { index, entry ->
                    Column(modifier = Modifier.fillMaxWidth().animateItem()) {
                        val blocks = entryBlocks[entry.sortOrder] ?: emptyList()
                        
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart || it == SwipeToDismissBoxValue.StartToEnd) {
                                    if (index > 0) {
                                        onMergePrevious(index)
                                        true
                                    } else false
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (index > 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                Box(Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) {
                                    if (index > 0) Text("Fusionar con anterior", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            content = {
                                EntryRow(
                                    entry = entry,
                                    isLowConfidence = index in lowConfidenceIndices,
                                    isPreviewMode = isPreviewMode,
                                    blocks = blocks,
                                    originalBitmap = croppedBitmap,
                                    onChange = { onEntryChange(index, it) }
                                )
                            },
                            enableDismissFromStartToEnd = index > 0,
                            enableDismissFromEndToStart = index > 0
                        )
                        
                        // Sequence Booster (+)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { onInsertAfter(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Insertar fila debajo",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = validationResult?.isValid == true || showOverlay
        ) {
            Text(if (showOverlay) "Volver a la lista" else "Continuar")
        }
    }
}

@Composable
private fun EntryRow(
    entry: TableEntry,
    isLowConfidence: Boolean,
    isPreviewMode: Boolean,
    blocks: List<OcrBlock> = emptyList(),
    originalBitmap: Bitmap? = null,
    onChange: (TableEntry) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Column {
        if (isFocused && originalBitmap != null && blocks.isNotEmpty()) {
            MagnifierOverlay(bitmap = originalBitmap, blocks = blocks)
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isLowConfidence) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isPreviewMode) {
                    Box(modifier = Modifier.width(130.dp)) {
                        Text(
                            text = if (entry.minRoll == entry.maxRoll) "${entry.minRoll}" else "${entry.minRoll}-${entry.maxRoll}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    MarkdownText(
                        markdown = entry.text,
                        modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (entry.subTableRef != null) {
                        val isLinked = entry.subTableId != null
                        Icon(
                            imageVector = if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = "Enlace a @${entry.subTableRef}",
                            tint = if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = "${entry.minRoll}",
                        onValueChange = { val v = it.toIntOrNull() ?: entry.minRoll; onChange(entry.copy(minRoll = v)) },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                    Text("-", modifier = Modifier.padding(horizontal = 4.dp))
                    OutlinedTextField(
                        value = "${entry.maxRoll}",
                        onValueChange = { val v = it.toIntOrNull() ?: entry.maxRoll; onChange(entry.copy(maxRoll = v)) },
                        modifier = Modifier.width(60.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = entry.text,
                        onValueChange = { onChange(entry.copy(text = it)) },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused },
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (entry.subTableRef != null) {
                                    val isLinked = entry.subTableId != null
                                    Icon(
                                        imageVector = if (isLinked) Icons.Default.Link else Icons.Default.LinkOff,
                                        contentDescription = "Enlace a @${entry.subTableRef}",
                                        tint = if (isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (isLowConfidence) {
                                    Icon(Icons.Default.Warning, "Baja confianza", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MagnifierOverlay(bitmap: Bitmap, blocks: List<OcrBlock>) {
    val combinedRect = remember(blocks) {
        var left = Float.MAX_VALUE
        var top = Float.MAX_VALUE
        var right = Float.MIN_VALUE
        var bottom = Float.MIN_VALUE
        blocks.forEach { b ->
            left = minOf(left, b.boundingBox.left)
            top = minOf(top, b.boundingBox.top)
            right = maxOf(right, b.boundingBox.right)
            bottom = maxOf(bottom, b.boundingBox.bottom)
        }
        // Expandir un poco el margen
        android.graphics.Rect(
            (left - 20f).toInt().coerceAtLeast(0),
            (top - 10f).toInt().coerceAtLeast(0),
            (right + 20f).toInt().coerceAtMost(bitmap.width),
            (bottom + 10f).toInt().coerceAtMost(bitmap.height)
        )
    }

    if (combinedRect.width() <= 0 || combinedRect.height() <= 0) return

    val crop = remember(combinedRect) {
        Bitmap.createBitmap(bitmap, combinedRect.left, combinedRect.top, combinedRect.width(), combinedRect.height())
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(bottom = 4.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        androidx.compose.foundation.Image(
            bitmap = crop.asImageBitmap(),
            contentDescription = "Original OCR Snippet",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    }
}
