package com.deckapp.feature.hexploration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.hex.CreateHexMapUseCase
import com.deckapp.core.domain.usecase.hex.DeleteHexMapUseCase
import com.deckapp.core.domain.usecase.hex.GetHexMapsUseCase
import com.deckapp.core.model.HexMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HexMapListUiState(
    val maps: List<HexMap> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val newMapName: String = "",
    val newMapRows: String = "10",
    val newMapCols: String = "10",
    val isRadial: Boolean = false,
    val newMapRadius: String = "5"
)

@HiltViewModel
class HexMapListViewModel @Inject constructor(
    private val getHexMapsUseCase: GetHexMapsUseCase,
    private val createHexMapUseCase: CreateHexMapUseCase,
    private val deleteHexMapUseCase: DeleteHexMapUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HexMapListUiState())
    val uiState: StateFlow<HexMapListUiState> = _uiState.asStateFlow()

    init {
        getHexMapsUseCase()
            .onEach { maps -> _uiState.update { it.copy(maps = maps, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun dismissCreateDialog() = _uiState.update { it.copy(showCreateDialog = false, newMapName = "") }

    fun onNameChange(name: String) = _uiState.update { it.copy(newMapName = name) }
    fun onRowsChange(rows: String) = _uiState.update { it.copy(newMapRows = rows) }
    fun onColsChange(cols: String) = _uiState.update { it.copy(newMapCols = cols) }
    fun onRadialChange(radial: Boolean) = _uiState.update { it.copy(isRadial = radial) }
    fun onRadiusChange(radius: String) = _uiState.update { it.copy(newMapRadius = radius) }

    fun createMap(onCreated: (Long) -> Unit) {
        val state = _uiState.value
        if (state.newMapName.isBlank()) return

        val rows = state.newMapRows.toIntOrNull()?.coerceIn(3, 40) ?: 10
        val cols = state.newMapCols.toIntOrNull()?.coerceIn(3, 40) ?: 10
        val radius = state.newMapRadius.toIntOrNull()?.coerceIn(1, 25) ?: 5

        viewModelScope.launch {
            val id = createHexMapUseCase(
                name = state.newMapName.trim(),
                rows = rows,
                cols = cols,
                radius = if (state.isRadial) radius else null
            )
            _uiState.update { it.copy(showCreateDialog = false, newMapName = "") }
            onCreated(id)
        }
    }

    fun deleteMap(id: Long) {
        viewModelScope.launch { deleteHexMapUseCase(id) }
    }
}
