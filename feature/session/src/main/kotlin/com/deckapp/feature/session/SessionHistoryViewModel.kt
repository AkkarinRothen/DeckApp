package com.deckapp.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SessionHistoryUiState(
    val session: Session? = null,
    val timeline: List<EventItem> = emptyList(),
    val totalDrawn: Int = 0,
    val totalDiscarded: Int = 0,
    val durationMinutes: Long = 0,
    val isLoading: Boolean = true
)

data class EventItem(
    val event: DrawEvent,
    val card: Card? = null
)

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<SessionHistoryUiState> = combine(
        sessionRepository.getSessionById(sessionId),
        sessionRepository.getEventsForSession(sessionId)
    ) { session, events ->
        if (session == null) return@combine SessionHistoryUiState(isLoading = false)

        // Obtener cards únicas para el log (ignorando eventos de sistema sin carta)
        val cardIds = events.mapNotNull { it.cardId }.distinct()
        // Cargamos las cartas de forma simplificada (podría optimizarse con un getCardsByIds)
        val cardsMap = mutableMapOf<Long, Card>()
        cardIds.forEach { id ->
            val card = cardRepository.getCardById(id).firstOrNull()
            if (card != null) cardsMap[id] = card
        }

        val timeline = events.reversed().map { event ->
            EventItem(event = event, card = cardsMap[event.cardId])
        }

        val totalDrawn = events.count { it.action == DrawAction.DRAW }
        val totalDiscarded = events.count { it.action == DrawAction.DISCARD }
        
        val endTime = session.endedAt ?: System.currentTimeMillis()
        val duration = (endTime - session.createdAt) / (1000 * 60)

        SessionHistoryUiState(
            session = session,
            timeline = timeline,
            totalDrawn = totalDrawn,
            totalDiscarded = totalDiscarded,
            durationMinutes = duration,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionHistoryUiState()
    )
}
