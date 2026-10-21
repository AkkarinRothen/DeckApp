package com.deckapp.feature.draw.delegates

import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.domain.usecase.*
import com.deckapp.core.domain.usecase.mythic.FateCheckUseCase
import com.deckapp.core.domain.usecase.mythic.MythicReferenceData
import com.deckapp.core.domain.usecase.mythic.RollMythicFateUseCase
import com.deckapp.core.model.*
import com.deckapp.feature.draw.SessionUiState
import com.deckapp.feature.draw.components.threads.ThreadsOverlayTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Delegado encargado de las operaciones del Oráculo Mítico, diario en solitario,
 * gestión de hilos/personajes y captura de inspiración en el tablero.
 */
class SessionMythicDelegate(
    private val sessionId: Long,
    private val _uiState: MutableStateFlow<SessionUiState>,
    private val scope: CoroutineScope,
    private val mythicRepository: MythicRepository,
    private val sessionRepository: SessionRepository,
    private val rollMythicFateUseCase: RollMythicFateUseCase,
    private val notesMutex: Mutex,
    private val onUpdateNotes: (String) -> Unit,
    private val updateBoardMemory: ((BoardSessionMemory) -> BoardSessionMemory) -> Unit,
    private val focusScene: (Long?) -> Unit,
    private val onLogOracle: ((question: String, answer: String, chaos: Int) -> Unit)? = null
) {

    fun performMythicFateCheck(question: String, prob: ProbabilityLevel) {
        val mythicId = _uiState.value.linkedMythicSession?.id ?: return
        val chaos = _uiState.value.linkedMythicSession?.chaosFactor ?: 5
        scope.launch {
            val roll = rollMythicFateUseCase(mythicId, question, prob)
            onLogOracle?.invoke(question, roll.result.name, chaos)
        }
    }

    fun quickMythicCheck() = performMythicFateCheck("Consulta rápida", ProbabilityLevel.FIFTY_FIFTY)

    fun updateMythicState(transform: (com.deckapp.feature.draw.model.state.SoloMythicUiState) -> com.deckapp.feature.draw.model.state.SoloMythicUiState) {
        _uiState.update { it.copy(soloMythic = transform(it.soloMythic)) }
    }

    fun updateMythicChaos(delta: Int) {
        val session = _uiState.value.linkedMythicSession ?: return
        val next = (session.chaosFactor + delta).coerceIn(1, 9)
        scope.launch {
            mythicRepository.updateChaosFactor(session.id, next)
        }
    }

    fun setBoardMythicProbability(prob: ProbabilityLevel) {
        updateMythicState { it.copy(probability = prob) }
    }

    fun updateBoardMythicChaos(delta: Int) {
        val currentChaos = _uiState.value.linkedMythicSession?.chaosFactor ?: _uiState.value.boardMythicChaos
        val next = (currentChaos + delta).coerceIn(1, 9)
        updateMythicState { it.copy(chaos = next) }
        _uiState.value.linkedMythicSession?.let { session ->
            scope.launch {
                mythicRepository.updateChaosFactor(session.id, next)
            }
        }
    }

    fun rollBoardMythicFate(
        probability: ProbabilityLevel = _uiState.value.boardMythicProbability,
        forcedRoll: Int? = null,
        question: String = ""
    ) {
        val chaos = _uiState.value.linkedMythicSession?.chaosFactor ?: _uiState.value.boardMythicChaos
        val rollValue = forcedRoll ?: Random.nextInt(1, 101)
        val fateCheckUseCase = FateCheckUseCase()
        val (result, isRandomEvent) = fateCheckUseCase(probability, chaos, rollValue)

        val fateChart = arrayOf(
            intArrayOf(95, 97, 99, 99, 99, 99, 99, 99, 99),
            intArrayOf(75, 83, 90, 95, 97, 99, 99, 99, 99),
            intArrayOf(55, 65, 75, 83, 90, 95, 97, 99, 99),
            intArrayOf(35, 45, 55, 65, 75, 83, 90, 95, 97),
            intArrayOf(25, 35, 45, 55, 65, 75, 83, 90, 95),
            intArrayOf(15, 25, 35, 45, 55, 65, 75, 83, 90),
            intArrayOf(10, 15, 25, 35, 45, 55, 65, 75, 83),
            intArrayOf(5, 10, 15, 25, 35, 45, 55, 65, 75),
            intArrayOf(3, 5, 10, 15, 25, 35, 45, 55, 65)
        )
        val targetThreshold = fateChart[probability.ordinal][chaos - 1]

        val randomEventDesc = if (isRandomEvent) {
            val action = MythicReferenceData.actionKeywords.random()
            val subject = MythicReferenceData.subjectKeywords.random()
            "$action · $subject"
        } else null

        val fateItem = BoardMythicFateResult(
            odds = probability,
            chaos = chaos,
            roll = rollValue,
            targetThreshold = targetThreshold,
            fateResult = result,
            isRandomEvent = isRandomEvent,
            randomEventDescription = randomEventDesc,
            question = question.trim(),
            sceneTitle = activeSoloScene()?.title.orEmpty(),
            sceneId = activeSoloScene()?.id
        )

        updateMythicState { state ->
            state.copy(
                probability = probability,
                chaos = chaos,
                lastResult = fateItem,
                history = (listOf(fateItem) + state.history).take(10)
            )
        }

        onLogOracle?.invoke(question.ifBlank { "Pregunta al Destino" }, result.name, chaos)

        _uiState.value.linkedMythicSession?.let { session ->
            scope.launch {
                mythicRepository.saveRoll(
                    MythicRoll(
                        sessionId = session.id,
                        question = question.trim().ifBlank { "Consulta Bento" },
                        probability = probability,
                        chaosFactor = chaos,
                        roll = rollValue,
                        result = result,
                        isRandomEvent = isRandomEvent,
                        eventAction = randomEventDesc ?: "",
                        sceneNumber = session.sceneNumber
                    )
                )
            }
        }
    }

    fun rollBoardMythicRandomEvent(): String {
        val action = MythicReferenceData.actionKeywords.random()
        val subject = MythicReferenceData.subjectKeywords.random()
        val eventText = "$action · $subject"
        updateMythicState { it.copy(lastEvent = eventText) }
        return eventText
    }

    fun appendToNotes(textToAppend: String) {
        val current = _uiState.value.dmNotes
        val updated = if (current.isBlank()) textToAppend else "$current\n\n$textToAppend"
        onUpdateNotes(updated)
    }

    fun activeSoloScene(): Scene? = _uiState.value.let { state ->
        state.scenes.firstOrNull { it.id == state.focusedSceneId && !it.isCompleted }
            ?: state.scenes.firstOrNull { !it.isCompleted }
    }

    fun saveSoloScene(scene: Scene, onSuccess: () -> Unit) {
        if (_uiState.value.isSoloSceneBusy || scene.title.isBlank() || scene.sessionId != sessionId) return
        updateMythicState { it.copy(isSoloSceneBusy = true) }
        scope.launch {
            try {
                val id = sessionRepository.upsertScene(scene)
                focusScene(id)
                onSuccess()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo guardar la escena: ${error.message}") }
            } finally {
                updateMythicState { it.copy(isSoloSceneBusy = false) }
            }
        }
    }

    fun closeSoloScene(scene: Scene, closure: SoloSceneClosure, onSuccess: () -> Unit) {
        if (_uiState.value.isSoloSceneBusy || scene.sessionId != sessionId) return
        updateMythicState { it.copy(isSoloSceneBusy = true) }
        scope.launch {
            try {
                notesMutex.withLock {
                    sessionRepository.updateDmNotes(sessionId, _uiState.value.dmNotes)
                    val closed = CloseSoloSceneUseCase(sessionRepository)(scene, closure)
                    _uiState.update { state ->
                        state.copy(
                            dmNotes = listOf(state.dmNotes, closed.journalEntry).filter { it.isNotBlank() }.joinToString("\n\n"),
                            focusedSceneId = closed.nextSceneId
                        )
                    }
                    val savedNotes = _uiState.value.dmNotes
                    sessionRepository.updateDmNotes(sessionId, savedNotes)
                    _uiState.update { state -> if (state.dmNotes == savedNotes) state.copy(isSavingNotes = false) else state }
                }
                updateBoardMemory { it.copy(focusedSceneId = _uiState.value.focusedSceneId) }
                if (closure.chaosDelta != 0) updateBoardMythicChaos(closure.chaosDelta)
                onSuccess()
                _uiState.update { it.copy(snackbarMessage = "Escena cerrada y guardada en el diario") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo cerrar la escena: ${error.message}") }
            } finally {
                updateMythicState { it.copy(isSoloSceneBusy = false) }
            }
        }
    }

    suspend fun ensureLinkedMythicSessionId(): Long {
        val currentId = _uiState.value.linkedMythicSession?.id ?: _uiState.value.session?.linkedMythicSessionId
        if (currentId != null) return currentId
        val sessionName = "Mítico - ${_uiState.value.session?.name ?: "Sesión"}"
        val newSessionId = mythicRepository.saveSession(MythicSession(name = sessionName))
        linkMythicSession(newSessionId)
        return newSessionId
    }

    fun linkMythicSession(mythicSessionId: Long?) {
        _uiState.update { it.copy(linkedMythicSession = null) }
        scope.launch {
            sessionRepository.updateLinkedMythicSession(sessionId, mythicSessionId)
        }
    }

    fun createAndLinkMythicSession(name: String? = null) {
        scope.launch {
            val sessionName = name ?: "Mítico - ${_uiState.value.session?.name ?: "Sesión"}"
            val mythicId = mythicRepository.saveSession(MythicSession(name = sessionName))
            linkMythicSession(mythicId)
            _uiState.update { it.copy(snackbarMessage = "Sesión Mythic vinculada: $sessionName") }
        }
    }

    fun toggleThreadsOverlayExpanded() {
        _uiState.update { it.copy(isThreadsOverlayExpanded = !it.isThreadsOverlayExpanded) }
    }

    fun selectThreadsTab(tab: ThreadsOverlayTab) {
        _uiState.update { it.copy(selectedThreadsTab = tab) }
    }

    fun updateModalsState(transform: (com.deckapp.feature.draw.model.state.SessionModalsUiState) -> com.deckapp.feature.draw.model.state.SessionModalsUiState) {
        _uiState.update { it.copy(modals = transform(it.modals)) }
    }

    fun openCreateThreadDialog() {
        updateModalsState { it.copy(showThreadEditDialog = true, editingMythicThread = null) }
    }

    fun openEditThreadDialog(thread: MythicThread) {
        updateModalsState { it.copy(showThreadEditDialog = true, editingMythicThread = thread) }
    }

    fun closeThreadDialog() {
        updateModalsState { it.copy(showThreadEditDialog = false, editingMythicThread = null) }
    }

    fun openCreateCharacterDialog() {
        updateModalsState { it.copy(showMythicCharacterEditDialog = true, editingMythicCharacter = null) }
    }

    fun openEditCharacterDialog(character: MythicCharacter) {
        updateModalsState { it.copy(showMythicCharacterEditDialog = true, editingMythicCharacter = character) }
    }

    fun closeCharacterDialog() {
        updateModalsState { it.copy(showMythicCharacterEditDialog = false, editingMythicCharacter = null) }
    }

    fun saveMythicThread(id: Long, description: String) {
        if (description.isBlank()) return
        scope.launch {
            val mythicId = ensureLinkedMythicSessionId()
            if (id == 0L) {
                mythicRepository.addThread(MythicThread(sessionId = mythicId, description = description.trim()))
            } else {
                val existing = _uiState.value.mythicState?.threads?.find { it.id == id }
                mythicRepository.addThread(
                    existing?.copy(description = description.trim())
                        ?: MythicThread(id = id, sessionId = mythicId, description = description.trim())
                )
            }
            updateModalsState { it.copy(showThreadEditDialog = false, editingMythicThread = null) }
        }
    }

    fun saveMythicCharacter(id: Long, name: String, notes: String) {
        if (name.isBlank()) return
        scope.launch {
            val mythicId = ensureLinkedMythicSessionId()
            if (id == 0L) {
                mythicRepository.addCharacter(MythicCharacter(sessionId = mythicId, name = name.trim(), notes = notes.trim()))
            } else {
                val existing = _uiState.value.mythicState?.characters?.find { it.id == id }
                mythicRepository.addCharacter(
                    existing?.copy(name = name.trim(), notes = notes.trim())
                        ?: MythicCharacter(id = id, sessionId = mythicId, name = name.trim(), notes = notes.trim())
                )
            }
            updateModalsState { it.copy(showMythicCharacterEditDialog = false, editingMythicCharacter = null) }
        }
    }


    fun rollRandomThreadOrCharacter() {
        val tab = _uiState.value.selectedThreadsTab
        val roll = (1..100).random()
        if (tab == ThreadsOverlayTab.THREADS) {
            val activeThreads = _uiState.value.mythicState?.threads?.filterNot { it.isResolved } ?: emptyList()
            if (activeThreads.isEmpty()) {
                updateMythicState { it.copy(lastRandomMythicPick = "🎲 [d100: $roll] No hay hilos activos registrados para sortear.") }
            } else {
                val picked = activeThreads.random()
                updateMythicState { it.copy(lastRandomMythicPick = "🎲 [d100: $roll] Hilo Sorteado: \"${picked.description}\"") }
            }
        } else {
            val characters = _uiState.value.mythicState?.characters ?: emptyList()
            if (characters.isEmpty()) {
                updateMythicState { it.copy(lastRandomMythicPick = "🎲 [d100: $roll] No hay PNJs registrados en la lista de Mythic.") }
            } else {
                val picked = characters.random()
                val notesSnippet = if (picked.notes.isNotBlank()) " (${picked.notes})" else ""
                updateMythicState { it.copy(lastRandomMythicPick = "🎲 [d100: $roll] PNJ Sorteado: \"${picked.name}\"$notesSnippet") }
            }
        }
    }

    fun clearRandomMythicPick() {
        updateMythicState { it.copy(lastRandomMythicPick = null) }
    }

    fun copyLastRandomPickToDiary() {
        val pick = _uiState.value.lastRandomMythicPick ?: return
        scope.launch {
            val currentNotes = _uiState.value.dmNotes
            val addition = "\n\n> **Oráculo de Trama / Evento:** $pick\n"
            val newNotes = currentNotes + addition
            _uiState.update { it.copy(dmNotes = newNotes, soloMythic = it.soloMythic.copy(lastRandomMythicPick = null)) }
            sessionRepository.updateDmNotes(sessionId, newNotes)
        }
    }

    fun copyThreadsSummaryToDiary() {
        scope.launch {
            val threads = _uiState.value.mythicState?.threads ?: emptyList()
            val characters = _uiState.value.mythicState?.characters ?: emptyList()

            val activeThreads = threads.filterNot { it.isResolved }
            val resolvedThreads = threads.filter { it.isResolved }

            val sb = java.lang.StringBuilder()
            sb.append("\n\n### 🧵 Resumen de Hilos y Amenazas\n")

            if (activeThreads.isNotEmpty()) {
                sb.append("**Hilos Activos:**\n")
                activeThreads.forEach { sb.append("- [ ] ${it.description}\n") }
            }
            if (resolvedThreads.isNotEmpty()) {
                sb.append("**Hilos Resueltos:**\n")
                resolvedThreads.forEach { sb.append("- [x] ${it.description}\n") }
            }
            if (characters.isNotEmpty()) {
                sb.append("\n**Personajes y Amenazas:**\n")
                characters.forEach {
                    val note = if (it.notes.isNotBlank()) " — *${it.notes}*" else ""
                    sb.append("- **${it.name}**$note\n")
                }
            }

            val currentNotes = _uiState.value.dmNotes
            val newNotes = currentNotes + sb.toString()
            _uiState.update { it.copy(dmNotes = newNotes) }
            sessionRepository.updateDmNotes(sessionId, newNotes)
        }
    }

    fun addMythicCharacter(name: String) {
        if (name.isBlank()) return
        scope.launch {
            val mythicId = ensureLinkedMythicSessionId()
            mythicRepository.addCharacter(MythicCharacter(sessionId = mythicId, name = name.trim()))
        }
    }

    fun deleteMythicCharacter(id: Long) {
        scope.launch {
            mythicRepository.deleteCharacter(id)
        }
    }

    fun updateMythicCharacterNotes(id: Long, notes: String) {
        scope.launch {
            mythicRepository.updateCharacterNotes(id, notes)
        }
    }

    fun sendMythicCharacterToNotes(character: MythicCharacter) {
        scope.launch {
            val addition = "\n\n[Personaje Mítico: ${character.name}]\n${character.notes}".trim()
            val newNotes = (_uiState.value.dmNotes + "\n\n" + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun addMythicThread(description: String) {
        if (description.isBlank()) return
        scope.launch {
            val mythicId = ensureLinkedMythicSessionId()
            mythicRepository.addThread(MythicThread(sessionId = mythicId, description = description.trim()))
        }
    }

    fun updateMythicThreadDescription(id: Long, description: String) {
        saveMythicThread(id, description)
    }

    fun toggleMythicThreadStatus(id: Long, isResolved: Boolean) {
        scope.launch {
            mythicRepository.updateThreadStatus(id, isResolved)
        }
    }

    fun deleteMythicThread(id: Long) {
        scope.launch {
            mythicRepository.deleteThread(id)
        }
    }

    fun sendMythicThreadToNotes(thread: MythicThread) {
        scope.launch {
            val addition = "\n\n[Hilo Mítico: ${thread.description}]"
            val newNotes = (_uiState.value.dmNotes + "\n\n" + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun sendRawToNotes(text: String) {
        scope.launch {
            val currentNotes = _uiState.value.dmNotes
            val addition = "\n\n$text".trim()
            val newNotes = (currentNotes + "\n\n" + addition).trim()
            _uiState.update { it.copy(dmNotes = newNotes) }
            notesMutex.withLock {
                if (_uiState.value.dmNotes == newNotes) sessionRepository.updateDmNotes(sessionId, newNotes)
            }
        }
    }

    fun captureBoardInspiration(item: BoardInspiration) {
        updateBoardMemory { it.captureInspiration(item) }
    }

    fun setBoardInspirationStatus(id: String, status: InspirationStatus) {
        updateBoardMemory { it.setInspirationStatus(id, status) }
    }

    fun sendBoardInspirationToDiary(id: String) {
        if (_uiState.value.isSendingInspiration || _uiState.value.isSoloSceneBusy) return
        val item = _uiState.value.boardMemory.inspiration.firstOrNull { it.id == id } ?: return
        if (item.status == InspirationStatus.USED) return
        updateMythicState { it.copy(isSendingInspiration = true) }
        _uiState.update { it.copy(isSavingNotes = true) }
        scope.launch {
            var savedNotes: String? = null
            try {
                notesMutex.withLock {
                    val entry = item.toDiaryEntry()
                    _uiState.update { state ->
                        val notes = if (entry in state.dmNotes) state.dmNotes
                            else listOf(state.dmNotes, entry).filter { it.isNotBlank() }.joinToString("\n\n")
                        state.copy(dmNotes = notes)
                    }
                    savedNotes = _uiState.value.dmNotes
                    sessionRepository.updateDmNotes(sessionId, savedNotes.orEmpty())
                }
                setBoardInspirationStatus(id, InspirationStatus.USED)
                _uiState.update { it.copy(snackbarMessage = "Inspiración guardada en el diario") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(snackbarMessage = "No se pudo guardar: ${error.message}") }
            } finally {
                updateMythicState { it.copy(isSendingInspiration = false) }
                _uiState.update { it.copy(
                    isSavingNotes = if (it.dmNotes == savedNotes || savedNotes == null) false else it.isSavingNotes) }
            }
        }
    }
}
