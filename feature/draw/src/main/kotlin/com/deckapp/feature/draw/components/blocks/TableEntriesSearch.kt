package com.deckapp.feature.draw.components.blocks

import com.deckapp.core.model.TableEntry
import java.text.Normalizer
import java.util.Locale

internal fun filterTableEntries(entries: List<TableEntry>, query: String): List<TableEntry> {
    val words = normalizeSearchText(query).trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return entries
    return entries.filter { entry ->
        val text = normalizeSearchText(entry.text)
        words.all { it in text }
    }
}

private fun normalizeSearchText(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
