package com.deckapp.feature.draw.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deckapp.core.model.WikiEntry
import com.deckapp.core.ui.components.MarkdownText

@Composable
fun WikiQuickPreviewDialog(
    wikiEntry: WikiEntry,
    onDismiss: () -> Unit
) {
    var showFullImage by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    if (showFullImage && !wikiEntry.imagePath.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showFullImage = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = wikiEntry.imagePath,
                    contentDescription = wikiEntry.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar imagen", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = wikiEntry.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val textToCopy = buildString {
                            appendLine(wikiEntry.title)
                            appendLine()
                            append(wikiEntry.content)
                        }
                        clipboard.setText(AnnotatedString(textToCopy))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!wikiEntry.imagePath.isNullOrBlank()) {
                    AsyncImage(
                        model = wikiEntry.imagePath,
                        contentDescription = wikiEntry.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop
                    )
                }

                if (wikiEntry.content.isBlank()) {
                    Text(
                        text = "Sin contenido disponible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    MarkdownText(
                        markdown = wikiEntry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

