package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.domain.usecase.mythic.SceneCheckResult
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.scene.SceneFramingDraft
import com.deckapp.feature.draw.scene.buildFramedSceneJournalEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Delegado encargado de la gestión de Relojes de Tensión (Sprint 61),
 * la botonera de acciones rápidas Hotbar (Sprint 59) y
 * el Asistente de Encuadre de Escenas (Sprint 60).
 */
class SessionClocksDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val sessionRepository: SessionRepository,
    private val notesMutex: Mutex,
    private val onAddBoardBlock: (type: SessionBoardItemType, size: SessionBoardItemSize, titleOverride: String, resourceId: Long) -> Unit,
    private val onRollSmartDice: (formula: String, description: String) -> Unit,
    private val onRollMythicRandomEvent: () -> Unit,
    private val onRollMythicFate: (ProbabilityLevel) -> Unit,
    private val onRollTable: (Long) -> Unit,
    private val onDrawCard: (Long) -> Unit,
    private val onUpdateBoardMythicChaos: (Int) -> Unit,
    private val onFocusScene: (Long?) -> Unit
) {

    // --- Tension Clocks & Overlay (Sprint 61) ---

    fun toggleClocksOverlayExpanded() {
        _uiState.update { it.copy(isClocksOverlayExpanded = !it.isClocksOverlayExpanded) }
    }

    fun openCreateClockDialog() {
        _uiState.update { it.copy(showClockEditDialog = true, editingClock = null) }
    }

    fun openEditClockDialog(clock: TensionClock) {
        _uiState.update { it.copy(showClockEditDialog = true, editingClock = clock) }
    }

    fun closeClockEditDialog() {
        _uiState.update { it.copy(showClockEditDialog = false, editingClock = null) }
    }

    fun dismissClockImpactAlert() {
        _uiState.update { it.copy(clockImpactAlert = null) }
    }

    fun saveClock(clock: TensionClock) {
        val currentClocks = _uiState.value.tensionClocks
        val exists = currentClocks.any { it.id == clock.id }
        val updatedClocks = if (exists) {
            currentClocks.map { if (it.id == clock.id) clock else it }
        } else {
            currentClocks + clock
        }
        val isNowCompleted = clock.progress >= clock.segments
        _uiState.update { state ->
            state.copy(
                tensionClocks = updatedClocks,
                showClockEditDialog = false,
                editingClock = null,
                clockImpactAlert = if (isNowCompleted && !clock.isCompleted) clock else state.clockImpactAlert
            )
        }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
    }

    fun tickClock(clockId: Long, amount: Int) {
        val currentClocks = _uiState.value.tensionClocks
        var completedClock: TensionClock? = null
        val updatedClocks = currentClocks.map { clock ->
            if (clock.id == clockId) {
                val newProgress = (clock.progress + amount).coerceIn(0, clock.segments)
                val isNowCompleted = newProgress >= clock.segments
                val updated = clock.copy(progress = newProgress, isCompleted = isNowCompleted)
                if (isNowCompleted && !clock.isCompleted) {
                    completedClock = updated
                }
                updated
            } else clock
        }
        _uiState.update { state ->
            state.copy(
                tensionClocks = updatedClocks,
                clockImpactAlert = completedClock ?: state.clockImpactAlert
            )
        }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
    }

    fun resetClock(clockId: Long) {
        val currentClocks = _uiState.value.tensionClocks
        val updatedClocks = currentClocks.map { clock ->
            if (clock.id == clockId) {
                clock.copy(progress = 0, isCompleted = false)
            } else clock
        }
        _uiState.update { state ->
            state.copy(
                tensionClocks = updatedClocks,
                clockImpactAlert = if (state.clockImpactAlert?.id == clockId) null else state.clockImpactAlert
            )
        }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
    }

    fun deleteClock(clockId: Long) {
        val currentClocks = _uiState.value.tensionClocks
        val updatedClocks = currentClocks.filterNot { it.id == clockId }
        _uiState.update { state ->
            state.copy(
                tensionClocks = updatedClocks,
                clockImpactAlert = if (state.clockImpactAlert?.id == clockId) null else state.clockImpactAlert
            )
        }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
    }

    fun triggerClockNow(clock: TensionClock) {
        val updated = clock.copy(progress = clock.segments, isCompleted = true)
        saveClock(updated)
        _uiState.update { it.copy(clockImpactAlert = updated) }
    }

    fun copyClockImpactToDiary(clock: TensionClock) {
        val consequenceText = clock.triggerDescription.ifBlank { "El evento o peligro programado se ha desencadenado." }
        val diaryEntry = "\n\n### ⏰ Detonación de Reloj: ${clock.name}\n> **Consecuencia:** $consequenceText\n*Progreso alcanzado: ${clock.segments}/${clock.segments}*\n"
        val currentNotes = _uiState.value.dmNotes
        val updatedNotes = currentNotes + diaryEntry
        _uiState.update { it.copy(dmNotes = updatedNotes, clockImpactAlert = null) }
        scope.launch {
            sessionRepository.updateDmNotes(sessionId, updatedNotes)
        }
    }

    fun updateClockDescriptionAndProgress(clockId: Long, newDescription: String, newProgress: Int) {
        val currentClocks = _uiState.value.tensionClocks
        val updatedClocks = currentClocks.map { clock ->
            if (clock.id == clockId) {
                clock.copy(
                    triggerDescription = newDescription,
                    progress = newProgress.coerceIn(0, clock.segments),
                    isCompleted = newProgress >= clock.segments
                )
            } else clock
        }
        _uiState.update { state ->
            state.copy(tensionClocks = updatedClocks)
        }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
    }

    fun createClockAndAddToBoard(
        name: String,
        segments: Int,
        colorHex: String?,
        triggerDescription: String,
        targetGroupId: Long? = null
    ) {
        val clockId = System.currentTimeMillis()
        val newClock = TensionClock(
            id = clockId,
            sessionId = sessionId,
            name = name,
            segments = segments,
            progress = 0,
            colorHex = colorHex,
            triggerDescription = triggerDescription
        )
        val updatedClocks = _uiState.value.tensionClocks + newClock
        _uiState.update { it.copy(tensionClocks = updatedClocks) }
        scope.launch {
            sessionRepository.updateTensionClocks(sessionId, updatedClocks)
        }
        onAddBoardBlock(
            SessionBoardItemType.CLOCK,
            SessionBoardItemSize.S,
            name,
            clockId
        )
    }

    // --- Session Hotbar Actions (Sprint 59) ---

    fun toggleHotbarExpanded() {
        _uiState.update { it.copy(isHotbarExpanded = !it.isHotbarExpanded) }
    }

    fun openCreateHotbarAction() {
        _uiState.update { it.copy(showHotbarActionDialog = true, editingHotbarAction = null) }
    }

    fun openEditHotbarAction(action: SessionHotbarAction) {
        _uiState.update { it.copy(showHotbarActionDialog = true, editingHotbarAction = action) }
    }

    fun dismissHotbarActionDialog() {
        _uiState.update { it.copy(showHotbarActionDialog = false, editingHotbarAction = null) }
    }

    fun saveHotbarAction(action: SessionHotbarAction) {
        val currentActions = _uiState.value.session?.hotbarActions ?: defaultHotbarActions()
        val index = currentActions.indexOfFirst { it.id == action.id }
        val updatedActions = if (index >= 0) {
            currentActions.toMutableList().apply { set(index, action) }
        } else {
            currentActions + action
        }
        _uiState.update { state ->
            state.copy(
                session = state.session?.copy(hotbarActions = updatedActions),
                showHotbarActionDialog = false,
                editingHotbarAction = null
            )
        }
        scope.launch {
            sessionRepository.updateHotbarActions(sessionId, updatedActions)
        }
    }

    fun deleteHotbarAction(actionId: String) {
        val currentActions = _uiState.value.session?.hotbarActions ?: defaultHotbarActions()
        val updatedActions = currentActions.filterNot { it.id == actionId }
        _uiState.update { state ->
            state.copy(
                session = state.session?.copy(hotbarActions = updatedActions),
                showHotbarActionDialog = false,
                editingHotbarAction = null
            )
        }
        scope.launch {
            sessionRepository.updateHotbarActions(sessionId, updatedActions)
        }
    }

    fun executeHotbarAction(action: SessionHotbarAction) {
        when (action.type) {
            HotbarActionType.DICE_ROLL -> {
                onRollSmartDice(action.payload, action.label)
            }
            HotbarActionType.ORACLE_QUERY -> {
                when (action.payload.uppercase()) {
                    "RANDOM_EVENT" -> onRollMythicRandomEvent()
                    "CERTAIN" -> onRollMythicFate(ProbabilityLevel.CERTAIN)
                    "NEAR_SURE", "NEARLY_CERTAIN" -> onRollMythicFate(ProbabilityLevel.NEARLY_CERTAIN)
                    "VERY_LIKELY" -> onRollMythicFate(ProbabilityLevel.VERY_LIKELY)
                    "LIKELY" -> onRollMythicFate(ProbabilityLevel.LIKELY)
                    "UNLIKELY" -> onRollMythicFate(ProbabilityLevel.UNLIKELY)
                    "VERY_UNLIKELY" -> onRollMythicFate(ProbabilityLevel.VERY_UNLIKELY)
                    "NEAR_IMPOSSIBLE", "NEARLY_IMPOSSIBLE" -> onRollMythicFate(ProbabilityLevel.NEARLY_IMPOSSIBLE)
                    "IMPOSSIBLE" -> onRollMythicFate(ProbabilityLevel.IMPOSSIBLE)
                    else -> onRollMythicFate(ProbabilityLevel.FIFTY_FIFTY)
                }
            }
            HotbarActionType.RANDOM_TABLE -> {
                val tableId = action.payload.toLongOrNull()
                if (tableId != null) {
                    onRollTable(tableId)
                } else {
                    _uiState.update { it.copy(snackbarMessage = "Tabla no vinculada: ${action.label}") }
                }
            }
            HotbarActionType.DRAW_CARD -> {
                val deckId = action.payload.toLongOrNull() ?: _uiState.value.selectedDeckId
                if (deckId != null) {
                    onDrawCard(deckId)
                } else {
                    _uiState.update { it.copy(snackbarMessage = "No hay mazo seleccionado para robar") }
                }
            }
        }
    }

    // --- Scene Framing Assistant (Sprint 60) ---

    fun openSceneFraming() {
        _uiState.update { it.copy(showSceneFramingDialog = true) }
    }

    fun dismissSceneFraming() {
        _uiState.update { it.copy(showSceneFramingDialog = false) }
    }

    fun confirmSceneFraming(
        draft: SceneFramingDraft,
        closePrevious: Boolean,
        prevSummary: String,
        chaosDelta: Int
    ) {
        scope.launch {
            try {
                val currentActiveScene = _uiState.value.scenes.find { it.id == _uiState.value.focusedSceneId && !it.isCompleted }
                    ?: _uiState.value.scenes.firstOrNull { !it.isCompleted }

                if (closePrevious && currentActiveScene != null) {
                    val closure = SoloSceneClosure(
                        summary = prevSummary.ifBlank { "Conclusión de la escena." },
                        chaosDelta = chaosDelta
                    )
                    notesMutex.withLock {
                        sessionRepository.updateDmNotes(sessionId, _uiState.value.dmNotes)
                        val closed = CloseSoloSceneUseCase(sessionRepository)(currentActiveScene, closure)
                        _uiState.update { state ->
                            state.copy(
                                dmNotes = listOf(state.dmNotes, closed.journalEntry).filter { it.isNotBlank() }.joinToString("\n\n")
                            )
                        }
                    }
                    if (closure.chaosDelta != 0) {
                        onUpdateBoardMythicChaos(closure.chaosDelta)
                    }
                }

                val sceneCount = _uiState.value.scenes.size + 1
                val newScene = Scene(
                    sessionId = sessionId,
                    title = draft.title,
                    content = SoloSceneContext(
                        location = draft.location,
                        objective = draft.objective,
                        situation = draft.adjustedSituation.ifBlank { draft.expectation },
                        pendingQuestion = ""
                    ).toSceneContent(),
                    sortOrder = (_uiState.value.scenes.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                    linkedTableId = draft.linkedTableId,
                    linkedDeckId = draft.linkedDeckId
                )

                val newSceneId = sessionRepository.upsertScene(newScene)
                onFocusScene(newSceneId)

                // Registro estructurado en Markdown en el diario
                val journalEntry = buildFramedSceneJournalEntry(sceneCount, draft)
                notesMutex.withLock {
                    val currentNotes = _uiState.value.dmNotes
                    val updatedNotes = if (currentNotes.isBlank()) journalEntry else "$currentNotes\n\n$journalEntry"
                    _uiState.update { it.copy(dmNotes = updatedNotes, showSceneFramingDialog = false) }
                    sessionRepository.updateDmNotes(sessionId, updatedNotes)
                }

                val snackMsg = when (draft.checkOutcome?.result) {
                    SceneCheckResult.INTERRUPTED -> "¡Escena Interrumpida! Evento: ${draft.checkOutcome.eventFocus}"
                    SceneCheckResult.ALTERED -> "Escena Alterada: ${draft.title}"
                    else -> "Escena \"${draft.title}\" iniciada"
                }
                _uiState.update { it.copy(snackbarMessage = snackMsg) }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(snackbarMessage = "Error al encuadrar escena: ${e.message}", showSceneFramingDialog = false) }
            }
        }
    }
}
