package com.deckapp.feature.reference

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckapp.core.domain.repository.ManualRepository
import com.deckapp.core.domain.repository.WikiRepository
import com.deckapp.core.domain.usecase.reference.PerformOcrUseCase
import com.deckapp.core.model.ManualBookmark
import com.deckapp.core.model.OcrResult
import com.deckapp.core.model.WikiEntry
import com.deckapp.core.model.WikiCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ManualViewerUiState(
    val manualName: String = "Cargando...",
    val pageCount: Int = 0,
    val isLoading: Boolean = true,
    val pdfRenderer: PdfRenderer? = null,
    val bookmarks: List<ManualBookmark> = emptyList(),
    val isOcrMode: Boolean = false,
    val ocrResult: OcrResult? = null
)

@HiltViewModel
class ManualViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val manualRepository: ManualRepository,
    private val wikiRepository: WikiRepository,
    private val performOcrUseCase: PerformOcrUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val manualId: Long = savedStateHandle["manualId"] ?: 0L

    private val _uiState = MutableStateFlow(ManualViewerUiState())
    val uiState: StateFlow<ManualViewerUiState> = _uiState.asStateFlow()

    private var fileDescriptor: ParcelFileDescriptor? = null

    fun loadManual(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Cargar Manual y Marcadores en paralelo
            combine(
                manualRepository.getManualById(id),
                manualRepository.getBookmarksForManual(id)
            ) { manual, bookmarks -> manual to bookmarks }
                .firstOrNull()?.let { (manual, bookmarks) ->
                    if (manual == null) return@let

                    try {
                        val uri = Uri.parse(manual.uri)
                        fileDescriptor = if (uri.scheme == "content") {
                            context.contentResolver.openFileDescriptor(uri, "r")
                        } else {
                            ParcelFileDescriptor.open(File(uri.path ?: manual.uri), ParcelFileDescriptor.MODE_READ_ONLY)
                        }

                        if (fileDescriptor != null) {
                            val renderer = PdfRenderer(fileDescriptor!!)
                            _uiState.update { it.copy(
                                manualName = manual.title,
                                pageCount = renderer.pageCount,
                                pdfRenderer = renderer,
                                bookmarks = bookmarks,
                                isLoading = false
                            ) }
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun toggleOcrMode() {
        _uiState.update { it.copy(isOcrMode = !it.isOcrMode) }
    }

    fun performOcr(pageIndex: Int, area: Rect) {
        val renderer = _uiState.value.pdfRenderer ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val page = renderer.openPage(pageIndex)
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    // Recortar el área seleccionada (ajustando escala)
                    val cropRect = Rect(
                        (area.left * 2).toInt(), (area.top * 2).toInt(),
                        (area.right * 2).toInt(), (area.bottom * 2).toInt()
                    )
                    
                    // Asegurar límites
                    val finalRect = Rect(
                        cropRect.left.coerceIn(0, bitmap.width),
                        cropRect.top.coerceIn(0, bitmap.height),
                        cropRect.right.coerceIn(0, bitmap.width),
                        cropRect.bottom.coerceIn(0, bitmap.height)
                    )

                    if (finalRect.width() > 0 && finalRect.height() > 0) {
                        val croppedBitmap = Bitmap.createBitmap(bitmap, finalRect.left, finalRect.top, finalRect.width(), finalRect.height())
                        val result = performOcrUseCase(croppedBitmap)
                        _uiState.update { it.copy(ocrResult = result) }
                    }
                } catch (e: Exception) {
                    // Ignorar errores de renderizado en OCR
                }
            }
        }
    }

    fun clearOcrResult() {
        _uiState.update { it.copy(ocrResult = null) }
    }

    fun saveOcrToWiki() {
        val result = _uiState.value.ocrResult ?: return
        val manualName = _uiState.value.manualName
        viewModelScope.launch {
            // Buscar o crear categoría "Capturas de Manuales"
            val categories = wikiRepository.getCategories().first()
            var categoryId = categories.find { it.name == "Capturas de Manuales" }?.id
            
            if (categoryId == null) {
                categoryId = wikiRepository.saveCategory(WikiCategory(name = "Capturas de Manuales", iconName = "description"))
            }

            val entry = WikiEntry(
                categoryId = categoryId,
                title = "Captura: $manualName",
                content = result.text
            )
            wikiRepository.saveEntry(entry)
            clearOcrResult()
        }
    }

    override fun onCleared() {
        super.onCleared()
        _uiState.value.pdfRenderer?.close()
        fileDescriptor?.close()
    }
}
