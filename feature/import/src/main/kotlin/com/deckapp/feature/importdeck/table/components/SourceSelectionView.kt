package com.deckapp.feature.importdeck.table.components

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextFields
import android.graphics.Bitmap
import androidx.compose.material3.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.deckapp.core.model.TableImportSource

@Composable
fun SourceSelectionView(
    onSelect: (TableImportSource) -> Unit,
    onFileSelect: (Uri) -> Unit,
    recentPdfs: List<Pair<Uri, String>>,
    browsedPdfs: List<Pair<Uri, String>>,
    browsedThumbnails: Map<Uri, Bitmap?>,
    isSearching: Boolean,
    onSearchExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Selecciona una fuente",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceCard(
                title = "Imagen / PDF",
                icon = Icons.Default.Description,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TableImportSource.OCR_IMAGE) }
            )
            SourceCard(
                title = "CSV / Excel",
                icon = Icons.Default.FileUpload,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TableImportSource.CSV_TEXT) }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SourceCard(
                title = "Texto Plano",
                icon = Icons.Default.TextFields,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TableImportSource.PLAIN_TEXT) }
            )
            SourceCard(
                title = "Markdown",
                icon = Icons.Default.TableChart,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(TableImportSource.MARKDOWN_TABLE) }
            )
        }
        
        Spacer(Modifier.height(32.dp))
        
        if (recentPdfs.isNotEmpty()) {
            Text(
                "Archivos recientes",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(recentPdfs) { (uri, name) ->
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(Icons.Default.Description, null) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        trailingContent = {
                            Button(onClick = { onFileSelect(uri) }) {
                                Text("Abrir")
                            }
                        }
                    )
                }
            }
        } else {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "No hay archivos recientes",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (browsedPdfs.isNotEmpty()) {
            Text(
                "Archivos encontrados",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(browsedPdfs) { (uri, name) ->
                    val thumbnail = browsedThumbnails[uri]
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = {
                            if (thumbnail != null) {
                                Image(
                                    bitmap = thumbnail.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small)
                                )
                            } else {
                                Icon(Icons.Default.Description, null, modifier = Modifier.size(56.dp))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        trailingContent = {
                            Button(onClick = { onFileSelect(uri) }) {
                                Text("Importar")
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onSearchExternal,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSearching
        ) {
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Default.Search, null)
                Spacer(Modifier.width(8.dp))
                Text("Buscar en el dispositivo")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
