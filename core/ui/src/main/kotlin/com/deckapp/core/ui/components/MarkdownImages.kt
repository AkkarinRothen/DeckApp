package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

internal val markdownImagePattern = Regex("(?m)^[ \\t]*!\\[([^]\\n]*)]\\(([^)\\n]+)\\)[ \\t]*$")

/** Coil supports packaged WebP assets as well as regular local image files. */
@Composable
internal fun MarkdownWithImages(
    markdown: String,
    modifier: Modifier,
    color: Color,
    style: TextStyle,
    maxLines: Int?,
    onWikiLinkClicked: ((String) -> Unit)?
) {
    Column(modifier) {
        var cursor = 0
        markdownImagePattern.findAll(markdown).forEach { match ->
            val before = markdown.substring(cursor, match.range.first).trim()
            if (before.isNotEmpty()) {
                MarkdownText(before, color = color, style = style, maxLines = maxLines,
                    onWikiLinkClicked = onWikiLinkClicked)
            }
            AsyncImage(
                model = match.groupValues[2],
                contentDescription = match.groupValues[1].ifBlank { "Ilustración del manual" },
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 480.dp)
            )
            cursor = match.range.last + 1
        }
        val after = markdown.substring(cursor).trim()
        if (after.isNotEmpty()) {
            MarkdownText(after, color = color, style = style, maxLines = maxLines,
                onWikiLinkClicked = onWikiLinkClicked)
        }
    }
}
