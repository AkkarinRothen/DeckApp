package com.deckapp.feature.draw.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.deckapp.core.ui.components.MarkdownText
import com.deckapp.core.ui.components.MarkdownToolbar

enum class NotesTabMode {
    NOTES, ACTIVITY
}

/**
 * Pestaña unificada de Registro: Notas libres y Actividad / Bitácora.
 * Soporta formato Markdown, persistencia de borrador y teclado flotante sin solapamientos.
 */
@Composable
fun NotesTab(
    notes: String,
    isSaving: Boolean,
    onNotesChange: (String) -> Unit,
    onAddQuickNote: (() -> Unit)? = null,
    initialMode: NotesTabMode = NotesTabMode.NOTES,
    saveError: Boolean = false,
    onRetrySave: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(initialMode) }
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(notes))
    }
    val clipboard = LocalClipboardManager.current
    val editorScrollState = rememberScrollState()
    val activityScrollState = rememberScrollState()
    
    // Sincronizar el estado externo cuando cambia externamente y no está siendo editado activamente
    LaunchedEffect(notes) {
        if (notes != textFieldValue.text && !isSaving) {
            textFieldValue = textFieldValue.copy(text = notes)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Cabecera con selector de superficie y estado de guardado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.height(36.dp)
            ) {
                SegmentedButton(
                    selected = mode == NotesTabMode.NOTES,
                    onClick = { mode = NotesTabMode.NOTES },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Notas", style = MaterialTheme.typography.labelMedium)
                }
                SegmentedButton(
                    selected = mode == NotesTabMode.ACTIVITY,
                    onClick = { mode = NotesTabMode.ACTIVITY },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Actividad", style = MaterialTheme.typography.labelMedium)
                }
            }

            AnimatedContent(
                targetState = when {
                    saveError -> "error"
                    isSaving -> "saving"
                    else -> "saved"
                },
                label = "SavingIndicator"
            ) { state ->
                when (state) {
                    "error" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Error", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            if (onRetrySave != null) {
                                TextButton(
                                    onClick = onRetrySave,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Reintentar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    "saving" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("Guardando...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Guardado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        when (mode) {
            NotesTabMode.NOTES -> {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it
                            onNotesChange(it.text)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(editorScrollState),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    "Escribe aquí las notas del DM, sucesos de la partida o recordatorios...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Toolbar de Markdown posicionada por encima del teclado IME
                MarkdownToolbar(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onNotesChange(it.text)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            }
            NotesTabMode.ACTIVITY -> {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(activityScrollState)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bitácora de Sesión",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { clipboard.setText(AnnotatedString(textFieldValue.text)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar bitácora", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (textFieldValue.text.isBlank()) {
                            Text(
                                "No hay actividad registrada aún. Los resultados de tablas, oráculos y apuntes narrativos se mostrarán aquí.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            MarkdownText(
                                markdown = textFieldValue.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
