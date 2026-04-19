package com.deckapp.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.Card
import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawEvent
import com.deckapp.core.model.Session
import com.deckapp.core.model.TableRollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed class TimelineEvent {
    abstract val timestamp: Long
    data class CardEvent(val event: DrawEvent, val card: Card?) : TimelineEvent() {
        override val timestamp = event.timestamp
    }
    data class TableEvent(val result: TableRollResult) : TimelineEvent() {
        override val timestamp = result.timestamp
    }
}

data class SessionHistoryUiState(
    val session: Session? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val totalDrawn: Int = 0,
    val totalDiscarded: Int = 0,
    val totalRolls: Int = 0,
    val durationMinutes: Long = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    val uiState: StateFlow<SessionHistoryUiState> = combine(
        sessionRepository.getSessionById(sessionId),
        sessionRepository.getEventsForSession(sessionId),
        tableRepository.getSessionRollLog(sessionId)
    ) { session, events, rollResults ->
        if (session == null) return@combine SessionHistoryUiState(isLoading = false)

        val cardIds = events.mapNotNull { it.cardId }.distinct()
        val cardsMap = mutableMapOf<Long, Card>()
        cardIds.forEach { id ->
            val card = cardRepository.getCardById(id).firstOrNull()
            if (card != null) cardsMap[id] = card
        }

        val cardEvents = events.map { event ->
            TimelineEvent.CardEvent(event = event, card = cardsMap[event.cardId])
        }
        val tableEvents = rollResults.map { TimelineEvent.TableEvent(it) }
        val timeline = (cardEvents + tableEvents).sortedByDescending { it.timestamp }

        val totalDrawn = events.count { it.action == DrawAction.DRAW }
        val totalDiscarded = events.count { it.action == DrawAction.DISCARD }

        val endTime = session.endedAt ?: System.currentTimeMillis()
        val duration = (endTime - session.createdAt) / (1000 * 60)

        SessionHistoryUiState(
            session = session,
            timeline = timeline,
            totalDrawn = totalDrawn,
            totalDiscarded = totalDiscarded,
            totalRolls = rollResults.size,
            durationMinutes = duration,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionHistoryUiState()
    )
}
