package com.deckapp.feature.mythic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.mythic.*
import com.deckapp.core.model.MythicSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MythicSessionListUiState(
    val sessions: List<MythicSession> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MythicSessionListViewModel @Inject constructor(
    private val getMythicSessionsUseCase: GetMythicSessionsUseCase,
    private val createMythicSessionUseCase: CreateMythicSessionUseCase,
    private val deleteMythicSessionUseCase: DeleteMythicSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MythicSessionListUiState())
    val uiState: StateFlow<MythicSessionListUiState> = _uiState.asStateFlow()

    init {
        getMythicSessionsUseCase()
            .onEach { sessions ->
                _uiState.update { it.copy(sessions = sessions, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun createSession(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = createMythicSessionUseCase(name)
            onCreated(id)
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            deleteMythicSessionUseCase(id)
        }
    }
}
