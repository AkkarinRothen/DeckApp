package com.deckapp.core.model

/**
 * Representa un bloque de texto detectado por OCR.
 * [boundingBox] se usa para la heurística de proximidad espacial (agrupación en filas/columnas).
 * [confidence] es el promedio de confianza de los elementos individuales detectados por ML Kit (0.0–1.0).
 * Bloques con confidence < 0.5 deben tratarse como candidatos a revisión manual.
 */
data class OcrBlock(
    val text: String,
    val boundingBox: RectModel,
    val confidence: Float
)

data class OcrResult(
    val text: String,
    val confidence: Float
)

/**
 * Rectángulo en coordenadas Float para preservar precisión al escalar bitmaps.
 */
data class RectModel(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
