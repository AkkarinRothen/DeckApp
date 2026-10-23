package com.deckapp.feature.deck

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.deckapp.core.model.Card
import com.deckapp.core.ui.util.toCoilModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/** Rasteriza la cara activa a JPEG para compartir solo la imagen, sin notas ni metadatos del mazo. */
internal suspend fun createCardFaceShareUri(context: Context, card: Card): Uri = withContext(Dispatchers.IO) {
    val imagePath = card.activeFace.imagePath
        ?: throw IllegalStateException("Esta cara no tiene una imagen para compartir")
    val model = toCoilModel(imagePath)
        ?: throw IllegalStateException("No se encontró la imagen de la carta")

    val request = ImageRequest.Builder(context)
        .data(model)
        .size(2048, 2048)
        .allowHardware(false)
        .build()
    val result = ImageLoader(context).execute(request)
    if (result !is SuccessResult) throw IllegalStateException("No se pudo cargar la imagen de la carta")

    val drawable = result.drawable
    val sourceWidth = drawable.intrinsicWidth.coerceAtLeast(1)
    val sourceHeight = drawable.intrinsicHeight.coerceAtLeast(1)
    val scale = minOf(1f, 2048f / maxOf(sourceWidth, sourceHeight))
    val width = (sourceWidth * scale).roundToInt().coerceAtLeast(1)
    val height = (sourceHeight * scale).roundToInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)

        val imageDirectory = File(context.cacheDir, "images").apply { mkdirs() }
        val imageFile = File(imageDirectory, "card-${card.id}-face-${card.currentFaceIndex}.jpg")
        FileOutputStream(imageFile).use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)) {
                throw IllegalStateException("No se pudo preparar el JPEG")
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
    } finally {
        bitmap.recycle()
    }
}
