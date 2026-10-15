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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.PaperSurface
import com.deckapp.core.ui.components.paperBackground
import com.deckapp.core.ui.components.ThemeChapterHeader
import com.deckapp.core.ui.components.categoryEmblem
import com.deckapp.feature.tables.components.TableEntriesPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onRoll: () -> Unit,
    onExport: () -> Unit,
    onEdit: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Cabecera con botón de exportar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeChapterHeader(table.name, Modifier.weight(1f), categoryEmblem(table.category))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar tabla",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onExport) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Exportar tabla",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = buildString {
                    if (table.tags.isNotEmpty()) {
                        append(table.tags.joinToString("  ·  ") { it.name })
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

            Spacer(Modifier.height(24.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeckButton(
                    onClick = onRoll,
                    enabled = !isRolling,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lastResult == null) "Tirar" else "Tirar de nuevo")
                }
                DeckOutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cerrar")
                }
            }

            TableEntriesPreview(table, if (isRolling) null else lastResult?.rollValue)

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
