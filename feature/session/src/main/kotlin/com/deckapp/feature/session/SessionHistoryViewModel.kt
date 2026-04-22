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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionStats(
    val totalDrawn: Int = 0,
    val totalDiscarded: Int = 0,
    val totalRolls: Int = 0,
    val durationMinutes: Long = 0,
    val mostUsedDeckName: String? = null,
    val neverDrawnCount: Int = 0,
    val usagePercentage: Float = 0f
)

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
    val filteredTimeline: List<TimelineEvent> = emptyList(),
    val stats: SessionStats = SessionStats(),
    val availableDecks: Map<Long, String> = emptyMap(),
    val availableActions: List<DrawAction> = emptyList(),
    val deckFilter: Long? = null,
    val actionFilter: DrawAction? = null,
    val searchQuery: String = "",
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

    private val _deckFilter = MutableStateFlow<Long?>(null)
    private val _actionFilter = MutableStateFlow<DrawAction?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<SessionHistoryUiState> = combine(
        sessionRepository.getSessionById(sessionId),
        sessionRepository.getEventsForSession(sessionId),
        tableRepository.getSessionRollLog(sessionId),
        _deckFilter,
        _actionFilter,
        _searchQuery
    ) { flows ->
        val session = flows[0] as Session?
        val events = flows[1] as List<DrawEvent>
        val rollResults = flows[2] as List<TableRollResult>
        val deckFilter = flows[3] as Long?
        val actionFilter = flows[4] as DrawAction?
        val searchQuery = flows[5] as String

        if (session == null) return@combine SessionHistoryUiState(isLoading = false)

        val cardIds = events.mapNotNull { it.cardId }.distinct()
        val cardsMap = mutableMapOf<Long, Card>()
        cardIds.forEach { id ->
            val card = cardRepository.getCardById(id).firstOrNull()
            if (card != null) cardsMap[id] = card
        }

        // Available decks for filtering (from events)
        val deckIdsInvolved = events.mapNotNull { cardsMap[it.cardId]?.stackId }.distinct()
        val deckNames = deckIdsInvolved.associateWith { id ->
            cardRepository.getDeckById(id).firstOrNull()?.name ?: "Mazo #$id"
        }

        // Available actions for filtering
        val actionsInvolved = events.map { it.action }.distinct().sortedBy { it.name }

        val cardEvents = events.map { event ->
            TimelineEvent.CardEvent(event = event, card = cardsMap[event.cardId])
        }
        val tableEvents = rollResults.map { TimelineEvent.TableEvent(it) }
        val fullTimeline = (cardEvents + tableEvents).sortedByDescending { it.timestamp }

        // Filter logic
        val filteredTimeline = fullTimeline.filter { event ->
            val matchesDeck = when (event) {
                is TimelineEvent.CardEvent -> deckFilter == null || event.card?.stackId == deckFilter
                is TimelineEvent.TableEvent -> deckFilter == null
            }
            val matchesAction = when (event) {
                is TimelineEvent.CardEvent -> actionFilter == null || event.event.action == actionFilter
                is TimelineEvent.TableEvent -> actionFilter == null
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                when (event) {
                    is TimelineEvent.CardEvent -> event.card?.title?.contains(searchQuery, ignoreCase = true) == true
                    is TimelineEvent.TableEvent -> event.result.resolvedText.contains(searchQuery, ignoreCase = true) == true
                }
            }
            matchesDeck && matchesAction && matchesSearch
        }

        // Stats calculation
        val totalDrawn = events.count { it.action == DrawAction.DRAW }
        val totalDiscarded = events.count { it.action == DrawAction.DISCARD }
        val endTime = session.endedAt ?: System.currentTimeMillis()
        val duration = (endTime - session.createdAt) / (1000 * 60)

        // Basic extra stats
        val deckUsage = events.mapNotNull { cardsMap[it.cardId]?.stackId }
            .groupingBy { it }.eachCount()
        val mostUsedDeckId = deckUsage.maxByOrNull { it.value }?.key
        val mostUsedDeckName = mostUsedDeckId?.let { deckNames[it] }

        val stats = SessionStats(
            totalDrawn = totalDrawn,
            totalDiscarded = totalDiscarded,
            totalRolls = rollResults.size,
            durationMinutes = duration,
            mostUsedDeckName = mostUsedDeckName
        )

        SessionHistoryUiState(
            session = session,
            timeline = fullTimeline,
            filteredTimeline = filteredTimeline,
            stats = stats,
            availableDecks = deckNames,
            availableActions = actionsInvolved,
            deckFilter = deckFilter,
            actionFilter = actionFilter,
            searchQuery = searchQuery,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionHistoryUiState()
    )

    fun setDeckFilter(deckId: Long?) { _deckFilter.value = deckId }
    fun setActionFilter(action: DrawAction?) { _actionFilter.value = action }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
}
