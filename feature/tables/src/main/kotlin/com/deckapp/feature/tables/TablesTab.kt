package com.deckapp.feature.tables

import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckTextButton

import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.SelectionActionBar
import com.deckapp.core.ui.components.PaperCard
import com.deckapp.core.ui.components.paperBackground
import com.deckapp.core.model.TableDensity
import com.deckapp.core.ui.theme.LocalTableAppearance
import com.deckapp.feature.tables.library.TablePackDialog
import sh.calvin.reorderable.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TablesTab(
    sessionId: Long?,
    onCreateTable: (Long?) -> Unit,
    onEditTable: (Long) -> Unit,
    onImportTable: () -> Unit,
    viewModel: TablesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    var showExportDialog by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    
    val lazyColumnState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableState = sh.calvin.reorderable.rememberReorderableLazyListState(lazyColumnState) { from, to ->
        val newList = uiState.tables.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        viewModel.updateTableOrder(newList)
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
    }
    
    // Sincronizar ID de sesión con el ViewModel para filtrado reactivo
    LaunchedEffect(sessionId) {
        viewModel.setSessionId(sessionId)
    }

    // Auto-copiar al portapapeles cuando sale un resultado (Quick Roll o Detail)
    LaunchedEffect(uiState.lastResult) {
        uiState.lastResult?.let { result ->
            clipboard.setText(AnnotatedString(result.resolvedText))
        }
    }

    if (uiState.showPackDialog) {
        TablePackDialog(
            packs = uiState.availableTablePacks,
            onDismiss = { viewModel.setShowPackDialog(false) },
            onInstall = viewModel::installTablePack,
            onRemove = viewModel::removeTablePack
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val data = uiState.exportData
        if (data != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(data) }
                viewModel.clearExportData()
            } catch (_: Exception) { }
        }
    }

    // Al detectar datos de exportación nuevos, lanzamos el selector de archivos
    LaunchedEffect(uiState.exportData) {
        if (uiState.exportData != null && uiState.exportFilename != null) {
            exportLauncher.launch(uiState.exportFilename!!)
        }
    }

    LaunchedEffect(uiState.activeTable, sessionId) {
        if (uiState.activeTable != null && sessionId != null) {
            viewModel.loadRecentResults(sessionId)
        }
    }

    LaunchedEffect(sessionId) {
        viewModel.setSession(sessionId)
    }

    if (uiState.activeTable != null) {
        val activeTable = uiState.activeTable!!
        TableDetailSheet(
            table = activeTable,
            lastResult = uiState.lastResult?.takeIf { it.tableId == activeTable.id }
                ?: uiState.sessionRollLog.firstOrNull { it.tableId == activeTable.id },
            recentResults = uiState.recentResults,
            isRolling = uiState.isRolling,
            autoRollOnOpen = uiState.autoRollOnOpen,
            onRoll = { viewModel.rollTable(activeTable.id, sessionId) },
            onSelectEntry = if (sessionId != null) {
                { index: Int, die: Int? ->
                    viewModel.selectTableEntry(activeTable.id, index, sessionId, die)
                }
            } else null,
            onExport = { showExportDialog = true },
            onEdit = {
                viewModel.closeTable()
                onEditTable(activeTable.id)
            },
            onDismiss = { viewModel.closeTable() }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar Tabla") },
            text = { Text("Elige el formato de exportación:") },
            confirmButton = {
                DeckTextButton(onClick = {
                    viewModel.exportTable(ExportFormat.CSV)
                    showExportDialog = false 
                }) { Text("CSV (Excel/VTT)") }
            },
            dismissButton = {
                DeckTextButton(onClick = {
                    viewModel.exportTable(ExportFormat.MARKDOWN)
                    showExportDialog = false 
                }) { Text("Markdown (Notas)") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().paperBackground(tintColor = MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Buscar tablas…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(onClick = { viewModel.toggleViewMode() }) {
                    Icon(
                        if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.ViewModule,
                        contentDescription = "Cambiar vista"
                    )
                }

                IconButton(onClick = { viewModel.toggleReorderMode() }) {
                    Icon(
                        if (uiState.isReorderMode) Icons.Default.Check else Icons.Default.Sort,
                        contentDescription = "Ordenar tablas",
                        tint = if (uiState.isReorderMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (uiState.allTags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTagIds.isEmpty(),
                            onClick = { viewModel.clearFilters() },
                            label = { Text("Todos") }
                        )
                    }
                    items(uiState.allTags) { tag ->
                        val tagLabel = when (tag.name) {
                            "choir_of_flesh.json" -> "Choir of Flesh"
                            "story_engine_deck_of_worlds.json" -> "Deck of Worlds"
                            "story_engine_deck_of_worlds_expansions.json" -> "Deck of Worlds (Exp.)"
                            "new_story_engine.json" -> "New Story Engine"
                            "dnd_5e_2024.json" -> "D&D 5e"
                            "twilight_2000_4e.json" -> "Twilight 2000"
                            else -> tag.name.removeSuffix(".json").replace("_", " ")
                        }
                        FilterChip(
                            selected = tag.id in uiState.selectedTagIds,
                            onClick = { viewModel.toggleTagFilter(tag.id) },
                            label = { Text(tagLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(tag.color).copy(alpha = 0.2f),
                                selectedLabelColor = Color(tag.color)
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (sessionId != null && uiState.sessionRollLog.isNotEmpty()) {
                RollLogPanel(
                    log = uiState.sessionRollLog,
                    expanded = uiState.rollLogExpanded,
                    onToggleExpanded = { viewModel.toggleRollLogExpanded() },
                    onClear = { viewModel.clearSessionRollLog(sessionId) },
                    onTableLinkClick = { viewModel.openTableByName(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.groupedTables.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay tablas",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        DeckButton(onClick = { onCreateTable(sessionId) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear tabla")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = lazyColumnState,
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sección Destacados: Anclados y Recientes
                    if (uiState.pinnedTables.isNotEmpty() || uiState.recentTables.isNotEmpty()) {
                        item {
                            FeaturedTablesSections(
                                pinnedTables = uiState.pinnedTables,
                                recentTables = uiState.recentTables,
                                onTableClick = { table ->
                                    viewModel.openTable(table)
                                },
                                onQuickRoll = viewModel::openTableForRoll
                            )
                        }
                    }

                    // Lista principal agrupada
                    uiState.groupedTables.forEach { (bundle, tables) ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = bundle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        
                        items(tables, key = { it.id }) { table ->
                            val isSelected = table.id in uiState.selectedTableIds
                            ReorderableItem(reorderableState, key = table.id) { isDragging ->
                                val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                                TableListItem(
                                    table = table,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.selectedTableIds.isNotEmpty(),
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .draggableHandle(
                                            onDragStarted = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress) },
                                            enabled = uiState.isReorderMode
                                        ),
                                    elevation = elevation,
                                    onLongClick = { 
                                        if (uiState.selectedTableIds.isEmpty() && uiState.isSimplifiedMode) {
                                            viewModel.toggleTableSelection(table.id)
                                        } else {
                                            viewModel.toggleTableSelection(table.id)
                                        }
                                    },
                                    onClick = {
                                        if (uiState.selectedTableIds.isNotEmpty()) {
                                            viewModel.toggleTableSelection(table.id)
                                        } else {
                                            viewModel.openTable(table)
                                        }
                                    },
                                    onQuickRoll = { viewModel.openTableForRoll(table.id) },
                                    onTogglePin = { viewModel.togglePin(table) },
                                    quickRollResult = uiState.quickRollResults[table.id],
                                    onDismissResult = { viewModel.clearQuickRoll(table.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.selectedTableIds.isNotEmpty()) {
            SelectionActionBar(
                count = uiState.selectedTableIds.size,
                onClear = { viewModel.clearSelection() },
                onDelete = { viewModel.bulkDelete() },
                onTogglePin = { viewModel.bulkTogglePin(it) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        } else {
            var fabMenuExpanded by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = { fabMenuExpanded = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Acciones de tablas")
                }

                DropdownMenu(
                    expanded = fabMenuExpanded,
                    onDismissRequest = { fabMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nueva tabla") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            fabMenuExpanded = false
                            onCreateTable(sessionId)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Importar tabla") },
                        leadingIcon = { Icon(Icons.Default.LibraryAdd, contentDescription = null) },
                        onClick = {
                            fabMenuExpanded = false
                            onImportTable()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Packs de tablas") },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                        onClick = {
                            fabMenuExpanded = false
                            viewModel.setShowPackDialog(true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedTablesSections(
    pinnedTables: List<RandomTable>,
    recentTables: List<RandomTable>,
    onTableClick: (RandomTable) -> Unit,
    onQuickRoll: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        if (pinnedTables.isNotEmpty()) {
            Text(
                "Favoritos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pinnedTables) { table ->
                    FeaturedTableCard(table, onTableClick, onQuickRoll)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (recentTables.isNotEmpty()) {
            Text(
                "Utilizadas recientemente",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(recentTables) { table ->
                    FeaturedTableCard(table, onTableClick, onQuickRoll)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeaturedTableCard(
    table: RandomTable,
    onClick: (RandomTable) -> Unit,
    onQuickRoll: (Long) -> Unit
) {
    PaperCard(
        onClick = { onClick(table) },
        modifier = Modifier.width(170.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Casino,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    table.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    table.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { onQuickRoll(table.id) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Tirar",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RollLogPanel(
    log: List<TableRollResult>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onClear: () -> Unit,
    onTableLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PaperCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Historial de sesión · ${log.size} tiradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar historial", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onToggleExpanded, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (expanded) {
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(log, key = { it.id }) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = result.rollValue.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = result.tableName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.4f)
                            )
                            val annotatedText = remember(result.resolvedText) {
                                buildAnnotatedString {
                                    val text = result.resolvedText
                                    append(text)
                                    val regex = Regex("""@([\w\s\u00C0-\u024F]+)""")
                                    regex.findAll(text).forEach { match ->
                                        val start = match.range.first
                                        val end = match.range.last + 1
                                        addStyle(
                                            style = SpanStyle(
                                                color = Color(0xFF64B5F6),
                                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            start = start,
                                            end = end
                                        )
                                        addStringAnnotation(
                                            tag = "TABLE_LINK",
                                            annotation = match.groupValues[1].trim(),
                                            start = start,
                                            end = end
                                        )
                                    }
                                }
                            }

                            androidx.compose.foundation.text.ClickableText(
                                text = annotatedText,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                maxLines = 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(0.6f),
                                onClick = { offset ->
                                    annotatedText.getStringAnnotations(tag = "TABLE_LINK", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            onTableLinkClick(annotation.item)
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TableListItem(
    table: RandomTable,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    modifier: Modifier = Modifier,
    elevation: androidx.compose.ui.unit.Dp = 0.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickRoll: () -> Unit,
    onTogglePin: () -> Unit,
    quickRollResult: TableRollResult? = null,
    onDismissResult: () -> Unit = {}
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val accentColor = table.tags.firstOrNull()?.let { Color(it.color) }
        ?: MaterialTheme.colorScheme.primary
    val compact = LocalTableAppearance.current.density == TableDensity.COMPACT

    PaperCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onLongClick = onLongClick,
        containerColor = containerColor,
        elevation = elevation,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(accentColor, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                }
                .padding(horizontal = 16.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (quickRollResult != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .dragAndDropSource {
                                detectTapGestures(
                                    onLongPress = {
                                        startTransfer(
                                            DragAndDropTransferData(
                                                clipData = ClipData.newPlainText(
                                                    "table_result",
                                                    quickRollResult.resolvedText
                                                )
                                            )
                                        )
                                    }
                                )
                            }
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = quickRollResult.rollValue.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = quickRollResult.resolvedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        table.tags.take(3).forEach { tag ->
                            Surface(
                                color = Color(tag.color).copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = tag.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (table.tags.size > 3) {
                            Text(
                                text = "+${table.tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (table.tags.isNotEmpty()) Text("·", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${table.rollFormula} · ${table.entries.size} entradas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                if (quickRollResult != null) {
                    IconButton(onClick = onDismissResult, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (table.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = null,
                            tint = if (table.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = onQuickRoll) {
                    Icon(
                        if (quickRollResult != null) Icons.Default.Refresh else Icons.Default.Casino,
                        contentDescription = "Tirar ${table.name}",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onLongClick() }
                )
            }
        }
    }
}
