package com.deckapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.DeckCollection

/** Automatic themed cover, with an image fallback and the collection's saved emblem. */
@Composable
fun CollectionCover(collection: DeckCollection, modifier: Modifier = Modifier) {
    var failed by remember(collection.imageUrl) { mutableStateOf(false) }
    val image = collection.imageUrl != null && !failed
    val accent = Color(collection.color)
    val ink = if (image) Color.White else MaterialTheme.colorScheme.onSurface
    Box(modifier.fillMaxSize().paperBackground(tintColor = lerp(MaterialTheme.colorScheme.surface, accent, 0.1f))) {
        if (image) {
            AsyncImage(collection.imageUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                onError = { failed = true })
            Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.85f)))))
        } else {
            Box(Modifier.matchParentSize().drawBehind {
                val inset = 12.dp.toPx()
                drawRect(accent.copy(alpha = 0.35f), Offset(inset, inset),
                    Size((size.width - inset * 2).coerceAtLeast(0f), (size.height - inset * 2).coerceAtLeast(0f)),
                    style = Stroke(1.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.12f), radius = 42.dp.toPx(), center = Offset(size.width / 2, size.height * 0.35f))
            })
        }
        Icon(collection.icon.toIcon(), null, Modifier.size(52.dp).align(Alignment.TopCenter).padding(top = 4.dp)
            .offset(y = 24.dp), tint = ink)
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(collection.name, style = MaterialTheme.typography.titleMedium, color = ink,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${collection.resourceCount} recursos", style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.8f))
        }
    }
}
