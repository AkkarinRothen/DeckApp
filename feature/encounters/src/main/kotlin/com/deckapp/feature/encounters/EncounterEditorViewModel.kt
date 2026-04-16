package com.deckapp.feature.encounters

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.EncounterRepository
import com.deckapp.core.model.Condition
import com.deckapp.core.model.Encounter
import com.deckapp.core.model.EncounterCreature
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EncounterEditorUiState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val creatures: List<EncounterCreature> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class EncounterEditorViewModel @Inject constructor(
    private val encounterRepository: EncounterRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val encounterId: Long = savedStateHandle["encounterId"] ?: 0L
    private val _uiState = MutableStateFlow(EncounterEditorUiState(id = encounterId))
    val uiState: StateFlow<EncounterEditorUiState> = _uiState.asStateFlow()

    init {
        if (encounterId != 0L) {
            loadEncounter()
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun loadEncounter() {
        viewModelScope.launch {
            encounterRepository.getEncounterById(encounterId).first()?.let { encounter ->
                _uiState.update { it.copy(
                    name = encounter.name,
                    description = encounter.description,
                    creatures = encounter.creatures,
                    isLoading = false
                ) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun addCreature() {
        val count = _uiState.value.creatures.size
        val newCreature = EncounterCreature(
            encounterId = encounterId,
            name = "Nueva Criatura ${count + 1}",
            maxHp = 10,
            currentHp = 10,
            sortOrder = count
        )
        _uiState.update { it.copy(creatures = it.creatures + newCreature) }
    }

    fun updateCreature(index: Int, updated: EncounterCreature) {
        val list = _uiState.value.creatures.toMutableList()
        if (index in list.indices) {
            list[index] = updated
            _uiState.update { it.copy(creatures = list) }
        }
    }

    fun removeCreature(index: Int) {
        val list = _uiState.value.creatures.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.update { it.copy(creatures = list) }
        }
    }

    fun save() {
        if (_uiState.value.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre no puede estar vacío") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val encounter = Encounter(
                id = encounterId,
                name = _uiState.value.name,
                description = _uiState.value.description,
                creatures = _uiState.value.creatures
            )
            try {
                encounterRepository.saveEncounter(encounter)
                _uiState.update { it.copy(isSaving = false, saved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, saved = false) }
    }
}
