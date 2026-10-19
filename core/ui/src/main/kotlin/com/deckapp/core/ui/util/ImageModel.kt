package com.deckapp.core.ui.util

import android.net.Uri
import java.io.File

/**
 * Normaliza una ruta o URI de imagen para ser interpretada correctamente por Coil.
 * Soporta URIs de assets ("file:///android_asset/..."), content URIs ("content://..."),
 * URLs web ("http(s)://..."), URIs de archivo ("file://...") y rutas locales relativas/absolutas.
 */
fun toCoilModel(path: String?): Any? {
    if (path == null) return null
    return when {
        path.startsWith("file:///android_asset/") -> path
        path.startsWith("content://") -> Uri.parse(path)
        path.startsWith("http://") || path.startsWith("https://") -> path
        path.startsWith("file://") -> Uri.parse(path)
        else -> File(path)
    }
}

@JvmName("extensionToCoilModel")
fun String?.toCoilModel(): Any? = toCoilModel(this)

