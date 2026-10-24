package com.deckapp.feature.draw.components.playmat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.deckapp.core.model.PlaymatDeckPlacement
import com.deckapp.core.model.PlaymatElementGroup
import com.deckapp.core.model.SessionDeckRef

internal enum class DeckManagerWindowClass { COMPACT, MEDIUM, EXPANDED }

/** Responsive deck library: a bottom sheet on compact windows and a persistent side panel on wide ones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlaymatDeckManagerDialog(
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    deckCardCounts: Map<Long, Int>,
    placements: List<PlaymatDeckPlacement>,
    favoriteDeckIds: Set<Long>,
    groups: List<PlaymatElementGroup>,
    resources: List<PlaymatSearchResult>,
    windowClass: DeckManagerWindowClass,
    onSetVisible: (Long, Boolean) -> Unit,
    onSetFavorite: (Long, Boolean) -> Unit,
    onReorderDecks: (List<Long>) -> Unit,
    onFocusResource: (PlaymatSearchResult) -> Unit,
    onQuickAction: (PlaymatSearchResult) -> Unit,
    onCreateGroupForDeck: (String, Long) -> Unit,
    onAssignDeckToGroup: (Long, String?) -> Unit,
    onManageGroups: () -> Unit,
    onDismiss: () -> Unit
) {
    if (windowClass != DeckManagerWindowClass.COMPACT) {
        val panelWidth = if (windowClass == DeckManagerWindowClass.EXPANDED) 380.dp else 320.dp
        Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, top = 58.dp, bottom = 12.dp)
                    .width(panelWidth)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                DeckLibraryContent(
                    deckRefs, deckNames, deckCardCounts, placements, favoriteDeckIds, groups, resources,
                    onSetVisible, onSetFavorite, onReorderDecks, onFocusResource, onQuickAction, onCreateGroupForDeck, onAssignDeckToGroup,
                    onManageGroups, onDismiss, Modifier.fillMaxSize()
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            DeckLibraryContent(
                deckRefs, deckNames, deckCardCounts, placements, favoriteDeckIds, groups, resources,
                onSetVisible, onSetFavorite, onReorderDecks, onFocusResource, onQuickAction, onCreateGroupForDeck, onAssignDeckToGroup,
                onManageGroups, onDismiss,
                Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun DeckLibraryContent(
    deckRefs: List<SessionDeckRef>,
    deckNames: Map<Long, String>,
    deckCardCounts: Map<Long, Int>,
    placements: List<PlaymatDeckPlacement>,
    favoriteDeckIds: Set<Long>,
    groups: List<PlaymatElementGroup>,
    resources: List<PlaymatSearchResult>,
    onSetVisible: (Long, Boolean) -> Unit,
    onSetFavorite: (Long, Boolean) -> Unit,
    onReorderDecks: (List<Long>) -> Unit,
    onFocusResource: (PlaymatSearchResult) -> Unit,
    onQuickAction: (PlaymatSearchResult) -> Unit,
    onCreateGroupForDeck: (String, Long) -> Unit,
    onAssignDeckToGroup: (Long, String?) -> Unit,
    onManageGroups: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedGroupId by remember(groups) { mutableStateOf<String?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showResources by remember { mutableStateOf(false) }
    var selectedResourceKind by remember { mutableStateOf(PlaymatSearchKind.ALL) }
    var collapsedGroupIds by remember(groups.map { it.id }) { mutableStateOf(emptySet<String>()) }
    var deckOrder by remember(deckRefs, deckNames) {
        mutableStateOf(deckRefs.distinctBy { it.stackId }.sortedWith(
            compareBy<SessionDeckRef> { it.sortOrder }.thenBy { deckNames[it.stackId].orEmpty().lowercase() }
        ).map { it.stackId })
    }
    val latestDeckOrder = rememberUpdatedState(deckOrder)
    var draggingDeckId by remember { mutableStateOf<Long?>(null) }
    var dragRemainderPx by remember { mutableFloatStateOf(0f) }
    var groupMenuDeckId by remember { mutableStateOf<Long?>(null) }
    var createGroupDeckId by remember { mutableStateOf<Long?>(null) }
    var newGroupName by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val placementByDeck = remember(placements) { placements.associateBy { it.stackId } }
    val sortedDecks = remember(deckRefs, deckNames, deckOrder) {
        val refsById = deckRefs.distinctBy { it.stackId }.associateBy { it.stackId }
        val ordered = deckOrder.mapNotNull(refsById::get)
        (ordered + deckRefs.distinctBy { it.stackId }.filter { it.stackId !in deckOrder })
    }
    val groupIdsByDeck = remember(groups) {
        groups.flatMap { group -> group.memberIds.mapNotNull { memberId ->
            memberId.removePrefix("deck:").toLongOrNull()?.let { it to group.id }
        } }.groupBy({ it.first }, { it.second })
    }
    val filteredDecks = remember(sortedDecks, query, selectedGroupId, groupIdsByDeck, favoritesOnly, favoriteDeckIds) {
        sortedDecks.filter { ref ->
            val matchesText = query.isBlank() || deckNames[ref.stackId].orEmpty().contains(query.trim(), ignoreCase = true)
            val memberships = groupIdsByDeck[ref.stackId].orEmpty()
            val matchesFavorite = !favoritesOnly || ref.stackId in favoriteDeckIds
            val matchesGroup = when (selectedGroupId) {
                null -> true
                UNGROUPED_FILTER -> memberships.isEmpty()
                else -> selectedGroupId in memberships
            }
            matchesText && matchesGroup && matchesFavorite
        }
    }
    val deckSections = remember(filteredDecks, favoritesOnly, groups, groupIdsByDeck) {
        if (favoritesOnly) {
            listOf(DeckSection(FAVORITES_SECTION, "Favoritos", filteredDecks))
        } else {
            val grouped = groups.mapNotNull { group ->
                val members = filteredDecks.filter { group.id in groupIdsByDeck[it.stackId].orEmpty() }
                if (members.isEmpty()) null else DeckSection(group.id, group.name, members, group.isHidden)
            }
            val ungrouped = filteredDecks.filter { groupIdsByDeck[it.stackId].isNullOrEmpty() }
            grouped + if (ungrouped.isNotEmpty()) listOf(DeckSection(UNGROUPED_FILTER, "Sin grupo", ungrouped)) else emptyList()
        }
    }
    val visibleCount = sortedDecks.count { placementByDeck[it.stackId]?.isVisibleOnPlaymat ?: true }
    val activeFavoriteCount = sortedDecks.count { it.stackId in favoriteDeckIds }
    val visibleFilteredCount = filteredDecks.count { placementByDeck[it.stackId]?.isVisibleOnPlaymat ?: true }
    val canReorder = query.isBlank() && !favoritesOnly && filteredDecks.size > 1
    val filteredResources = remember(resources, query, selectedResourceKind) {
        filterPlaymatSearchResults(resources, query, selectedResourceKind)
    }

    if (createGroupDeckId != null) {
        AlertDialog(
            onDismissRequest = { createGroupDeckId = null },
            title = { Text("Crear grupo para mazo") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nombre del grupo") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val deckId = createGroupDeckId
                    if (deckId != null && newGroupName.isNotBlank()) {
                        onCreateGroupForDeck(newGroupName.trim(), deckId)
                        selectedGroupId = null
                        newGroupName = ""
                        createGroupDeckId = null
                    }
                }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { createGroupDeckId = null }) { Text("Cancelar") } }
        )
    }

    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mazos y recursos", style = MaterialTheme.typography.titleLarge)
                Text("$visibleCount de ${sortedDecks.size} mazos visibles en la mesa", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onManageGroups) {
                Icon(Icons.Default.FolderOpen, contentDescription = "Organizar grupos")
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar biblioteca")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Buscar") }
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showResources,
                onClick = { showResources = false },
                label = { Text("Mazos · ${sortedDecks.size}") },
                leadingIcon = { Icon(Icons.Default.Style, null) }
            )
            FilterChip(
                selected = showResources,
                onClick = { showResources = true },
                label = { Text("Piezas · ${resources.size}") },
                leadingIcon = { Icon(Icons.Default.Category, null) }
            )
        }
        if (showResources) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlaymatSearchKind.values().forEach { kind ->
                    FilterChip(
                        selected = selectedResourceKind == kind,
                        onClick = { selectedResourceKind = kind },
                        label = { Text(kind.label) }
                    )
                }
            }
        }
        if (!showResources) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selectedGroupId == null, onClick = { selectedGroupId = null }, label = { Text("Todos") })
            groups.forEach { group ->
                val count = group.memberIds.count { member -> member.startsWith("deck:") }
                FilterChip(
                    selected = selectedGroupId == group.id,
                    onClick = { selectedGroupId = if (selectedGroupId == group.id) null else group.id },
                    label = { Text("${group.name} · $count") },
                    leadingIcon = { Icon(if (group.isHidden) Icons.Default.VisibilityOff else Icons.Default.Folder, null, Modifier.size(16.dp)) }
                )
            }
            FilterChip(
                selected = selectedGroupId == UNGROUPED_FILTER,
                onClick = { selectedGroupId = if (selectedGroupId == UNGROUPED_FILTER) null else UNGROUPED_FILTER },
                label = { Text("Sin grupo") }
            )
            FilterChip(
                selected = favoritesOnly,
                onClick = { favoritesOnly = !favoritesOnly },
                label = { Text("Favoritos · $activeFavoriteCount") },
                leadingIcon = { Icon(if (favoritesOnly) Icons.Default.Star else Icons.Default.StarBorder, null, Modifier.size(16.dp)) }
            )
        }
        }
        if (!showResources && deckRefs.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No hay mazos vinculados a esta sesión.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (!showResources) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${filteredDecks.size} mazos", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { filteredDecks.forEach { onSetVisible(it.stackId, true) } }) { Text("Mostrar") }
                TextButton(onClick = { filteredDecks.forEach { onSetVisible(it.stackId, false) } }) { Text("Ocultar") }
            }
            if (!canReorder) {
                Text("Arrastra el asa para ordenar mazos dentro de cada grupo. Limpia la búsqueda para reordenar.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                deckSections.forEach { section ->
                    item(key = "deck-section:${section.id}") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().clickable {
                                collapsedGroupIds = if (section.id in collapsedGroupIds) collapsedGroupIds - section.id else collapsedGroupIds + section.id
                            }
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when {
                                        section.isHidden -> Icons.Default.VisibilityOff
                                        section.id == FAVORITES_SECTION -> Icons.Default.Star
                                        else -> Icons.Default.Folder
                                    },
                                    null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("${section.title} · ${section.decks.size}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                Icon(if (section.id in collapsedGroupIds) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (section.id in collapsedGroupIds) "Expandir ${section.title}" else "Plegar ${section.title}")
                            }
                        }
                    }
                    if (section.id !in collapsedGroupIds) items(section.decks, key = { it.stackId }) { ref ->
                    val visible = placementByDeck[ref.stackId]?.isVisibleOnPlaymat ?: true
                    val count = deckCardCounts[ref.stackId] ?: 0
                    val memberships = groupIdsByDeck[ref.stackId].orEmpty()
                    val groupLabel = memberships.firstOrNull()?.let { id -> groups.firstOrNull { it.id == id }?.name }
                        ?: if (memberships.isEmpty()) "Sin grupo" else "${memberships.size} grupos"
                    Surface(shape = RoundedCornerShape(14.dp), color = if (draggingDeckId == ref.stackId)
                        MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onSetVisible(ref.stackId, !visible) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DragIndicator,
                                contentDescription = if (canReorder) "Arrastrar para reordenar" else "Reordena desde Todos",
                                tint = if (canReorder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp).pointerInput(ref.stackId, section.id, canReorder) {
                                    if (canReorder) detectDragGesturesAfterLongPress(
                                        onDragStart = { draggingDeckId = ref.stackId; dragRemainderPx = 0f },
                                        onDragEnd = {
                                            draggingDeckId = null
                                            dragRemainderPx = 0f
                                            onReorderDecks(latestDeckOrder.value)
                                        },
                                        onDragCancel = { draggingDeckId = null; dragRemainderPx = 0f },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragRemainderPx += dragAmount.y
                                            val rowStep = with(density) { 76.dp.toPx() }
                                            val direction = if (dragRemainderPx > 0f) 1 else -1
                                            var workingOrder = latestDeckOrder.value
                                            while (kotlin.math.abs(dragRemainderPx) >= rowStep) {
                                                val sectionIds = section.decks.map { it.stackId }.toSet()
                                                val sectionOrder = workingOrder.filter { it in sectionIds }
                                                val from = sectionOrder.indexOf(ref.stackId)
                                                val to = (from + direction).coerceIn(0, sectionOrder.lastIndex)
                                                if (from != to) {
                                                    val movedSection = sectionOrder.toMutableList().also { order ->
                                                        val moved = order.removeAt(from)
                                                        order.add(to, moved)
                                                    }
                                                    val globalPositions = workingOrder.indices.filter { workingOrder[it] in sectionIds }
                                                    workingOrder = workingOrder.toMutableList().also { order ->
                                                        globalPositions.forEachIndexed { index, globalIndex ->
                                                            order[globalIndex] = movedSection[index]
                                                        }
                                                    }
                                                    deckOrder = workingOrder
                                                }
                                                dragRemainderPx -= direction * rowStep
                                            }
                                        }
                                    )
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Style, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(deckNames[ref.stackId] ?: "Mazo", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text("$groupLabel · $count cartas", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            IconButton(onClick = { onSetFavorite(ref.stackId, ref.stackId !in favoriteDeckIds) }) {
                                Icon(
                                    if (ref.stackId in favoriteDeckIds) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (ref.stackId in favoriteDeckIds) "Quitar de favoritos" else "Agregar a favoritos",
                                    tint = if (ref.stackId in favoriteDeckIds) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box {
                                IconButton(onClick = { groupMenuDeckId = ref.stackId }) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = "Mover a un grupo")
                                }
                                DropdownMenu(
                                    expanded = groupMenuDeckId == ref.stackId,
                                    onDismissRequest = { groupMenuDeckId = null }
                                ) {
                                    Text("Mover a grupo", Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    groups.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group.name) },
                                            leadingIcon = { Icon(Icons.Default.Folder, null) },
                                            trailingIcon = if (group.id in memberships) {{ Icon(Icons.Default.Check, null) }} else null,
                                            onClick = {
                                                onAssignDeckToGroup(ref.stackId, group.id)
                                                groupMenuDeckId = null
                                            }
                                        )
                                    }
                                    if (groups.isEmpty()) {
                                        DropdownMenuItem(text = { Text("Sin grupos todavía") }, onClick = { groupMenuDeckId = null }, enabled = false)
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Quitar de los grupos") },
                                        leadingIcon = { Icon(Icons.Default.FolderOff, null) },
                                        onClick = { onAssignDeckToGroup(ref.stackId, null); groupMenuDeckId = null }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Crear grupo con este mazo…") },
                                        leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                                        onClick = {
                                            groupMenuDeckId = null
                                            newGroupName = ""
                                            createGroupDeckId = ref.stackId
                                        }
                                    )
                                }
                            }
                            Icon(
                                if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (visible) "Visible en mesa" else "Oculto",
                                tint = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Checkbox(checked = visible, onCheckedChange = { onSetVisible(ref.stackId, it) })
                        }
                    }
                    }
                }
                if (filteredDecks.isEmpty()) item {
                    Text("No hay mazos en este grupo con ese nombre.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text("${filteredResources.size} piezas · toca para enfocar", style = MaterialTheme.typography.labelLarge)
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groups.forEach { group ->
                    val groupResources = filteredResources.filter { group.id in it.groupIds }
                    val totalGroupResources = resources.count { group.id in it.groupIds }
                    val groupSearchResult = resources.firstOrNull { it.id == "group:${group.id}" }
                    item(key = "group-header:${group.id}") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().clickable {
                                collapsedGroupIds = if (group.id in collapsedGroupIds) collapsedGroupIds - group.id else collapsedGroupIds + group.id
                            }
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (group.isHidden) Icons.Default.VisibilityOff else Icons.Default.Folder, null)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(
                                        (if (totalGroupResources == 0) "Vacío · 0 recursos" else "${groupResources.size}/$totalGroupResources recursos") + if (group.isHidden) " · oculto" else "",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (groupSearchResult != null) {
                                    IconButton(onClick = { onFocusResource(groupSearchResult) }) {
                                        Icon(Icons.Default.CenterFocusStrong, contentDescription = "Enfocar grupo")
                                    }
                                }
                                Icon(if (group.id in collapsedGroupIds) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (group.id in collapsedGroupIds) "Expandir grupo" else "Plegar grupo")
                            }
                        }
                    }
                    if (group.id !in collapsedGroupIds) {
                        items(groupResources, key = { "resource:${group.id}:${it.id}" }) { result ->
                            PlaymatResourceRow(result, onFocusResource, onQuickAction, Modifier.padding(start = 12.dp))
                        }
                    }
                }
                val ungroupedResources = filteredResources.filter { it.groupIds.isEmpty() }
                item(key = "ungrouped-header") {
                    Surface(
                        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().clickable {
                            collapsedGroupIds = if (UNGROUPED_FILTER in collapsedGroupIds) collapsedGroupIds - UNGROUPED_FILTER else collapsedGroupIds + UNGROUPED_FILTER
                        }
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sin grupo · ${ungroupedResources.size}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Icon(if (UNGROUPED_FILTER in collapsedGroupIds) Icons.Default.ExpandMore else Icons.Default.ExpandLess, null)
                        }
                    }
                }
                if (UNGROUPED_FILTER !in collapsedGroupIds) {
                    items(ungroupedResources, key = { "resource:ungrouped:${it.id}" }) { result ->
                        PlaymatResourceRow(result, onFocusResource, onQuickAction, Modifier.padding(start = 12.dp))
                    }
                }
                if (filteredResources.isEmpty()) item {
                    Text("No se encontraron piezas.", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private const val UNGROUPED_FILTER = "__ungrouped__"
private const val FAVORITES_SECTION = "__favorites__"

private data class DeckSection(
    val id: String,
    val title: String,
    val decks: List<SessionDeckRef>,
    val isHidden: Boolean = false
)

@Composable
private fun PlaymatResourceRow(
    result: PlaymatSearchResult,
    onFocusResource: (PlaymatSearchResult) -> Unit,
    onQuickAction: (PlaymatSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val hidden = result.hiddenGroupIds.isNotEmpty()
    val canFocus = result.position != null || result.focusGroupId != null
    Surface(
        shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(resourceIcon(result.kind), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(result.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Text(
                    "${result.kind.itemLabel} · ${result.context}" + if (hidden) " · En grupo oculto" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hidden) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            TextButton(onClick = { onFocusResource(result) }, enabled = canFocus) {
                Text(if (hidden) "Mostrar y enfocar" else "Enfocar", maxLines = 1)
            }
            if (!hidden && canFocus && result.kind in setOf(PlaymatSearchKind.CARD, PlaymatSearchKind.DECK, PlaymatSearchKind.DISCARD)) {
                IconButton(onClick = { onQuickAction(result) }) {
                    Icon(
                        when (result.kind) {
                            PlaymatSearchKind.CARD -> Icons.Default.OpenInNew
                            PlaymatSearchKind.DECK -> Icons.Default.AddCard
                            else -> Icons.Default.Cached
                        },
                        contentDescription = when (result.kind) {
                            PlaymatSearchKind.CARD -> "Abrir detalle"
                            PlaymatSearchKind.DECK -> "Robar carta"
                            else -> "Reintegrar descarte"
                        }
                    )
                }
            }
        }
    }
}

private fun resourceIcon(kind: PlaymatSearchKind) = when (kind) {
    PlaymatSearchKind.CARD -> Icons.Default.Style
    PlaymatSearchKind.DECK, PlaymatSearchKind.DISCARD -> Icons.Default.Layers
    PlaymatSearchKind.TOKEN -> Icons.Default.Person
    PlaymatSearchKind.CLOCK -> Icons.Default.HourglassTop
    PlaymatSearchKind.NOTE -> Icons.Default.StickyNote2
    PlaymatSearchKind.RESULT -> Icons.Default.TableChart
    PlaymatSearchKind.GROUP -> Icons.Default.Folder
    PlaymatSearchKind.CONNECTION -> Icons.Default.Share
    PlaymatSearchKind.ALL -> Icons.Default.Category
}
