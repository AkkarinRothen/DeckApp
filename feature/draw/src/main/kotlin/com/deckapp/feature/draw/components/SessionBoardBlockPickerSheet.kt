package com.deckapp.feature.draw.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.blocks.CreateClockDialog
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionBoardBlockPickerSheet(
    boardItems: List<SessionBoardItem>,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    linkedHexMapId: Long?,
    linkedMythicSession: MythicSession?,
    recentTableRolls: List<TableRollResult>,
    tensionClocks: List<TensionClock>,
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    allDecks: List<CardStack> = emptyList(),
    deckCardCounts: Map<Long, Int> = emptyMap(),
    allBoardItems: List<SessionBoardItem> = boardItems,
    boardGroups: List<SessionBoardGroup> = emptyList(),
    viewLabels: SessionBoardViewLabels = SessionBoardViewLabels(),
    onShowOnBoard: (SessionBoardItem) -> Unit = {},
    targetGroupId: Long? = null,
    targetGroups: List<SessionBoardGroup> = emptyList(),
    onTargetGroupChange: (Long?) -> Unit = {},
    onRemoveBoardItem: (Long) -> Unit = {},
    onAssignBoardItemsToGroup: (List<Long>, Long?) -> Unit = { _, _ -> },
    wikiEntries: List<WikiEntry> = emptyList(),
    onPick: (SessionBoardItemType, Long?) -> Unit,
    onPickMultipleResources: (List<Long>, List<Long>) -> Unit = { _, _ -> },
    onCreateGroupClicked: () -> Unit = {},
    onCreateSmartGroup: (name: String, autoFilterType: String) -> Unit = { _, _ -> },
    onCreateClock: (name: String, segments: Int, colorHex: String?, triggerDescription: String) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pickerPrefs = remember(context) {
        context.getSharedPreferences("board_block_picker", Context.MODE_PRIVATE)
    }
    var query by rememberSaveable { mutableStateOf(pickerPrefs.getString("query", "").orEmpty()) }
    var filter by rememberSaveable { mutableStateOf(BoardPickerFilter.entries.firstOrNull { it.name == pickerPrefs.getString("filter", null) } ?: BoardPickerFilter.ALL) }
    var showFullLibraryTables by rememberSaveable { mutableStateOf(pickerPrefs.getBoolean("library", true)) }
    var selectedTableCategory by rememberSaveable { mutableStateOf(pickerPrefs.getString("category", null)) }
    var selectedCollection by rememberSaveable { mutableStateOf(pickerPrefs.getString("collection", null)) }
    var quickFilter by rememberSaveable { mutableStateOf(BoardPickerQuickFilter.entries.firstOrNull { it.name == pickerPrefs.getString("quick", null) } ?: BoardPickerQuickFilter.ALL) }
    var sortMode by rememberSaveable { mutableStateOf(BoardPickerSort.entries.firstOrNull { it.name == pickerPrefs.getString("sort", null) } ?: BoardPickerSort.NAME) }
    var savedPresetsJson by rememberSaveable { mutableStateOf(pickerPrefs.getString("saved_presets", "[]").orEmpty()) }
    var customLabelsJson by rememberSaveable { mutableStateOf(pickerPrefs.getString("custom_labels", "{}").orEmpty()) }
    var visibleLimit by rememberSaveable { mutableIntStateOf(50) }
    var showCreateClockDialog by rememberSaveable { mutableStateOf(false) }
    val selectedTableIds = remember { mutableStateListOf<Long>() }
    val selectedDeckIds = remember { mutableStateListOf<Long>() }
    val selectedBoardItemIds = remember { mutableStateListOf<Long>() }
    var showCreateMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showCollectionMenu by remember { mutableStateOf(false) }
    var showPresetsMenu by remember { mutableStateOf(false) }
    var showManagePresetsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetName by rememberSaveable { mutableStateOf("") }
    var showBulkSelectMenu by remember { mutableStateOf(false) }
    var showTargetGroupMenu by remember { mutableStateOf(false) }
    var showMoveGroupMenu by remember { mutableStateOf(false) }
    var showRemoveBoardItemsDialog by remember { mutableStateOf(false) }
    var previewEntry by remember { mutableStateOf<BoardPickerEntry?>(null) }
    var editingLabelsEntry by remember { mutableStateOf<BoardPickerEntry?>(null) }
    var editingLabelsText by rememberSaveable { mutableStateOf("") }
    val savedPresets = remember(savedPresetsJson) { parseBoardPickerPresets(savedPresetsJson) }
    val customLabelsByKey = remember(customLabelsJson) { parseCustomPickerLabels(customLabelsJson) }
    LaunchedEffect(query, filter, showFullLibraryTables, selectedTableCategory, selectedCollection, quickFilter, sortMode) {
        pickerPrefs.edit()
            .putString("query", query)
            .putString("filter", filter.name)
            .putBoolean("library", showFullLibraryTables)
            .putString("category", selectedTableCategory)
            .putString("collection", selectedCollection)
            .putString("quick", quickFilter.name)
            .putString("sort", sortMode.name)
            .apply()
    }
    LaunchedEffect(query, filter, showFullLibraryTables, selectedTableCategory, selectedCollection, quickFilter, sortMode) {
        visibleLimit = 50
    }
    val favoriteKeys = remember(pickerPrefs) {
        mutableStateListOf<String>().apply { addAll(pickerPrefs.getStringSet("favorites", emptySet()).orEmpty()) }
    }
    val recentKeys = remember(pickerPrefs) {
        mutableStateListOf<String>().apply {
            addAll(pickerPrefs.getString("recent", "").orEmpty().split("|").filter { it.isNotBlank() })
        }
    }
    fun recordRecent(type: SessionBoardItemType, id: Long?) {
        val key = pickerKey(type, id)
        recentKeys.remove(key)
        recentKeys.add(0, key)
        while (recentKeys.size > 30) recentKeys.removeAt(recentKeys.lastIndex)
        pickerPrefs.edit().putString("recent", recentKeys.joinToString("|")).apply()
    }
    fun applyPreset(preset: BoardPickerPreset) {
        query = preset.query
        filter = preset.filter
        showFullLibraryTables = preset.showFullLibraryTables
        selectedTableCategory = preset.category
        selectedCollection = preset.collection
        quickFilter = preset.quickFilter
        sortMode = preset.sort
        showPresetsMenu = false
    }
    fun persistPresets(updated: List<BoardPickerPreset>) {
        savedPresetsJson = encodeBoardPickerPresets(updated)
        pickerPrefs.edit().putString("saved_presets", savedPresetsJson).apply()
    }
    fun saveCustomLabels(entry: BoardPickerEntry, values: Set<String>) {
        val updated = customLabelsByKey.toMutableMap()
        val key = pickerKey(entry.type, entry.resourceId)
        if (values.isEmpty()) updated.remove(key) else updated[key] = values
        customLabelsJson = encodeCustomPickerLabels(updated)
        pickerPrefs.edit().putString("custom_labels", customLabelsJson).apply()
    }
    fun toggleSelectedResources(entries: List<BoardPickerEntry>) {
        val allSelected = entries.isNotEmpty() && entries.all { entry ->
            when (entry.type) {
                SessionBoardItemType.TABLE -> entry.resourceId in selectedTableIds
                SessionBoardItemType.DECK -> entry.resourceId in selectedDeckIds
                else -> false
            }
        }
        entries.forEach { entry ->
            val id = entry.resourceId ?: return@forEach
            val selection = if (entry.type == SessionBoardItemType.TABLE) selectedTableIds else selectedDeckIds
            if (allSelected) selection.remove(id) else if (id !in selection) selection.add(id)
        }
    }

    if (showCreateClockDialog) {
        CreateClockDialog(
            onDismiss = { showCreateClockDialog = false },
            onConfirm = { name, segments, colorHex, triggerDescription ->
                showCreateClockDialog = false
                onCreateClock(name, segments, colorHex, triggerDescription)
                onDismiss()
            }
        )
    }

    val availableTableCategories: List<String> = remember(tablesInSession, allTables, showFullLibraryTables) {
        val baseTables = if (showFullLibraryTables) (tablesInSession + allTables).distinctBy { it.id } else tablesInSession
        baseTables.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val allEntries: List<BoardPickerEntry> = remember(allBoardItems, tablesInSession, allTables, showFullLibraryTables, linkedHexMapId, linkedMythicSession, tensionClocks, deckRefs, deckNames, allDecks, wikiEntries, customLabelsJson, deckCardCounts) {
        buildBoardPickerEntries(
            boardItems = allBoardItems,
            tablesInSession = tablesInSession,
            allTables = if (showFullLibraryTables) allTables else emptyList(),
            linkedHexMapId = linkedHexMapId,
            linkedMythicSession = linkedMythicSession,
            tensionClocks = tensionClocks,
            deckRefs = deckRefs,
            deckNames = deckNames,
            allDecks = allDecks,
            wikiEntries = wikiEntries,
            deckCardCounts = deckCardCounts
        ).map { entry ->
            val customLabels = customLabelsByKey[pickerKey(entry.type, entry.resourceId)].orEmpty()
            val allLabels = entry.collections + customLabels
            entry.copy(
                collections = allLabels,
                customLabels = customLabels,
                searchText = listOf(entry.title, entry.subtitle, entry.preview).plus(allLabels).joinToString(" ").lowercase()
            )
        }
    }

    val collections = remember(allEntries, filter) {
        allEntries.asSequence()
            .filter { filter == BoardPickerFilter.ALL || it.filter == filter }
            .flatMap { it.collections.asSequence() }
            .distinct()
            .sorted()
            .toList()
    }
    val recentTableKeys = remember(recentTableRolls) {
        recentTableRolls.map { pickerKey(SessionBoardItemType.TABLE, it.tableId) }.distinct()
    }
    val filteredEntries: List<BoardPickerEntry> = remember(
        allEntries, query, filter, selectedTableCategory, selectedCollection,
        quickFilter, favoriteKeys.toList(), recentKeys.toList(), recentTableKeys
    ) {
        val normalizedQuery = query.trim().lowercase()
        val recentOrder = (recentKeys + recentTableKeys).distinct().withIndex().associate { it.value to it.index }
        val favoriteSet = favoriteKeys.toSet()
        val matches = allEntries.filter { entry ->
            val matchesFilter = (filter == BoardPickerFilter.ALL || entry.filter == filter)
            val matchesCategory = selectedTableCategory == null ||
                (entry.type == SessionBoardItemType.TABLE && entry.subtitle.equals(selectedTableCategory, ignoreCase = true))
            val matchesCollection = selectedCollection == null || selectedCollection in entry.collections
            val key = pickerKey(entry.type, entry.resourceId)
            val matchesQuickFilter = when (quickFilter) {
                BoardPickerQuickFilter.ALL -> true
                BoardPickerQuickFilter.FAVORITES -> key in favoriteSet
                BoardPickerQuickFilter.RECENT -> key in recentOrder
            }
            val matchesQuery = normalizedQuery.isBlank() || entry.searchText.contains(normalizedQuery)
            matchesFilter && matchesCategory && matchesCollection && matchesQuickFilter && matchesQuery
        }
        when (sortMode) {
            BoardPickerSort.NAME -> matches.sortedBy { it.title.lowercase() }
            BoardPickerSort.RECENT -> matches.sortedWith(compareBy<BoardPickerEntry> { recentOrder[pickerKey(it.type, it.resourceId)] ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })
            BoardPickerSort.FAVORITES -> matches.sortedWith(compareBy<BoardPickerEntry> { if (pickerKey(it.type, it.resourceId) in favoriteSet) 0 else 1 }.thenBy { it.title.lowercase() })
        }
    }
    val typeCount = remember(allEntries, filter) {
        allEntries.count { filter == BoardPickerFilter.ALL || it.filter == filter }
    }
    val placedResourceKeys = remember(allBoardItems) {
        allBoardItems.map { pickerKey(it.type, it.resourceId) }.toSet()
    }
    val selectableFilteredEntries = filteredEntries.filter { entry ->
        entry.resourceId != null &&
            (entry.type == SessionBoardItemType.TABLE || entry.type == SessionBoardItemType.DECK) &&
            pickerKey(entry.type, entry.resourceId) !in placedResourceKeys
    }
    val visibleSelectableEntries = filteredEntries.take(visibleLimit).filter { entry ->
        entry.resourceId != null && (entry.type == SessionBoardItemType.TABLE || entry.type == SessionBoardItemType.DECK) &&
            pickerKey(entry.type, entry.resourceId) !in placedResourceKeys
    }
    val allVisibleResourcesSelected = visibleSelectableEntries.isNotEmpty() && visibleSelectableEntries.all { entry ->
        when (entry.type) {
            SessionBoardItemType.TABLE -> entry.resourceId in selectedTableIds
            SessionBoardItemType.DECK -> entry.resourceId in selectedDeckIds
            else -> false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).imePadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Agregar bloque", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Busca un recurso o explora por tipo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    TextButton(onClick = { showCreateMenu = true }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Text("Crear")
                    }
                    DropdownMenu(expanded = showCreateMenu, onDismissRequest = { showCreateMenu = false }) {
                        DropdownMenuItem(text = { Text("Nueva carpeta") },
                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                            onClick = { showCreateMenu = false; onDismiss(); onCreateGroupClicked() })
                        DropdownMenuItem(text = { Text("Nuevo reloj") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) },
                            onClick = { showCreateMenu = false; showCreateClockDialog = true })
                        HorizontalDivider()
                        listOf(
                            Triple("Carpeta de tablas", "Tablas", "TABLE"),
                            Triple("Carpeta de relojes", "Relojes", "CLOCK"),
                            Triple("Carpeta de notas", "Notas", "NOTES")
                        ).forEach { (label, name, type) ->
                            DropdownMenuItem(text = { Text(label) },
                                onClick = { showCreateMenu = false; onDismiss(); onCreateSmartGroup(name, type) })
                        }
                    }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotEmpty()) ({
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                    }
                }) else null,
                placeholder = { Text("Buscar tablas, mazos, notas...") }
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardPickerFilter.entries.forEach { chip ->
                    FilterChip(selected = filter == chip,
                        onClick = {
                            filter = chip
                            selectedTableCategory = null
                            selectedCollection = null
                        },
                        label = { Text(chip.label) })
                }
                Spacer(Modifier.width(4.dp))
                FilterChip(
                    selected = quickFilter == BoardPickerQuickFilter.FAVORITES,
                    onClick = {
                        quickFilter = if (quickFilter == BoardPickerQuickFilter.FAVORITES)
                            BoardPickerQuickFilter.ALL else BoardPickerQuickFilter.FAVORITES
                    },
                    label = { Text("Favoritos") },
                    leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = quickFilter == BoardPickerQuickFilter.RECENT,
                    onClick = {
                        quickFilter = if (quickFilter == BoardPickerQuickFilter.RECENT)
                            BoardPickerQuickFilter.ALL else BoardPickerQuickFilter.RECENT
                    },
                    label = { Text("Recientes") },
                    leadingIcon = { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)) }
                )
                Box {
                    TextButton(onClick = { showPresetsMenu = true }) {
                        Icon(Icons.Default.BookmarkBorder, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Guardados")
                    }
                    DropdownMenu(expanded = showPresetsMenu, onDismissRequest = { showPresetsMenu = false }) {
                        if (savedPresets.isEmpty()) {
                            DropdownMenuItem(text = { Text("Todavía no guardaste filtros") }, enabled = false, onClick = {})
                        } else {
                            savedPresets.forEach { preset ->
                                DropdownMenuItem(text = { Text(preset.name) }, onClick = { applyPreset(preset) })
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Administrar filtros") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { showPresetsMenu = false; showManagePresetsDialog = true }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Guardar filtros actuales") },
                            leadingIcon = { Icon(Icons.Default.BookmarkAdd, null) },
                            onClick = { newPresetName = ""; showPresetsMenu = false; showSavePresetDialog = true }
                        )
                    }
                }
            }
            if (filter == BoardPickerFilter.TABLES) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(selected = !showFullLibraryTables,
                        onClick = {
                            showFullLibraryTables = false
                            selectedTableCategory = null
                            selectedCollection = null
                        },
                        label = { Text("En sesión") })
                    FilterChip(selected = showFullLibraryTables,
                        onClick = {
                            showFullLibraryTables = true
                            selectedTableCategory = null
                            selectedCollection = null
                        },
                        label = { Text("Biblioteca") })
                    if (availableTableCategories.isNotEmpty()) {
                        Box {
                            TextButton(onClick = { showCategoryMenu = true }) {
                                Text(selectedTableCategory ?: "Categoría")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }) {
                                DropdownMenuItem(text = { Text("Todas las categorías") },
                                    onClick = { selectedTableCategory = null; showCategoryMenu = false })
                                availableTableCategories.forEach { category ->
                                    DropdownMenuItem(text = { Text(category) },
                                        onClick = { selectedTableCategory = category; showCategoryMenu = false })
                                }
                            }
                        }
                    }
                    if (collections.isNotEmpty()) {
                        Box {
                            TextButton(onClick = { showCollectionMenu = true }) {
                                Text("Colección: " + (selectedCollection ?: "Todas"))
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = showCollectionMenu,
                                onDismissRequest = { showCollectionMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Todas las colecciones") },
                                    onClick = { selectedCollection = null; showCollectionMenu = false }
                                )
                                collections.forEach { collection ->
                                    DropdownMenuItem(
                                        text = { Text(collection) },
                                        onClick = {
                                            selectedCollection = collection
                                            showCollectionMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (collections.isNotEmpty() && filter == BoardPickerFilter.DECKS) {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    TextButton(onClick = { showCollectionMenu = true }) {
                        Text((if (filter == BoardPickerFilter.TABLES) "Colección: " else "Etiqueta: ") +
                            (selectedCollection ?: "Todas"))
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = showCollectionMenu, onDismissRequest = { showCollectionMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = { selectedCollection = null; showCollectionMenu = false }
                        )
                        collections.forEach { collection ->
                            DropdownMenuItem(
                                text = { Text(collection) },
                                onClick = {
                                    selectedCollection = collection
                                    showCollectionMenu = false
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((if (query.isNotBlank()) "Resultados · " else "Disponibles · ") + filteredEntries.size + " de " + typeCount,
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Box {
                    IconButton(enabled = selectableFilteredEntries.isNotEmpty(), onClick = { showBulkSelectMenu = true }) {
                        Icon(imageVector = Icons.Default.SelectAll, contentDescription = "Opciones de selección múltiple")
                    }
                    DropdownMenu(expanded = showBulkSelectMenu, onDismissRequest = { showBulkSelectMenu = false }) {
                        DropdownMenuItem(
                            text = { Text((if (allVisibleResourcesSelected) "Desmarcar" else "Seleccionar") + " lote cargado (${visibleSelectableEntries.size})") },
                            enabled = visibleSelectableEntries.isNotEmpty(),
                            onClick = { toggleSelectedResources(visibleSelectableEntries); showBulkSelectMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text((if (selectableFilteredEntries.isNotEmpty() && selectableFilteredEntries.all { entry ->
                                if (entry.type == SessionBoardItemType.TABLE) entry.resourceId in selectedTableIds else entry.resourceId in selectedDeckIds
                            }) "Desmarcar" else "Seleccionar") + " todos los resultados (${selectableFilteredEntries.size})") },
                            enabled = selectableFilteredEntries.isNotEmpty(),
                            onClick = { toggleSelectedResources(selectableFilteredEntries); showBulkSelectMenu = false }
                        )
                    }
                }
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(sortMode.label)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        BoardPickerSort.entries.forEach { option -> DropdownMenuItem(
                            text = { Text(option.label) }, leadingIcon = { if (sortMode == option) Icon(Icons.Default.Check, null) },
                            onClick = { sortMode = option; showSortMenu = false }) }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredEntries.isEmpty()) {
                    item {
                        Surface(shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Text(
                                text = when {
                                    quickFilter == BoardPickerQuickFilter.FAVORITES ->
                                        "Aún no hay favoritos disponibles. Marca recursos con la estrella."
                                    quickFilter == BoardPickerQuickFilter.RECENT ->
                                        "Aún no hay recursos recientes disponibles."
                                    query.isBlank() -> "No hay recursos disponibles con estos filtros."
                                    else -> "No hay resultados para la búsqueda."
                                },
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredEntries.take(visibleLimit), key = { it.type.name + "_" + (it.resourceId ?: it.title) }) { entry ->
                        val matchingBoardItems = allBoardItems.filter { it.type == entry.type && it.resourceId == entry.resourceId }
                        val matchingBoardItem = matchingBoardItems.firstOrNull()
                        val currentViewItems = boardItems.filter { it.type == entry.type && it.resourceId == entry.resourceId }
                        val usage = matchingBoardItem?.let { item ->
                            val group = boardGroups.firstOrNull { it.id == item.groupId }?.name
                            "En mesa · ${viewLabels.labelFor(item.boardView)}" + (group?.let { " · $it" } ?: "") +
                                if (matchingBoardItems.size > 1) " · +${matchingBoardItems.size - 1} ubicaciones" else ""
                        }
                        PickerEntryRow(
                            entry = entry,
                            isSelected = entry.resourceId != null && when (entry.type) {
                                SessionBoardItemType.TABLE -> entry.resourceId in selectedTableIds
                                SessionBoardItemType.DECK -> entry.resourceId in selectedDeckIds
                                else -> false
                            },
                            isFavorite = pickerKey(entry.type, entry.resourceId) in favoriteKeys,
                            usageLabel = usage,
                            deckCardCount = entry.resourceId?.let { deckCardCounts[it] },
                            isBoardItemSelected = currentViewItems.any { it.id in selectedBoardItemIds },
                            canSelectBoardItem = currentViewItems.isNotEmpty(),
                            onShowUsage = { matchingBoardItem?.let(onShowOnBoard) },
                            onToggleBoardItemSelection = {
                                currentViewItems.forEach { item ->
                                    if (item.id in selectedBoardItemIds) selectedBoardItemIds.remove(item.id)
                                    else selectedBoardItemIds.add(item.id)
                                }
                            },
                            onPreview = { previewEntry = entry },
                            onEditLabels = {
                                editingLabelsEntry = entry
                                editingLabelsText = entry.customLabels.sorted().joinToString(", ")
                            },
                            onToggleFavorite = {
                                val key = pickerKey(entry.type, entry.resourceId)
                                if (key in favoriteKeys) favoriteKeys.remove(key) else favoriteKeys.add(key)
                                pickerPrefs.edit().putStringSet("favorites", favoriteKeys.toSet()).apply()
                            },
                            onPick = {
                                if (entry.type == SessionBoardItemType.TABLE && entry.resourceId != null) {
                                    selectedTableIds.toggle(entry.resourceId)
                                } else if (entry.type == SessionBoardItemType.DECK && entry.resourceId != null) {
                                    selectedDeckIds.toggle(entry.resourceId)
                                } else {
                                    recordRecent(entry.type, entry.resourceId)
                                    onPick(entry.type, entry.resourceId)
                                }
                            }
                        )
                    }
                    if (filteredEntries.size > visibleLimit) item {
                        OutlinedButton(onClick = { visibleLimit += 50 }, modifier = Modifier.fillMaxWidth()) {
                            Text("Cargar más (${filteredEntries.size - visibleLimit} restantes)")
                        }
                    }
                }
            }
            if (selectedTableIds.isNotEmpty() || selectedDeckIds.isNotEmpty()) {
                HorizontalDivider()
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        if (targetGroups.isNotEmpty()) {
                            Box {
                                TextButton(onClick = { showTargetGroupMenu = true }) {
                                    Icon(Icons.Default.Folder, null, Modifier.size(18.dp))
                                    Text("Añadir en: " + (targetGroups.firstOrNull { it.id == targetGroupId }?.name ?: "Sin carpeta"))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                                DropdownMenu(expanded = showTargetGroupMenu, onDismissRequest = { showTargetGroupMenu = false }) {
                                    DropdownMenuItem(text = { Text("Sin carpeta") }, onClick = { onTargetGroupChange(null); showTargetGroupMenu = false })
                                    targetGroups.forEach { group -> DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = { onTargetGroupChange(group.id); showTargetGroupMenu = false }
                                    ) }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                listOfNotNull(
                                    selectedTableIds.takeIf { it.isNotEmpty() }?.let { it.size.toString() + " tablas" },
                                    selectedDeckIds.takeIf { it.isNotEmpty() }?.let { it.size.toString() + " mazos" }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                selectedTableIds.forEach { pickerKey(SessionBoardItemType.TABLE, it).let { key -> if (key !in favoriteKeys) favoriteKeys.add(key) } }
                                selectedDeckIds.forEach { pickerKey(SessionBoardItemType.DECK, it).let { key -> if (key !in favoriteKeys) favoriteKeys.add(key) } }
                                pickerPrefs.edit().putStringSet("favorites", favoriteKeys.toSet()).apply()
                            }) { Icon(Icons.Default.Star, contentDescription = "Marcar selección como favorita") }
                            TextButton(onClick = {
                                selectedTableIds.clear()
                                selectedDeckIds.clear()
                            }) { Text("Limpiar") }
                            Button(onClick = {
                                selectedTableIds.forEach { recordRecent(SessionBoardItemType.TABLE, it) }
                                selectedDeckIds.forEach { recordRecent(SessionBoardItemType.DECK, it) }
                                onPickMultipleResources(selectedTableIds.toList(), selectedDeckIds.toList())
                                selectedTableIds.clear()
                                selectedDeckIds.clear()
                            }) { Text("Agregar " + (selectedTableIds.size + selectedDeckIds.size)) }
                        }
                    }
                }
            }
            val selectedCurrentBoardItems = boardItems.filter { it.id in selectedBoardItemIds }
            if (selectedCurrentBoardItems.isNotEmpty()) {
                HorizontalDivider()
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Text("${selectedCurrentBoardItems.size} bloques seleccionados", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box {
                                TextButton(onClick = { showMoveGroupMenu = true }) {
                                    Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                                    Text("Mover a carpeta")
                                }
                                DropdownMenu(expanded = showMoveGroupMenu, onDismissRequest = { showMoveGroupMenu = false }) {
                                    DropdownMenuItem(text = { Text("Sin carpeta") }, onClick = {
                                        onAssignBoardItemsToGroup(selectedCurrentBoardItems.map { it.id }, null)
                                        selectedBoardItemIds.clear(); showMoveGroupMenu = false
                                    })
                                    targetGroups.forEach { group -> DropdownMenuItem(text = { Text(group.name) }, onClick = {
                                        onAssignBoardItemsToGroup(selectedCurrentBoardItems.map { it.id }, group.id)
                                        selectedBoardItemIds.clear(); showMoveGroupMenu = false
                                    }) }
                                }
                            }
                            IconButton(onClick = {
                                selectedCurrentBoardItems.forEach { pickerKey(it.type, it.resourceId).let { key -> if (key !in favoriteKeys) favoriteKeys.add(key) } }
                                pickerPrefs.edit().putStringSet("favorites", favoriteKeys.toSet()).apply()
                            }) { Icon(Icons.Default.Star, contentDescription = "Marcar bloques seleccionados como favoritos") }
                            TextButton(onClick = { selectedBoardItemIds.clear() }) { Text("Limpiar") }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { showRemoveBoardItemsDialog = true }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp)); Text("Quitar")
                            }
                        }
                    }
                }
            }
            if (showSavePresetDialog) {
                AlertDialog(
                    onDismissRequest = { showSavePresetDialog = false },
                    title = { Text("Guardar filtros") },
                    text = { OutlinedTextField(value = newPresetName, onValueChange = { newPresetName = it }, label = { Text("Nombre") }, singleLine = true) },
                    confirmButton = { TextButton(enabled = newPresetName.isNotBlank(), onClick = {
                        val preset = BoardPickerPreset(newPresetName.trim(), query, filter, showFullLibraryTables, selectedTableCategory, selectedCollection, quickFilter, sortMode)
                        persistPresets((savedPresets.filterNot { it.name.equals(preset.name, true) } + preset).takeLast(20))
                        showSavePresetDialog = false
                    }) { Text("Guardar") } },
                    dismissButton = { TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancelar") } }
                )
            }
            if (showManagePresetsDialog) {
                AlertDialog(
                    onDismissRequest = { showManagePresetsDialog = false },
                    title = { Text("Filtros guardados") },
                    text = {
                        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                            savedPresets.forEach { preset ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(preset.name, Modifier.weight(1f))
                                    IconButton(onClick = { persistPresets(savedPresets.filterNot { it.name == preset.name }) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar ${preset.name}")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { showManagePresetsDialog = false }) { Text("Listo") } }
                )
            }
            if (editingLabelsEntry != null) {
                val entry = editingLabelsEntry!!
                AlertDialog(
                    onDismissRequest = { editingLabelsEntry = null },
                    title = { Text("Etiquetas de ${entry.title}") },
                    text = {
                        OutlinedTextField(
                            value = editingLabelsText,
                            onValueChange = { editingLabelsText = it },
                            label = { Text("Etiquetas separadas por coma") },
                            supportingText = { Text("Se guardan en este dispositivo y sirven para buscar y filtrar.") }
                        )
                    },
                    confirmButton = { TextButton(onClick = {
                        saveCustomLabels(entry, editingLabelsText.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.toSet())
                        editingLabelsEntry = null
                    }) { Text("Guardar") } },
                    dismissButton = { TextButton(onClick = { editingLabelsEntry = null }) { Text("Cancelar") } }
                )
            }
            if (previewEntry != null) {
                val entry = previewEntry!!
                val existingBoardItem = allBoardItems.firstOrNull { it.type == entry.type && it.resourceId == entry.resourceId }
                AlertDialog(
                    onDismissRequest = { previewEntry = null },
                    title = { Text(entry.title) },
                    text = {
                        Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (entry.subtitle.isNotBlank()) Text(entry.subtitle, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            if (entry.preview.isNotBlank()) Text(entry.preview)
                            entry.availableCardCount?.let { Text("$it cartas disponibles") }
                            if (entry.collections.isNotEmpty()) Text("Colecciones y etiquetas: ${entry.collections.sorted().joinToString(" · ")}")
                            if (entry.detailLines.isNotEmpty()) {
                                HorizontalDivider()
                                Text(if (entry.type == SessionBoardItemType.TABLE) "Muestra de resultados" else "Detalles", fontWeight = FontWeight.Bold)
                                entry.detailLines.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                                if (entry.type == SessionBoardItemType.TABLE && entry.detailLines.count { !it.startsWith("Tirada:") } == 8) Text("Se muestran hasta 8 resultados.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { previewEntry = null }) { Text("Cerrar") }
                    },
                    dismissButton = {
                        if (existingBoardItem != null) {
                            TextButton(onClick = {
                                previewEntry = null
                                onShowOnBoard(existingBoardItem)
                            }) { Text("Ver en mesa") }
                        } else {
                            TextButton(onClick = {
                                recordRecent(entry.type, entry.resourceId)
                                previewEntry = null
                                onPick(entry.type, entry.resourceId)
                            }) { Text("Añadir") }
                        }
                    }
                )
            }
            if (showRemoveBoardItemsDialog) {
                AlertDialog(
                    onDismissRequest = { showRemoveBoardItemsDialog = false },
                    title = { Text("Quitar bloques de la mesa") },
                    text = { Text("Se quitarán ${selectedBoardItemIds.size} bloques de esta vista. Los recursos originales seguirán en tu biblioteca.") },
                    confirmButton = { TextButton(onClick = {
                        selectedBoardItemIds.toList().forEach(onRemoveBoardItem)
                        selectedBoardItemIds.clear()
                        showRemoveBoardItemsDialog = false
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Quitar") } },
                    dismissButton = { TextButton(onClick = { showRemoveBoardItemsDialog = false }) { Text("Cancelar") } }
                )
            }
        }
    }
}

@Composable
private fun PickerEntryRow(
    entry: BoardPickerEntry,
    isSelected: Boolean,
    isFavorite: Boolean,
    usageLabel: String?,
    deckCardCount: Int?,
    isBoardItemSelected: Boolean,
    canSelectBoardItem: Boolean,
    onShowUsage: () -> Unit,
    onToggleBoardItemSelection: () -> Unit,
    onPreview: () -> Unit,
    onEditLabels: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = if (usageLabel != null) onShowUsage else onPick),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.padding(8.dp)) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = entry.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (entry.isInSession) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (usageLabel != null) "✓ En mesa" else "⭐ En sesión",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (entry.subtitle.isNotBlank()) {
                    Text(
                        text = entry.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (entry.preview.isNotBlank()) {
                    Text(
                        text = entry.preview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                deckCardCount?.let { Text("$it cartas disponibles", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                usageLabel?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary) }
                if (entry.customLabels.isNotEmpty()) {
                    Text("Etiquetas: ${entry.customLabels.sorted().joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            var showRowActions by remember(entry.type, entry.resourceId) { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showRowActions = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más acciones para ${entry.title}")
                }
                DropdownMenu(expanded = showRowActions, onDismissRequest = { showRowActions = false }) {
                    DropdownMenuItem(text = { Text("Vista previa") }, leadingIcon = { Icon(Icons.Default.Visibility, null) },
                        onClick = { showRowActions = false; onPreview() })
                    DropdownMenuItem(text = { Text("Editar etiquetas") }, leadingIcon = { Icon(Icons.Default.Label, null) },
                        onClick = { showRowActions = false; onEditLabels() })
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (usageLabel != null) {
                FilledTonalButton(onClick = onShowUsage, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Ver") }
                if (canSelectBoardItem) {
                    Checkbox(checked = isBoardItemSelected, onCheckedChange = { onToggleBoardItemSelection() }, modifier = Modifier.size(40.dp))
                }
            } else if (entry.type == SessionBoardItemType.TABLE || entry.type == SessionBoardItemType.DECK) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onPick() },
                    modifier = Modifier.size(40.dp)
                )
            } else {
                FilledTonalButton(onClick = onPick, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Text("Añadir")
                }
            }
        }
    }
}

private fun MutableList<Long>.toggle(id: Long) {
    if (contains(id)) remove(id) else add(id)
}

private fun pickerKey(type: SessionBoardItemType, id: Long?): String = type.name + ":" + (id ?: 0L)

private data class BoardPickerEntry(
    val type: SessionBoardItemType,
    val resourceId: Long?,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val preview: String,
    val filter: BoardPickerFilter,
    val isInSession: Boolean = false,
    val collections: Set<String> = emptySet(),
    val customLabels: Set<String> = emptySet(),
    val detailLines: List<String> = emptyList(),
    val availableCardCount: Int? = null,
    val searchText: String = listOf(title, subtitle, preview).plus(collections).joinToString(" ").lowercase()
)

private enum class BoardPickerSort(val label: String) { NAME("Nombre"), RECENT("Más recientes"), FAVORITES("Favoritos primero") }

private enum class BoardPickerQuickFilter(val label: String) {
    ALL("Disponibles"),
    FAVORITES("Favoritos"),
    RECENT("Recientes")
}

private enum class BoardPickerFilter(val label: String) {
    ALL("Todas"),
    TABLES("Tablas"),
    DECKS("Mazos"),
    NARRATIVE("Narrativa"),
    EXPLORATION("Exploración"),
    UTILITIES("Utilidades")
}

private data class BoardPickerPreset(
    val name: String,
    val query: String,
    val filter: BoardPickerFilter,
    val showFullLibraryTables: Boolean,
    val category: String?,
    val collection: String?,
    val quickFilter: BoardPickerQuickFilter,
    val sort: BoardPickerSort
)

private inline fun <reified T : Enum<T>> pickerEnum(value: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun parseBoardPickerPresets(json: String): List<BoardPickerPreset> = runCatching {
    val array = JSONArray(json)
    (0 until array.length()).mapNotNull { index ->
        val item = array.optJSONObject(index) ?: return@mapNotNull null
        val name = item.optString("name").trim()
        if (name.isBlank()) return@mapNotNull null
        BoardPickerPreset(
            name = name,
            query = item.optString("query"),
            filter = pickerEnum(item.optString("filter"), BoardPickerFilter.ALL),
            showFullLibraryTables = item.optBoolean("library", true),
            category = item.optString("category").takeIf { it.isNotBlank() && it != "null" },
            collection = item.optString("collection").takeIf { it.isNotBlank() && it != "null" },
            quickFilter = pickerEnum(item.optString("quick"), BoardPickerQuickFilter.ALL),
            sort = pickerEnum(item.optString("sort"), BoardPickerSort.NAME)
        )
    }
}.getOrDefault(emptyList())

private fun encodeBoardPickerPresets(presets: List<BoardPickerPreset>): String = JSONArray().apply {
    presets.forEach { preset ->
        put(JSONObject()
            .put("name", preset.name)
            .put("query", preset.query)
            .put("filter", preset.filter.name)
            .put("library", preset.showFullLibraryTables)
            .put("category", preset.category)
            .put("collection", preset.collection)
            .put("quick", preset.quickFilter.name)
            .put("sort", preset.sort.name))
    }
}.toString()

private fun parseCustomPickerLabels(json: String): Map<String, Set<String>> = runCatching {
    val data = JSONObject(json)
    buildMap {
        val keys = data.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val values = data.optJSONArray(key) ?: continue
            put(key, (0 until values.length()).mapNotNull { values.optString(it).trim().takeIf { label -> label.isNotBlank() } }.toSet())
        }
    }
}.getOrDefault(emptyMap())

private fun encodeCustomPickerLabels(labels: Map<String, Set<String>>): String = JSONObject().apply {
    labels.filterValues { it.isNotEmpty() }.forEach { (key, values) -> put(key, JSONArray(values.toList())) }
}.toString()

private fun buildBoardPickerEntries(
    boardItems: List<SessionBoardItem>,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    linkedHexMapId: Long?,
    linkedMythicSession: MythicSession?,
    tensionClocks: List<TensionClock>,
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    allDecks: List<CardStack>,
    wikiEntries: List<WikiEntry> = emptyList(),
    deckCardCounts: Map<Long, Int> = emptyMap()
): List<BoardPickerEntry> {
    val entries = mutableListOf<BoardPickerEntry>()
    val usedStaticTypes = boardItems.map { it.type }.toSet()
    val usedTableIds = boardItems.filter { it.type == SessionBoardItemType.TABLE }.mapNotNull { it.resourceId }.toSet()
    val usedClockIds = boardItems.filter { it.type == SessionBoardItemType.CLOCK }.mapNotNull { it.resourceId }.toSet()
    val usedDeckIds = boardItems.filter { it.type == SessionBoardItemType.DECK }.mapNotNull { it.resourceId }.toSet()
    val usedWikiMapIds = boardItems.filter { it.type == SessionBoardItemType.WIKI_MAP }.mapNotNull { it.resourceId }.toSet()

    val decksById = allDecks.associateBy { it.id }
    deckRefs.forEach { ref ->
        val deckName = deckNames[ref.stackId] ?: "Mazo"
        val deck = decksById[ref.stackId]
        entries += BoardPickerEntry(
            type = SessionBoardItemType.DECK,
            resourceId = ref.stackId,
            icon = Icons.Default.Style,
            title = deckName,
            subtitle = deck?.tags?.joinToString(" · ") { it.name }?.ifBlank { "Mazo de cartas" }
                ?: "Mazo de cartas",
            preview = deck?.description?.ifBlank { "Añade este mazo interactivo al tablero." }
                ?: "Añade este mazo interactivo al tablero.",
            filter = BoardPickerFilter.DECKS,
            isInSession = ref.stackId in usedDeckIds,
            collections = deck?.tags?.map { it.name }?.toSet().orEmpty(),
            detailLines = listOfNotNull(
                deck?.description?.takeIf { it.isNotBlank() },
                deckCardCounts[ref.stackId]?.let { "$it cartas disponibles" },
                deck?.tags?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "Etiqueta: ${it.name}" }
            ),
            availableCardCount = deckCardCounts[ref.stackId]
        )
    }

    val sessionTableIds = tablesInSession.map { it.id }.toSet()
    val candidateTables = (tablesInSession + allTables)
        .distinctBy { it.id }
        .sortedWith(compareByDescending<RandomTable> { it.id in sessionTableIds }.thenBy { it.name })

    candidateTables.forEach { table ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.TABLE,
            resourceId = table.id,
            icon = Icons.Default.TableChart,
            title = table.name,
            subtitle = table.category,
            preview = table.description.ifBlank { "Fórmula ${table.rollFormula}" },
            filter = BoardPickerFilter.TABLES,
            isInSession = table.id in sessionTableIds || table.id in usedTableIds,
            collections = listOfNotNull(table.bundleName, table.sourcePack)
                .filter { it.isNotBlank() }.toSet(),
            detailLines = listOf("Tirada: ${table.rollFormula}") + table.entries.sortedBy { it.sortOrder }.take(8).map { entry ->
                val range = if (entry.minRoll == entry.maxRoll) entry.minRoll.toString() else "${entry.minRoll}–${entry.maxRoll}"
                "$range · ${entry.text}"
            }
        )
    }

    // Wiki Entries as Maps
    wikiEntries.filter { !it.imagePath.isNullOrBlank() }.forEach { entry ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.WIKI_MAP,
            resourceId = entry.id,
            icon = Icons.Default.Place,
            title = entry.title,
            subtitle = "Imagen interactiva (Wiki)",
            preview = "Mostrar la imagen con anotaciones e identificación de partes.",
            filter = BoardPickerFilter.EXPLORATION,
            isInSession = entry.id in usedWikiMapIds
        )
    }

    val candidateClocks = tensionClocks.sortedBy { it.name }

    candidateClocks.forEach { clock ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.CLOCK,
            resourceId = clock.id,
            icon = Icons.Default.Schedule,
            title = clock.name,
            subtitle = if (clock.isCompleted) "Completado" else "Tensión (${clock.progress}/${clock.segments})",
            preview = clock.triggerDescription.ifBlank { "Toca para avanzar el reloj." },
            filter = BoardPickerFilter.UTILITIES,
            isInSession = clock.id in usedClockIds
        )
    }

    fun addStaticEntry(
        type: SessionBoardItemType,
        icon: ImageVector,
        title: String,
        subtitle: String,
        preview: String,
        filter: BoardPickerFilter,
        enabled: Boolean = true
    ) {
        if (enabled) {
            entries += BoardPickerEntry(type, null, icon, title, subtitle, preview, filter, isInSession = type in usedStaticTypes)
        }
    }

    addStaticEntry(
        type = SessionBoardItemType.NOTES,
        icon = Icons.Default.NoteAlt,
        title = "Notas de sesión",
        subtitle = "Narrativa",
        preview = "Cuaderno principal y notas rápidas en un mismo bloque.",
        filter = BoardPickerFilter.NARRATIVE
    )
    addStaticEntry(
        type = SessionBoardItemType.MYTHIC,
        icon = Icons.Default.AutoAwesome,
        title = "Panel Mythic",
        subtitle = linkedMythicSession?.name ?: "Narrativa",
        preview = linkedMythicSession?.let { "Caos ${it.chaosFactor} y escena ${it.sceneNumber}" }
            ?: "Acceso al centro Mythic de la sesión.",
        filter = BoardPickerFilter.NARRATIVE
    )
    addStaticEntry(
        type = SessionBoardItemType.MAP,
        icon = Icons.Default.Map,
        title = "Mapa",
        subtitle = if (linkedHexMapId != null) "Exploración activa" else "Exploración",
        preview = if (linkedHexMapId != null) "Ir al mapa conectado a esta sesión." else "Centro de exploración y hexcrawl.",
        filter = BoardPickerFilter.EXPLORATION
    )
    addStaticEntry(
        type = SessionBoardItemType.NPCS,
        icon = Icons.Default.Groups,
        title = "NPCs",
        subtitle = "Narrativa",
        preview = "Panel de personajes, voces y contexto.",
        filter = BoardPickerFilter.NARRATIVE
    )
    addStaticEntry(
        type = SessionBoardItemType.COMBAT,
        icon = Icons.Default.CrisisAlert,
        title = "Combate",
        subtitle = "Utilidad táctica",
        preview = "Acceso rápido al tracker y al resumen del encuentro.",
        filter = BoardPickerFilter.UTILITIES
    )
    addStaticEntry(
        type = SessionBoardItemType.REFERENCE,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        title = "Referencia",
        subtitle = "Consulta",
        preview = "Entrada rápida al contexto de reglas y referencia.",
        filter = BoardPickerFilter.UTILITIES
    )
    addStaticEntry(
        type = SessionBoardItemType.DICE,
        icon = Icons.Default.Casino,
        title = "Dados",
        subtitle = "Utilidad",
        preview = "Bloque rápido para tiradas y utilidades de mesa.",
        filter = BoardPickerFilter.UTILITIES
    )
    addStaticEntry(
        type = SessionBoardItemType.MICROSETTING,
        icon = Icons.Default.Public,
        title = "Microsetting de Mundo",
        subtitle = "The Story Engine: Deck of Worlds",
        preview = "Cluster interactivo para combinar Región, Hito, Nombre, Origen, Atributo y Acontecimiento.",
        filter = BoardPickerFilter.NARRATIVE
    )

    return entries
}
