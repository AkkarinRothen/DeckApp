package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionLogRepository
import com.deckapp.core.model.SessionLogEntry
import com.deckapp.core.model.SessionLogEventType
import com.deckapp.core.model.SessionNote
import com.deckapp.feature.draw.SessionUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Delegado encargado de la Bitácora de Sesión, Notas Rápidas y Modo Zen del Playmat.
 */
class SessionLogDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val sessionLogRepository: SessionLogRepository
) {
    private var logsJob: Job? = null
    private var notesJob: Job? = null

    init {
        observeLogs()
        observeNotes(_uiState.value.focusedSceneId)
    }

    fun onSceneChanged(newSceneId: Long?) {
        observeNotes(newSceneId)
        if (_uiState.value.sessionLog.isFilterCurrentScene) {
            observeLogs()
        }
    }

    private fun observeLogs() {
        logsJob?.cancel()
        logsJob = scope.launch {
            val filterScene = _uiState.value.sessionLog.isFilterCurrentScene
            val sceneId = _uiState.value.focusedSceneId
            val flow = if (filterScene && sceneId != null) {
                sessionLogRepository.getLogsForScene(sessionId, sceneId)
            } else {
                sessionLogRepository.getLogsForSession(sessionId)
            }
            flow.collectLatest { list ->
                _uiState.update { state ->
                    state.copy(sessionLog = state.sessionLog.copy(logs = list))
                }
            }
        }
    }

    private fun observeNotes(sceneId: Long?) {
        notesJob?.cancel()
        notesJob = scope.launch {
            sessionLogRepository.getNoteForScene(sessionId, sceneId).collectLatest { note ->
                _uiState.update { state ->
                    state.copy(
                        sessionLog = state.sessionLog.copy(
                            currentNote = note,
                            noteDraftMarkdown = note?.contentMarkdown ?: ""
                        )
                    )
                }
            }
        }
    }

    fun toggleFilterCurrentScene() {
        _uiState.update { state ->
            val next = !state.sessionLog.isFilterCurrentScene
            state.copy(sessionLog = state.sessionLog.copy(isFilterCurrentScene = next))
        }
        observeLogs()
    }

    fun toggleDrawer() {
        _uiState.update { state ->
            val next = !state.playmat.isSessionDrawerOpen
            state.copy(playmat = state.playmat.copy(isSessionDrawerOpen = next))
        }
    }

    fun setDrawerTab(tab: Int) {
        _uiState.update { state ->
            state.copy(playmat = state.playmat.copy(sessionDrawerTab = tab))
        }
    }

    fun toggleZenMode() {
        _uiState.update { state ->
            val next = !state.playmat.isZenMode
            state.copy(playmat = state.playmat.copy(isZenMode = next))
        }
    }

    fun addManualNote(text: String) {
        scope.launch {
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.MANUAL,
                title = "Apunte",
                details = text
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun logDiceRoll(formula: String, total: Int, details: String) {
        scope.launch {
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.DICE,
                title = "🎲 Tirada: $formula ➔ $total",
                details = details
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun logCardAction(cardTitle: String, action: String, details: String = "") {
        scope.launch {
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.CARD,
                title = "🃏 $cardTitle ($action)",
                details = details
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun logTokenHpChange(tokenName: String, delta: Int, newHp: Int, maxHp: Int) {
        scope.launch {
            val deltaStr = if (delta > 0) "+$delta" else "$delta"
            val action = if (delta < 0) "Daño" else "Curación"
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.TOKEN,
                title = "🛡️ $tokenName ($action: $deltaStr)",
                details = "Salud: $newHp/$maxHp HP"
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun logOracleRoll(question: String, answer: String, chaosFactor: Int) {
        scope.launch {
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.ORACLE,
                title = "🔮 Oráculo: $answer",
                details = "\"$question\" (Caos: $chaosFactor)"
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun logClockStep(clockTitle: String, current: Int, max: Int) {
        scope.launch {
            val completed = current >= max
            val status = if (completed) "¡COMPLETADO!" else "$current/$max segmentos"
            val entry = SessionLogEntry(
                sessionId = sessionId,
                sceneId = _uiState.value.focusedSceneId,
                eventType = SessionLogEventType.CLOCK,
                title = "⏰ Reloj: $clockTitle ($status)",
                details = if (completed) "El evento del reloj ha detonado." else "Progreso: $current de $max segmentos."
            )
            sessionLogRepository.logEvent(entry)
        }
    }

    fun saveSceneNote(markdown: String) {
        scope.launch {
            val current = _uiState.value.sessionLog.currentNote
            val sceneId = _uiState.value.focusedSceneId
            val note = SessionNote(
                id = current?.id ?: 0,
                sessionId = sessionId,
                sceneId = sceneId,
                contentMarkdown = markdown,
                updatedAt = System.currentTimeMillis()
            )
            sessionLogRepository.saveNote(note)
        }
    }

    fun clearLogs() {
        scope.launch {
            sessionLogRepository.clearLogs(sessionId)
        }
    }
}
