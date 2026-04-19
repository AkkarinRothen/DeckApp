package com.deckapp.feature.npcs

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.GetNpcsUseCase
import com.deckapp.core.model.Npc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NpcListViewModel @Inject constructor(
    private val getNpcsUseCase: com.deckapp.core.domain.usecase.GetNpcsUseCase,
    private val saveNpcUseCase: com.deckapp.core.domain.usecase.SaveNpcUseCase,
    private val tableRepository: com.deckapp.core.domain.repository.TableRepository,
    private val rollTableUseCase: com.deckapp.core.domain.usecase.RollTableUseCase,
    private val settingsRepository: com.deckapp.core.domain.repository.SettingsRepository
) : ViewModel() {

    private val _isSimplifiedMode = mutableStateOf(settingsRepository.getSimplifiedModeEnabled())
    val isSimplifiedMode: State<Boolean> = _isSimplifiedMode

    val uiState: StateFlow<NpcListUiState> = getNpcsUseCase()
        .map { npcs -> NpcListUiState.Success(npcs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NpcListUiState.Loading
        )

    fun quickGenerateNpc(onDone: (String) -> Unit) {
        viewModelScope.launch {
            // Intentar obtener un nombre de las tablas integradas (tables_names.json suele tener IDs 25000+)
            val name = try {
                val tableId = 25000L // Tabla de nombres genérica
                val result = rollTableUseCase(tableId, null)
                result.resolvedText.split(" ").firstOrNull() ?: "Nuevo NPC"
            } catch (_: Exception) {
                "Nuevo NPC #${(System.currentTimeMillis() % 1000)}"
            }

            val newNpc = Npc(
                name = name,
                description = "Generado rápidamente",
                maxHp = (10..20).random(),
                currentHp = 0, // se sincroniza en repo
                armorClass = (10..15).random()
            )
            saveNpcUseCase(newNpc, null)
            onDone(name)
        }
    }
}

sealed interface NpcListUiState {
    data object Loading : NpcListUiState
    data class Success(val npcs: List<Npc>) : NpcListUiState
}
