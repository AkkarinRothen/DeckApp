package com.deckapp.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagManagerUiState(
    val tags: List<Tag> = emptyList(),
    val editingTag: Tag? = null,
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class TagManagerViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManagerUiState())
    val uiState: StateFlow<TagManagerUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            cardRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(tags = tags, isLoading = false) }
            }
        }
    }

    fun startEditing(tag: Tag?) {
        _uiState.update { it.copy(editingTag = tag ?: Tag(0, "", 0xFF6200EE.toInt())) }
    }

    fun updateEditingTag(tag: Tag) {
        _uiState.update { it.copy(editingTag = tag) }
    }

    fun stopEditing() {
        _uiState.update { it.copy(editingTag = null) }
    }

    fun saveTag() {
        val tag = _uiState.value.editingTag ?: return
        if (tag.name.isBlank()) return
        
        viewModelScope.launch {
            if (tag.id == 0L) {
                cardRepository.saveTag(tag)
            } else {
                cardRepository.updateTag(tag)
            }
            _uiState.update { it.copy(editingTag = null, message = "Etiqueta guardada") }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            cardRepository.deleteTag(tagId)
            _uiState.update { it.copy(message = "Etiqueta eliminada") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
