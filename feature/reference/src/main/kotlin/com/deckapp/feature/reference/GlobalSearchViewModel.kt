package com.deckapp.feature.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.deckapp.core.domain.usecase.GlobalSearchUseCase
import com.deckapp.core.domain.usecase.SearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import javax.inject.Inject

data class GlobalSearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false
)

@HiltViewModel
@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GlobalSearchViewModel @Inject constructor(
    private val globalSearchUseCase: GlobalSearchUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalSearchUiState())
    val uiState: StateFlow<GlobalSearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.map { it.query }
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(emptyList())
                    else flow {
                        delay(250)
                        emitAll(globalSearchUseCase(query))
                    }
                }
                .collect { results ->
                    _uiState.update { it.copy(results = results, isSearching = false) }
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update {
            it.copy(
                query = newQuery,
                results = emptyList(),
                isSearching = newQuery.isNotBlank()
            )
        }
    }
}
