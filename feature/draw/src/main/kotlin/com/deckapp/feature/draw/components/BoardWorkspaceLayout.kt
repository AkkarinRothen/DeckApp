package com.deckapp.feature.draw.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.deckapp.core.model.BoardSessionMemory
import com.deckapp.core.model.BoardViewMemory
import com.deckapp.core.model.BoardDistribution
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.core.model.SessionBoardView

@Composable
internal fun BoardWorkspaceLayout(
    sessionId: Long,
    view: SessionBoardView,
    rootItems: List<SessionBoardItem>,
    isEditing: Boolean,
    isFolderOpen: Boolean,
    itemTitle: (SessionBoardItem) -> String,
    workspaceMemory: BoardSessionMemory = BoardSessionMemory(),
    onLayoutChanged: (SessionBoardView, BoardViewMemory) -> Unit = { _, _ -> },
    workspaceActions: @Composable () -> Unit = {},
    soloSceneHeader: @Composable () -> Unit = {},
    renderBlock: @Composable (SessionBoardItem, Modifier) -> Unit,
    gridContent: @Composable (Int, List<SessionBoardItem>) -> Unit
) {
    val layout = workspaceMemory.views[view.name] ?: BoardViewMemory()
    val modeName = layout.distribution.name
    val selectedPrimaryId = layout.primaryBlockId
    var modeMenu by remember { mutableStateOf(false) }
    var primaryMenu by remember { mutableStateOf(false) }
    var openedToolId by rememberSaveable(sessionId, view) { mutableStateOf<Long?>(null) }
    val mode = BoardWorkspaceMode.valueOf(modeName)
    val active = !isEditing && !isFolderOpen
    val primary = if (active) boardWorkspacePrimary(rootItems, mode, selectedPrimaryId) else null
    val tools = if (active) boardWorkspaceTools(rootItems, mode, primary?.id) else emptyList()
    val toolIds = tools.map { it.id }.toSet()
    val mainItems = if (primary == null) rootItems else {
        listOf(primary.copy(size = SessionBoardItemSize.L)) +
            rootItems.filter { it.id != primary.id && it.id !in toolIds }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp && tools.isNotEmpty()
        val sceneHeaderMaxHeight = maxHeight * 0.4f
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (active) {
                // The controls scroll on narrow screens instead of squeezing their labels.
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Box {
                            OutlinedButton(onClick = { modeMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text("Distribución: ${mode.label}")
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                            DropdownMenu(expanded = modeMenu, onDismissRequest = { modeMenu = false }) {
                                BoardWorkspaceMode.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            onLayoutChanged(view, BoardViewMemory(BoardDistribution.valueOf(option.name)))
                                            openedToolId = null
                                            modeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item { workspaceActions() }
                    if (primary != null) {
                        item {
                            Box {
                                OutlinedButton(onClick = { primaryMenu = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Text("Protagonista: ${itemTitle(primary)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 220.dp))
                                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                                }
                                DropdownMenu(expanded = primaryMenu, onDismissRequest = { primaryMenu = false }) {
                                    rootItems.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(itemTitle(item)) },
                                            onClick = { onLayoutChanged(view, layout.copy(primaryBlockId = item.id)); primaryMenu = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (active && mode == BoardWorkspaceMode.SOLO) {
                // Leave room for writing in landscape and when the keyboard reduces the viewport.
                Box(Modifier.fillMaxWidth().heightIn(max = sceneHeaderMaxHeight).verticalScroll(rememberScrollState())) {
                    soloSceneHeader()
                }
            }
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                    gridContent(boardWorkspaceColumns(maxWidth.value.toInt()), mainItems)
                }
                if (wide) {
                    Surface(
                        modifier = Modifier.width(320.dp).fillMaxHeight(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Herramientas a mano", style = MaterialTheme.typography.titleSmall)
                            Text(
                                if (tools.any { it.isPinned }) "Bloques fijados" else "Sugeridas · fijá bloques para personalizar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 24.dp)
                            ) {
                                items(tools, key = { it.id }) { tool ->
                                    renderBlock(tool, Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
            if (!wide && tools.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("Herramientas a mano", style = MaterialTheme.typography.labelSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tools, key = { it.id }) { tool ->
                                AssistChip(
                                    onClick = { openedToolId = tool.id },
                                    label = { Text(itemTitle(tool), maxLines = 1) },
                                    leadingIcon = if (tool.isPinned) {
                                        { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(wide, active) {
            if (wide || !active) openedToolId = null
        }
        val openedTool = tools.firstOrNull { it.id == openedToolId }
        if (!wide && active && openedTool != null) {
            Dialog(onDismissRequest = { openedToolId = null }) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Herramienta", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            IconButton(onClick = { openedToolId = null }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar herramienta")
                            }
                        }
                        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
                            item { renderBlock(openedTool.copy(size = SessionBoardItemSize.L), Modifier.fillMaxWidth()) }
                        }
                    }
                }
            }
        }
    }
}
