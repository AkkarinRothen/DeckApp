package com.deckapp.feature.settings

import com.deckapp.feature.settings.components.GeminiApiKeyEditor

import com.deckapp.feature.settings.components.GeneralPreferencesCard

import com.deckapp.core.ui.components.DeckCard
import com.deckapp.core.ui.components.DeckButton
import com.deckapp.core.ui.components.DeckOutlinedButton
import com.deckapp.core.ui.components.DeckTextButton
import com.deckapp.core.ui.icons.DeckIcons

import com.deckapp.feature.settings.components.TableAppearanceCard
import com.deckapp.feature.settings.components.ThemeComponentsPreview
import com.deckapp.feature.settings.components.AppearancePresetsCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.deckapp.core.model.AppTheme

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
    ) { uri -> uri?.let { viewModel.inspectBackup(it) } }

    var category by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("General") }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(category) { listState.scrollToItem(0) }
    LaunchedEffect(category, uiState.autoCheckUpdates) {
        if (category == "Acerca de" && uiState.autoCheckUpdates) viewModel.checkForUpdates()
    }

    // Diálogo de Inspección Previa y Verificación de Integridad
    uiState.pendingBackupInspection?.let { inspection ->
        val counts = inspection.manifest.counts
        val requiredMb = inspection.requiredSizeBytes / (1024 * 1024)
        val availableMb = inspection.availableSizeBytes / (1024 * 1024)
        val hasEnoughSpace = inspection.availableSizeBytes > inspection.requiredSizeBytes
        val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
        val dateText = dateFormat.format(Date(inspection.manifest.createdAt))

        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupInspection() },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Inspección de Backup", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Copia generada el $dateText (v${inspection.manifest.appVersion})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Contenido detectado:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("• Mazos: ${counts.decks}  |  Cartas: ${counts.cards}", style = MaterialTheme.typography.bodySmall)
                            Text("• Tablas aleatorias: ${counts.randomTables}", style = MaterialTheme.typography.bodySmall)
                            Text("• PNJs: ${counts.npcs}  |  Artículos Wiki: ${counts.wikiEntries}", style = MaterialTheme.typography.bodySmall)
                            Text("• Sesiones: ${counts.sessions}  |  Mapas tácticos: ${counts.tacticalMaps}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasEnoughSpace) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Espacio libre en disco:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "$availableMb MB disp. (req. ~$requiredMb MB)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasEnoughSpace) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = "⚠️ Advertencia: La restauración reemplazará tu base de datos actual con la del archivo seleccionado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRestore() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restaurar y Reemplazar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBackupInspection() }) {
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
            Column {
                TopAppBar(title = { Text("Ajustes") })
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("General", "Apariencia y lectura", "Partidas", "Importación e IA", "Datos", "Acerca de")) { label ->
                        FilterChip(selected = category == label, onClick = { category = label }, label = { Text(label) })
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (category == "Datos" && uiState.isLoading && uiState.decksStorage.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (category) {
                "General" -> {
                    item {
                        GeneralPreferencesCard(
                            uiState.generalPreferences, "General", viewModel::setGeneralPreferences
                        )
                    }
                }
                "Apariencia y lectura" -> {
                    item {
                        GeneralPreferencesCard(
                            uiState.generalPreferences, "Lectura", viewModel::setGeneralPreferences
                        )
                    }
                    // ── Apariencia ──────────────────────────────────────────
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

                    item {
                        Text(
                            "Apariencia",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        ThemeSelectorCard(
                            currentTheme = uiState.appTheme,
                            onThemeSelected = { viewModel.setAppTheme(it) }
                        )
                    }

                    item {
                        TableAppearanceCard(
                            appearance = uiState.tableAppearance,
                            onTextureSelected = viewModel::setTextureIntensity,
                            onDensitySelected = viewModel::setTableDensity
                        )
                    }

                    item {
                        AppearancePresetsCard(uiState.tableAppearance.accent, uiState.appearancePresets,
                            viewModel::setThemeAccent, viewModel::saveAppearancePreset,
                            viewModel::applyAppearancePreset, viewModel::deleteAppearancePreset)
                    }
                }
                "Partidas" -> {
                    item {
                        GeneralPreferencesCard(
                            uiState.generalPreferences, "Partidas", viewModel::setGeneralPreferences
                        )
                    }
                    // ── Experiencia de Usuario ───────────────────────────────────
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }

                    item {
                        Text(
                            "Experiencia de Usuario",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    item {
                        SimplifiedModeToggle(
                            enabled = uiState.simplifiedModeEnabled,
                            onToggle = { viewModel.setSimplifiedModeEnabled(it) }
                        )
                    }
                }
                "Importación e IA" -> {
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
                            onKeyChanged = viewModel::setGeminiApiKey,
                            isTesting = uiState.isTestingConnection,
                            connectionMessage = uiState.connectionMessage,
                            onTest = viewModel::testGeminiConnection
                        )
                    }

                    item {
                        AutoVisionToggle(
                            enabled = uiState.autoVisionEnabled,
                            apiKeyConfigured = uiState.geminiApiKey.isNotBlank(),
                            onToggle = { viewModel.setAutoVisionEnabled(it) }
                        )
                    }
                }
                "Datos" -> {
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
                                restoreBackupLauncher.launch("application/zip")
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
                            systemTotalBytes = uiState.systemTotalBytes,
                            systemFreeBytes = uiState.systemFreeBytes,
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
                }
                "Acerca de" -> {
                    item {
                        AppUpdateCard(
                            uiState = uiState,
                            onAutoCheckChanged = viewModel::setAutoCheckUpdates,
                            onCheck = viewModel::checkForUpdates,
                            onDownload = viewModel::downloadUpdate,
                            onInstall = viewModel::installDownloadedUpdate
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
                        DeckCard(modifier = Modifier.fillMaxWidth()) {
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
                        DeckCard(modifier = Modifier.fillMaxWidth()) {
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
    }
}

@Composable
private fun AppUpdateCard(
    uiState: SettingsUiState,
    onAutoCheckChanged: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    DeckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Actualizaciones", style = MaterialTheme.typography.titleMedium)
                    Text("Versiones publicadas en GitHub Releases", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Buscar al abrir esta sección", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = uiState.autoCheckUpdates, onCheckedChange = onAutoCheckChanged)
            }

            uiState.appUpdateMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.appUpdateStatus == AppUpdateStatus.DOWNLOADING) {
                val progress = uiState.appUpdateProgress
                if (progress == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    Text("$progress%", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            uiState.appUpdateInfo?.let { update ->
                if (update.releaseNotes.isNotBlank()) {
                    Text(update.releaseNotes, style = MaterialTheme.typography.bodySmall,
                        maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (uiState.appUpdateStatus == AppUpdateStatus.AVAILABLE) {
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Descargar ${update.version}")
                    }
                }
            }

            uiState.downloadedUpdateFile?.let {
                Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.SystemUpdateAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.installPermissionRequired) "Permitir e instalar" else "Instalar actualización")
                }
            }

            OutlinedButton(
                onClick = onCheck,
                enabled = uiState.appUpdateStatus != AppUpdateStatus.CHECKING &&
                    uiState.appUpdateStatus != AppUpdateStatus.DOWNLOADING,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.appUpdateStatus == AppUpdateStatus.CHECKING) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Buscar actualizaciones")
            }

            Text(
                "La instalación la confirma Android. Las versiones deben estar firmadas con la misma clave que la app instalada.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageSummaryCard(
    totalSizeBytes: Long,
    systemTotalBytes: Long,
    systemFreeBytes: Long,
    deckCount: Int,
    isClearing: Boolean,
    onClearCache: () -> Unit
) {
    val usedByAppPercent = if (systemTotalBytes > 0) totalSizeBytes.toFloat() / systemTotalBytes else 0f
    val otherUsedPercent = if (systemTotalBytes > 0) (systemTotalBytes - systemFreeBytes - totalSizeBytes).toFloat() / systemTotalBytes else 0f

    DeckCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Biblioteca: ${formatBytes(totalSizeBytes)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "$deckCount recursos guardados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }

                if (isClearing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    DeckOutlinedButton(
                        onClick = onClearCache,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Vaciar caché", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.fillMaxSize()) {
                        // Otros (gris/oscuro)
                        Box(Modifier.fillMaxHeight().fillMaxWidth(otherUsedPercent).background(MaterialTheme.colorScheme.outlineVariant))
                        // App (Primary)
                        Box(Modifier.fillMaxHeight().fillMaxWidth(if (1f - otherUsedPercent > 0) usedByAppPercent / (1f - otherUsedPercent) else 0f).background(MaterialTheme.colorScheme.primary))
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(4.dp))
                        Text("Esta App", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Libre: ${formatBytes(systemFreeBytes)} de ${formatBytes(systemTotalBytes)}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    DeckCard(modifier = Modifier.fillMaxWidth()) {
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
private fun AutoVisionToggle(
    enabled: Boolean,
    apiKeyConfigured: Boolean,
    onToggle: (Boolean) -> Unit
) {
    DeckCard(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun SimplifiedModeToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    DeckCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Modo Simplificado (Roleplay Directo)", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Agiliza flujos de trabajo: clic en mazo para robar directamente, menos pasos para acciones comunes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
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
    DeckCard(modifier = Modifier.fillMaxWidth()) {
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
                    DeckButton(
                        onClick = onCreateBackup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Crear Backup")
                    }
                    DeckOutlinedButton(
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeSelectorCard(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    DeckCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tema visual",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Elige la atmósfera visual para tus partidas. El tema Pergamino viste la app con tonos cálidos y texturas de papel antiguo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionButton(
                    title = "Oscuro",
                    subtitle = "OLED",
                    icon = DeckIcons.Moon,
                    isSelected = currentTheme == AppTheme.DARK,
                    onClick = { onThemeSelected(AppTheme.DARK) },
                    modifier = Modifier.widthIn(min = 120.dp)
                )

                ThemeOptionButton(
                    title = "Pergamino",
                    subtitle = "TTRPG",
                    icon = DeckIcons.Journal,
                    isSelected = currentTheme == AppTheme.PARCHMENT,
                    onClick = { onThemeSelected(AppTheme.PARCHMENT) },
                    modifier = Modifier.widthIn(min = 120.dp)
                )

                ThemeOptionButton(
                    title = "Claro",
                    subtitle = "Moderno",
                    icon = DeckIcons.Sun,
                    isSelected = currentTheme == AppTheme.LIGHT,
                    onClick = { onThemeSelected(AppTheme.LIGHT) },
                    modifier = Modifier.widthIn(min = 120.dp)
                )
                ThemeOptionButton(title = "Pergamino nocturno", subtitle = "Tinta marfil", icon = DeckIcons.Candle,
                    isSelected = currentTheme == AppTheme.PARCHMENT_NIGHT,
                    onClick = { onThemeSelected(AppTheme.PARCHMENT_NIGHT) }, modifier = Modifier.widthIn(min = 120.dp))
            }
            ThemeComponentsPreview()
        }
    }
}

@Composable
private fun ThemeOptionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
