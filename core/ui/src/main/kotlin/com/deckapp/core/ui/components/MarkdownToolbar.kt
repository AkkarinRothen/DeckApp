package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Acciones de formato soportadas por el toolbar de Markdown.
 */
enum class MarkdownActionType(
    val icon: ImageVector,
    val label: String,
    val openTag: String,
    val closeTag: String = "",
    val isPrefix: Boolean = false
) {
    BOLD(Icons.Default.FormatBold, "Negrita", "**", "**"),
    ITALIC(Icons.Default.FormatItalic, "Cursiva", "_", "_"),
    H1(Icons.Default.Title, "Título 1", "# ", "", true),
    H2(Icons.Default.Title, "Título 2", "## ", "", true),
    LIST(Icons.AutoMirrored.Filled.List, "Lista", "- ", "", true),
    CHECKBOX(Icons.Default.CheckBox, "Tareas", "[ ] ", "", true),
    CODE(Icons.Default.Code, "Código", "`", "`"),
    LINK(Icons.Default.Link, "Enlace", "[", "](url)"),
    SEPARATOR(Icons.Default.HorizontalRule, "Separador", "\n---\n", "", true)
}

/**
 * Toolbar horizontal con botones de acción para insertar sintaxis Markdown en un [TextFieldValue].
 */
@Composable
fun MarkdownToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(MarkdownActionType.entries) { action ->
                IconButton(
                    onClick = { onValueChange(applyMarkdownAction(value, action)) }
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Aplica una acción de Markdown al estado actual del TextField.
 * Maneja tanto el envoltorio de selección como la inserción de prefijos.
 */
private fun applyMarkdownAction(value: TextFieldValue, action: MarkdownActionType): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val selectedText = text.substring(selection.start, selection.end)

    return if (action.isPrefix) {
        // Acciones tipo prefijo (H1, List, etc) -> Insertar al inicio de la línea o posición actual
        val before = text.substring(0, selection.start)
        val after = text.substring(selection.start)
        
        // Si no estamos al inicio de una línea, forzar un salto de línea antes del prefijo
        val prefix = if (selection.start > 0 && text[selection.start - 1] != '\n') {
            "\n${action.openTag}"
        } else {
            action.openTag
        }
        
        val newText = before + prefix + after
        val newPosition = selection.start + prefix.length
        
        TextFieldValue(
            text = newText,
            selection = TextRange(newPosition)
        )
    } else {
        // Acciones tipo envoltorio (Bold, Italic, Link)
        val before = text.substring(0, selection.start)
        val after = text.substring(selection.end)
        
        val newText = before + action.openTag + selectedText + action.closeTag + after
        
        // Posicionar cursor: si había selección, al final del cierre. Si no, en medio.
        val newSelection = if (selection.collapsed) {
            TextRange(selection.start + action.openTag.length)
        } else {
            TextRange(selection.start + action.openTag.length + selectedText.length + action.closeTag.length)
        }
        
        TextFieldValue(
            text = newText,
            selection = newSelection
        )
    }
}
