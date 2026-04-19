package com.deckapp.feature.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.SystemRule
import com.deckapp.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RuleEditorUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val gameSystem: String = "General",
    val category: String = "General",
    val isPinned: Boolean = false,
    val isPreviewMode: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val distinctSystems: List<String> = emptyList(),
    val tags: List<Tag> = emptyList()
)

@HiltViewModel
class RuleEditorViewModel @Inject constructor(
    private val referenceRepository: ReferenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RuleEditorUiState())
    val uiState: StateFlow<RuleEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(distinctSystems = referenceRepository.getDistinctSystems().first()) }
        }
    }

    fun loadRule(ruleId: Long, prefilledSystem: String = "") {
        if (ruleId == -1L) {
            _uiState.update { it.copy(
                gameSystem = if (prefilledSystem.isNotBlank()) prefilledSystem else "General"
            ) }
            return
        }

        viewModelScope.launch {
            val rule = referenceRepository.getRuleById(ruleId)
            if (rule != null) {
                _uiState.update { it.copy(
                    id = rule.id,
                    title = rule.title,
                    content = rule.content,
                    gameSystem = rule.gameSystem,
                    category = rule.category,
                    isPinned = rule.isPinned,
                    tags = rule.tags
                ) }
            }
        }
    }

    fun onTitleChanged(title: String) = _uiState.update { it.copy(title = title) }
    fun onContentChanged(content: String) = _uiState.update { it.copy(content = content) }
    fun onSystemChanged(system: String) = _uiState.update { it.copy(gameSystem = system) }
    fun onCategoryChanged(category: String) = _uiState.update { it.copy(category = category) }
    fun togglePinned() = _uiState.update { it.copy(isPinned = !it.isPinned) }
    fun togglePreviewMode() = _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val rule = SystemRule(
                id = state.id,
                title = state.title,
                content = state.content,
                gameSystem = state.gameSystem,
                category = state.category,
                isPinned = state.isPinned,
                tags = state.tags,
                lastUpdated = System.currentTimeMillis()
            )
            referenceRepository.saveSystemRule(rule)
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
