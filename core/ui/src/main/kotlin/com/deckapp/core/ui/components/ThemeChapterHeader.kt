package com.deckapp.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.AppTheme
import com.deckapp.core.ui.theme.LocalAppTheme

fun categoryEmblem(category: String): ImageVector {
    val name = category.lowercase()
    return when {
        listOf("tesoro", "botín", "botin", "loot", "equipo").any(name::contains) -> Icons.Default.Inventory2
        listOf("magia", "magic", "arcano").any(name::contains) -> Icons.Default.AutoAwesome
        listOf("encuentro", "combate", "encounter").any(name::contains) -> Icons.Default.Shield
        listOf("nombre", "personaje", "npc", "social").any(name::contains) -> Icons.Default.Groups
        else -> Icons.Default.Explore
    }
}

@Composable
fun ThemeChapterHeader(title: String, modifier: Modifier = Modifier, emblem: ImageVector = Icons.Default.AutoAwesome) {
    val theme = LocalAppTheme.current
    val parchment = theme == AppTheme.PARCHMENT || theme == AppTheme.PARCHMENT_NIGHT
    val line = MaterialTheme.colorScheme.primary.copy(alpha = if (parchment) 0.5f else 0.25f)
    Row(modifier.fillMaxWidth().drawBehind {
        drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        if (parchment) drawLine(line, Offset(0f, size.height - 4.dp.toPx()),
            Offset(size.width, size.height - 4.dp.toPx()), 0.5.dp.toPx())
    }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(emblem, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
    }
}
