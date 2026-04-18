package com.deckapp.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.createBackup(it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.restoreBackup(it) } }

    var showRestoreConfirm by remember { mutableStateOf(false) }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar biblioteca") },
            text = { Text("Esta acción BORRARÁ todos tus mazos, tablas y notas actuales para reemplazarlos por los del backup. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        restoreBackupLauncher.launch("application/zip")
                    }
                ) {
                    Text("Restaurar y Reemplazar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LaunchedEffect(uiState.cacheClearedMessage, uiState.backupMessage) {
        uiState.cacheClearedMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
        uiState.backupMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
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
            // ── Copia de Seguridad ──────────────────────────────────────
            item {
                Text(
                    "Copia de Seguridad",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                BackupCard(
                    isProcessing = uiState.isBackingUp || uiState.isRestoring,
                    onCreateBackup = {
                        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                        createBackupLauncher.launch("deckapp_backup_$dateStr.zip")
                    },
                    onRestoreBackup = {
                        showRestoreConfirm = true
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

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

            item {
                GeminiApiKeyEditor(
                    currentKey = uiState.geminiApiKey,
                    onKeyChanged = { viewModel.setGeminiApiKey(it) }
                )
            }

            item {
                AutoVisionToggle(
                    enabled = uiState.autoVisionEnabled,
                    apiKeyConfigured = uiState.geminiApiKey.isNotBlank(),
                    onToggle = { viewModel.setAutoVisionEnabled(it) }
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
                        Text("• Kotlinx Serialization (Backup Engine)", style = MaterialTheme.typography.bodySmall)
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

@Composable
private fun GeminiApiKeyEditor(currentKey: String, onKeyChanged: (String) -> Unit) {
    var text by remember(currentKey) { mutableStateOf(currentKey) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Gemini AI (Google)",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "API Key para procesar tablas con IA. Los datos se enviarán a Google.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Google AI API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (text != currentKey) {
                        IconButton(onClick = { onKeyChanged(text) }) {
                            Icon(Icons.Default.Check, "Guardar")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun AutoVisionToggle(
    enabled: Boolean,
    apiKeyConfigured: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Vision AI automático", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (apiKeyConfigured) "Usa Vision AI en cada reconocimiento de tabla"
                    else "Requiere configurar la API Key de Gemini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled && apiKeyConfigured,
                onCheckedChange = onToggle,
                enabled = apiKeyConfigured
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L             -> "$bytes B"
    bytes < 1024L * 1024L     -> "${"%.1f".format(bytes / 1024.0)} KB"
    bytes < 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    else                      -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
}

@Composable
private fun BackupCard(
    isProcessing: Boolean,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Gestión de Biblioteca",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Protege tu trabajo exportando un backup o restaura uno existente. Útil para mover tus datos a otro dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Procesando backup...", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCreateBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Crear Backup")
                    }
                    OutlinedButton(
                        onClick = onRestoreBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Restaurar")
                    }
                }
            }
        }
    }
}
