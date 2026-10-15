package com.deckapp.core.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun SmartText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    npcNames: List<String> = emptyList(),
    tableNames: List<String> = emptyList(),
    onTagClicked: (String) -> Unit = {},
    onDiceClicked: (String) -> Unit = {}
) {
    if (text.isBlank()) return

    val diceRegex = Regex("""\b(\d+d\d+([+-]\d+)?)\b""", RegexOption.IGNORE_CASE)
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        diceRegex.findAll(text).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            val diceFormula = matchResult.value
            pushStringAnnotation(tag = "DICE", annotation = diceFormula)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(diceFormula)
            }
            pop()
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "DICE", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onDiceClicked(annotation.item)
                    return@ClickableText
                }
            annotatedString.getStringAnnotations(tag = "TAG", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onTagClicked(annotation.item)
                    return@ClickableText
                }
        }
    )
}
