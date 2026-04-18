package com.deckapp.feature.npcs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.usecase.GetNpcsUseCase
import com.deckapp.core.model.Npc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NpcListViewModel @Inject constructor(
    private val getNpcsUseCase: GetNpcsUseCase
) : ViewModel() {

    val uiState: StateFlow<NpcListUiState> = getNpcsUseCase()
        .map { npcs -> NpcListUiState.Success(npcs) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NpcListUiState.Loading
        )
}

sealed interface NpcListUiState {
    data object Loading : NpcListUiState
    data class Success(val npcs: List<Npc>) : NpcListUiState
}
