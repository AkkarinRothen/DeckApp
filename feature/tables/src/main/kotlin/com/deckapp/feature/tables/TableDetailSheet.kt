package com.deckapp.feature.tables

import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckOutlinedButton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollMode
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.PaperSurface
import com.deckapp.core.ui.components.paperBackground
import com.deckapp.core.ui.components.ThemeChapterHeader
import com.deckapp.core.ui.components.categoryEmblem
import com.deckapp.feature.tables.components.TableEntriesPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * BottomSheet de resultado de tirada para una tabla aleatoria.
 *
 * Muestra:
 * - Número del dado + resultado resuelto del último tiro
 * - Botón "Tirar de nuevo"
 * - Historial de los últimos 5 resultados en esta sesión
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailSheet(
    table: RandomTable,
    lastResult: TableRollResult?,
    recentResults: List<TableRollResult>,
    isRolling: Boolean,
    autoRollOnOpen: Boolean = false,
    onRoll: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit = {},
    onSelectEntry: ((Int, Int?) -> Unit)? = null,
    onSendToNotes: ((tableName: String, resultText: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var revealKey by remember(table.id) { mutableIntStateOf(0) }
    var rollingIndex by remember(table.id) { mutableStateOf<Int?>(null) }
    var manualDie by remember(table.id) { mutableStateOf("") }
    var manualError by remember(table.id) { mutableStateOf<String?>(null) }
    var headerMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(revealKey) {
        if (revealKey > 0 && table.entries.isNotEmpty()) {
            repeat(12) { step ->
                rollingIndex = (step * table.entries.size / 12).coerceAtMost(table.entries.lastIndex)
                delay(60)
            }
            rollingIndex = null
        }
    }
    LaunchedEffect(table.id, autoRollOnOpen) {
        if (autoRollOnOpen) { revealKey++; onRoll() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .paperBackground(tintColor = MaterialTheme.colorScheme.surface, alpha = 0.06f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera compacta con menú de administración secundario
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeChapterHeader(table.name, Modifier.weight(1f), categoryEmblem(table.category))
                Box {
                    IconButton(onClick = { headerMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = headerMenuExpanded,
                        onDismissRequest = { headerMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar tabla") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = { headerMenuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar tabla") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = { headerMenuExpanded = false; onExport() }
                        )
                    }
                }
            }
            Text(
                text = buildString {
                    if (table.tags.isNotEmpty()) {
                        append(table.tags.joinToString("  ·  ") {
                            it.name.removeSuffix(".json").replace("_", " ")
                        })
                        append("  ·  ")
                    }
                    append(table.rollFormula)
                    append("  ·  ${table.entries.size} entradas")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DeckButton(onClick = { revealKey++; onRoll() }, enabled = !isRolling,
                    modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lastResult == null) "Tirar" else "Tirar de nuevo")
                }
                DeckOutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
            }
            if (onSelectEntry != null) {
                Text("Modo manual: consulta la tabla y toca la fila obtenida con tus dados.",
                    style = MaterialTheme.typography.bodySmall)
                if (table.rollMode == TableRollMode.RANGE && table.entries.any { it.minRoll != it.maxRoll }) {
                    OutlinedTextField(value = manualDie, onValueChange = { value ->
                        if (value.all(Char::isDigit)) { manualDie = value; manualError = null }
                    }, label = { Text("Número del dado para rangos (si corresponde)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                manualError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
            TableEntriesPreview(table, if (isRolling) null else lastResult?.rollValue,
                onSelectEntry = onSelectEntry?.let { select -> { entryIndex ->
                    val entry = table.entries[entryIndex]
                    val die = manualDie.toIntOrNull()
                    if (table.rollMode == TableRollMode.RANGE && entry.minRoll != entry.maxRoll && die == null)
                        manualError = "Escribe el número del dado para este rango."
                    else if (table.rollMode == TableRollMode.RANGE && die != null && die !in entry.minRoll..entry.maxRoll)
                        manualError = "El dado no pertenece a la fila elegida."
                    else { manualError = null; select(entryIndex, die) }
                } }, rollingIndex = rollingIndex, revealKey = revealKey,
                selectedEntryIndex = lastResult?.selectedEntryIndex)

            // Resultado principal
            AnimatedContent(
                targetState = if (isRolling) "rolling" else lastResult?.resolvedText ?: "—",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "roll_result"
            ) { text ->
                if (isRolling) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                } else if (lastResult != null) {
                    PaperSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        textureAlpha = 0.04f
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RESULTADO DE LA TIRADA", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            // Dado
                            PaperSurface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lastResult.rollValue.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            if (onSendToNotes != null) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { onSendToNotes(table.name, text) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Registrar en Diario")
                                }
                            }
                        }
                    }
                } else {
                    // Estado inicial (sin tirada)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Pulsa para tirar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Historial de tiradas de esta sesión
            if (recentResults.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Historial de esta sesión",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentResults) { result ->
                        HistoryRow(result)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(result: TableRollResult) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(result.timestamp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = result.rollValue.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Text(
            text = result.resolvedText,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = timeStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
