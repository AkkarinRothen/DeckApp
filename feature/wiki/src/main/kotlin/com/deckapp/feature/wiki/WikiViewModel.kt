package com.deckapp.feature.wiki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.WikiRepository
import com.deckapp.core.model.WikiCategory
import com.deckapp.core.model.WikiEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WikiUiState(
    val categories: List<WikiCategory> = emptyList(),
    val entries: List<WikiEntry> = emptyList(),
    val pinnedEntries: List<WikiEntry> = emptyList(),
    val recentEntries: List<WikiEntry> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class WikiViewModel @Inject constructor(
    private val wikiRepository: WikiRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<WikiUiState> = combine(
        wikiRepository.getCategories(),
        _selectedCategoryId.flatMapLatest { id ->
            if (id != null) wikiRepository.getEntriesByCategory(id)
            else flowOf(emptyList())
        },
        wikiRepository.getPinnedEntries(),
        wikiRepository.getRecentEntries(5),
        _searchQuery
    ) { categories, entries, pinned, recent, query ->
        WikiUiState(
            categories = categories,
            entries = if (query.isBlank()) entries else emptyList(), // Search replaces entry list
            pinnedEntries = pinned,
            recentEntries = recent,
            selectedCategoryId = _selectedCategoryId.value,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WikiUiState()
    )

    fun selectCategory(id: Long?) {
        _selectedCategoryId.value = id
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun saveCategory(name: String, icon: String = "Description") {
        viewModelScope.launch {
            wikiRepository.saveCategory(WikiCategory(name = name, iconName = icon))
        }
    }

    fun deleteEntry(entry: WikiEntry) {
        viewModelScope.launch {
            wikiRepository.deleteEntry(entry)
        }
    }
}
