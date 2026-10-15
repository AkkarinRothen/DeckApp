package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.model.SessionBoardItemSize
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.ui.components.SmartText
import com.deckapp.feature.draw.components.BoardBlockMeta

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
    onRecordCombatNote: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    fun triggerRollFeedback() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onRollTable(item.resourceId ?: 0L)
    }

    if (item.size == SessionBoardItemSize.S || (compactInFolder && item.size != SessionBoardItemSize.L)) {
        CompactTableBlockContent(
            item = item,
            meta = meta,
            latestTableRoll = latestTableRoll,
            tableFormula = tableFormula,
            compactInFolder = compactInFolder,
            hasActiveEncounter = hasActiveEncounter,
            onRoll = { triggerRollFeedback() },
            onSendToNotes = onSendToNotes,
            onRecordCombatNote = onRecordCombatNote
        )
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera contextual: Fórmula del dado + Acciones del resultado (copiar, notas, combate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val effectiveFormula = tableFormula
                if (!effectiveFormula.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = "🎲 $effectiveFormula",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Acciones rápidas para el resultado más reciente
                if (latestTableRoll != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                clipboardManager.setText(AnnotatedString(latestTableRoll.resolvedText))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar resultado",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (onSendToNotes != null) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSendToNotes(latestTableRoll.tableName, latestTableRoll.resolvedText)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "Enviar resultado a notas",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (hasActiveEncounter && onRecordCombatNote != null) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onRecordCombatNote("${latestTableRoll.tableName}: ${latestTableRoll.resolvedText}")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CrisisAlert,
                                    contentDescription = "Enviar resultado a combate",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Resultado Principal Destacado
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(
                    0.5.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (latestTableRoll != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.align(Alignment.Top)
                        ) {
                            Text(
                                text = "[${latestTableRoll.rollValue}]",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        SmartText(
                            text = latestTableRoll.resolvedText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            npcNames = npcNames,
                            tableNames = tableNames,
                            onTagClicked = onTagClicked,
                            onDiceClicked = onDiceClicked
                        )
                    } else {
                        SmartText(
                            text = meta.preview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            npcNames = npcNames,
                            tableNames = tableNames,
                            onTagClicked = onTagClicked,
                            onDiceClicked = onDiceClicked
                        )
                    }
                }
            }

            // Mini-historial de tiradas anteriores (si hay más de una en la lista reciente)
            val previousRolls = recentRolls.drop(1).take(2)
            if (previousRolls.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Tiradas anteriores:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    previousRolls.forEach { prev ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    clipboardManager.setText(AnnotatedString(prev.resolvedText))
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "[${prev.rollValue}]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = prev.resolvedText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar tirada previa",
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Botón Principal de Tirar
            FilledTonalButton(
                onClick = { triggerRollFeedback() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = if (item.isPinned) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = meta.color.copy(alpha = 0.12f),
                        contentColor = meta.color
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Casino,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = if (compactInFolder) "Tirar" else meta.ctaLabel, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CompactTableBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    latestTableRoll: TableRollResult?,
    tableFormula: String?,
    compactInFolder: Boolean,
    hasActiveEncounter: Boolean,
    onRoll: () -> Unit,
    onSendToNotes: ((tableName: String, resultText: String) -> Unit)?,
    onRecordCombatNote: ((String) -> Unit)?
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (latestTableRoll != null) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "[${latestTableRoll.rollValue}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            Text(
                text = latestTableRoll.resolvedText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    clipboardManager.setText(AnnotatedString(latestTableRoll.resolvedText))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
            }
        } else {
            val effectiveFormula = tableFormula ?: meta.preview
            Text(
                text = if (!tableFormula.isNullOrBlank()) "🎲 $tableFormula" else "Sin tiradas aún",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalIconButton(
            onClick = onRoll,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = "Tirar",
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
