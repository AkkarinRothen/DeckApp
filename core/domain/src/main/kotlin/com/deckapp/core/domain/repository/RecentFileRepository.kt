package com.deckapp.core.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Tipo de archivo reciente.
 */
enum class RecentFileType { PDF, FOLDER }

/**
 * Modelo de dominio para un archivo o carpeta accedida recientemente.
 */
data class RecentFile(
    val uri: Uri,
    val name: String,
    val type: RecentFileType,
    val lastAccessed: Long
)

/**
 * Repositorio para gestionar el historial de archivos y carpetas accedidas.
 */
interface RecentFileRepository {
    /**
     * Obtiene los archivos recientes ordenados por acceso.
     * @param limit Cantidad máxima de registros.
     */
    fun getRecentFiles(limit: Int = 10): Flow<List<RecentFile>>

    /**
     * Registra el acceso a un archivo o carpeta.
     * Si ya existe, actualiza el timestamp.
     */
    suspend fun addRecentFile(uri: Uri, name: String, type: RecentFileType)

    /**
     * Elimina un registro del historial.
     */
    suspend fun removeRecentFile(uri: Uri)
}
