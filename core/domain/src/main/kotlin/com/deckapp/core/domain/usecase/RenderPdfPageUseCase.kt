package com.deckapp.core.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * UseCase para renderizar páginas de un PDF a Bitmap.
 * Encapsula el acceso al [FileRepository] de forma que los ViewModels
 * de feature modules no necesiten inyectar el repositorio directamente.
 */
class RenderPdfPageUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend fun getPageCount(uri: Uri): Int = fileRepository.getPdfPageCount(uri)

    suspend fun renderPage(uri: Uri, pageIndex: Int, targetWidth: Int = 1200): Bitmap? =
        fileRepository.renderPdfPageToBitmap(uri, pageIndex, targetWidth)
}
