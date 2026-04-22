package com.deckapp.feature.wiki

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.WikiRepository
import com.deckapp.core.model.WikiEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WikiEntryUiState(
    val entryId: Long? = null,
    val categoryId: Long? = null,
    val title: String = "",
    val content: TextFieldValue = TextFieldValue(""),
    val imagePath: String? = null,
    val isPinned: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isPreviewMode: Boolean = false
)

@HiltViewModel
class WikiEntryViewModel @Inject constructor(
    private val wikiRepository: WikiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var pendingImageUri: Uri? = null

    private val entryId: Long? = savedStateHandle.get<Long>("entryId")?.takeIf { it > 0 }
    private val categoryId: Long? = savedStateHandle.get<Long>("categoryId")

    private val _uiState = MutableStateFlow(WikiEntryUiState(entryId = entryId, categoryId = categoryId))
    val uiState: StateFlow<WikiEntryUiState> = _uiState.asStateFlow()

    init {
        if (entryId != null) {
            viewModelScope.launch {
                val entry = wikiRepository.getEntryById(entryId)
                if (entry != null) {
                    _uiState.update { it.copy(
                        title = entry.title,
                        content = TextFieldValue(entry.content),
                        imagePath = entry.imagePath
                    ) }
                }
            }
        }
    }

    fun togglePinned() {
        _uiState.update { it.copy(isPinned = !it.isPinned, isSaved = false) }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isSaved = false) }
    }

    fun onContentChange(newContent: TextFieldValue) {
        _uiState.update { it.copy(content = newContent, isSaved = false) }
    }

    fun onImageSelected(uri: Uri?) {
        if (uri == null) return
        pendingImageUri = uri
        _uiState.update { it.copy(imagePath = uri.toString(), isSaved = false) }
    }

    fun togglePreview() {
        _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) }
    }

    fun saveEntry() {
        val state = _uiState.value
        if (state.title.isBlank() || state.categoryId == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = System.currentTimeMillis()
            
            var currentImagePath = state.imagePath
            
            // Si hay una imagen nueva pendiente de guardar
            pendingImageUri?.let { uri ->
                // Necesitamos un ID para el archivo, si es nuevo usamos un placeholder o guardamos después
                val tempId = state.entryId ?: (now / 1000) // Evitar duplicados si es nuevo
                val localPath = wikiRepository.saveEntryImage(uri, tempId)
                if (localPath != null) currentImagePath = localPath
            }

            val entry = WikiEntry(
                id = state.entryId ?: 0,
                title = state.title,
                content = state.content.text,
                categoryId = state.categoryId,
                imagePath = currentImagePath,
                isPinned = state.isPinned,
                lastUpdated = now
            )
            val newId = wikiRepository.saveEntry(entry)
            _uiState.update { it.copy(entryId = newId, isSaving = false, isSaved = true) }
        }
    }
}
