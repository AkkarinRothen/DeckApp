package com.deckapp.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.domain.repository.SettingsRepository
import com.deckapp.core.domain.usecase.backup.CreateBackupUseCase
import com.deckapp.core.domain.usecase.backup.RestoreBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DeckStorageInfo(
    val id: Long,
    val name: String,
    val sizeBytes: Long
)

data class SettingsUiState(
    val appVersion: String = "",
    val decksStorage: List<DeckStorageInfo> = emptyList(),
    val totalSizeBytes: Long = 0L,
    val isLoading: Boolean = true,
    val isClearingCache: Boolean = false,
    val cacheClearedMessage: String? = null,
    val jpegQuality: Int = 90,
    val geminiApiKey: String = "",
    val autoVisionEnabled: Boolean = true,
    val simplifiedModeEnabled: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupMessage: String? = null,
    val systemTotalBytes: Long = 0L,
    val systemFreeBytes: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository,
    private val settingsRepository: SettingsRepository,
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
        val quality = settingsRepository.getJpegQuality()
        val apiKey = settingsRepository.getGeminiApiKey()
        val autoVision = settingsRepository.getAutoVisionEnabled()
        val simplified = settingsRepository.getSimplifiedModeEnabled()
        _uiState.update { 
            it.copy(
                appVersion = version, 
                jpegQuality = quality, 
                geminiApiKey = apiKey, 
                autoVisionEnabled = autoVision,
                simplifiedModeEnabled = simplified
            ) 
        }
        loadStorageInfo()
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
        settingsRepository.setGeminiApiKey(key)
        _uiState.update { it.copy(geminiApiKey = key) }
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
