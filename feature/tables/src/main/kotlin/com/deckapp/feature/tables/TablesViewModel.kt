package com.deckapp.feature.tables

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.CardRepository
import android.content.Context
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.domain.usecase.ExportTableUseCase
import com.deckapp.core.domain.usecase.InvertTableRangesUseCase
import com.deckapp.core.domain.usecase.RollTableUseCase
import com.deckapp.core.domain.usecase.AddQuickNoteUseCase
import com.deckapp.core.domain.usecase.reference.InstallStarterPackUseCase
import com.deckapp.core.domain.usecase.reference.RemoveStarterPackUseCase
import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableBundle
import com.deckapp.core.model.Tag
import com.deckapp.core.model.TableRollResult
import com.deckapp.core.model.backup.FullBackupDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

enum class ExportFormat { CSV, MARKDOWN }

data class TablePackInfo(
    val assetName: String,
    val displayName: String,
    val isInstalled: Boolean
)

data class TablesUiState(
    val tables: List<RandomTable> = emptyList(),
    val filteredTables: List<RandomTable> = emptyList(),
    val pinnedTables: List<RandomTable> = emptyList(),
    val recentTables: List<RandomTable> = emptyList(),
    val bundles: List<TableBundle> = emptyList(),
    val groupedTables: Map<String, List<RandomTable>> = emptyMap(),
    val allTags: List<Tag> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedCategory: String? = null,
    val selectedTableIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val activeTable: RandomTable? = null,
    val sessionTableIds: Set<Long> = emptySet(),
    val showAllTables: Boolean = true,
    val isGridView: Boolean = false,
    val lastResult: TableRollResult? = null,
    val recentResults: List<TableRollResult> = emptyList(),
    val isLoading: Boolean = true,
    val isRolling: Boolean = false,
    val snackbarMessage: String? = null,
    val exportData: String? = null,
    val exportFilename: String? = null,
    val isReorderMode: Boolean = false,
    val showPackDialog: Boolean = false,
    val availableTablePacks: List<TablePackInfo> = emptyList(),
    val quickRollResults: Map<Long, TableRollResult> = emptyMap(),
    val sessionRollLog: List<TableRollResult> = emptyList(),
    val rollLogExpanded: Boolean = true,
    val isSimplifiedMode: Boolean = false
)

@HiltViewModel
class TablesViewModel @Inject constructor(
    private val tableRepository: TableRepository,
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val rollTableUseCase: RollTableUseCase,
    private val exportTableUseCase: ExportTableUseCase,
    private val invertTableRangesUseCase: InvertTableRangesUseCase,
    private val referenceRepository: ReferenceRepository,
    private val settingsRepository: com.deckapp.core.domain.repository.SettingsRepository,
    private val installStarterPackUseCase: InstallStarterPackUseCase,
    private val removeStarterPackUseCase: RemoveStarterPackUseCase,
    private val addQuickNoteUseCase: AddQuickNoteUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(TablesUiState())
    val uiState: StateFlow<TablesUiState> = _uiState.asStateFlow()

    private val _sessionId = MutableStateFlow<Long?>(null)

    init {
        _uiState.update { it.copy(isSimplifiedMode = settingsRepository.getSimplifiedModeEnabled()) }
        loadBundledTablesIfEmpty()
        loadData()
        observeInstalledPacks()
    }

    private fun observeInstalledPacks() {
        viewModelScope.launch {
            referenceRepository.getInstalledPackNames().collect { installed ->
                val allAssets = context.assets.list("starter_packs") ?: emptyArray()
                val tablePacks = allAssets
                    .filter { it.endsWith(".json") && it.startsWith("tables_") }
                    .map { fileName ->
                        TablePackInfo(
                            assetName = fileName,
                            displayName = fileName.removeSuffix(".json")
                                .removePrefix("tables_")
                                .replace("_", " ")
                                .split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } },
                            isInstalled = installed.contains(fileName)
                        )
                    }
                _uiState.update { it.copy(availableTablePacks = tablePacks) }
            }
        }
    }

    private fun loadBundledTablesIfEmpty() {
        viewModelScope.launch {
            try {
                if (tableRepository.countBuiltInTables() == 0) {
                    val json = Json { ignoreUnknownKeys = true }
                    val assetName = "tables_adventure.json"
                    val jsonString = context.assets.open("starter_packs/$assetName").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
                    val pack = json.decodeFromString<FullBackupDto>(jsonString)
                    referenceRepository.importStarterPack(pack, assetName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setSessionId(id: Long?) {
        if (_sessionId.value == id) return
        _sessionId.value = id
        
        // Cargar tablas de la sesión y log de resultados si hay sesión
        id?.let { sid ->
            viewModelScope.launch {
                sessionRepository.getTablesForSession(sid).collect { tables ->
                    _uiState.update { it.copy(
                        sessionTableIds = tables.map { t -> t.id }.toSet(),
                        showAllTables = false // Por defecto mostrar solo las de la sesión si estamos en una
                    ) }
                }
            }
            
            viewModelScope.launch {
                sessionRepository.getEventsForSession(sid).collect { events ->
                    // Filtrar eventos de tablas y convertirlos a resultados
                    // (Lógica simplificada para el log de la sesión)
                }
            }
        }
    }

    private fun loadData() {
        combine(
            tableRepository.getAllTables(),
            tableRepository.getAllBundles(),
            cardRepository.getAllTags(),
            _uiState.map { it.searchQuery }.distinctUntilChanged(),
            _uiState.map { it.selectedTagIds }.distinctUntilChanged(),
            _uiState.map { it.selectedCategory }.distinctUntilChanged(),
            _uiState.map { it.showAllTables }.distinctUntilChanged(),
            _uiState.map { it.sessionTableIds }.distinctUntilChanged()
        ) { args ->
            @Suppress("UNCHECKED_CAST")
            computeNewState(
                tables = args[0] as List<RandomTable>,
                bundles = args[1] as List<TableBundle>,
                tags = args[2] as List<Tag>,
                query = args[3] as String,
                selectedTagIds = args[4] as Set<Long>,
                selectedCategory = args[5] as String?,
                showAll = args[6] as Boolean,
                sessionTableIds = args[7] as Set<Long>
            )
        }.onEach { newState ->
            _uiState.value = newState
        }.launchIn(viewModelScope)
    }

    private fun computeNewState(
        tables: List<RandomTable>,
        bundles: List<TableBundle>,
        tags: List<Tag>,
        query: String,
        selectedTagIds: Set<Long>,
        selectedCategory: String?,
        showAll: Boolean,
        sessionTableIds: Set<Long>
    ): TablesUiState {
        val bundleMap = bundles.associate { it.id to it.name }
        
        // Tablas ancladas (siempre de entre las disponibles en la vista actual)
        val pinned = tables.filter { it.isPinned && (showAll || it.id in sessionTableIds) }
        
        // Tablas recientes basadas en el log de la sesión
        val recentIds = _uiState.value.sessionRollLog.take(10).map { it.tableId }.distinct()
        val recent = tables.filter { it.id in recentIds }

        val filtered = tables.filter { table ->
            val matchesSession = showAll || table.id in sessionTableIds
            val matchesQuery = query.isEmpty() || 
                    table.name.contains(query, ignoreCase = true) ||
                    table.description.contains(query, ignoreCase = true)
            val matchesTags = selectedTagIds.isEmpty() || 
                    table.tags.any { it.id in selectedTagIds }
            val matchesCategory = selectedCategory == null || table.category == selectedCategory
            
            matchesSession && matchesQuery && matchesTags && matchesCategory
        }.map { table ->
            if (table.bundleName == null && table.bundleId != null) {
                table.copy(bundleName = bundleMap[table.bundleId])
            } else table
        }.sortedWith(compareBy({ it.category }, { it.sortOrder }, { it.name }))

        val categories = tables.map { it.category }.distinct().sorted()
        val grouped = filtered.groupBy { it.category }

        return _uiState.value.copy(
            tables = tables,
            filteredTables = filtered,
            pinnedTables = pinned,
            recentTables = recent,
            bundles = bundles,
            groupedTables = grouped,
            allTags = tags,
            availableCategories = categories,
            isLoading = false
        )
    }

    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    
    fun setSession(sessionId: Long?) {
        if (sessionId == null) {
            _uiState.update { it.copy(sessionTableIds = emptySet(), showAllTables = true, sessionRollLog = emptyList()) }
            return
        }
        viewModelScope.launch {
            sessionRepository.getTablesForSession(sessionId).collect { refs ->
                _uiState.update { state ->
                    state.copy(
                        sessionTableIds = refs.map { it.id }.toSet(),
                        showAllTables = false
                    )
                }
            }
        }
        viewModelScope.launch {
            tableRepository.getSessionRollLog(sessionId).collect { log ->
                _uiState.update { it.copy(sessionRollLog = log) }
            }
        }
    }

    fun toggleViewMode() = _uiState.update { it.copy(isGridView = !it.isGridView) }
    fun setShowAllTables(show: Boolean) = _uiState.update { it.copy(showAllTables = show) }

    fun setCategoryFilter(category: String?) = _uiState.update { it.copy(selectedCategory = category) }

    fun toggleTagFilter(tagId: Long) {
        _uiState.update { state ->
            val current = state.selectedTagIds
            val updated = if (tagId in current) current - tagId else current + tagId
            state.copy(selectedTagIds = updated)
        }
    }

    fun clearFilters() {
        _uiState.update { it.copy(selectedTagIds = emptySet(), searchQuery = "", selectedCategory = null) }
    }

    fun toggleTableSelection(tableId: Long) {
        _uiState.update { state ->
            val current = state.selectedTableIds
            val updated = if (tableId in current) current - tableId else current + tableId
            state.copy(selectedTableIds = updated)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedTableIds = emptySet()) }
    fun openTable(table: RandomTable) = _uiState.update { it.copy(activeTable = table) }
    fun closeTable() = _uiState.update { it.copy(activeTable = null) }

    fun togglePin(table: RandomTable) {
        viewModelScope.launch {
            tableRepository.updatePinnedState(table.id, !table.isPinned)
        }
    }

    fun bulkDelete() {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkDeleteTables(ids)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Eliminadas ${ids.size} tablas") }
        }
    }

    fun bulkTogglePin(pin: Boolean) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkUpdatePinnedState(ids, pin)
            _uiState.update { it.copy(selectedTableIds = emptySet()) }
        }
    }

    fun bulkAddTag(tagId: Long) {
        val ids = _uiState.value.selectedTableIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            tableRepository.bulkAddTagToTables(ids, tagId)
            _uiState.update { it.copy(selectedTableIds = emptySet(), snackbarMessage = "Etiqueta añadida a ${ids.size} tablas") }
        }
    }

    fun duplicateTable(tableId: Long) {
        viewModelScope.launch {
            val table = tableRepository.getTableWithEntries(tableId) ?: return@launch
            val newTable = table.copy(
                id = 0,
                name = "${table.name} (Copia)",
                isPinned = false,
                createdAt = System.currentTimeMillis()
            )
            tableRepository.saveTable(newTable)
            _uiState.update { it.copy(snackbarMessage = "Tabla duplicada") }
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            tableRepository.deleteTable(tableId)
            _uiState.update { it.copy(snackbarMessage = "Tabla eliminada") }
        }
    }

    fun invertTable(tableId: Long) {
        viewModelScope.launch {
            invertTableRangesUseCase(tableId)
            _uiState.update { it.copy(snackbarMessage = "Rangos invertidos") }
        }
    }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun rollTable(tableId: Long, sessionId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRolling = true) }
            try {
                val result = rollTableUseCase(tableId, sessionId)
                
                // Registrar en el historial automáticamente
                val tableName = _uiState.value.tables.find { it.id == tableId }?.name ?: "Tabla"
                addQuickNoteUseCase(
                    content = "[$tableName] Resultado: ${result.resolvedText}",
                    sessionId = sessionId
                )

                _uiState.update { state ->
                    state.copy(
                        lastResult = result,
                        isRolling = false,
                        quickRollResults = state.quickRollResults + (tableId to result)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRolling = false) }
            }
        }
    }

    fun clearQuickRoll(tableId: Long) {
        _uiState.update { it.copy(quickRollResults = it.quickRollResults - tableId) }
    }

    fun exportTable(format: ExportFormat) {
        val table = _uiState.value.activeTable ?: return
        val data = when (format) {
            ExportFormat.CSV -> exportTableUseCase.toCsv(table)
            ExportFormat.MARKDOWN -> exportTableUseCase.toMarkdown(table)
        }
        val extension = when (format) {
            ExportFormat.CSV -> "csv"
            ExportFormat.MARKDOWN -> "md"
        }
        _uiState.update { it.copy(
            exportData = data,
            exportFilename = "${table.name.replace(" ", "_")}.$extension"
        ) }
    }

    fun clearExportData() {
        _uiState.update { it.copy(exportData = null, exportFilename = null) }
    }

    fun loadRecentResults(sessionId: Long) {
        val tableId = _uiState.value.activeTable?.id ?: return
        viewModelScope.launch {
            tableRepository.getRecentResultsForTable(sessionId, tableId).collect { results ->
                _uiState.update { it.copy(recentResults = results) }
            }
        }
    }

    fun toggleReorderMode() {
        _uiState.update { it.copy(isReorderMode = !it.isReorderMode) }
    }

    fun updateTableOrder(newOrder: List<RandomTable>) {
        _uiState.update { it.copy(tables = newOrder) }
    }

    fun saveSortOrder() {
        viewModelScope.launch {
            val orderedIds = _uiState.value.tables.map { it.id }
            tableRepository.updateTablesSortOrder(orderedIds)
            _uiState.update { it.copy(isReorderMode = false, snackbarMessage = "Orden de tablas guardado") }
        }
    }

    fun toggleRollLogExpanded() = _uiState.update { it.copy(rollLogExpanded = !it.rollLogExpanded) }

    fun clearSessionRollLog(sessionId: Long) {
        viewModelScope.launch {
            tableRepository.clearSessionRollLog(sessionId)
        }
    }

    fun setShowPackDialog(show: Boolean) = _uiState.update { it.copy(showPackDialog = show) }

    fun installTablePack(assetName: String) {
        viewModelScope.launch {
            try {
                val json = Json { ignoreUnknownKeys = true }
                val jsonString = context.assets.open("starter_packs/$assetName").bufferedReader().use { it.readText() }.trimStart('\uFEFF')
                val pack = json.decodeFromString<FullBackupDto>(jsonString)
                installStarterPackUseCase(pack, assetName)
                _uiState.update { it.copy(showPackDialog = false, snackbarMessage = "Pack instalado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al instalar: ${e.message}") }
            }
        }
    }

    fun removeTablePack(assetName: String) {
        viewModelScope.launch {
            try {
                removeStarterPackUseCase(assetName)
                _uiState.update { it.copy(showPackDialog = false, snackbarMessage = "Pack eliminado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Error al eliminar: ${e.message}") }
            }
        }
    }
}
