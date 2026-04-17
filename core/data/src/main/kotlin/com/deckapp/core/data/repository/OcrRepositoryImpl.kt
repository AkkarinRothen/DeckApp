package com.deckapp.core.data.repository

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.OcrException
import com.deckapp.core.domain.repository.OcrRepository
import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.RectModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrRepositoryImpl @Inject constructor() : OcrRepository {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Procesa el bitmap iterando a nivel [TextLine] (Block → Line → Element).
     * Usar Line como unidad base da bounding boxes más precisos que Block,
     * y elimina la necesidad de re-separar bloques multilínea en el use case.
     * La confidence se calcula como promedio de los TextElements de cada línea.
     */
    override suspend fun recognizeText(bitmap: Bitmap): Result<List<OcrBlock>> {
        return try {
            // Aplicar pre-procesamiento para mejorar la calidad del OCR
            val processedBitmap = ImagePreprocessor.prepare(bitmap)
            
            val image = InputImage.fromBitmap(processedBitmap, 0)
            val result = recognizer.process(image).await()
            val blocks = result.textBlocks.flatMap { block ->
                block.lines.mapNotNull { line ->
                    val rect = line.boundingBox ?: return@mapNotNull null
                    val confidence = if (line.elements.isEmpty()) 1.0f
                    else line.elements.mapNotNull { it.confidence }.average().toFloat()
                    OcrBlock(
                        text = line.text,
                        boundingBox = RectModel(
                            left = rect.left.toFloat(),
                            top = rect.top.toFloat(),
                            right = rect.right.toFloat(),
                            bottom = rect.bottom.toFloat()
                        ),
                        confidence = confidence
                    )
                }
            }
            Result.success(blocks)
        } catch (e: Exception) {
            Result.failure(OcrException("ML Kit falló al procesar la imagen: ${e.message}", e))
        }
    }
}
