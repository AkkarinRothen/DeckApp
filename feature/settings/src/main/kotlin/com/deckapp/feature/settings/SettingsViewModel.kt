package com.deckapp.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    val jpegQuality: Int = 90
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val prefs = context.getSharedPreferences("deckapp_settings", Context.MODE_PRIVATE)

    init {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
        val quality = prefs.getInt("jpeg_quality", 90)
        _uiState.update { it.copy(appVersion = version, jpegQuality = quality) }
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

            _uiState.update {
                it.copy(
                    decksStorage = storageList,
                    totalSizeBytes = storageList.sumOf { d -> d.sizeBytes },
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
        prefs.edit().putInt("jpeg_quality", quality).apply()
        _uiState.update { it.copy(jpegQuality = quality) }
    }

    fun clearMessage() = _uiState.update { it.copy(cacheClearedMessage = null) }
}
