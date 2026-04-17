package com.deckapp.core.data.repository

import android.graphics.Bitmap
import com.deckapp.core.domain.repository.AiApiException
import com.deckapp.core.domain.repository.AiStreamEvent
import com.deckapp.core.domain.repository.AiTableRepository
import com.deckapp.core.domain.repository.AiTableSuggestions
import com.deckapp.core.model.TableEntry
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GeminiAiRepository @Inject constructor() : AiTableRepository {

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

        return executeGeminiRequest("procesar la tabla") {
            model.generateContent(content { text(prompt) }).text
        }
    }

    override suspend fun recognizeTableFromImage(bitmap: Bitmap, apiKey: String): AiTableSuggestions {
        requireApiKey(apiKey)
        val model = getVisionModel(apiKey)
        val scaledBitmap = scaleBitmapForVision(bitmap)

        val prompt = """
            Eres un experto en TTRPG. Analiza esta imagen de una tabla aleatoria de un manual de rol.

            REGLAS:
            1. Extrae todas las filas con su rango numérico y texto descriptivo.
            2. Ignora encabezados, títulos, pies de página y líneas decorativas.
            3. Si hay varias columnas, mézclalas de forma legible en el campo 'text'.
            4. Sugiere un nombre descriptivo para la tabla, la fórmula de dado (ej: "1d20") y la categoría (ej: "Encuentros").
            5. Responde ÚNICAMENTE con JSON: {"name":"...","formula":"1d6","category":"...","entries":[{"min":1,"max":1,"text":"..."}]}.
            6. Sin texto adicional fuera del JSON.
        """.trimIndent()

        return executeGeminiRequest("analizar la imagen con Vision") {
            model.generateContent(content {
                image(scaledBitmap)
                text(prompt)
            }).text
        }
    }

    override fun streamTableFromImage(bitmap: Bitmap, apiKey: String): Flow<AiStreamEvent> = flow {
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

        var buffer = ""
        var sortOrder = 0

        try {
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

            // Línea restante al terminar el stream
            val remaining = buffer.trim()
            parseStreamLine(remaining, sortOrder)?.let { emit(it) }
        } catch (e: AiApiException) {
            throw e
        } catch (e: Exception) {
            throw AiApiException(friendlyGeminiError(e), e)
        }
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

    private suspend fun executeGeminiRequest(
        operationLabel: String,
        block: suspend () -> String?
    ): AiTableSuggestions = try {
        parseResponse(block())
    } catch (e: AiApiException) {
        throw e
    } catch (e: Exception) {
        throw AiApiException(friendlyGeminiError(e), e)
    }

    private fun friendlyGeminiError(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            "quota" in msg || "resource_exhausted" in msg || "429" in msg -> {
                val retryMatch = Regex("""retry in ([\d.]+)s""").find(e.message ?: "")
                val retryInfo = retryMatch?.let { " Vuelve a intentarlo en ${it.groupValues[1].toFloatOrNull()?.let { s -> "%.0f".format(s) } ?: it.groupValues[1]} segundos." } ?: ""
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
        const val TEXT_MODEL_NAME = "gemini-2.0-flash-lite"  // texto OCR: más rápido, menor cuota
        const val VISION_MODEL_NAME = "gemini-2.0-flash"     // visión multimodal: mayor calidad
        const val API_VERSION = "v1beta"
        const val VISION_MAX_WIDTH = 800
    }
}
