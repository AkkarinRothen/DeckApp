package com.deckapp.feature.encounters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.StartEncounterUseCase
import com.deckapp.core.model.Encounter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EncounterListUiState(
    val encounters: List<Encounter> = emptyList(),
    val activeSessionId: Long? = null,
    val isLoading: Boolean = true,
    val startedEncounterId: Long? = null, // Para señal de navegación a la sesión
    val errorMessage: String? = null
)

@HiltViewModel
class EncounterListViewModel @Inject constructor(
    private val encounterRepository: EncounterRepository,
    private val sessionRepository: SessionRepository,
    private val startEncounterUseCase: StartEncounterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncounterListUiState())
    val uiState: StateFlow<EncounterListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            encounterRepository.getAllEncounters(),
            sessionRepository.getActiveSession()
        ) { encounters, session ->
            _uiState.update { it.copy(
                encounters = encounters,
                activeSessionId = session?.id,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)
    }

    fun startEncounter(encounterId: Long) {
        val sessionId = _uiState.value.activeSessionId
        if (sessionId == null) {
            _uiState.update { it.copy(errorMessage = "Inicia una sesión primero para activar el combate") }
            return
        }

        viewModelScope.launch {
            try {
                startEncounterUseCase(encounterId, sessionId)
                _uiState.update { it.copy(startedEncounterId = encounterId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun deleteEncounter(id: Long) {
        viewModelScope.launch {
            encounterRepository.deleteEncounter(id)
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, startedEncounterId = null) }
    }
}
