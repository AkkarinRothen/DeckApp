package com.deckapp.feature.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.SessionRepository
import com.deckapp.core.model.Scene
import com.deckapp.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlannerUiState(
    val session: Session? = null,
    val scenes: List<Scene> = emptyList(),
    val allDecks: List<com.deckapp.core.model.CardStack> = emptyList(),
    val allTables: List<com.deckapp.core.model.RandomTable> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
)

@HiltViewModel
class SessionPlannerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val cardRepository: com.deckapp.core.domain.repository.CardRepository,
    private val tableRepository: com.deckapp.core.domain.repository.TableRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                sessionRepository.getSessionById(sessionId),
                sessionRepository.getScenesForSession(sessionId),
                cardRepository.getAllDecks(),
                tableRepository.getAllTables()
            ) { session, scenes, decks, tables ->
                _uiState.update { it.copy(
                    session = session,
                    scenes = scenes,
                    allDecks = decks,
                    allTables = tables,
                    isLoading = false
                ) }
            }.collect()
        }
    }

    fun addScene() {
        val nextOrder = (_uiState.value.scenes.maxOfOrNull { it.sortOrder } ?: -1) + 1
        viewModelScope.launch {
            sessionRepository.upsertScene(
                Scene(
                    sessionId = sessionId,
                    title = "Nueva Escena",
                    sortOrder = nextOrder
                )
            )
        }
    }

    fun updateSceneTitle(sceneId: Long, title: String) {
        val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return
        viewModelScope.launch {
            sessionRepository.upsertScene(scene.copy(title = title))
        }
    }

    fun updateSceneContent(sceneId: Long, content: String) {
        val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return
        viewModelScope.launch {
            sessionRepository.upsertScene(scene.copy(content = content))
        }
    }

    fun deleteScene(sceneId: Long) {
        viewModelScope.launch {
            sessionRepository.deleteScene(sceneId)
        }
    }

    fun moveSceneUp(sceneId: Long) {
        val scenes = _uiState.value.scenes
        val index = scenes.indexOfFirst { it.id == sceneId }
        if (index > 0) {
            val scene = scenes[index]
            val previous = scenes[index - 1]
            viewModelScope.launch {
                sessionRepository.updateSceneOrder(scene.id, previous.sortOrder)
                sessionRepository.updateSceneOrder(previous.id, scene.sortOrder)
            }
        }
    }

    fun moveSceneDown(sceneId: Long) {
        val scenes = _uiState.value.scenes
        val index = scenes.indexOfFirst { it.id == sceneId }
        if (index != -1 && index < scenes.size - 1) {
            val scene = scenes[index]
            val next = scenes[index + 1]
            viewModelScope.launch {
                sessionRepository.updateSceneOrder(scene.id, next.sortOrder)
                sessionRepository.updateSceneOrder(next.id, scene.sortOrder)
            }
        }
    }
    fun updateSceneTrigger(sceneId: Long, tableId: Long?, deckId: Long?) {
        val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return
        viewModelScope.launch {
            sessionRepository.upsertScene(scene.copy(linkedTableId = tableId, linkedDeckId = deckId))
        }
    }

    fun updateSceneMood(sceneId: Long, imagePath: String?) {
        val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return
        viewModelScope.launch {
            sessionRepository.upsertScene(scene.copy(imagePath = imagePath))
        }
    }

    fun toggleSceneAlternative(sceneId: Long) {
        val scene = _uiState.value.scenes.find { it.id == sceneId } ?: return
        viewModelScope.launch {
            sessionRepository.upsertScene(scene.copy(isAlternative = !scene.isAlternative))
        }
    }
}
