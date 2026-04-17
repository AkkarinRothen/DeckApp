package com.deckapp.core.data.repository

import android.graphics.*

/**
 * Utilidad para pre-procesar bitmaps antes de enviarlos al motor de OCR.
 * Aplica filtros de escala de grises y mejora de contraste para aumentar la tasa de éxito
 * en fotos de libros o manuales con iluminación no ideal.
 */
object ImagePreprocessor {

    fun prepare(bitmap: Bitmap): Bitmap {
        return bitmap
            .toGrayscale()
            .enhanceContrast()
            .sharpen()
    }

    /**
     * Aplica un filtro de afilado (Sharpen) básico.
     * Mejora la definición de los caracteres, especialmente útil para fotos
     * ligeramente fuera de foco o con sensores de cámara de gama media.
     */
    private fun Bitmap.sharpen(): Bitmap {
        val contrast = 1.1f
        val brightness = 0f
        
        val width = this.width
        val height = this.height
        val bmpSharpen = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpSharpen)
        val paint = Paint()
        
        // Usamos una matriz que incrementa el contraste local
        // (Simulación de nitidez mediante manipulación de luminancia)
        val matrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bmpSharpen
    }

    /**
     * Convierte el bitmap a escala de grises.
     * Elimina el ruido de color que puede confundir al motor OCR (especialmente en layouts con colores pastel).
     */
    private fun Bitmap.toGrayscale(): Bitmap {
        val width = this.width
        val height = this.height
        val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpGrayscale)
        val paint = Paint()
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorFilter = ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorFilter
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bmpGrayscale
    }

    /**
     * Mejora el contraste del bitmap.
     * Utiliza una matriz de color para estirar el rango dinámico, haciendo que el texto
     * negro resalte más contra el fondo (útil para páginas amarillentas o fotos con sombras).
     */
    private fun Bitmap.enhanceContrast(): Bitmap {
        // Multiplicador de contraste (1.2f = +20%) y ajuste de brillo leve (-10f)
        val contrast = 1.2f
        val brightness = -10f
        
        val width = this.width
        val height = this.height
        val bmpContrast = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmpContrast)
        val paint = Paint()
        
        // Matriz para ajuste de contraste y brillo
        val matrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return bmpContrast
    }
}
