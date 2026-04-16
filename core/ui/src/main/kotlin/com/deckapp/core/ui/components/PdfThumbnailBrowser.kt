package com.deckapp.core.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Navegador de miniaturas de PDF. 
 * Muestra una sección de "Recientes" (horizontal) y una de "Explorador" (grilla).
 */
@Composable
fun PdfThumbnailBrowser(
    recentPdfs: List<Pair<Uri, String>>,
    browsedPdfs: List<Pair<Uri, String>>,
    onRenderPage: suspend (Uri) -> Bitmap?,
    onPdfSelected: (Uri) -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        
        // ── Sección Recientes ─────────────────────────────────────────
        if (recentPdfs.isNotEmpty()) {
            Text(
                text = "Recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recentPdfs) { (uri, name) ->
                    PdfThumbnailItem(
                        uri = uri,
                        label = name,
                        onRenderPage = onRenderPage,
                        onClick = { onPdfSelected(uri) }
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }

        // ── Sección Explorador ────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (browsedPdfs.isNotEmpty()) "En la carpeta" else "Manuales y PDFs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = onOpenFolder,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cargar Carpeta")
            }
        }

        if (browsedPdfs.isNotEmpty()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp) // Limitar altura para que no empuje todo
            ) {
                items(browsedPdfs) { (uri, name) ->
                    PdfThumbnailItem(
                        uri = uri,
                        label = name,
                        onRenderPage = onRenderPage,
                        onClick = { onPdfSelected(uri) }
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selecciona una carpeta para ver tus PDFs con miniaturas y agilizar la importación.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
