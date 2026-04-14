package com.deckapp.feature.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionListUiState(
    val activeSessions: List<Session> = emptyList(),
    val pastSessions: List<Session> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    val uiState = sessionRepository.getAllSessions()
        .map { sessions ->
            SessionListUiState(
                activeSessions = sessions.filter { it.isActive },
                pastSessions = sessions.filter { !it.isActive },
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionListUiState()
        )

    fun endSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.endSession(sessionId)
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
        }
    }

    fun renameSession(sessionId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            sessionRepository.renameSession(sessionId, trimmed)
        }
    }

    fun cloneSession(sessionId: Long) {
        viewModelScope.launch {
            val original = sessionRepository.getSessionById(sessionId).first() ?: return@launch
            val decks = sessionRepository.getDecksForSession(sessionId).first()

            val newSessionId = sessionRepository.createSession(
                original.copy(
                    id = 0L,
                    name = "${original.name} (clon)",
                    isActive = true,
                    createdAt = System.currentTimeMillis(),
                    endedAt = null
                )
            )

            decks.forEach { ref ->
                sessionRepository.addDeckToSession(ref.copy(sessionId = newSessionId))
            }
        }
    }
}
