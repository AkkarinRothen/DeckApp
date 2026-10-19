package com.deckapp.feature.draw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.blocks.CreateClockDialog

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
    wikiEntries: List<WikiEntry> = emptyList(),
    onPick: (SessionBoardItemType, Long?) -> Unit,
    onPickMultipleTables: (List<Long>) -> Unit = {},
    onCreateGroupClicked: () -> Unit = {},
    onCreateSmartGroup: (name: String, autoFilterType: String) -> Unit = { _, _ -> },
    onCreateClock: (name: String, segments: Int, colorHex: String?, triggerDescription: String) -> Unit = { _, _, _, _ -> },
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(BoardPickerFilter.ALL) }
    var showFullLibraryTables by rememberSaveable { mutableStateOf(false) }
    var selectedTableCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateClockDialog by rememberSaveable { mutableStateOf(false) }
    val selectedTableIds = remember { mutableStateListOf<Long>() }

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
        val baseTables = if (showFullLibraryTables || tablesInSession.isEmpty()) (tablesInSession + allTables).distinctBy { it.id } else tablesInSession
        baseTables.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val allEntries: List<BoardPickerEntry> = remember(boardItems, tablesInSession, allTables, showFullLibraryTables, linkedHexMapId, linkedMythicSession, tensionClocks, deckRefs, wikiEntries) {
        val baseTables = if (showFullLibraryTables || tablesInSession.isEmpty()) (tablesInSession + allTables).distinctBy { it.id } else tablesInSession
        buildBoardPickerEntries(
            boardItems = boardItems,
            tablesInSession = baseTables,
            allTables = allTables,
            linkedHexMapId = linkedHexMapId,
            linkedMythicSession = linkedMythicSession,
            tensionClocks = tensionClocks,
            deckRefs = deckRefs,
            deckNames = deckNames,
            wikiEntries = wikiEntries
        )
    }

    val recentEntries: List<BoardPickerEntry> = remember(recentTableRolls, allEntries) {
        val recentTableIds: List<Long> = recentTableRolls.map { it.tableId }.distinct()
        recentTableIds.mapNotNull { tableId: Long ->
            allEntries.firstOrNull { it.type == SessionBoardItemType.TABLE && it.resourceId == tableId }
        }.take(3)
    }

    val filteredEntries: List<BoardPickerEntry> = remember(allEntries, query, filter, selectedTableCategory) {
        allEntries.filter { entry ->
            val matchesFilter = (filter == BoardPickerFilter.ALL || entry.filter == filter)
            val matchesCategory = selectedTableCategory == null || entry.type != SessionBoardItemType.TABLE || entry.subtitle.equals(selectedTableCategory, ignoreCase = true)
            val matchesQuery = (query.isBlank() ||
                entry.title.contains(query, ignoreCase = true) ||
                entry.subtitle.contains(query, ignoreCase = true) ||
                entry.preview.contains(query, ignoreCase = true))
            matchesFilter && matchesCategory && matchesQuery
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Agregar bloque", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = if (selectedTableIds.isEmpty()) {
                        "Busca por nombre o categoria para poblar tu mesa de sesion."
                    } else {
                        "${selectedTableIds.size} tablas listas para agregar de una sola vez."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Buscar tablas, notas, mapa o Mythic...") }
            )

            if (selectedTableIds.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${selectedTableIds.size} seleccionadas",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { selectedTableIds.clear() }) {
                        Text("Limpiar")
                    }
                    FilledTonalButton(onClick = {
                        onPickMultipleTables(selectedTableIds.toList())
                        selectedTableIds.clear()
                    }) {
                        Icon(Icons.Default.Add, null)
                        Text("Agregar tablas")
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardPickerFilter.entries.forEach { chip ->
                    AssistChip(
                        onClick = { filter = chip },
                        label = { Text(chip.label) },
                        leadingIcon = if (filter == chip) ({ Icon(Icons.Default.ViewModule, null) }) else null
                    )
                }
            }

            if (filter == BoardPickerFilter.ALL || filter == BoardPickerFilter.TABLES) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showFullLibraryTables,
                        onClick = { showFullLibraryTables = !showFullLibraryTables },
                        label = { Text(if (showFullLibraryTables) "Toda la biblioteca" else "Tablas de la sesiÃ³n") },
                        leadingIcon = {
                            Icon(
                                if (showFullLibraryTables) Icons.Default.AllInclusive else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                if (availableTableCategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTableCategory == null,
                            onClick = { selectedTableCategory = null },
                            label = { Text("Todas") }
                        )
                        availableTableCategories.forEach { category ->
                            FilterChip(
                                selected = selectedTableCategory == category,
                                onClick = {
                                    selectedTableCategory = if (selectedTableCategory == category) null else category
                                },
                                label = { Text(category) }
                            )
                        }
                    }
                }
            }

            // Discovery: Create Group option
            if (query.isBlank() && filter == BoardPickerFilter.ALL) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onCreateGroupClicked()
                        },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Default.Folder, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nueva carpeta", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Crea una seccion para organizar tus tablas y notas.", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Discovery: Nuevo Reloj de Progreso
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreateClockDialog = true },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nuevo Reloj de Progreso", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text("Crea un reloj de tensiÃ³n (4, 6 u 8 seg) para rastrear eventos.", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Crear Carpeta Inteligente",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Agrupa automÃ¡ticamente elementos sin tener que arrastrarlos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        AssistChip(
                            onClick = {
                                onDismiss()
                                onCreateSmartGroup("Tablas", "TABLE")
                            },
                            label = { Text("Tablas") },
                            leadingIcon = { Icon(Icons.Default.TableChart, null, modifier = Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = {
                                onDismiss()
                                onCreateSmartGroup("Relojes", "CLOCK")
                            },
                            label = { Text("Relojes") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp)) }
                        )
                        AssistChip(
                            onClick = {
                                onDismiss()
                                onCreateSmartGroup("Notas", "NOTES")
                            },
                            label = { Text("Notas") },
                            leadingIcon = { Icon(Icons.Default.NoteAlt, null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            if (query.isBlank() && recentEntries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recientes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    recentEntries.forEach { entry ->
                        PickerEntryRow(
                            entry = entry,
                            isSelected = entry.resourceId != null && entry.resourceId in selectedTableIds,
                            onPick = {
                                if (entry.type == SessionBoardItemType.TABLE && entry.resourceId != null) {
                                    selectedTableIds.toggle(entry.resourceId)
                                } else {
                                    onPick(entry.type, entry.resourceId)
                                }
                            }
                        )
                    }
                }
            }

            if (filteredEntries.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Text(
                        text = "No hay resultados para \"$query\" con ese filtro.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries, key = { "${it.type}_${it.resourceId ?: it.title}" }) { entry ->
                        PickerEntryRow(
                            entry = entry,
                            isSelected = entry.resourceId != null && entry.resourceId in selectedTableIds,
                            onPick = {
                                if (entry.type == SessionBoardItemType.TABLE && entry.resourceId != null) {
                                    selectedTableIds.toggle(entry.resourceId)
                                } else {
                                    onPick(entry.type, entry.resourceId)
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PickerEntryRow(
    entry: BoardPickerEntry,
    isSelected: Boolean,
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
            .clickable(onClick = onPick),
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
                Text(
                    text = entry.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun MutableList<Long>.toggle(id: Long) {
    if (contains(id)) remove(id) else add(id)
}

private data class BoardPickerEntry(
    val type: SessionBoardItemType,
    val resourceId: Long?,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val preview: String,
    val filter: BoardPickerFilter
)

private enum class BoardPickerFilter(val label: String) {
    ALL("Todas"),
    TABLES("Tablas"),
    NARRATIVE("Narrativa"),
    EXPLORATION("Exploracion"),
    UTILITIES("Utilidades")
}

private fun buildBoardPickerEntries(
    boardItems: List<SessionBoardItem>,
    tablesInSession: List<RandomTable>,
    allTables: List<RandomTable>,
    linkedHexMapId: Long?,
    linkedMythicSession: MythicSession?,
    tensionClocks: List<TensionClock>,
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    wikiEntries: List<WikiEntry> = emptyList()
): List<BoardPickerEntry> {
    val entries = mutableListOf<BoardPickerEntry>()
    val usedStaticTypes = boardItems.map { it.type }.toSet()
    val usedTableIds = boardItems.filter { it.type == SessionBoardItemType.TABLE }.mapNotNull { it.resourceId }.toSet()
    val usedClockIds = boardItems.filter { it.type == SessionBoardItemType.CLOCK }.mapNotNull { it.resourceId }.toSet()
    val usedDeckIds = boardItems.filter { it.type == SessionBoardItemType.DECK }.mapNotNull { it.resourceId }.toSet()
    val usedWikiMapIds = boardItems.filter { it.type == SessionBoardItemType.WIKI_MAP }.mapNotNull { it.resourceId }.toSet()

    deckRefs.filter { it.stackId !in usedDeckIds }.forEach { ref ->
        val deckName = deckNames[ref.stackId] ?: "Mazo"
        entries += BoardPickerEntry(
            type = SessionBoardItemType.DECK,
            resourceId = ref.stackId,
            icon = Icons.Default.Style,
            title = deckName,
            subtitle = "Mazo de cartas",
            preview = "Toca para aÃ±adir este mazo interactivo al tablero.",
            filter = BoardPickerFilter.UTILITIES
        )
    }

    val candidateTables = (tablesInSession + allTables)
        .distinctBy { it.id }
        .filter { it.id !in usedTableIds }
        .sortedBy { it.name }

    candidateTables.forEach { table ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.TABLE,
            resourceId = table.id,
            icon = Icons.Default.TableChart,
            title = table.name,
            subtitle = table.category,
            preview = table.description.ifBlank { "Formula ${table.rollFormula}" },
            filter = BoardPickerFilter.TABLES
        )
    }

    // Wiki Entries as Maps
    wikiEntries.filter { it.id !in usedWikiMapIds && !it.imagePath.isNullOrBlank() }.forEach { entry ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.WIKI_MAP,
            resourceId = entry.id,
            icon = Icons.Default.Place,
            title = entry.title,
            subtitle = "Imagen interactiva (Wiki)",
            preview = "Mostrar la imagen con anotaciones e identificaciÃ³n de partes.",
            filter = BoardPickerFilter.EXPLORATION
        )
    }

    val candidateClocks = tensionClocks
        .filter { it.id !in usedClockIds }
        .sortedBy { it.name }

    candidateClocks.forEach { clock ->
        entries += BoardPickerEntry(
            type = SessionBoardItemType.CLOCK,
            resourceId = clock.id,
            icon = Icons.Default.Schedule,
            title = clock.name,
            subtitle = if (clock.isCompleted) "Completado" else "TensiÃ³n (${clock.progress}/${clock.segments})",
            preview = clock.triggerDescription.ifBlank { "Toca para avanzar el reloj." },
            filter = BoardPickerFilter.UTILITIES
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
        if (!usedStaticTypes.contains(type) && enabled) {
            entries += BoardPickerEntry(type, null, icon, title, subtitle, preview, filter)
        }
    }

    addStaticEntry(
        type = SessionBoardItemType.NOTES,
        icon = Icons.Default.NoteAlt,
        title = "Notas de sesion",
        subtitle = "Narrativa",
        preview = "Cuaderno principal y notas rapidas en un mismo bloque.",
        filter = BoardPickerFilter.NARRATIVE
    )
    addStaticEntry(
        type = SessionBoardItemType.MYTHIC,
        icon = Icons.Default.AutoAwesome,
        title = "Panel Mythic",
        subtitle = linkedMythicSession?.name ?: "Narrativa",
        preview = linkedMythicSession?.let { "Caos ${it.chaosFactor} y escena ${it.sceneNumber}" }
            ?: "Acceso al centro Mythic de la sesion.",
        filter = BoardPickerFilter.NARRATIVE
    )
    addStaticEntry(
        type = SessionBoardItemType.MAP,
        icon = Icons.Default.Map,
        title = "Mapa",
        subtitle = if (linkedHexMapId != null) "Exploracion activa" else "Exploracion",
        preview = if (linkedHexMapId != null) "Ir al mapa conectado a esta sesion." else "Centro de exploracion y hexcrawl.",
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
        subtitle = "Utilidad tactica",
        preview = "Acceso rapido al tracker y al resumen del encuentro.",
        filter = BoardPickerFilter.UTILITIES
    )
    addStaticEntry(
        type = SessionBoardItemType.REFERENCE,
        icon = Icons.AutoMirrored.Filled.MenuBook,
        title = "Referencia",
        subtitle = "Consulta",
        preview = "Entrada rapida al contexto de reglas y referencia.",
        filter = BoardPickerFilter.UTILITIES
    )
    addStaticEntry(
        type = SessionBoardItemType.DICE,
        icon = Icons.Default.Casino,
        title = "Dados",
        subtitle = "Utilidad",
        preview = "Bloque rapido para tiradas y utilidades de mesa.",
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