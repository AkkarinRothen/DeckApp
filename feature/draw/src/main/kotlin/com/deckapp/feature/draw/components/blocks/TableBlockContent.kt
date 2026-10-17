package com.deckapp.feature.draw.components.blocks

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.model.RandomTable
import com.deckapp.core.ui.components.SmartText
import com.deckapp.feature.draw.components.BoardBlockMeta
import kotlinx.coroutines.delay

@Composable
fun TableBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    latestTableRoll: TableRollResult?,
    recentRolls: List<TableRollResult> = emptyList(),
    tableFormula: String? = null,
    compactInFolder: Boolean,
    npcNames: List<String>,
    tableNames: List<String>,
    hasActiveEncounter: Boolean = false,
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String) -> Unit,
    onRollTable: (Long) -> Unit,
    onSendToNotes: ((tableName: String, resultText: String) -> Unit)? = null,
    onRecordCombatNote: ((String) -> Unit)? = null,
    randomTable: RandomTable? = null,
    pinnedTableRoll: TableRollResult? = null,
    onPinTableRoll: ((TableRollResult) -> Unit)? = null,
    onUnpinTableRoll: (() -> Unit)? = null,
    onOpenTable: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val compact = item.size == SessionBoardItemSize.S ||
        (compactInFolder && item.size != SessionBoardItemSize.L)
    val large = item.size == SessionBoardItemSize.L
    var historyExpanded by rememberSaveable(item.id, item.resourceId, item.size) { mutableStateOf(large) }
    // Keep the opened result as a snapshot, even if another block rolls this table.
    var inspectedResult by remember(item.id, item.resourceId) { mutableStateOf<TableRollResult?>(null) }
    var showEntries by rememberSaveable(item.id, item.resourceId) { mutableStateOf(false) }
    val displayedResult = pinnedTableRoll ?: latestTableRoll
    val previousRolls = recentRolls.filterNot { it == displayedResult }.take(if (large) 5 else 2)
    val resultActions: @Composable (TableRollResult) -> Unit = { result ->
        val isPinned = result == pinnedTableRoll
        TableResultActions(
            result = result,
            hasActiveEncounter = hasActiveEncounter,
            onSendToNotes = onSendToNotes,
            onRecordCombatNote = onRecordCombatNote,
            isPinned = isPinned,
            onTogglePin = if (isPinned) onUnpinTableRoll
                else onPinTableRoll?.let { pin -> { pin(result) } }
        )
    }

    if (item.resourceId == null) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text(
                "Sin tabla vinculada",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Asigna una tabla a este bloque para tirar directamente desde la mesa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (onOpenTable != null) {
                FilledTonalButton(
                    onClick = onOpenTable,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Elegir tabla")
                }
            }
        }
        return
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (!tableFormula.isNullOrBlank()) {
                    Text(
                        text = tableFormula,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = when {
                        pinnedTableRoll != null -> "Resultado fijado"
                        displayedResult == null -> "Lista para tirar"
                        else -> "Último resultado"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            displayedResult?.let { resultActions(it) }
        }

        TableResultPreview(
            result = displayedResult,
            compact = compact,
            large = large,
            npcNames = npcNames,
            tableNames = tableNames,
            onTagClicked = onTagClicked,
            onDiceClicked = onDiceClicked
        )
        if (displayedResult != null || compact) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                displayedResult?.let { result ->
                    TextButton(
                        onClick = { inspectedResult = result },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("Ver completo") }
                }
                Spacer(Modifier.weight(1f))
                if (compact) {
                    FilledTonalIconButton(
                        onClick = {
                            item.resourceId?.let { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onRollTable(id)
                            }
                        },
                        enabled = item.resourceId != null,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = "Tirar ${meta.title}")
                    }
                }
            }
        }

        if (pinnedTableRoll != null && onUnpinTableRoll != null) {
            TextButton(onClick = onUnpinTableRoll, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Volver a última tirada")
            }
        }

        if (pinnedTableRoll != null && latestTableRoll != null && pinnedTableRoll != latestTableRoll) {
            TextButton(
                onClick = { inspectedResult = latestTableRoll },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Ver nueva tirada · ${latestTableRoll.rollValue}") }
        }

        if (!compact && previousRolls.isNotEmpty()) {
            TextButton(
                onClick = { historyExpanded = !historyExpanded },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Text("Tiradas recientes (${previousRolls.size})", modifier = Modifier.weight(1f))
                Icon(
                    if (historyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (historyExpanded) "Ocultar historial" else "Mostrar historial"
                )
            }
            if (historyExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = if (large) 240.dp else 120.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    previousRolls.forEach { result ->
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth().clickable(
                                onClickLabel = "Ver resultado anterior completo",
                                onClick = { inspectedResult = result }
                            )
                        ) {
                            Row(
                                modifier = Modifier.heightIn(min = 48.dp).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RollValueBadge(result.rollValue)
                                Text(
                                    result.resolvedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!compact) {
            FilledTonalButton(
                onClick = {
                    item.resourceId?.let { id ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRollTable(id)
                    }
                },
                enabled = item.resourceId != null,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (latestTableRoll == null) "Tirar tabla" else "Tirar de nuevo")
            }
            if (large && randomTable != null) {
                TextButton(onClick = { showEntries = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("Consultar entradas de la tabla")
                }
            }
        }
    }

    if (showEntries && randomTable != null) {
        TableEntriesDialog(
            table = randomTable,
            onDismissRequest = { showEntries = false },
            npcNames = npcNames,
            tableNames = tableNames,
            onTagClicked = onTagClicked,
            onDiceClicked = onDiceClicked
        )
    }

    inspectedResult?.let { result ->
        AlertDialog(
            onDismissRequest = { inspectedResult = null },
            title = { Text(result.tableName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RollValueBadge(result.rollValue)
                        Spacer(Modifier.weight(1f))
                        resultActions(result)
                    }
                    // A distinct scroll position for each inspected roll.
                    key(result) {
                        SmartText(
                            text = result.resolvedText,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
                                .verticalScroll(rememberScrollState()),
                            style = MaterialTheme.typography.bodyLarge,
                            npcNames = npcNames,
                            tableNames = tableNames,
                            onTagClicked = onTagClicked,
                            onDiceClicked = onDiceClicked
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { inspectedResult = null }) { Text("Cerrar") } }
        )
    }
}

@Composable
private fun TableResultPreview(
    result: TableRollResult?,
    compact: Boolean,
    large: Boolean,
    npcNames: List<String>,
    tableNames: List<String>,
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String) -> Unit
) {
    var previousResult by remember { mutableStateOf(result) }
    var highlighted by remember { mutableStateOf(false) }
    LaunchedEffect(result) {
        if (result != previousResult) {
            previousResult = result
            highlighted = result != null
            delay(650)
        }
        highlighted = false
    }
    val background by animateColorAsState(
        targetValue = when {
            highlighted -> MaterialTheme.colorScheme.primaryContainer
            result == null -> MaterialTheme.colorScheme.surfaceContainerLow
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        },
        label = "tableResultBackground"
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        // Fixed reading area prevents long results from pushing neighbouring blocks down.
        Crossfade(
            targetState = result,
            modifier = Modifier.fillMaxWidth().height(if (compact) 88.dp else if (large) 200.dp else 120.dp),
            label = "tableResult"
        ) { current ->
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (current != null) {
                    RollValueBadge(current.rollValue)
                    key(current) {
                        SmartText(
                            text = current.resolvedText,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                                .then(if (large) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                            maxLines = if (large) Int.MAX_VALUE else if (compact) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                            npcNames = npcNames,
                            tableNames = tableNames,
                            onTagClicked = onTagClicked,
                            onDiceClicked = onDiceClicked
                        )
                    }
                } else {
                    Text(
                        "Tirá para descubrir el primer resultado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RollValueBadge(value: Int) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TableResultActions(
    result: TableRollResult,
    hasActiveEncounter: Boolean,
    onSendToNotes: ((String, String) -> Unit)?,
    onRecordCombatNote: ((String) -> Unit)?,
    isPinned: Boolean = false,
    onTogglePin: (() -> Unit)? = null
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var menuExpanded by remember(result) { mutableStateOf(false) }
    var copied by remember(result) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    IconButton(
        onClick = {
            clipboard.setText(AnnotatedString(result.resolvedText))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            copied = true
        },
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = if (copied) "Resultado copiado" else "Copiar resultado",
            tint = if (copied) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier.size(20.dp).semantics { liveRegion = LiveRegionMode.Polite }
        )
    }
    if (onTogglePin != null || onSendToNotes != null || (hasActiveEncounter && onRecordCombatNote != null)) {
        Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más acciones del resultado")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                if (onTogglePin != null) {
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Dejar de fijar resultado" else "Fijar este resultado") },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                }
                if (onSendToNotes != null) {
                    DropdownMenuItem(
                        text = { Text("Enviar a notas") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onSendToNotes(result.tableName, result.resolvedText)
                        }
                    )
                }
                if (hasActiveEncounter && onRecordCombatNote != null) {
                    DropdownMenuItem(
                        text = { Text("Enviar a combate") },
                        leadingIcon = { Icon(Icons.Default.CrisisAlert, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRecordCombatNote("${result.tableName}: ${result.resolvedText}")
                        }
                    )
                }
            }
        }
    }
}
