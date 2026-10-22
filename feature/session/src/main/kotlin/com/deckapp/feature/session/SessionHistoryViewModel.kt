package com.deckapp.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.SessionLogRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class HistoryCategoryFilter {
    ALL,
    DICE_AND_TABLES,
    CARDS,
    COMBAT,
    CLOCKS
}

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

    data class LogEvent(val entry: SessionLogEntry) : TimelineEvent() {
        override val timestamp = entry.timestamp
    }
}

data class SessionHistoryUiState(
    val session: Session? = null,
    val timeline: List<TimelineEvent> = emptyList(),
    val filteredTimeline: List<TimelineEvent> = emptyList(),
    val stats: SessionStats = SessionStats(),
    val categoryFilter: HistoryCategoryFilter = HistoryCategoryFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: CardRepository,
    private val tableRepository: TableRepository,
    private val sessionLogRepository: SessionLogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _categoryFilter = MutableStateFlow(HistoryCategoryFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<SessionHistoryUiState> = combine(
        sessionRepository.getSessionById(sessionId),
        sessionRepository.getEventsForSession(sessionId),
        tableRepository.getSessionRollLog(sessionId),
        sessionLogRepository.getLogsForSession(sessionId),
        _categoryFilter,
        _searchQuery
    ) { flows ->
        val session = flows[0] as Session?
        val events = flows[1] as List<DrawEvent>
        val rollResults = flows[2] as List<TableRollResult>
        val logEntries = flows[3] as List<SessionLogEntry>
        val categoryFilter = flows[4] as HistoryCategoryFilter
        val searchQuery = flows[5] as String

        if (session == null) return@combine SessionHistoryUiState(isLoading = false)

        val cardIds = events.mapNotNull { it.cardId }.distinct()
        val cardsMap = mutableMapOf<Long, Card>()
        cardIds.forEach { id ->
            val card = cardRepository.getCardById(id).firstOrNull()
            if (card != null) cardsMap[id] = card
        }

        // Available decks for stats
        val deckIdsInvolved = events.mapNotNull { cardsMap[it.cardId]?.stackId }.distinct()
        val deckNames = deckIdsInvolved.associateWith { id ->
            cardRepository.getDeckById(id).firstOrNull()?.name ?: "Mazo #$id"
        }

        val cardEvents = events.map { event ->
            TimelineEvent.CardEvent(event = event, card = cardsMap[event.cardId])
        }
        val tableEvents = rollResults.map { TimelineEvent.TableEvent(it) }
        val logEvents = logEntries.map { TimelineEvent.LogEvent(it) }

        // Combinar y desduplicar eventos repetidos / solapados en un umbral de 300ms
        val rawTimeline = (cardEvents + tableEvents + logEvents).sortedByDescending { it.timestamp }
        val deduplicatedTimeline = mutableListOf<TimelineEvent>()

        for (item in rawTimeline) {
            val lastAdded = deduplicatedTimeline.lastOrNull()
            val isDuplicate = when {
                lastAdded == null -> false
                item is TimelineEvent.TableEvent && lastAdded is TimelineEvent.LogEvent -> {
                    // Si el log es de tipo ORACLE o DICE con el mismo nombre y timestamp cercano (<500ms)
                    kotlin.math.abs(item.timestamp - lastAdded.timestamp) < 500 &&
                        (lastAdded.entry.title.contains(item.result.tableName, ignoreCase = true) ||
                         lastAdded.entry.details.contains(item.result.resolvedText, ignoreCase = true))
                }
                item is TimelineEvent.LogEvent && lastAdded is TimelineEvent.TableEvent -> {
                    kotlin.math.abs(item.timestamp - lastAdded.timestamp) < 500 &&
                        (item.entry.title.contains(lastAdded.result.tableName, ignoreCase = true) ||
                         item.entry.details.contains(lastAdded.result.resolvedText, ignoreCase = true))
                }
                item is TimelineEvent.LogEvent && lastAdded is TimelineEvent.LogEvent -> {
                    item.entry.title == lastAdded.entry.title &&
                        item.entry.details == lastAdded.entry.details &&
                        kotlin.math.abs(item.timestamp - lastAdded.timestamp) < 1000
                }
                else -> false
            }

            if (!isDuplicate) {
                deduplicatedTimeline.add(item)
            }
        }

        // Filtro por categoría
        val filteredTimeline = deduplicatedTimeline.filter { event ->
            val matchesCategory = when (categoryFilter) {
                HistoryCategoryFilter.ALL -> true
                HistoryCategoryFilter.DICE_AND_TABLES -> {
                    when (event) {
                        is TimelineEvent.TableEvent -> true
                        is TimelineEvent.LogEvent -> event.entry.eventType in listOf(
                            SessionLogEventType.DICE,
                            SessionLogEventType.ORACLE
                        )
                        is TimelineEvent.CardEvent -> false
                    }
                }
                HistoryCategoryFilter.CARDS -> {
                    when (event) {
                        is TimelineEvent.CardEvent -> true
                        is TimelineEvent.LogEvent -> event.entry.eventType == SessionLogEventType.CARD
                        is TimelineEvent.TableEvent -> false
                    }
                }
                HistoryCategoryFilter.COMBAT -> {
                    when (event) {
                        is TimelineEvent.LogEvent -> event.entry.eventType == SessionLogEventType.TOKEN ||
                            event.entry.title.contains("combate", ignoreCase = true) ||
                            event.entry.title.contains("herida", ignoreCase = true) ||
                            event.entry.title.contains("iniciativa", ignoreCase = true) ||
                            event.entry.details.contains("daño", ignoreCase = true)
                        else -> false
                    }
                }
                HistoryCategoryFilter.CLOCKS -> {
                    when (event) {
                        is TimelineEvent.LogEvent -> event.entry.eventType == SessionLogEventType.CLOCK
                        else -> false
                    }
                }
            }

            val matchesSearch = if (searchQuery.isBlank()) true else {
                when (event) {
                    is TimelineEvent.CardEvent -> event.card?.title?.contains(searchQuery, ignoreCase = true) == true ||
                        event.event.metadata?.contains(searchQuery, ignoreCase = true) == true
                    is TimelineEvent.TableEvent -> event.result.tableName.contains(searchQuery, ignoreCase = true) ||
                        event.result.resolvedText.contains(searchQuery, ignoreCase = true)
                    is TimelineEvent.LogEvent -> event.entry.title.contains(searchQuery, ignoreCase = true) ||
                        event.entry.details.contains(searchQuery, ignoreCase = true)
                }
            }

            matchesCategory && matchesSearch
        }

        // Stats calculation
        val totalDrawn = events.count { it.action == DrawAction.DRAW }
        val totalDiscarded = events.count { it.action == DrawAction.DISCARD }
        val endTime = session.endedAt ?: System.currentTimeMillis()
        val duration = (endTime - session.createdAt) / (1000 * 60)

        val deckUsage = events.mapNotNull { cardsMap[it.cardId]?.stackId }
            .groupingBy { it }.eachCount()
        val mostUsedDeckId = deckUsage.maxByOrNull { it.value }?.key
        val mostUsedDeckName = mostUsedDeckId?.let { deckNames[it] }

        val stats = SessionStats(
            totalDrawn = totalDrawn,
            totalDiscarded = totalDiscarded,
            totalRolls = rollResults.size + logEntries.count { it.eventType == SessionLogEventType.DICE },
            durationMinutes = duration,
            mostUsedDeckName = mostUsedDeckName
        )

        SessionHistoryUiState(
            session = session,
            timeline = deduplicatedTimeline,
            filteredTimeline = filteredTimeline,
            stats = stats,
            categoryFilter = categoryFilter,
            searchQuery = searchQuery,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SessionHistoryUiState()
    )

    fun setCategoryFilter(filter: HistoryCategoryFilter) {
        _categoryFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
