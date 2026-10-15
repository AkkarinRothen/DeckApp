package com.deckapp.feature.draw.components.blocks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deckapp.core.model.BoardMythicFateResult
import com.deckapp.core.model.DiceRollResult
import com.deckapp.core.model.SessionBoardItem
import com.deckapp.core.ui.components.SmartText
import com.deckapp.feature.draw.components.BoardBlockMeta
import com.deckapp.core.domain.usecase.oracleJournalEntry
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    compactInFolder: Boolean,
    notesContent: String = "",
    onUpdateNotes: (String) -> Unit = {},
    npcNames: List<String>,
    tableNames: List<String>,
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String) -> Unit,
    lastMythicResult: BoardMythicFateResult? = null,
    lastDiceResult: DiceRollResult? = null,
    onGenerateSpark: (() -> String)? = null
) {
    var isEditingInline by remember { mutableStateOf(false) }
    var textValue by remember(notesContent) { mutableStateOf(TextFieldValue(notesContent)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isEditingInline) {
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    onUpdateNotes(newValue.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 220.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = meta.color,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Fila de herramientas rápida + botón Confirmar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Formato rápido y Solo Journaling (con scroll horizontal)
                Row(
                    modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Negrita
                    TextButton(
                        onClick = {
                            val text = textValue.text
                            val selection = textValue.selection
                            val newText = text.substring(0, selection.start) + "**" + text.substring(selection.start, selection.end) + "**" + text.substring(selection.end)
                            val newSelectionStart = selection.start + 2
                            val newSelectionEnd = selection.end + 2
                            textValue = TextFieldValue(newText, TextRange(newSelectionStart, newSelectionEnd))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("B", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }

                    // Cursiva
                    TextButton(
                        onClick = {
                            val text = textValue.text
                            val selection = textValue.selection
                            val newText = text.substring(0, selection.start) + "*" + text.substring(selection.start, selection.end) + "*" + text.substring(selection.end)
                            val newSelectionStart = selection.start + 1
                            val newSelectionEnd = selection.end + 1
                            textValue = TextFieldValue(newText, TextRange(newSelectionStart, newSelectionEnd))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("I", style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                    }

                    // Marca de Tiempo
                    TextButton(
                        onClick = {
                            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                            val timeStr = "[${LocalTime.now().format(formatter)}] "
                            val text = textValue.text
                            val selection = textValue.selection
                            val newText = text.substring(0, selection.start) + timeStr + text.substring(selection.start)
                            val newCursor = selection.start + timeStr.length
                            textValue = TextFieldValue(newText, TextRange(newCursor))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("⏰", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Solo Journal: Insertar Oráculo
                    TextButton(
                        onClick = {
                            val snippet = if (lastMythicResult != null) {
                                "\n${oracleJournalEntry(lastMythicResult)}\n"
                            } else {
                                "\n> **Pregunta:** \n> **Respuesta:** pendiente\n"
                            }
                            val text = textValue.text
                            val sel = textValue.selection
                            val newText = text.substring(0, sel.start) + snippet + text.substring(sel.start)
                            val newCursor = sel.start + snippet.length
                            textValue = TextFieldValue(newText, TextRange(newCursor))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("🔮 Oráculo", style = MaterialTheme.typography.labelSmall)
                    }

                    // Solo Journal: Insertar Dado
                    TextButton(
                        onClick = {
                            val snippet = if (lastDiceResult != null) {
                                "\n> 🎲 **Tirada** (${lastDiceResult.expression}): **${lastDiceResult.total}**\n"
                            } else {
                                "\n> 🎲 **Tirada**: d20\n"
                            }
                            val text = textValue.text
                            val sel = textValue.selection
                            val newText = text.substring(0, sel.start) + snippet + text.substring(sel.start)
                            val newCursor = sel.start + snippet.length
                            textValue = TextFieldValue(newText, TextRange(newCursor))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("🎲 Dado", style = MaterialTheme.typography.labelSmall)
                    }

                    // Solo Journal: Insertar Chispa
                    TextButton(
                        onClick = {
                            val spark = onGenerateSpark?.invoke() ?: run {
                                val a = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.actionKeywords.random()
                                val s = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.subjectKeywords.random()
                                "$a · $s"
                            }
                            val snippet = "\n> ⚡ **Chispa**: $spark\n"
                            val text = textValue.text
                            val sel = textValue.selection
                            val newText = text.substring(0, sel.start) + snippet + text.substring(sel.start)
                            val newCursor = sel.start + snippet.length
                            textValue = TextFieldValue(newText, TextRange(newCursor))
                            onUpdateNotes(newText)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("⚡ Chispa", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Confirmar / Salir
                FilledIconButton(
                    onClick = { isEditingInline = false },
                    modifier = Modifier.size(32.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = meta.color)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmar",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        } else {
            // Tira de estampas rápidas de Solo Journaling (Modo Lectura)
            if (!compactInFolder) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { isEditingInline = true },
                        label = { Text("Escribir", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        modifier = Modifier.height(28.dp)
                    )

                    if (lastMythicResult != null) {
                        SuggestionChip(
                            onClick = {
                                val snippet = oracleJournalEntry(lastMythicResult)
                                val updated = if (notesContent.isBlank()) snippet else "$notesContent\n\n$snippet"
                                onUpdateNotes(updated)
                            },
                            label = { Text("+ 🔮 ${lastMythicResult.answerLabel}", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    if (lastDiceResult != null) {
                        SuggestionChip(
                            onClick = {
                                val snippet = "> 🎲 **Tirada** (${lastDiceResult.expression}): **${lastDiceResult.total}**"
                                val updated = if (notesContent.isBlank()) snippet else "$notesContent\n\n$snippet"
                                onUpdateNotes(updated)
                            },
                            label = { Text("+ 🎲 ${lastDiceResult.total}", fontSize = 11.sp) },
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    SuggestionChip(
                        onClick = {
                            val spark = onGenerateSpark?.invoke() ?: run {
                                val a = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.actionKeywords.random()
                                val s = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.subjectKeywords.random()
                                "$a · $s"
                            }
                            val snippet = "> ⚡ **Chispa**: $spark"
                            val updated = if (notesContent.isBlank()) snippet else "$notesContent\n\n$snippet"
                            onUpdateNotes(updated)
                        },
                        label = { Text("+ ⚡ Chispa", fontSize = 11.sp) },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onDoubleClick = { isEditingInline = true },
                        onClick = {}
                    )
            ) {
                if (meta.accentNote != null && !compactInFolder) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        SmartText(
                            text = meta.accentNote,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            npcNames = npcNames,
                            tableNames = tableNames,
                            onTagClicked = onTagClicked,
                            onDiceClicked = onDiceClicked
                        )
                    }
                }

                SmartText(
                    text = meta.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compactInFolder) 2 else Int.MAX_VALUE,
                    overflow = if (compactInFolder) TextOverflow.Ellipsis else TextOverflow.Clip,
                    npcNames = npcNames,
                    tableNames = tableNames,
                    onTagClicked = onTagClicked,
                    onDiceClicked = onDiceClicked
                )
            }
        }
    }
}
