package com.deckapp.core.ui.components

import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Renderiza texto Markdown usando Markwon dentro de Compose.
 * Usa AndroidView con un TextView para compatibilidad con Markwon (View-based).
 *
 * @param markdown Texto en formato Markdown a renderizar.
 * @param color    Color del texto — por defecto `onSurface` del tema actual.
 * @param style    Estilo de tipografía — por defecto `bodyMedium`.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int? = null
) {
    val context = LocalContext.current
    val colorInt = color.toArgb()
    val fontSizeSp = style.fontSize.value

    val markwon = remember(context) { Markwon.create(context) }

    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextColor(colorInt)
                textSize = fontSizeSp
                background = null
                maxLines?.let { setMaxLines(it) }
            }
        },
        update = { textView ->
            textView.setTextColor(colorInt)
            textView.textSize = fontSizeSp
            maxLines?.let { textView.setMaxLines(it) }
            markwon.setMarkdown(textView, markdown)
        },
        modifier = modifier
    )
}
