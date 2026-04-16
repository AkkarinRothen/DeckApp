package com.deckapp.core.domain.usecase

import android.net.Uri
import com.deckapp.core.domain.repository.FileRepository
import javax.inject.Inject

/**
 * UseCase para leer el contenido de texto de un URI de archivo (CSV, JSON, TXT).
 * Encapsula el acceso a [FileRepository] para que los ViewModels de feature
 * no dependan directamente del repositorio de archivos.
 */
class ReadTextFromUriUseCase @Inject constructor(
    private val fileRepository: FileRepository
) {
    suspend operator fun invoke(uri: Uri): String = fileRepository.readTextFromUri(uri)
}
