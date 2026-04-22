package com.deckapp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.AddQuickNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuickNoteViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val addQuickNoteUseCase: AddQuickNoteUseCase
) : ViewModel() {

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId = _activeSessionId.asStateFlow()

    init {
        // Observar sesión activa
        sessionRepository.getActiveSession()
            .onEach { session ->
                _activeSessionId.value = session?.id
            }
            .launchIn(viewModelScope)
    }

    fun saveNote(content: String, manualSessionId: Long? = null, mythicSessionId: Long? = null) {
        viewModelScope.launch {
            addQuickNoteUseCase(
                content = content,
                sessionId = manualSessionId ?: _activeSessionId.value,
                mythicSessionId = mythicSessionId
            )
        }
    }
}
