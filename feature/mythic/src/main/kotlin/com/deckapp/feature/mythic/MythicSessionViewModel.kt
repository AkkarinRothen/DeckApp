package com.deckapp.feature.mythic

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.MythicRepository
import com.deckapp.core.domain.repository.TableRepository
import com.deckapp.core.domain.usecase.SearchResult
import com.deckapp.core.domain.usecase.GlobalSearchResultType
import com.deckapp.core.domain.usecase.mythic.*
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

data class MythicSessionUiState(
    val session: MythicSession? = null,
    val characters: List<MythicCharacter> = emptyList(),
    val threads: List<MythicThread> = emptyList(),
    val rolls: List<MythicRoll> = emptyList(),
    val allTables: List<RandomTable> = emptyList(),
    val isLoading: Boolean = true,
    val isFateChecking: Boolean = false,
    val sceneCheckResult: SceneCheckResult? = null
)

@HiltViewModel
class MythicSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mythicRepository: MythicRepository,
    private val tableRepository: TableRepository,
    private val rollMythicFateUseCase: RollMythicFateUseCase,
    private val sceneCheckUseCase: SceneCheckUseCase,
    private val manageMythicSessionUseCase: ManageMythicSessionUseCase,
    private val deleteMythicSessionUseCase: DeleteMythicSessionUseCase
) : ViewModel() {

    private val sessionId: Long = savedStateHandle["sessionId"] ?: 0L

    private val _uiState = MutableStateFlow(MythicSessionUiState())
    val uiState: StateFlow<MythicSessionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            mythicRepository.getSessionById(sessionId),
            mythicRepository.getCharacters(sessionId),
            mythicRepository.getThreads(sessionId),
            mythicRepository.getRolls(sessionId),
            tableRepository.getAllTables()
        ) { session, characters, threads, rolls, tables ->
            MythicSessionUiState(
                session = session,
                characters = characters,
                threads = threads,
                rolls = rolls,
                allTables = tables,
                isLoading = false
            )
        }.onEach { newState ->
            _uiState.update { it.copy(
                session = newState.session,
                characters = newState.characters,
                threads = newState.threads,
                rolls = newState.rolls,
                allTables = newState.allTables,
                isLoading = false
            ) }
        }.launchIn(viewModelScope)
    }

    fun updateChaosFactor(delta: Int) {
        val current = _uiState.value.session?.chaosFactor ?: 5
        val next = (current + delta).coerceIn(1, 9)
        viewModelScope.launch {
            manageMythicSessionUseCase.updateChaosFactor(sessionId, next)
        }
    }

    fun updateSceneNumber(delta: Int) {
        val current = _uiState.value.session?.sceneNumber ?: 1
        val next = (current + delta).coerceAtLeast(1)
        viewModelScope.launch {
            manageMythicSessionUseCase.updateSceneNumber(sessionId, next)
        }
    }

    fun checkScene() {
        val cf = _uiState.value.session?.chaosFactor ?: 5
        val roll = Random.nextInt(1, 11)
        val result = sceneCheckUseCase(cf, roll)
        _uiState.update { it.copy(sceneCheckResult = result) }
    }

    fun clearSceneCheck() = _uiState.update { it.copy(sceneCheckResult = null) }

    fun performFateCheck(question: String, probability: ProbabilityLevel) {
        _uiState.update { it.copy(isFateChecking = true) }
        viewModelScope.launch {
            try {
                rollMythicFateUseCase(sessionId, question, probability)
            } finally {
                _uiState.update { it.copy(isFateChecking = false) }
            }
        }
    }

    fun quickFateCheck() {
        performFateCheck("Consulta rápida", ProbabilityLevel.FIFTY_FIFTY)
    }

    fun finishScene(notes: String, pcInControl: Boolean) {
        val currentSession = _uiState.value.session ?: return
        val chaosDelta = if (pcInControl) -1 else 1
        
        viewModelScope.launch {
            // 1. Guardar log de la escena (podría ser un Roll especial o una nota en Threads)
            // Por ahora, solo ajustamos CF e incrementamos escena
            manageMythicSessionUseCase.updateChaosFactor(sessionId, (currentSession.chaosFactor + chaosDelta).coerceIn(1, 9))
            manageMythicSessionUseCase.updateSceneNumber(sessionId, currentSession.sceneNumber + 1)
        }
    }

    // Configuration
    fun setTables(actionTableId: Long?, subjectTableId: Long?) {
        val current = _uiState.value.session ?: return
        viewModelScope.launch {
            manageMythicSessionUseCase.saveSession(
                current.copy(actionTableId = actionTableId, subjectTableId = subjectTableId)
            )
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            deleteMythicSessionUseCase(id)
        }
    }

    // Characters
    fun addCharacter(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            manageMythicSessionUseCase.addCharacter(sessionId, name)
        }
    }

    fun deleteCharacter(id: Long) {
        viewModelScope.launch {
            manageMythicSessionUseCase.deleteCharacter(id)
        }
    }

    // Threads
    fun addThread(description: String) {
        if (description.isBlank()) return
        viewModelScope.launch {
            manageMythicSessionUseCase.addThread(sessionId, description)
        }
    }

    fun toggleThreadStatus(id: Long, isResolved: Boolean) {
        viewModelScope.launch {
            manageMythicSessionUseCase.updateThreadStatus(id, isResolved)
        }
    }

    fun deleteThread(id: Long) {
        viewModelScope.launch {
            manageMythicSessionUseCase.deleteThread(id)
        }
    }

    fun linkSearchResult(result: SearchResult) {
        viewModelScope.launch {
            when (result.type) {
                GlobalSearchResultType.NPC -> addCharacter(result.title)
                GlobalSearchResultType.WIKI, GlobalSearchResultType.RULE -> addThread(result.title)
                else -> { /* Ignorar otros */ }
            }
        }
    }
}
