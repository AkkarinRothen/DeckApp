package com.deckapp.feature.settings

import com.deckapp.core.domain.repository.AiTableRepository

import com.deckapp.core.model.GeneralPreferences

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.usecase.backup.CreateBackupUseCase
import com.deckapp.core.domain.usecase.backup.RestoreBackupUseCase
import com.deckapp.core.model.AppTheme
import com.deckapp.core.model.TableAppearance
import com.deckapp.core.model.TableDensity
import com.deckapp.core.model.TextureIntensity
import com.deckapp.core.model.ThemeAccent
import com.deckapp.core.model.AppearancePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class DeckStorageInfo(
    val id: Long,
    val name: String,
    val sizeBytes: Long
)

data class AppUpdateInfo(
    val version: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val downloadUrl: String
)

enum class AppUpdateStatus { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, DOWNLOADED, ERROR }
private class NoPublishedReleaseException : Exception()

data class SettingsUiState(
    val generalPreferences: GeneralPreferences = GeneralPreferences(),
    val isTestingConnection: Boolean = false,
    val connectionMessage: String? = null,
    val appVersion: String = "",
    val decksStorage: List<DeckStorageInfo> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val isLoading: Boolean = true,
    val isClearingCache: Boolean = false,
    val cacheClearedMessage: String? = null,
    val appTheme: AppTheme = AppTheme.DARK,
    val tableAppearance: TableAppearance = TableAppearance(),
    val appearancePresets: List<AppearancePreset> = emptyList(),
    val jpegQuality: Int = 90,
    val geminiApiKey: String = "",
    val autoVisionEnabled: Boolean = true,
    val simplifiedModeEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupMessage: String? = null,
    val systemTotalBytes: Long = 0L,
    val systemFreeBytes: Long = 0L,
    val pendingBackupInspection: com.deckapp.core.domain.repository.BackupInspectionData? = null,
    val pendingRestoreUri: android.net.Uri? = null,
    val appUpdateStatus: AppUpdateStatus = AppUpdateStatus.IDLE,
    val appUpdateInfo: AppUpdateInfo? = null,
    val appUpdateMessage: String? = null,
    val appUpdateProgress: Int? = null,
    val downloadedUpdateFile: File? = null,
    val autoCheckUpdates: Boolean = false,
    val installPermissionRequired: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val aiRepository: AiTableRepository,
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val updatePreferences = context.getSharedPreferences("app_update_preferences", Context.MODE_PRIVATE)
    private var updateJob: kotlinx.coroutines.Job? = null

    init {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
        val quality = settingsRepository.getJpegQuality()
        val apiKey = settingsRepository.getGeminiApiKey()
        val autoVision = settingsRepository.getAutoVisionEnabled()
        val simplified = settingsRepository.getSimplifiedModeEnabled()
        val theme = settingsRepository.getAppTheme()
        _uiState.update { 
            it.copy(
                generalPreferences = settingsRepository.getGeneralPreferences(),
                appVersion = version, 
                jpegQuality = quality, 
                geminiApiKey = apiKey, 
                autoVisionEnabled = autoVision,
                simplifiedModeEnabled = simplified,
                appTheme = theme,
                tableAppearance = settingsRepository.getTableAppearance(),
                appearancePresets = settingsRepository.getAppearancePresets(),
                autoCheckUpdates = updatePreferences.getBoolean("auto_check_updates", false)
            ) 
        }
        viewModelScope.launch {
            settingsRepository.observeGeneralPreferences().collect { preferences ->
                _uiState.update { it.copy(generalPreferences = preferences) }
            }
        }
        loadStorageInfo()
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        updatePreferences.edit().putBoolean("auto_check_updates", enabled).apply()
        _uiState.update { it.copy(autoCheckUpdates = enabled) }
    }

    fun checkForUpdates() {
        if (_uiState.value.appUpdateStatus == AppUpdateStatus.CHECKING ||
            _uiState.value.appUpdateStatus == AppUpdateStatus.DOWNLOADING) return
        updateJob?.cancel()
        _uiState.update {
            it.copy(appUpdateStatus = AppUpdateStatus.CHECKING, appUpdateMessage = null,
                appUpdateInfo = null, downloadedUpdateFile = null, appUpdateProgress = null,
                installPermissionRequired = false)
        }
        updateJob = viewModelScope.launch {
            try {
                val update = withContext(Dispatchers.IO) { fetchLatestUpdate() }
                if (update == null) {
                    _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.CURRENT, appUpdateMessage = "No se encontró una versión más nueva en GitHub Releases.") }
                } else {
                    _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.AVAILABLE, appUpdateInfo = update,
                        appUpdateMessage = "Está disponible la versión ${update.version}.") }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: NoPublishedReleaseException) {
                _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.CURRENT,
                    appUpdateMessage = "Todavía no hay una versión publicada en GitHub Releases.") }
            } catch (e: Exception) {
                val message = e.message ?: "No se pudo consultar GitHub Releases."
                _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.ERROR, appUpdateMessage = message) }
            }
        }
    }

    private fun fetchLatestUpdate(): AppUpdateInfo? {
        val connection = (URL("https://api.github.com/repos/AkkarinRothen/deckapp/releases/latest").openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "DeckApp-Android-Updater")
        }
        try {
            val statusCode = connection.responseCode
            if (statusCode == 404) throw NoPublishedReleaseException()
            if (statusCode !in 200..299) error("GitHub respondió con el código $statusCode.")
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (json.optBoolean("draft", true) || json.optBoolean("prerelease", false)) return null

            val version = json.optString("tag_name").removePrefix("v").removePrefix("V")
            val localVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
            if (!isVersionNewer(version, localVersion)) return null

            val assets = json.optJSONArray("assets") ?: error("La versión $version no tiene archivos descargables.")
            var downloadUrl: String? = null
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                val name = asset.optString("name")
                if (name.equals("DeckApp.apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url")
                    break
                }
            }
            val releaseUrl = json.optString("html_url")
            val safeDownloadUrl = downloadUrl?.takeIf {
                it.startsWith("https://github.com/AkkarinRothen/deckapp/releases/download/", ignoreCase = true)
            }
                ?: error("La versión $version no incluye el APK DeckApp.apk esperado.")
            return AppUpdateInfo(version, json.optString("body").take(1200), releaseUrl, safeDownloadUrl)
        } finally {
            connection.disconnect()
        }
    }

    private fun isVersionNewer(remote: String, local: String): Boolean {
        fun parts(value: String): List<Int>? = Regex("\\d+").findAll(value.substringBefore('-').substringBefore('+'))
            .mapNotNull { it.value.toIntOrNull() }.toList().takeIf { it.isNotEmpty() }
        val remoteParts = parts(remote) ?: return false
        val localParts = parts(local) ?: return false
        for (index in 0 until maxOf(remoteParts.size, localParts.size)) {
            val comparison = (remoteParts.getOrElse(index) { 0 }).compareTo(localParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    fun downloadUpdate() {
        val update = _uiState.value.appUpdateInfo ?: return
        if (_uiState.value.appUpdateStatus == AppUpdateStatus.DOWNLOADING) return
        updateJob?.cancel()
        _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.DOWNLOADING, appUpdateProgress = 0, appUpdateMessage = "Descargando ${update.version}…") }
        updateJob = viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { downloadUpdateFile(update) }
                val archiveInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_META_DATA)
                    ?: error("El archivo descargado no es un APK válido.")
                if (archiveInfo.packageName != context.packageName) error("El APK no corresponde a esta aplicación.")
                val installedInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val newCode = if (Build.VERSION.SDK_INT >= 28) archiveInfo.longVersionCode else archiveInfo.versionCode.toLong()
                val installedCode = if (Build.VERSION.SDK_INT >= 28) installedInfo.longVersionCode else installedInfo.versionCode.toLong()
                if (newCode <= installedCode) error("El APK descargado no tiene un número de versión superior al instalado.")
                _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.DOWNLOADED, downloadedUpdateFile = file,
                    appUpdateProgress = 100, appUpdateMessage = "Descarga completa. Podés instalar la actualización.") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.ERROR, appUpdateMessage = e.message ?: "No se pudo descargar la actualización.", appUpdateProgress = null) }
            }
        }
    }

    private suspend fun downloadUpdateFile(update: AppUpdateInfo): File {
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "DeckApp-Android-Updater")
            setRequestProperty("Accept", "application/octet-stream")
        }
        val targetDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "updates")
        if (!targetDir.exists() && !targetDir.mkdirs()) error("No se pudo preparar el almacenamiento para la actualización.")
        val target = File(targetDir, "DeckApp-${update.version}.apk")
        val partial = File(targetDir, "${target.name}.part")
        try {
            if (connection.responseCode !in 200..299) error("No se pudo descargar el APK (HTTP ${connection.responseCode}).")
            val contentLength = connection.contentLengthLong
            if (contentLength > 600L * 1024 * 1024) error("El APK supera el tamaño permitido para la descarga.")
            connection.inputStream.use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var received = 0L
                    var lastReportedAt = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        if (received > 600L * 1024 * 1024) error("El APK supera el tamaño permitido para la descarga.")
                        output.write(buffer, 0, count)
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (contentLength > 0 && now - lastReportedAt > 150) {
                            val progress = ((received * 100) / contentLength).toInt().coerceIn(0, 99)
                            _uiState.update { it.copy(appUpdateProgress = progress) }
                            lastReportedAt = now
                        }
                    }
                }
            }
            if (contentLength > 0 && partial.length() != contentLength) error("La descarga quedó incompleta.")
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) error("No se pudo guardar el APK descargado.")
            return target
        } catch (e: Exception) {
            partial.delete()
            throw e
        } finally {
            connection.disconnect()
        }
    }

    fun installDownloadedUpdate() {
        val file = _uiState.value.downloadedUpdateFile ?: return
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            _uiState.update { it.copy(installPermissionRequired = true, appUpdateMessage = "Android necesita que autorices instalaciones para DeckApp.") }
            openInstallPermissionSettings()
            return
        }
        try {
            _uiState.update { it.copy(installPermissionRequired = false) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _uiState.update { it.copy(appUpdateStatus = AppUpdateStatus.ERROR, appUpdateMessage = e.message ?: "Android no pudo abrir el instalador.") }
        }
    }

    private fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT < 26) return
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun setGeneralPreferences(preferences: GeneralPreferences) {
        settingsRepository.setGeneralPreferences(preferences)
    }

    private var connectionVersion = 0
    private var connectionJob: kotlinx.coroutines.Job? = null

    fun testGeminiConnection() {
        if (_uiState.value.isTestingConnection) return
        val key = _uiState.value.geminiApiKey
        if (key.isBlank()) return
        _uiState.update { it.copy(isTestingConnection = true, connectionMessage = null) }
        val version = connectionVersion
        connectionJob = viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeout(30_000) { aiRepository.testConnection(key) }
                if (version == connectionVersion) _uiState.update { it.copy(connectionMessage = "Conexión comprobada con esta clave.") }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                if (version == connectionVersion) _uiState.update { it.copy(connectionMessage = "La prueba agotó el tiempo de espera. Intenta nuevamente.") }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                if (version == connectionVersion) _uiState.update { it.copy(connectionMessage = "No se pudo comprobar la conexión. Revisa la clave, la red y la cuota de Gemini.") }
            } finally {
                if (version == connectionVersion) _uiState.update { it.copy(isTestingConnection = false) }
            }
        }
    }

    fun setAppTheme(theme: AppTheme) {
        settingsRepository.setAppTheme(theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun setTextureIntensity(texture: TextureIntensity) {
        val appearance = settingsRepository.getTableAppearance().copy(texture = texture)
        settingsRepository.setTableAppearance(appearance)
        _uiState.update { it.copy(tableAppearance = appearance) }
    }

    fun setTableDensity(density: TableDensity) {
        val appearance = settingsRepository.getTableAppearance().copy(density = density)
        settingsRepository.setTableAppearance(appearance)
        _uiState.update { it.copy(tableAppearance = appearance) }
    }

    fun setThemeAccent(accent: ThemeAccent) {
        val appearance = settingsRepository.getTableAppearance().copy(accent = accent)
        settingsRepository.setTableAppearance(appearance)
        _uiState.update { it.copy(tableAppearance = appearance) }
    }

    fun saveAppearancePreset(name: String) {
        settingsRepository.saveAppearancePreset(name)
        _uiState.update { it.copy(appearancePresets = settingsRepository.getAppearancePresets()) }
    }

    fun applyAppearancePreset(id: String) {
        settingsRepository.applyAppearancePreset(id)
        _uiState.update { it.copy(appTheme = settingsRepository.getAppTheme(), tableAppearance = settingsRepository.getTableAppearance()) }
    }

    fun deleteAppearancePreset(id: String) {
        settingsRepository.deleteAppearancePreset(id)
        _uiState.update { it.copy(appearancePresets = settingsRepository.getAppearancePresets()) }
    }

    fun loadStorageInfo() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val decks = cardRepository.getAllDecks().first()
            val decksDir = File(context.filesDir, "decks")

            val storageList = decks.map { deck ->
                val deckDir = File(decksDir, deck.id.toString())
                val sizeBytes = if (deckDir.exists())
                    deckDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                else 0L
                DeckStorageInfo(id = deck.id, name = deck.name, sizeBytes = sizeBytes)
            }.sortedByDescending { it.sizeBytes }

            val internalStorage = android.os.StatFs(context.filesDir.absolutePath)
            val totalSystem = internalStorage.blockCountLong * internalStorage.blockSizeLong
            val freeSystem = internalStorage.availableBlocksLong * internalStorage.blockSizeLong

            _uiState.update {
                it.copy(
                    decksStorage = storageList,
                    totalSizeBytes = storageList.sumOf { d -> d.sizeBytes },
                    systemTotalBytes = totalSystem,
                    systemFreeBytes = freeSystem,
                    isLoading = false
                )
            }
        }
    }

    fun clearCache() {
        _uiState.update { it.copy(isClearingCache = true, cacheClearedMessage = null) }
        viewModelScope.launch {
            fileRepository.clearCache()
            _uiState.update { it.copy(isClearingCache = false, cacheClearedMessage = "Caché limpiada") }
            loadStorageInfo()
        }
    }

    fun setJpegQuality(quality: Int) {
        settingsRepository.setJpegQuality(quality)
        _uiState.update { it.copy(jpegQuality = quality) }
    }
    
    fun setGeminiApiKey(key: String) {
        connectionVersion++
        connectionJob?.cancel()
        val trimmed = key.trim()
        settingsRepository.setGeminiApiKey(trimmed)
        _uiState.update { it.copy(geminiApiKey = trimmed, connectionMessage = null, isTestingConnection = false) }
    }

    fun setAutoVisionEnabled(enabled: Boolean) {
        settingsRepository.setAutoVisionEnabled(enabled)
        _uiState.update { it.copy(autoVisionEnabled = enabled) }
    }

    fun setSimplifiedModeEnabled(enabled: Boolean) {
        settingsRepository.setSimplifiedModeEnabled(enabled)
        _uiState.update { it.copy(simplifiedModeEnabled = enabled) }
    }

    fun createBackup(outputUri: Uri) {
        _uiState.update { it.copy(isBackingUp = true, backupMessage = "Generando archivo de backup...") }
        viewModelScope.launch {
            val result = createBackupUseCase(outputUri)
            _uiState.update { 
                it.copy(
                    isBackingUp = false, 
                    backupMessage = if (result.isSuccess) "Backup creado exitosamente." else "Error al crear backup: ${result.exceptionOrNull()?.message}"
                ) 
            }
        }
    }

    fun inspectBackup(uri: Uri) {
        _uiState.update { it.copy(isRestoring = true, backupMessage = "Analizando archivo de copia de seguridad...") }
        viewModelScope.launch {
            val result = fileRepository.inspectBackupZip(uri)
            _uiState.update { state ->
                state.copy(
                    isRestoring = false,
                    pendingBackupInspection = result.getOrNull(),
                    pendingRestoreUri = if (result.isSuccess) uri else null,
                    backupMessage = if (result.isFailure) "Error al inspeccionar backup: ${result.exceptionOrNull()?.message}" else null
                )
            }
        }
    }

    fun dismissBackupInspection() {
        _uiState.update { it.copy(pendingBackupInspection = null, pendingRestoreUri = null) }
    }

    fun confirmRestore() {
        val uri = _uiState.value.pendingRestoreUri ?: return
        dismissBackupInspection()
        restoreBackup(uri)
    }

    fun restoreBackup(zipUri: Uri) {
        _uiState.update { it.copy(isRestoring = true, backupMessage = "Restaurando biblioteca... No cierres la app.") }
        viewModelScope.launch {
            val result = restoreBackupUseCase(zipUri)
            _uiState.update { 
                it.copy(
                    isRestoring = false, 
                    backupMessage = if (result.isSuccess) "Biblioteca restaurada correctamente." else "Error al restaurar: ${result.exceptionOrNull()?.message}"
                ) 
            }
            if (result.isSuccess) {
                loadStorageInfo()
            }
        }
    }

    fun clearMessage() = _uiState.update { it.copy(cacheClearedMessage = null, backupMessage = null) }
}
