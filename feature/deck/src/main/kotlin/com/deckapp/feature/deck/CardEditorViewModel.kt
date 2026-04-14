package com.deckapp.feature.deck

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.domain.repository.FileRepository
import com.deckapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardEditorUiState(
    val title: String = "",
    val suit: String = "",
    val value: String = "",
    val faces: List<CardFace> = listOf(CardFace(name = "Frente")),
    val selectedFaceIndex: Int = 0,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isPickingImage: Boolean = false   // true mientras se copia la imagen
)

@HiltViewModel
class CardEditorViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val fileRepository: FileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cardId: Long? = savedStateHandle["cardId"]
    private val deckId: Long = checkNotNull(savedStateHandle["deckId"])

    private val _uiState = MutableStateFlow(CardEditorUiState())
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    init {
        if (cardId != null) {
            viewModelScope.launch {
                cardRepository.getCardById(cardId).firstOrNull()?.let { card ->
                    _uiState.update {
                        it.copy(
                            title = card.title,
                            suit = card.suit ?: "",
                            value = card.value?.toString() ?: "",
                            faces = card.faces
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateSuit(suit: String) = _uiState.update { it.copy(suit = suit) }
    fun updateValue(value: String) = _uiState.update { it.copy(value = value) }
    fun selectFace(index: Int) = _uiState.update { it.copy(selectedFaceIndex = index) }

    fun updateFaceName(faceIndex: Int, name: String) {
        val faces = _uiState.value.faces.toMutableList()
        if (faceIndex < faces.size) {
            faces[faceIndex] = faces[faceIndex].copy(name = name)
            _uiState.update { it.copy(faces = faces) }
        }
    }

    fun updateFaceImagePath(index: Int, path: String?) {
        val faces = _uiState.value.faces.toMutableList()
        if (index < faces.size) {
            faces[index] = faces[index].copy(imagePath = path)
            _uiState.update { it.copy(faces = faces) }
        }
    }

    /** Copia la imagen seleccionada por el usuario a almacenamiento interno. */
    fun pickFaceImage(faceIndex: Int, uri: Uri) {
        _uiState.update { it.copy(isPickingImage = true) }
        viewModelScope.launch {
            runCatching {
                val fileName = "face_${faceIndex}_${System.currentTimeMillis()}.jpg"
                fileRepository.copyImageToInternal(uri, deckId, fileName)
            }.onSuccess { path ->
                updateFaceImagePath(faceIndex, path)
            }
            _uiState.update { it.copy(isPickingImage = false) }
        }
    }

    fun updateFaceContentMode(index: Int, mode: CardContentMode) {
        val faces = _uiState.value.faces.toMutableList()
        if (index < faces.size) {
            val zoneCount = when (mode) {
                CardContentMode.IMAGE_ONLY -> 0
                CardContentMode.IMAGE_WITH_TEXT -> 1
                CardContentMode.REVERSIBLE -> 2
                CardContentMode.TOP_BOTTOM_SPLIT, CardContentMode.LEFT_RIGHT_SPLIT -> 2
                CardContentMode.FOUR_EDGE_CUES, CardContentMode.FOUR_QUADRANT -> 4
                CardContentMode.DOUBLE_SIDED_FULL -> 1
            }
            val zones = List(zoneCount) {
                faces[index].zones.getOrElse(it) { ContentZone() }
            }
            faces[index] = faces[index].copy(contentMode = mode, zones = zones)
            _uiState.update { it.copy(faces = faces) }
        }
    }

    fun updateZoneText(faceIndex: Int, zoneIndex: Int, text: String) {
        val faces = _uiState.value.faces.toMutableList()
        if (faceIndex < faces.size) {
            val zones = faces[faceIndex].zones.toMutableList()
            if (zoneIndex < zones.size) {
                zones[zoneIndex] = zones[zoneIndex].copy(text = text)
                faces[faceIndex] = faces[faceIndex].copy(zones = zones)
                _uiState.update { it.copy(faces = faces) }
            }
        }
    }

    fun addFace() {
        val faces = _uiState.value.faces + CardFace(name = "Cara ${_uiState.value.faces.size + 1}")
        _uiState.update { it.copy(faces = faces) }
    }

    /** Elimina una cara. Ajusta selectedFaceIndex si era la cara eliminada o la última. */
    fun removeFace(faceIndex: Int) {
        val current = _uiState.value
        if (current.faces.size <= 1) return   // siempre debe quedar al menos 1 cara
        val faces = current.faces.toMutableList().also { it.removeAt(faceIndex) }
        val newIndex = current.selectedFaceIndex.coerceAtMost(faces.size - 1)
        _uiState.update { it.copy(faces = faces, selectedFaceIndex = newIndex) }
    }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val card = Card(
                id = cardId ?: 0L,
                stackId = deckId,
                originDeckId = deckId,
                title = state.title,
                suit = state.suit.takeIf { it.isNotBlank() },
                value = state.value.toIntOrNull(),
                faces = state.faces,
                sortOrder = 0
            )
            cardRepository.saveCard(card)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}
