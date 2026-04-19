package com.deckapp.feature.reference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.usecase.reference.InstallStarterPackUseCase
import com.deckapp.core.domain.usecase.reference.RemoveStarterPackUseCase
import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.backup.FullBackupDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class ReferenceTabUiState(
    val referenceTables: List<ReferenceTable> = emptyList(),
    val systemRules: List<SystemRule> = emptyList(),
    val activeSystemFilters: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showStarterPackDialog: Boolean = false,
    val availablePacks: List<StarterPackInfo> = emptyList()
)

@HiltViewModel
class ReferenceTabViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val installStarterPackUseCase: InstallStarterPackUseCase,
    private val removeStarterPackUseCase: RemoveStarterPackUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val _uiState = MutableStateFlow(ReferenceTabUiState())
    val uiState: StateFlow<ReferenceTabUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _showStarterPackDialog = MutableStateFlow(false)

    val snackbarMessage = MutableStateFlow<String?>(null)

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            referenceRepository.getAllReferenceTables(),
            referenceRepository.getAllSystemRules(),
            referenceRepository.getInstalledPackNames(),
            _searchQuery,
            _showStarterPackDialog
        ) { tables, rules, installed, query, showDialog ->
            
            // Escaneo dinámico de assets
            val availableAssets = context.assets.list("starter_packs") ?: emptyArray()
            val packsInfo = availableAssets.filter { it.endsWith(".json") }.map { fileName ->
                StarterPackInfo(
                    assetName = fileName,
                    displayName = fileName.removeSuffix(".json").replace("_", " ").split(" ").joinToString(" ") { it.capitalize() },
                    isInstalled = installed.contains(fileName)
                )
            }

            _uiState.update { 
                it.copy(
                    referenceTables = tables,
                    systemRules = rules,
                    searchQuery = query,
                    isLoading = false,
                    showStarterPackDialog = showDialog,
                    availablePacks = packsInfo
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setShowStarterPackDialog(show: Boolean) {
        _showStarterPackDialog.value = show
    }

    fun toggleSystemFilter(system: String) {
        _uiState.update { state ->
            val newFilters = if (state.activeSystemFilters.contains(system)) {
                state.activeSystemFilters - system
            } else {
                state.activeSystemFilters + system
            }
            state.copy(activeSystemFilters = newFilters)
        }
    }

    fun installPack(assetName: String) {
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("starter_packs/$assetName").bufferedReader().use { it.readText() }
                val pack = json.decodeFromString<FullBackupDto>(jsonString)
                installStarterPackUseCase(pack, assetName)
                _showStarterPackDialog.value = false
                snackbarMessage.value = "Pack instalado correctamente"
            } catch (e: Exception) {
                e.printStackTrace()
                snackbarMessage.value = "Error al instalar: ${e.message}"
            }
        }
    }

    fun removePack(assetName: String) {
        viewModelScope.launch {
            try {
                removeStarterPackUseCase(assetName)
                _showStarterPackDialog.value = false
                snackbarMessage.value = "Pack eliminado"
            } catch (e: Exception) {
                snackbarMessage.value = "Error al eliminar: ${e.message}"
            }
        }
    }

    fun clearSnackbar() {
        snackbarMessage.value = null
    }

    fun togglePinnedTable(tableId: Long, pinned: Boolean) {
        viewModelScope.launch {
            referenceRepository.updateTablePinned(tableId, pinned)
        }
    }

    fun togglePinnedRule(ruleId: Long, pinned: Boolean) {
        viewModelScope.launch {
            referenceRepository.updateRulePinned(ruleId, pinned)
        }
    }
}
