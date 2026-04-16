package com.deckapp.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.cacheClearedMessage) {
        uiState.cacheClearedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ajustes") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading && uiState.decksStorage.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Almacenamiento ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Almacenamiento",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.loadStorageInfo() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            }

            item {
                StorageSummaryCard(
                    totalSizeBytes = uiState.totalSizeBytes,
                    deckCount = uiState.decksStorage.size,
                    isClearing = uiState.isClearingCache,
                    onClearCache = { viewModel.clearCache() }
                )
            }

            if (uiState.decksStorage.isNotEmpty()) {
                items(uiState.decksStorage, key = { it.id }) { info ->
                    DeckStorageRow(
                        info = info,
                        totalSizeBytes = uiState.totalSizeBytes
                    )
                }
            }

            // ── Calidad de imagen ───────────────────────────────────────
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "Importación",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                JpegQualitySelector(
                    currentQuality = uiState.jpegQuality,
                    onQualitySelected = { viewModel.setJpegQuality(it) }
                )
            }

            // ── Librerías ───────────────────────────────────────────────
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "Librerías de terceros",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("• Jetpack Compose (UI Framework)", style = MaterialTheme.typography.bodySmall)
                        Text("• Hilt (Inyección de dependencias)", style = MaterialTheme.typography.bodySmall)
                        Text("• Room (Base de datos local)", style = MaterialTheme.typography.bodySmall)
                        Text("• Coil (Carga de imágenes)", style = MaterialTheme.typography.bodySmall)
                        Text("• Markwon (Renderizado Markdown)", style = MaterialTheme.typography.bodySmall)
                        Text("• PdfRenderer (Framework nativo)", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ── Acerca de ───────────────────────────────────────────────
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

            item {
                Text(
                    "Acerca de",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(label = "Versión", value = uiState.appVersion)
                        InfoRow(label = "Licencia", value = "MIT / Personal")
                        InfoRow(label = "Desarrollado para", value = "DMs de TTRPG")
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(
    totalSizeBytes: Long,
    deckCount: Int,
    isClearing: Boolean,
    onClearCache: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        formatBytes(totalSizeBytes),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$deckCount mazo${if (deckCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onClearCache,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Limpiar caché", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}


@Composable
private fun DeckStorageRow(info: DeckStorageInfo, totalSizeBytes: Long) {
    val percentage = if (totalSizeBytes > 0) info.sizeBytes.toFloat() / totalSizeBytes else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = info.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                text = formatBytes(info.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun JpegQualitySelector(currentQuality: Int, onQualitySelected: (Int) -> Unit) {
    val options = listOf("Alta" to 95, "Media" to 85, "Baja" to 70)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Calidad de imagen al importar",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Afecta el tamaño de las imágenes guardadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (label, quality) ->
                    FilterChip(
                        selected = currentQuality == quality,
                        onClick = { onQualitySelected(quality) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L             -> "$bytes B"
    bytes < 1024L * 1024L     -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else                      -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}
