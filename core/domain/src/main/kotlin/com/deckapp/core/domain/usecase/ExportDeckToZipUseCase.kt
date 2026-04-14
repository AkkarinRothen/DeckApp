package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * Exporta las imágenes de un mazo a un archivo ZIP.
 */
class ExportDeckToZipUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(deckId: Long, outputUri: Uri): Result<Unit> {
        return fileRepository.zipDeckDirectory(deckId, outputUri)
    }
}
