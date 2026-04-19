package com.deckapp.core.data.repository

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.AiApiException
import com.deckapp.core.domain.repository.AiReferenceRepository
import com.deckapp.core.domain.repository.AiStreamEvent
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.AiTableSuggestions
import com.deckapp.core.domain.repository.ReferenceTableAiResult
import com.deckapp.core.model.TableEntry
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GeminiAiRepository @Inject constructor() : AiTableRepository, AiReferenceRepository {

    // ... (rest of the class)

    override suspend fun recognizeReferenceTableFromImage(bitmap: Bitmap, apiKey: String): ReferenceTableAiResult {
        requireApiKey(apiKey)
        val model = getVisionModel(apiKey)
        val scaledBitmap = scaleBitmapForVision(bitmap)

        val prompt = """
            Eres un experto en TTRPG. Analiza esta imagen de una tabla de referencia (reglas, equipos, condiciones, etc).
            Extrae los encabezados de las columnas y todas las filas de datos.

            REGLAS:
            1. Identifica claramente los encabezados de las columnas.
            2. Extrae cada fila manteniendo el orden de las columnas.
            3. Limpia el texto de errores de OCR comunes.
            4. Responde ÚNICAMENTE con JSON: {"headers":["col1", "col2"], "rows":[["val1", "val2"], ["val3", "val4"]]}.
            5. Sin explicaciones ni Markdown adicional fuera del JSON.
        """.trimIndent()

        var lastEx: Exception? = null
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val responseText = model.generateContent(content {
                    image(scaledBitmap)
                    text(prompt)
                }).text
                
                val text = responseText?.trim() ?: throw AiApiException("Gemini devolvió una respuesta vacía.")
                val jsonString = text.removeSurrounding("```json", "```").trim()
                val apiResponse = json.decodeFromString<AiReferenceResponse>(jsonString)
                
                return ReferenceTableAiResult(
                    headers = apiResponse.headers,
                    rows = apiResponse.rows
                )
            } catch (e: Exception) {
                if (!isRateLimitError(e) || attempt == MAX_RETRY_ATTEMPTS - 1) {
                    throw AiApiException(friendlyGeminiError(e), e)
                }
                lastEx = e
                delay(parseRetryDelayMs(e) ?: (RETRY_BASE_DELAY_MS shl attempt))
            }
        }
        throw AiApiException(friendlyGeminiError(lastEx!!), lastEx)
    }

    @Serializable
    private data class AiReferenceResponse(
        val headers: List<String>,
        val rows: List<List<String>>
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Two cached models: lightweight for text, full for vision.
    // Both invalidated together when the API key changes.
    private var cachedApiKey: String = ""
    private var cachedTextModel: GenerativeModel? = null
    private var cachedVisionModel: GenerativeModel? = null

    private fun getTextModel(apiKey: String): GenerativeModel {
        if (cachedApiKey != apiKey) invalidateCache(apiKey)
        return cachedTextModel ?: GenerativeModel(
            modelName = TEXT_MODEL_NAME,
            apiKey = apiKey,
            requestOptions = RequestOptions(apiVersion = API_VERSION)
        ).also { cachedTextModel = it }
    }

    private fun getVisionModel(apiKey: String): GenerativeModel {
        if (cachedApiKey != apiKey) invalidateCache(apiKey)
        return cachedVisionModel ?: GenerativeModel(
            modelName = VISION_MODEL_NAME,
            apiKey = apiKey,
            requestOptions = RequestOptions(apiVersion = API_VERSION)
        ).also { cachedVisionModel = it }
    }

    private fun invalidateCache(newApiKey: String) {
        cachedApiKey = newApiKey
        cachedTextModel = null
        cachedVisionModel = null
    }

    private fun requireApiKey(apiKey: String) {
        if (apiKey.isBlank()) throw AiApiException("La API Key de Gemini no está configurada en Ajustes.")
    }

    override suspend fun reconstructTable(rawText: String, apiKey: String): AiTableSuggestions {
        requireApiKey(apiKey)
        val model = getTextModel(apiKey)

        val prompt = """
            Eres un experto en herramientas para juegos de rol (TTRPG).
            Analiza el siguiente texto de una tabla TTRPG y conviértelo en entradas estructuradas.

            REGLAS:
            1. Cada entrada debe tener un rango numérico (min y max) y un texto descriptivo.
            2. Corrige errores de OCR (ej: 'l' por '1', 'S' por '5', 'B' por '8').
            3. Infiere rangos faltantes basándote en la secuencia de la tabla.
            4. Si hay varias columnas, mézclalas de forma legible en el campo 'text'.
            5. Sugiere un nombre descriptivo para la tabla, la fórmula de dado (ej: "1d20", "2d6") y la categoría (ej: "Encuentros", "Tesoros", "Clima").
            6. Responde ÚNICAMENTE con JSON: {"name":"...","formula":"1d6","category":"...","entries":[{"min":1,"max":1,"text":"..."}]}.
            7. Sin explicaciones ni Markdown adicional fuera del JSON.

            TEXTO A PROCESAR:
            $rawText
        """.trimIndent()

        return executeGeminiRequest { model.generateContent(content { text(prompt) }).text }
    }

    override fun streamTableFromImage(
        bitmap: Bitmap,
        apiKey: String,
        onRetryWait: (suspend (Long) -> Unit)?
    ): Flow<AiStreamEvent> = flow {
        requireApiKey(apiKey)
        val model = getVisionModel(apiKey)
        val scaledBitmap = scaleBitmapForVision(bitmap)

        val prompt = """
            Eres un experto en TTRPG. Analiza esta imagen de una tabla aleatoria.

            Responde en formato NDJSON (una línea JSON por línea, sin arrays externos):
            - PRIMERA línea: metadatos: {"meta":true,"name":"nombre de la tabla","formula":"1d20","category":"tipo"}
            - RESTO: una entrada por línea: {"min":1,"max":1,"text":"descripción"}

            Reglas:
            - Ignora encabezados, títulos y líneas decorativas.
            - Si hay varias columnas, mézclalas de forma legible en 'text'.
            - Sin explicaciones ni texto fuera de las líneas JSON.
        """.trimIndent()

        var lastEx: Exception? = null
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                var buffer = ""
                var sortOrder = 0

                model.generateContentStream(content {
                    image(scaledBitmap)
                    text(prompt)
                }).collect { response ->
                    buffer += response.text ?: ""
                    while (buffer.contains('\n')) {
                        val idx = buffer.indexOf('\n')
                        val line = buffer.substring(0, idx).trim()
                        buffer = buffer.substring(idx + 1)
                        parseStreamLine(line, sortOrder)?.let { event ->
                            emit(event)
                            if (event is AiStreamEvent.Entry) sortOrder++
                        }
                    }
                }

                val remaining = buffer.trim()
                parseStreamLine(remaining, sortOrder)?.let { emit(it) }
                return@flow
            } catch (e: AiApiException) {
                throw e
            } catch (e: Exception) {
                if (!isRateLimitError(e) || attempt == MAX_RETRY_ATTEMPTS - 1) {
                    throw AiApiException(friendlyGeminiError(e), e)
                }
                lastEx = e
                val delayMs = parseRetryDelayMs(e) ?: (RETRY_BASE_DELAY_MS shl attempt)
                if (onRetryWait != null) onRetryWait.invoke(delayMs) else delay(delayMs)
            }
        }
        throw AiApiException(friendlyGeminiError(lastEx!!), lastEx)
    }

    private fun parseStreamLine(line: String, sortOrder: Int): AiStreamEvent? {
        if (!line.startsWith('{') || !line.endsWith('}')) return null
        return try {
            if (line.contains("\"meta\"")) {
                val meta = json.decodeFromString<AiMetaLine>(line)
                AiStreamEvent.Metadata(meta.name, meta.formula, meta.category)
            } else {
                val entry = json.decodeFromString<AiEntry>(line)
                AiStreamEvent.Entry(TableEntry(minRoll = entry.min, maxRoll = entry.max, text = entry.text, sortOrder = sortOrder))
            }
        } catch (_: Exception) { null }
    }

    private suspend fun executeGeminiRequest(block: suspend () -> String?): AiTableSuggestions {
        var lastEx: Exception? = null
        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                return parseResponse(block())
            } catch (e: AiApiException) {
                throw e
            } catch (e: Exception) {
                if (!isRateLimitError(e) || attempt == MAX_RETRY_ATTEMPTS - 1) {
                    throw AiApiException(friendlyGeminiError(e), e)
                }
                lastEx = e
                delay(parseRetryDelayMs(e) ?: (RETRY_BASE_DELAY_MS shl attempt))
            }
        }
        throw AiApiException(friendlyGeminiError(lastEx!!), lastEx)
    }

    private fun isRateLimitError(e: Exception): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return "quota" in msg || "resource_exhausted" in msg || "429" in msg
    }

    private fun parseRetryDelayMs(e: Exception): Long? {
        val seconds = Regex("""retry in ([\d.]+)s""", RegexOption.IGNORE_CASE)
            .find(e.message ?: "")?.groupValues[1]?.toFloatOrNull() ?: return null
        return (seconds * 1000).toLong() + 500L
    }

    private fun scaleBitmapForVision(bitmap: Bitmap, maxWidth: Int = VISION_MAX_WIDTH): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val scale = maxWidth.toFloat() / bitmap.width
        val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, scaledHeight, true)
    }

    private fun parseResponse(responseText: String?): AiTableSuggestions {
        val text = responseText?.trim() ?: throw AiApiException("Gemini devolvió una respuesta vacía.")
        val jsonString = text.removeSurrounding("```json", "```").trim()
        val apiResponse = json.decodeFromString<AiTableResponse>(jsonString)
        val entries = apiResponse.entries.mapIndexed { index, entry ->
            TableEntry(minRoll = entry.min, maxRoll = entry.max, text = entry.text, sortOrder = index)
        }
        return AiTableSuggestions(
            entries = entries,
            suggestedName = apiResponse.name,
            suggestedFormula = apiResponse.formula.ifBlank { "1d6" },
            suggestedCategory = apiResponse.category
        )
    }

    private fun friendlyGeminiError(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            "quota" in msg || "resource_exhausted" in msg || "429" in msg -> {
                val retryMatch = Regex("""retry in ([\d.]+)s""").find(e.message ?: "")
                val retryInfo = retryMatch?.let {
                    " Vuelve a intentarlo en ${"%.0f".format(it.groupValues[1].toFloatOrNull() ?: 0f)} segundos."
                } ?: ""
                "Cuota gratuita de Gemini agotada.$retryInfo Revisa tu plan en ai.google.dev."
            }
            "api_key_invalid" in msg || "api key not valid" in msg || "401" in msg ->
                "API Key de Gemini inválida. Verifica la clave en Ajustes."
            "not_found" in msg || "404" in msg ->
                "Modelo de Gemini no disponible. Comprueba tu versión de la API."
            "network" in msg || "connect" in msg || "timeout" in msg || "unreachable" in msg ->
                "Sin conexión con Gemini. Verifica tu red e inténtalo de nuevo."
            "candidate" in msg && "safety" in msg ->
                "Gemini bloqueó la respuesta por filtros de seguridad. Intenta con otra imagen."
            else ->
                "Error de Gemini: ${e.localizedMessage?.take(120) ?: "desconocido"}"
        }
    }

    @Serializable
    private data class AiTableResponse(
        val entries: List<AiEntry>,
        val name: String = "",
        val formula: String = "1d6",
        val category: String = ""
    )

    @Serializable
    private data class AiEntry(val min: Int, val max: Int, val text: String)

    @Serializable
    private data class AiMetaLine(
        val meta: Boolean = false,
        val name: String = "",
        val formula: String = "1d6",
        val category: String = ""
    )

    private companion object {
        const val TEXT_MODEL_NAME = "gemini-2.0-flash-lite"   // texto OCR: rápido, menor cuota
        const val VISION_MODEL_NAME = "gemini-2.0-flash-lite" // visión: 30 RPM free vs 15 RPM de flash
        const val API_VERSION = "v1beta"
        const val VISION_MAX_WIDTH = 800
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_BASE_DELAY_MS = 2_000L
    }
}
