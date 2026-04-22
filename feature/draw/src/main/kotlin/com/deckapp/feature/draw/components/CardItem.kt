package com.deckapp.feature.draw.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.deckapp.core.model.Card
import com.deckapp.core.model.CardAspectRatio
import java.io.File

@Composable
fun CardItem(
    card: Card,
    aspectRatio: CardAspectRatio,
    showTitle: Boolean,
    backImagePath: String?,
    onClick: () -> Unit,
    onDiscard: () -> Unit,
    onReveal: () -> Unit,
    onRollTable: (Long) -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .aspectRatio(aspectRatio.ratio)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            if (card.isRevealed) {
                // Frente de la carta
                AsyncImage(
                    model = card.activeFace.imagePath?.let { File(it) },
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                if (showTitle) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    ) {
                        Text(
                            text = card.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(2.dp),
                            maxLines = 1
                        )
                    }
                }

                // Indicador de tabla vinculada
                card.linkedTableId?.let { tableId ->
                    IconButton(
                        onClick = { onRollTable(tableId) },
                        modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)
                    ) {
                        Icon(Icons.Default.Casino, null, tint = Color.White)
                    }
                }
            } else {
                // Dorso de la carta
                AsyncImage(
                    model = backImagePath?.let { File(it) },
                    contentDescription = "Dorso",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer),
                    contentScale = ContentScale.Crop
                )
                
                IconButton(
                    onClick = onReveal,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}
