package com.deckapp.feature.reference

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.repository.ManualRepository
import com.deckapp.core.domain.usecase.reference.InstallStarterPackUseCase
import com.deckapp.core.domain.usecase.reference.RemoveStarterPackUseCase
import com.deckapp.core.model.ReferenceTable
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.Manual
import com.deckapp.core.model.backup.FullBackupDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class StarterPackInfo(
    val assetName: String,
    val displayName: String,
    val isInstalled: Boolean
)

data class ReferenceListUiState(
    val tables: List<ReferenceTable> = emptyList(),
    val rules: List<SystemRule> = emptyList(),
    val manuals: List<Manual> = emptyList(),
    val availableSystems: List<String> = emptyList(),
    val activeSystemFilters: Set<String> = emptySet(),
    val searchQuery: String = "",
    val activePage: Int = 0,
    val isLoading: Boolean = false,
    val showStarterPackDialog: Boolean = false,
    val installedPacks: Set<String> = emptySet(),
    val availablePacks: List<StarterPackInfo> = emptyList()
)

@HiltViewModel
class ReferenceListViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository,
    private val manualRepository: ManualRepository,
    private val installStarterPackUseCase: InstallStarterPackUseCase,
    private val removeStarterPackUseCase: RemoveStarterPackUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val _searchQuery = MutableStateFlow("")
    private val _activeFilters = MutableStateFlow(setOf<String>())
    private val _activePage = MutableStateFlow(0)
    private val _showStarterPackDialog = MutableStateFlow(false)

    val uiState: StateFlow<ReferenceListUiState> = combine(
        referenceRepository.getAllReferenceTables(),
        referenceRepository.getAllSystemRules(),
        manualRepository.getAllManuals(),
        referenceRepository.getDistinctSystems(),
        referenceRepository.getInstalledPackNames(),
        _searchQuery,
        _activeFilters,
        _activePage,
        _showStarterPackDialog
    ) { flows ->
        val tables = flows[0] as List<ReferenceTable>
        val rules = flows[1] as List<SystemRule>
        val manuals = flows[2] as List<Manual>
        val systems = flows[3] as List<String>
        @Suppress("UNCHECKED_CAST")
        val installed = flows[4] as Set<String>
        val query = flows[5] as String
        val filters = flows[6] as Set<String>
        val page = flows[7] as Int
        val showStarter = flows[8] as Boolean

        val filteredTables = tables.filter { table ->
            (filters.isEmpty() || filters.contains(table.gameSystem)) &&
            (query.isBlank() || table.name.contains(query, ignoreCase = true) || table.description.contains(query, ignoreCase = true))
        }

        val filteredRules = rules.filter { rule ->
            (filters.isEmpty() || filters.contains(rule.gameSystem)) &&
            (query.isBlank() || rule.title.contains(query, ignoreCase = true) || rule.content.contains(query, ignoreCase = true))
        }

        val filteredManuals = manuals.filter { manual ->
            (filters.isEmpty() || filters.contains(manual.gameSystem)) &&
            (query.isBlank() || manual.title.contains(query, ignoreCase = true))
        }

        val availableAssets = context.assets.list("starter_packs") ?: emptyArray()
        val packsInfo = availableAssets.filter { it.endsWith(".json") }.map { fileName ->
            StarterPackInfo(
                assetName = fileName,
                displayName = fileName.removeSuffix(".json").replace("_", " ")
                    .split(" ").joinToString(" ") { word ->
                        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    },
                isInstalled = installed.contains(fileName)
            )
        }

        ReferenceListUiState(
            tables = filteredTables,
            rules = filteredRules,
            manuals = filteredManuals,
            availableSystems = systems,
            activeSystemFilters = filters,
            searchQuery = query,
            activePage = page,
            isLoading = false,
            showStarterPackDialog = showStarter,
            installedPacks = installed,
            availablePacks = packsInfo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReferenceListUiState(isLoading = true))

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSystemFilter(system: String) {
        val current = _activeFilters.value
        _activeFilters.value = if (current.contains(system)) current - system else current + system
    }

    fun setPage(page: Int) {
        _activePage.value = page
    }

    fun setShowStarterPackDialog(show: Boolean) {
        _showStarterPackDialog.value = show
    }

    fun addManual(title: String, uri: String, system: String) {
        viewModelScope.launch {
            manualRepository.saveManual(Manual(title = title, uri = uri, gameSystem = system))
        }
    }

    fun deleteManual(manual: Manual) {
        viewModelScope.launch {
            manualRepository.deleteManual(manual)
        }
    }

    fun installPack(assetName: String) {
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("starter_packs/$assetName").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
                val pack = json.decodeFromString<FullBackupDto>(jsonString)
                installStarterPackUseCase(pack, assetName)
                _showStarterPackDialog.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removePack(assetName: String) {
        viewModelScope.launch {
            try {
                removeStarterPackUseCase(assetName)
                _showStarterPackDialog.value = false
            } catch (e: Exception) { }
        }
    }

    fun deleteTable(id: Long) {
        viewModelScope.launch { referenceRepository.deleteReferenceTable(id) }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { referenceRepository.deleteSystemRule(id) }
    }

    fun togglePinnedTable(id: Long, pinned: Boolean) {
        viewModelScope.launch { referenceRepository.updateTablePinned(id, pinned) }
    }

    fun togglePinnedRule(id: Long, pinned: Boolean) {
        viewModelScope.launch { referenceRepository.updateRulePinned(id, pinned) }
    }
}
