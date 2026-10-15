package com.deckapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.TableDensity
import com.deckapp.core.ui.theme.LocalTableAppearance

@Composable
fun TableReferenceHeader(rangeLabel: String) {
    val compact = LocalTableAppearance.current.density == TableDensity.COMPACT
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer)
        .padding(horizontal = 12.dp, vertical = if (compact) 8.dp else 12.dp)) {
        Text(rangeLabel, Modifier.width(76.dp), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text("Resultado", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** Shared by settings preview and live table reference so spacing stays identical. */
@Composable
fun TableReferenceRow(
    range: String,
    text: String,
    alternate: Boolean,
    selected: Boolean = false,
    subTableLabel: String? = null
) {
    val compact = LocalTableAppearance.current.density == TableDensity.COMPACT
    val accent = MaterialTheme.colorScheme.primary
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        alternate -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f)
        else -> Color.Transparent
    }
    val ink = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier.fillMaxWidth().background(background)
            .drawBehind { if (selected) drawRect(accent, size = Size(3.dp.toPx(), size.height)) }
            .semantics { if (selected) stateDescription = "Última tirada" }
            .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(range, Modifier.width(76.dp).padding(end = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            if (selected) Text("ÚLTIMA TIRADA", style = MaterialTheme.typography.labelSmall, color = ink)
            Text(text, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = ink)
            if (subTableLabel != null) Text("↳ $subTableLabel", style = MaterialTheme.typography.labelSmall,
                color = if (selected) ink else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
