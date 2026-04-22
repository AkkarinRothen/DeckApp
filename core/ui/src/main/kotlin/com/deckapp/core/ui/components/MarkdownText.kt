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

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int? = null,
    onWikiLinkClicked: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val colorInt = color.toArgb()
    val fontSizeSp = style.fontSize.value

    // Procesar [[Links]] bidireccionales antes de pasar a Markwon
    val processedMarkdown = remember(markdown) {
        val regex = Regex("\\[\\[(.*?)\\]\\]")
        markdown.replace(regex) { matchResult ->
            val title = matchResult.groupValues[1]
            "[$title](wiki://$title)"
        }
    }

    val markwon = remember(context) { 
        Markwon.builder(context)
            .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
                override fun configureConfiguration(builder: io.noties.markwon.MarkwonConfiguration.Builder) {
                    builder.linkResolver { _, link ->
                        if (link.startsWith("wiki://")) {
                            val title = link.removePrefix("wiki://")
                            onWikiLinkClicked?.invoke(title)
                        } else {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(link))
                            context.startActivity(intent)
                        }
                    }
                }

                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    // Estilo para Box Text (Blockquote)
                    builder.blockQuoteColor(colorInt)
                        .blockQuoteWidth(4)
                        .blockMargin(24)
                }
            })
            .build()
    }

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
            markwon.setMarkdown(textView, processedMarkdown)
        },
        modifier = modifier
    )
}
