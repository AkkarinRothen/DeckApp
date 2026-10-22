package com.deckapp.feature.draw.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.deckapp.core.model.WikiEntry
import com.deckapp.core.ui.components.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiQuickPreviewDialog(
    wikiEntry: WikiEntry,
    onDismiss: () -> Unit,
    onSendToNotes: ((title: String, content: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFullImage by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var copiedFeedback by remember { mutableStateOf(false) }

    if (showFullImage && !wikiEntry.imagePath.isNullOrBlank()) {
        Dialog(onDismissRequest = { showFullImage = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AsyncImage(
                    model = wikiEntry.imagePath,
                    contentDescription = wikiEntry.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showFullImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabecera interactiva
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = wikiEntry.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Regla / Entrada de Enciclopedia",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val textToCopy = buildString {
                                appendLine(wikiEntry.title)
                                appendLine()
                                append(wikiEntry.content)
                            }
                            clipboard.setText(AnnotatedString(textToCopy))
                            copiedFeedback = true
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar contenido",
                            tint = if (copiedFeedback) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Acciones Rápidas (Enviar a Notas con 1 toque)
            if (onSendToNotes != null) {
                FilledTonalButton(
                    onClick = {
                        onSendToNotes(wikiEntry.title, wikiEntry.content)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar al Diario / Notas de Sesión", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Imagen si existe
            if (!wikiEntry.imagePath.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFullImage = true }
                ) {
                    AsyncImage(
                        model = wikiEntry.imagePath,
                        contentDescription = wikiEntry.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Ampliar imagen",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Contenido en Markdown
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(16.dp)) {
                    if (wikiEntry.content.isBlank()) {
                        Text(
                            text = "Sin contenido adicional disponible.",
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
            }
        }
    }
}
