# Mejoras de Reconocimiento y Procesamiento de Tablas

Plan técnico detallado con diagnóstico del código actual, problemas identificados y mejoras propuestas.

> Complementa `ADVANCED_TABLES.md` (visión general) y `TABLE_IMPORT.md` (flujo de usuario).
> Este documento es el referente para **implementación sprint a sprint**.

---

## Estado Actual del Pipeline

```
Imagen/PDF
    │
    ▼
(sin pre-procesamiento)
    │
    ▼
[ML Kit OCR] → List<OcrBlock>  ← solo nivel bloque, confidence=1.0f hardcoded
    │
    ▼
[AnalyzeTableImageUseCase]
    ├── splitMultiLineBlocks()    ← divide bloques con \n
    ├── clusterBlocks()           ← agrupa por proximidad (BFS)
    ├── analyzeCluster()
    │     ├── groupIntoLines()    ← agrupa por Y
    │     ├── splitLinesIntoTables() ← detecta reinicio de tabla
    │     └── processTableGroup()
    │           ├── detectTitle()
    │           ├── detectColumns()  ← calcula gutters X
    │           └── buildEntries()   ← convierte a TableEntry
    │
    ▼
List<AnalysisResult> → TableEntry[]
```

---

## Área 1: `AnalyzeTableImageUseCase`

### Problema 1.1 — `splitMultiLineBlocks` asume altura uniforme

**Código actual** (`AnalyzeTableImageUseCase.kt:53-68`):  
Divide la altura del bounding box igualmente entre líneas. Si el OCR capturó un bloque con
líneas de tamaño tipográfico diferente (título + cuerpo), los Y calculados son incorrectos.

**Mejora propuesta:**
- Intentar usar los saltos de línea como señal de separación relativa (usar índice, no proporción igual).
- Mantener el bounding box original como límites y ajustar solo centerY interpolado.

---

### Problema 1.2 — `clusterBlocks` usa thresholds relativos al bloque actual

**Código actual** (`AnalyzeTableImageUseCase.kt:99-109`):
```kotlin
val thresholdX = block.boundingBox.width * 2.5f
val thresholdY = block.boundingBox.height * 3.0f
```
Si el bloque de referencia es muy pequeño (ej. "1"), el umbral es mínimo y bloques cercanos
no se conectan. Si es muy grande (ej. título), agrupa demasiado.

**Mejora propuesta:**
- Calcular `avgBlockWidth` y `avgBlockHeight` del conjunto completo **antes** del BFS.
- Usar esos promedios como base del threshold, no el bloque individual.
- Parámetro `clusterThresholdMultiplierX = 3.0f` y `clusterThresholdMultiplierY = 2.5f` en el constructor para facilitar ajuste.

---

### Problema 1.3 — `groupIntoLines` compara con el bloque **anterior**, no con el centro de línea

**Código actual** (`AnalyzeTableImageUseCase.kt:230-232`):
```kotlin
val tolerance = prevBlock.boundingBox.height * 0.7f
if (Math.abs(block.boundingBox.centerY - prevBlock.boundingBox.centerY) < tolerance) {
```
Si el bloque previo tiene una altura atípica, la tolerancia cambia en cada comparación.

**Mejora propuesta:**
- Calcular la altura promedio de la línea actual al agregar bloques.
- Comparar `centerY` del bloque entrante con el `centerY promedio de la línea`, no con el último bloque.

---

### Problema 1.4 — `detectColumns` rechaza gutters con menos del 15% de "votos"

**Código actual** (`AnalyzeTableImageUseCase.kt:271`):
```kotlin
if (currentGroup.size >= (lines.size * 0.15).toInt().coerceAtLeast(2)) {
```
Tablas con 3+ columnas donde una columna tiene datos opcionales (algunas filas la dejan vacía)
no alcanzan el threshold y la columna desaparece.

**Mejora propuesta:**
- Reducir a `coerceAtLeast(1)` (al menos 1 voto) para tablas pequeñas (<10 líneas).
- Añadir un segundo pase de "relleno": si se detectan 2 gutters con >30%, buscar si hay un tercer
  gutter implícito entre ellos que tenga aunque sea 1 bloque.

---

### Problema 1.5 — `splitLinesIntoTables` falla con tablas de dado %

**Código actual** (`AnalyzeTableImageUseCase.kt:157-158`):
```kotlin
val resetThreshold = if (expectedCount > 1) 5 else 2
if (currentMin < lastMinRoll && currentMin <= resetThreshold) {
```
Las tablas de percentil (01-05, 06-10, ..., 96-100) reinician en "1" pero no en "96-100→1".
El threshold `<= 2` es demasiado estricto y pierde el reinicio.

**Mejora propuesta:**
- Añadir detección de modo percentil: si cualquier rango tiene `max >= 90`, activar modo
  `isPercentileTable = true` y usar `resetThreshold = 10`.
- Documentar esta lógica con un comentario explícito.

---

### Problema 1.6 — Sin manejo de tablas con bordes dibujados (líneas horizontales OCR-ruido)

**Descripción:** ML Kit a veces convierte líneas horizontales de separación en texto como `"——————"`, `"________"`, o `"| | |"`. Estos tokens rompen el parsing de líneas.

**Mejora propuesta:**
- Añadir un filtro en `splitMultiLineBlocks` o antes del análisis:
  ```kotlin
  fun isSeparatorNoise(text: String): Boolean {
      return text.matches(Regex("""^[-–—_|=\s]{3,}$"""))
  }
  ```
- Descartar bloques que pasen este filtro antes de enviarlos al pipeline.

---

## Área 2: `RangeParser`

### Problema 2.1 — No maneja rangos percentiles con cero inicial implícito

Formatos comunes en libros: `"01-05"`, `"96-00"` (donde 00 = 100).

**Mejora propuesta en `RangeParser.kt`:**
- Normalizar `"00"` → `100` al parsear.
- Añadir caso de prueba: `parse("96-00")` → `ParsedRange(96, 100)`.

---

### Problema 2.2 — `inferRollFormula` no detecta 2d6, 3d6, etc.

**Código actual** (`RangeParser.kt:95-99`): Solo busca dados estándar simples.

**Mejora propuesta:**
```kotlin
fun inferRollFormula(minValue: Int, maxValue: Int): String {
    // Si rango empieza en 2, podría ser 2d6 (2-12)
    if (minValue == 2 && maxValue == 12) return "2d6"
    if (minValue == 3 && maxValue == 18) return "3d6"
    // ... lógica existente para 1dX
}
```
Cambiar firma de `inferRollFormula(maxValue)` a `inferRollFormula(minValue, maxValue)`.

---

### Problema 2.3 — No detecta rangos con paréntesis

Formato frecuente en manuales impresos: `"(1-5)"`, `"(01–10)"`.

**Mejora propuesta:** Añadir un pre-procesado que elimine paréntesis externos antes del matching:
```kotlin
val cleaned = raw.trimStart().removePrefix("(").removeSuffix(")")
```

---

## Área 3: `PlainTextTableParser`

### Problema 3.1 — `ENUM_PATTERN` no cubre todos los separadores habituales

**Código actual** (`PlainTextTableParser.kt:80-82`):
```kotlin
private val ENUM_PATTERN = Regex("""^(\d{1,3}(?:\s*[-–—]\s*\d{1,3})?)\s*[.:)]\s*(.+)$""")
```
No cubre:
- `"1-5 Goblin"` (espacio como único separador, sin `.` ni `:`)
- `"1—5\tGoblin"` (tab como separador)
- `"[1-5] Goblin"` (corchetes, común en markdown)

**Mejora propuesta:**
```kotlin
private val ENUM_PATTERN = Regex(
    """^\[?(\d{1,3}(?:\s*[-–—]\s*\d{1,3})?)\]?\s*[.:)\t]?\s{1,4}(.+)$"""
)
```

---

### Problema 3.2 — `parseSimpleList` no infiere rangos desde contexto

Cuando la lista no tiene rangos explícitos, se asigna `min=index+1, max=index+1`.
Pero si la lista tiene 6 ítems, el usuario seguramente usa 1d6. La formula queda sin inferir.

**Mejora propuesta:** Llamar a `RangeParser.inferRollFormula(1, entries.size)` y exponer el
resultado junto con las entradas para que `ImportViewModel` lo use como `suggestedFormula`.

---

## Área 4: `CsvTableParser`

### Problema 4.1 — No maneja BOM UTF-8 de Excel

Archivos exportados desde Excel llevan `\uFEFF` al inicio. Esto corrompe la primera celda.

**Mejora propuesta:**
```kotlin
val lines = rawText.trimStart('\uFEFF').lines().filter { it.isNotBlank() }
```

---

### Problema 4.2 — `looksLikeHeader` es demasiado naive

**Código actual** (`CsvTableParser.kt:115-117`):
```kotlin
return firstRow.none { cell -> cell.trim().matches(Regex("""\d+.*""")) }
```
Una tabla cuyo header es `"1d6"` (empieza por dígito) se clasifica erróneamente como datos.

**Mejora propuesta:**
- Verificar si la primera fila es un rango válido (`RangeParser.parse()` devuelve non-null para
  todas las celdas numéricas) → si sí, no es header.
- Si la primera fila contiene palabras como `"roll"`, `"result"`, `"encounter"`, `"name"`,
  `"range"` → definitivamente es header.

---

### Problema 4.3 — Solo usa la primera columna de texto cuando hay 3+

Si un CSV tiene `Rango | Nombre | Descripción`, el parser usa `textColumnIndex = 1` y descarta
la descripción. Para tablas TTRPG ricas, ambas columnas son relevantes.

**Mejora propuesta:**
- Añadir campo `additionalColumns: List<Int>` a `ParseConfig`.
- En `parse()`, concatenar columnas adicionales con ` — ` (formato usado en `JsonTableParser`).
- En la UI de preview, mostrar todas las columnas y dejar que el usuario marque cuáles incluir.

---

## Área 5: `JsonTableParser`

### Problema 5.1 — Sin soporte para formato Roll20

Roll20 exporta tablas como:
```json
{
  "type": "rollabletable",
  "name": "...",
  "items": [
    { "weight": 1, "name": "Goblin" }
  ]
}
```

**Mejora propuesta:** Añadir rama en `parseObject()`:
```kotlin
val items = obj["items"]?.jsonArray
if (items != null) return parseRoll20Items(items, name, description)
```

---

### Problema 5.2 — Error opaco ante JSON inválido

**Código actual** lanza `IllegalArgumentException` con el mensaje de la excepción interna,
que puede ser confuso para el usuario.

**Mejora propuesta:** Crear `TableParseException` sellada con casos específicos:
```kotlin
sealed class TableParseException(msg: String) : Exception(msg) {
    class InvalidJson(cause: String) : TableParseException("JSON malformado: $cause")
    class UnknownFormat(hint: String) : TableParseException("Formato no reconocido. $hint")
    class EmptyResult : TableParseException("No se encontraron entradas en el archivo.")
}
```

---

---

## Área 6: Pre-procesamiento de Imagen ⭐ Mayor impacto

> Esta área no existe aún. Es la de mayor retorno porque mejora la calidad de entrada al OCR,
> amplificando el efecto de todas las demás mejoras. Un OCR más limpio reduce errores en cascada.

### Problema 6.1 — El bitmap se envía crudo a ML Kit

**Código actual** (`OcrRepositoryImpl.kt:19-20`):
```kotlin
val image = InputImage.fromBitmap(bitmap, 0)
return recognizer.process(image).await()
```
No hay ninguna transformación previa. Una foto de libro con sombra, curvatura de encuadernación
o contraste bajo produce bloques fragmentados y texto erróneo.

**Mejora propuesta — pipeline `ImagePreprocessor`:**

Nuevo objeto en `:core:data` o `:core:domain`:
```kotlin
object ImagePreprocessor {
    fun prepare(bitmap: Bitmap): Bitmap {
        return bitmap
            .toGrayscale()       // 1. Eliminar color (mejora contraste en OCR)
            .enhanceContrast()   // 2. Stretch de histograma adaptativo
            .sharpen()           // 3. Unsharp mask leve (texto borroso en fotos)
    }
}
```

Implementaciones:
- **Grayscale**: `ColorMatrix` con `setSaturation(0f)` — sin dependencias extra.
- **Contraste adaptativo**: operaciones de pixel con `Canvas` + `ColorMatrix` — sin OpenCV en fase 1.
- **Sharpen**: kernel de convolución 3×3 con `RenderScript` (API 21+) o `ColorMatrixColorFilter`.

Nota: OpenCV (`zynkware/Document-Scanning-Android-SDK`, ya en el plan) habilita técnicas más
avanzadas como CLAHE y deskew en fase 2. Esta mejora es fase 1 con Android puro.

---

### Problema 6.2 — Sin corrección de perspectiva/curvatura

Fotos de libros físicos tienen curvatura en el lomo. El texto en los bordes aparece rotado,
lo que desalinea los bounding boxes Y y rompe `groupIntoLines`.

**Mejora propuesta (fase 2, requiere OpenCV):**
- Detectar líneas horizontales de texto con Hough Transform.
- Calcular ángulo de skew promedio.
- Aplicar `warpAffine` para deskew antes de enviar al OCR.
- Exponer como paso opcional en el UI: "Corregir perspectiva" toggle.

---

### Problema 6.3 — Sin escalado inteligente del bitmap

**Código actual** (`TableImportViewModel.kt:112`):
```kotlin
val bitmap = renderPdfPageUseCase.renderPage(uri, pageIndex, 1200)
```
1200px es fijo. Para tablas densas (pequeña tipografía), el OCR necesita más resolución.
Para tablas simples (grandes), 1200px puede ser excesivo y lento.

**Mejora propuesta:**
- Añadir parámetro `targetDpi: Int` a `renderPage()`.
- Estimar tamaño de fuente de la tabla (altura promedio de bloques en preview) y ajustar DPI.
- Rango razonable: 150 DPI para tablas grandes, 300 DPI para tablas densas.

---

## Área 7: Jerarquía OCR (`OcrBlock` / `RectModel`) ⭐ Alto impacto

> ML Kit devuelve 3 niveles: Block → Line → Element. Actualmente solo usamos Block.
> Usar Line como unidad base mejoraría drásticamente la precisión de `groupIntoLines`.

### Problema 7.1 — Aplanamiento prematuro de la jerarquía

**Código actual** (`OcrRepositoryImpl.kt:23-33`): Itera `result.textBlocks` directamente.
Un `TextBlock` de ML Kit puede contener múltiples `TextLine`. Al concatenar su texto con `\n`,
obligamos a `splitMultiLineBlocks` a re-separar lo que ML Kit ya tenía separado.

**Mejora propuesta — usar `TextLine` como unidad base:**
```kotlin
result.textBlocks.flatMap { block ->
    block.lines.map { line ->
        OcrBlock(
            text = line.text,
            boundingBox = line.boundingBox.toRectModel(),
            confidence = line.elements.map { it.confidence }.average().toFloat()
        )
    }
}
```
Esto elimina la necesidad de `splitMultiLineBlocks` y proporciona bounding boxes más precisos.

---

### Problema 7.2 — `RectModel` usa `Int`, `confidence` siempre 1.0f

**Código actual** (`OcrResult.kt:13-23`, `OcrRepositoryImpl.kt:32`):
- Coordenadas `Int`: al escalar bitmaps (zoom, crop), hay pérdida de precisión acumulada.
- `confidence = 1.0f` hardcodeado: imposible filtrar entradas de baja confianza.

**Mejora propuesta:**
```kotlin
data class RectModel(
    val left: Float,   // Float en vez de Int
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class OcrBlock(
    val text: String,
    val boundingBox: RectModel,
    val confidence: Float   // real, de TextLine.elements promediados
)
```
Con `confidence` real, `AnalyzeTableImageUseCase` puede descartar bloques < 0.4f o marcarlos
en la UI de revisión con color de advertencia.

---

### Problema 7.3 — Sin campo de ángulo de rotación por bloque

ML Kit expone `TextBlock.recognizedLanguages` y el `boundingBox` como `Rect`, pero los bloques
detectados en páginas landscape tienen coordenadas incorrectas si la imagen no se rotó antes.

**Mejora propuesta:**
- Añadir `rotationDegrees: Int = 0` a `OcrBlock`.
- En `OcrRepositoryImpl`, pasar la rotación del dispositivo/PDF al `InputImage`:
  ```kotlin
  InputImage.fromBitmap(bitmap, rotationDegrees)
  ```
- Exponer `rotationDegrees` desde `TableImportViewModel` (ya recibe el bitmap del PDF).

---

## Área 8: `OcrRepositoryImpl` — Capa de Integración

### Problema 8.1 — Fallo silencioso sin diagnóstico

**Código actual** (`OcrRepositoryImpl.kt:35-38`):
```kotlin
} catch (e: Exception) {
    emptyList()
}
```
Si ML Kit falla (bitmap null, memoria insuficiente, licencia), el ViewModel recibe una lista
vacía y muestra "No se detectaron tablas" — sin distinción entre "tabla vacía" y "error real".

**Mejora propuesta:**
```kotlin
override suspend fun recognizeText(bitmap: Bitmap): Result<List<OcrBlock>> {
    return try {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        Result.success(/* mapeo */)
    } catch (e: Exception) {
        Result.failure(OcrException("ML Kit falló: ${e.message}", e))
    }
}
```
Cambiar la firma del repositorio a `Result<List<OcrBlock>>`. El ViewModel distingue entre
resultado vacío (tabla no encontrada) y error (mostrar mensaje específico).

---

### Problema 8.2 — Reconocedor Latin-only hardcodeado

**Código actual** (`OcrRepositoryImpl.kt:17`):
```kotlin
TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
```
`DEFAULT_OPTIONS` es Latin script únicamente. Tablas de TTRPG japonés (ej. Ryuutama en su
edición original) o con símbolos especiales pueden fallar.

**Mejora propuesta:**
- Corto plazo: no cambiar nada, documentar la limitación.
- Largo plazo: inyectar `TextRecognizerOptions` desde Hilt para permitir configuración
  del script en `SettingsViewModel`. Añadir opción "Script: Latin / CJK / Devanagari".

---

### Problema 8.3 — Instancia `@Singleton` puede tener problemas bajo presión de memoria

ML Kit recomienda cerrar el reconocedor cuando no se usa. Con `@Singleton`, el reconocedor
vive toda la vida de la app.

**Mejora propuesta:**
- Usar `@ActivityRetainedScoped` en vez de `@Singleton` para el repositorio OCR, de forma
  que se libere cuando el usuario sale del flujo de importación.
- O añadir `fun close()` llamado desde el ViewModel en `onCleared()`.

---

## Área 9: `TableImportViewModel` — Orquestación del Pipeline

### Problema 9.1 — `suggestTablePoints` engloba toda la página

**Código actual** (`TableImportViewModel.kt:125-138`): Calcula el bounding box de
**todos** los bloques OCR de la página. Si hay encabezado de capítulo, número de página y
notas al pie, la sugerencia cubre casi toda la imagen — no es útil.

**Mejora propuesta:**
- Filtrar bloques que probablemente son ruido antes de calcular el bounding box sugerido:
  - Descartar bloques con `top < pageHeight * 0.05` (encabezado de página).
  - Descartar bloques con `bottom > pageHeight * 0.95` (pie de página).
  - Descartar bloques cuyo texto sea un solo número corto (número de página).
- El resultado: la sugerencia rodea solo el cuerpo de contenido.

---

### Problema 9.2 — Stitching ajusta rolls con offset aritmético

**Código actual** (`TableImportViewModel.kt:248-253`):
```kotlin
val lastMax = state.editableEntries.maxOfOrNull { it.maxRoll } ?: 0
state.editableEntries + firstResult.entries.map {
    it.copy(minRoll = it.minRoll + lastMax, maxRoll = it.maxRoll + lastMax, ...)
}
```
Si la página 2 detectó rangos que ya empiezan desde 1 (tabla diferente con reinicio),
el offset los desplaza incorrectamente. El usuario esperaba que el stitching continuara
los rangos de la página anterior de forma inteligente.

**Mejora propuesta — dos modos de stitching:**
```
[Modo A] Continuar rangos: ajustar offset (comportamiento actual, renombrar claramente)
[Modo B] Anexar sin ajustar: solo concatenar entradas con sortOrder continuado
```
Añadir `StitchingMode` enum y un toggle en la UI de revisión.

---

### Problema 9.3 — Detección de formato por extensión de archivo únicamente

**Código actual** (`TableImportViewModel.kt:84-90`):
```kotlin
val mode = when {
    path.endsWith(".json", true) -> ImportMode.JSON
    path.endsWith(".csv", true) || path.endsWith(".tsv", true) -> ImportMode.CSV
    else -> ImportMode.PLAIN_TEXT
}
```
Un archivo `.txt` con contenido JSON, o un `.csv` con pipes, se parsea con el parser incorrecto.

**Mejora propuesta — detección por contenido como fallback:**
```kotlin
fun detectImportMode(path: String, content: String): ImportMode {
    // 1. Por extensión (rápido)
    val byExtension = /* lógica actual */
    if (byExtension != ImportMode.PLAIN_TEXT) return byExtension
    // 2. Por contenido si la extensión no es específica
    val trimmed = content.trimStart()
    return when {
        trimmed.startsWith("{") || trimmed.startsWith("[") -> ImportMode.JSON
        content.lines().take(3).any { it.contains(',') || it.contains(';') } -> ImportMode.CSV
        else -> ImportMode.PLAIN_TEXT
    }
}
```
Llamar *después* de `loadFileText`, no antes.

---

### Problema 9.4 — Sin retroalimentación de confianza al usuario

El estado `TableImportUiState` no tiene ningún campo relacionado con la calidad del OCR.
El usuario no sabe si una entrada fue detectada con confianza del 99% o del 40%.

**Mejora propuesta:**
- Añadir `lowConfidenceIndices: Set<Int>` a `TableImportUiState`.
- En la pantalla de revisión, resaltar las filas con baja confianza (borde ámbar + ícono).
- Requiere que `OcrBlock.confidence` sea real (ver Problema 7.2).

---

## Priorización Consolidada (todas las áreas)

> Ordenado de mayor a menor impacto esperado sobre la calidad del resultado final.

| # | Área | Mejora | Impacto | Esfuerzo | Sprint |
|---|------|--------|---------|----------|--------|
| 1 | 6 | Pre-procesamiento: grayscale + contraste | **Crítico** | Medio | Próximo |
| 2 | 7 | Usar `TextLine` como unidad base (jerarquía OCR) | **Crítico** | Medio | Próximo |
| 3 | 7 | `confidence` real + `RectModel` Float | Alto | Bajo | Próximo |
| 4 | 1 | Filtro ruido de bordes (separadores) | Alto | Bajo | Próximo |
| 5 | 4 | BOM UTF-8 en CSV | Alto | Mínimo | Próximo |
| 6 | 2 | Rango percentil `00=100` | Alto | Bajo | Próximo |
| 7 | 8 | `Result<>` en OcrRepository (errores reales) | Alto | Bajo | Próximo |
| 8 | 1 | Threshold clustering con promedios globales | Alto | Medio | Siguiente |
| 9 | 9 | `suggestTablePoints` filtrar ruido de página | Medio | Bajo | Siguiente |
| 10 | 9 | Detección de formato por contenido | Medio | Bajo | Siguiente |
| 11 | 9 | Retroalimentación de confianza en revisión | Medio | Medio | Siguiente |
| 12 | 3 | Ampliar ENUM_PATTERN en PlainText | Medio | Bajo | Siguiente |
| 13 | 4 | Header detection mejorada en CSV | Medio | Bajo | Siguiente |
| 14 | 1 | `groupIntoLines` por centro de línea | Medio | Medio | Futuro |
| 15 | 1 | Modo percentil en `splitLinesIntoTables` | Medio | Medio | Futuro |
| 16 | 9 | Stitching con `StitchingMode` enum | Medio | Medio | Futuro |
| 17 | 4 | Multi-columna configurable en CSV | Medio | Alto | Futuro |
| 18 | 6 | Deskew con OpenCV (fase 2) | Alto | Alto | Fase 2 |
| 19 | 6 | DPI adaptativo por densidad de tabla | Medio | Medio | Futuro |
| 20 | 8 | `@ActivityRetainedScoped` para OcrRepository | Bajo | Bajo | Futuro |
| 21 | 2 | `inferRollFormula` con 2d6/3d6 | Bajo | Bajo | Futuro |
| 22 | 5 | Soporte Roll20 JSON | Bajo | Bajo | Futuro |
| 23 | 5 | `TableParseException` sellada | Bajo | Medio | Futuro |
| 24 | 1 | Gutters con votos mínimos | Bajo | Medio | Futuro |
| 25 | 3 | `inferRollFormula` desde lista simple | Bajo | Bajo | Futuro |
| 26 | 8 | Script OCR configurable (CJK, etc.) | Bajo | Alto | Fase 2 |

---

## Notas de Testing

Para validar estas mejoras, usar como fixtures las tablas de la biblioteca del usuario:
- `Biblioteca/Tablas y Referencias/` — tablas escaneadas de manuales reales.
- El test ideal: foto de página de *The Book of Random Tables* con 2 columnas.
- Test de estrés: tablas percentiles (01-100) de manuales OSR.
- Test de regresión: confirmar que tablas de 1 sola columna no se rompen con la nueva lógica de gutters.
- Test de confianza: foto con sombra o curvatura → comprobar que las entradas de baja confianza se resaltan.
- Test landscape: PDF en orientación apaisada → comprobar rotación correcta.
