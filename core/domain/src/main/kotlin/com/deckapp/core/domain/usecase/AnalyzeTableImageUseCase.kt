package com.deckapp.core.domain.usecase

import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import javax.inject.Inject

/**
 * Lógica heurística para convertir bloques de texto detectados por OCR en entradas de tabla.
 * Refactorizado en Sprint 9 para usar [RangeParser] como motor centralizado de rangos.
 *
 * Estrategia:
 * 1. Agrupar bloques en "líneas" visuales por proximidad vertical (Y similar).
 * 2. Ordenar bloques dentro de cada línea por X (izquierda a derecha).
 * 3. En cada línea, buscar el rango usando RangeParser.
 * 4. Combinar el texto restante en la columna descriptiva.
 * 5. Manejar entradas multilínea (texto que continúa en la siguiente línea sin rango).
 */
class AnalyzeTableImageUseCase @Inject constructor() {

    /**
     * Resultado de analizar un cluster de bloques como una tabla.
     * [lowConfidenceIndices] contiene los índices de entries cuyo texto proviene
     * de bloques con confidence < [CONFIDENCE_THRESHOLD] — para resaltarlos en la UI.
     */
    data class AnalysisResult(
        val entries: List<TableEntry>,
        val suggestedName: String = "",
        val lowConfidenceIndices: Set<Int> = emptySet()
    )

    companion object {
        const val CONFIDENCE_THRESHOLD = 0.6f
    }

    operator fun invoke(blocks: List<OcrBlock>, expectedTableCount: Int = 0): List<AnalysisResult> {
        if (blocks.isEmpty()) return emptyList()

        // 0a. Filtrar ruido visual: líneas separadoras que ML Kit convierte en texto
        val cleanBlocks = blocks.filter { !isSeparatorNoise(it.text) }

        // 0b. Pre-segmentación: dividir bloques multilínea residuales (safety net)
        val segmentedBlocks = splitMultiLineBlocks(cleanBlocks)

        // Si el usuario declaró exactamente 1 tabla, saltear el clustering espacial:
        // cualquier separación que haga clusterBlocks llevaría a devolver 2+ resultados
        // ignorando la intención del usuario.
        if (expectedTableCount == 1) {
            return analyzeCluster(segmentedBlocks, expectedTableCount)
        }

        // 1. Agrupar bloques en clusters espaciales (tablas candidatas)
        val clusters = clusterBlocks(segmentedBlocks)

        // 2. Procesar cada cluster de forma independiente
        return clusters.flatMap { cluster ->
            analyzeCluster(cluster, expectedTableCount)
        }
    }

    /**
     * Detecta texto que es en realidad una línea separadora de tabla convertida por OCR.
     * Ejemplos: "——————", "________", "| | |", "=====".
     */
    private fun isSeparatorNoise(text: String): Boolean =
        text.trim().matches(Regex("""^[-–—_|=\s]{3,}$"""))

    /**
     * Divide bloques que el OCR ha agrupado con saltos de línea (\n).
     * Con TextLine como unidad base esto ocurre raramente, pero se mantiene como safety net.
     */
    private fun splitMultiLineBlocks(blocks: List<OcrBlock>): List<OcrBlock> {
        return blocks.flatMap { block ->
            if (!block.text.contains("\n")) {
                listOf(block)
            } else {
                val lines = block.text.split("\n").filter { it.isNotBlank() }
                val lineCount = lines.size
                val singleLineHeight = block.boundingBox.height / lineCount
                
                lines.mapIndexed { index, lineText ->
                    val newTop = block.boundingBox.top + (index * singleLineHeight)
                    val newBottom = newTop + singleLineHeight
                    OcrBlock(
                        text = lineText.trim(),
                        boundingBox = block.boundingBox.copy(
                            top = newTop,
                            bottom = newBottom
                        ),
                        confidence = block.confidence
                    )
                }
            }
        }
    }

    private fun clusterBlocks(blocks: List<OcrBlock>): List<List<OcrBlock>> {
        val clusters = mutableListOf<MutableList<OcrBlock>>()
        if (blocks.isEmpty()) return clusters

        // Calcular promedios globales una sola vez para que el threshold
        // no dependa del tamaño del bloque de referencia en cada comparación.
        // Esto evita que un bloque pequeño (ej. "1") tenga umbral mínimo y
        // que un bloque grande (ej. título) agrupe en exceso.
        val avgWidth = blocks.map { it.boundingBox.width }.average().toFloat().coerceIn(20f, 200f)
        val avgHeight = blocks.map { it.boundingBox.height }.average().toFloat().coerceIn(8f, 80f)
        val thresholdX = avgWidth * 3.0f
        val thresholdY = avgHeight * 2.5f

        // Ordenamos por Y para facilitar el agrupamiento inicial
        val sortedBlocks = blocks.sortedBy { it.boundingBox.centerY }

        // Usamos un enfoque de "componentes conectados" (BFS simple)
        val visited = mutableSetOf<Int>()

        for (i in sortedBlocks.indices) {
            if (i in visited) continue

            val currentCluster = mutableListOf<OcrBlock>()
            val queue = mutableListOf(i)
            visited.add(i)

            while (queue.isNotEmpty()) {
                val idx = queue.removeAt(0)
                val block = sortedBlocks[idx]
                currentCluster.add(block)

                for (j in sortedBlocks.indices) {
                    if (j in visited) continue
                    val target = sortedBlocks[j]
                    val dx = kotlin.math.abs(block.boundingBox.centerX - target.boundingBox.centerX)
                    val dy = kotlin.math.abs(block.boundingBox.centerY - target.boundingBox.centerY)
                    if (dx < thresholdX && dy < thresholdY) {
                        visited.add(j)
                        queue.add(j)
                    }
                }
            }
            if (currentCluster.size >= 3) {
                clusters.add(currentCluster)
            }
        }

        return clusters
    }

    private fun analyzeCluster(cluster: List<OcrBlock>, expectedTableCount: Int): List<AnalysisResult> {
        // 1. Agrupar por líneas (Y-coordinate con tolerancia)
        val sortedBlocks = cluster.sortedBy { it.boundingBox.centerY }
        val lines = groupIntoLines(sortedBlocks)
        if (lines.isEmpty()) return emptyList()

        // 2. Fragmentar las líneas en grupos de tablas individuales
        val tableGroups = splitLinesIntoTables(lines, expectedTableCount)

        // 3. Procesar cada grupo como una tabla independiente
        return tableGroups.mapNotNull { group ->
            processTableGroup(group)
        }
    }

    /**
     * Divide un set de líneas en grupos si detecta un reinicio de tabla o título nuevo.
     *
     * Si [expectedCount] == 1, no se divide en ningún caso — el usuario declaró
     * explícitamente que la imagen contiene una única tabla.
     * Si [expectedCount] == 0 (auto), se aplica la heurística conservadora (threshold 2).
     * Si [expectedCount] > 1, se aplica la heurística agresiva (threshold 5).
     */
    private fun splitLinesIntoTables(lines: List<List<OcrBlock>>, expectedCount: Int): List<List<List<OcrBlock>>> {
        if (lines.isEmpty()) return emptyList()

        // El usuario indicó exactamente 1 tabla: devolver todas las líneas como un único grupo
        if (expectedCount == 1) return listOf(lines)

        val groups = mutableListOf<MutableList<List<OcrBlock>>>()
        var currentGroup = mutableListOf(lines[0])
        groups.add(currentGroup)

        var lastMinRoll = RangeParser.parse(lines[0].firstOrNull()?.text ?: "")?.range?.min ?: 0

        // Detectar si la tabla usa rangos percentiles (01-100).
        // Si algún rango tiene max >= 90 asumimos modo percentil y usamos un
        // threshold de reinicio más alto (10) para no partir en "91-00" → "1".
        val maxRollSeen = lines.mapNotNull { line ->
            RangeParser.parse(line.firstOrNull()?.text ?: "")?.range?.max
        }.maxOrNull() ?: 0
        val isPercentileTable = maxRollSeen >= 90

        val resetThreshold = when {
            expectedCount > 1 -> 5
            isPercentileTable -> 10
            else -> 2
        }

        // Cuenta de rangos vistos en el grupo actual. Solo consideramos un "título
        // intermedio" como separador de tabla si ya hay al menos 3 entradas con rango
        // en el grupo — así evitamos partir una entrada multilínea cuyo texto OCR
        // llegó en un bloque separado del número de rango.
        var rangesInCurrentGroup = if (RangeParser.parse(lines[0].firstOrNull()?.text ?: "") != null) 1 else 0

        for (i in 1 until lines.size) {
            val line = lines[i]
            val firstText = line.firstOrNull()?.text ?: ""
            val rangeResult = RangeParser.parse(firstText)

            var shouldSplit = false

            if (rangeResult != null) {
                val currentMin = rangeResult.range.min
                if (currentMin < lastMinRoll && currentMin <= resetThreshold) {
                    shouldSplit = true
                }
                lastMinRoll = currentMin
                rangesInCurrentGroup++
            } else if (line.size == 1 && line[0].text.length > 3 && rangesInCurrentGroup >= 3) {
                // Título intermedio: solo válido si el grupo ya tiene suficientes entradas,
                // para no confundir una línea de continuación de texto con un nuevo encabezado.
                shouldSplit = true
                lastMinRoll = 0
            }

            if (shouldSplit) {
                currentGroup = mutableListOf(line)
                groups.add(currentGroup)
                rangesInCurrentGroup = 0
            } else {
                currentGroup.add(line)
            }
        }
        return groups
    }

    private fun processTableGroup(lines: List<List<OcrBlock>>): AnalysisResult? {
        if (lines.isEmpty()) return null
        
        // 1. Intentar encontrar un posible título
        val titleCandidate = detectTitle(lines)

        // 2. Detectar estructura de columnas (Gutters) específicas para este grupo
        val columnGutters = detectColumns(lines)

        // 3. Procesar cada línea clasificando bloques por columna
        val rawRows = lines.map { line ->
            // Ordenar por X antes de construir la fila
            val sortedLine = line.sortedBy { it.boundingBox.left }
            buildStructuredRow(sortedLine, columnGutters)
        }

        // 4. Convertir a TableEntry manejando multilínea inteligente
        val result = buildEntries(rawRows)

        if (result.entries.isEmpty()) return null

        return AnalysisResult(
            entries = result.entries,
            suggestedName = titleCandidate ?: "",
            lowConfidenceIndices = result.lowConfidenceIndices
        )
    }

    private fun detectTitle(lines: List<List<OcrBlock>>): String? {
        val firstLine = lines.firstOrNull() ?: return null
        // Si la primera línea es corta y no tiene un rango, podría ser el título
        if (firstLine.size == 1) {
            val text = firstLine[0].text
            if (RangeParser.parse(text) == null && text.length > 3) {
                return text
            }
        }
        return null
    }

    private fun groupIntoLines(sortedBlocks: List<OcrBlock>): List<MutableList<OcrBlock>> {
        val lines = mutableListOf<MutableList<OcrBlock>>()
        if (sortedBlocks.isEmpty()) return lines

        var currentLine = mutableListOf(sortedBlocks[0])
        // Usar el centerY promedio de la línea en construcción como referencia,
        // en lugar del centerY del bloque anterior. Esto evita que un bloque con
        // altura atípica (ej. un número grande) desplace la tolerancia del resto.
        var lineCenterY = sortedBlocks[0].boundingBox.centerY
        var lineAvgHeight = sortedBlocks[0].boundingBox.height
        lines.add(currentLine)

        for (i in 1 until sortedBlocks.size) {
            val block = sortedBlocks[i]
            val tolerance = lineAvgHeight * 0.7f
            if (kotlin.math.abs(block.boundingBox.centerY - lineCenterY) < tolerance) {
                currentLine.add(block)
                // Actualizar el centro y la altura promedio de la línea de forma incremental
                val n = currentLine.size.toFloat()
                lineCenterY = ((lineCenterY * (n - 1)) + block.boundingBox.centerY) / n
                lineAvgHeight = ((lineAvgHeight * (n - 1)) + block.boundingBox.height) / n
            } else {
                currentLine = mutableListOf(block)
                lineCenterY = block.boundingBox.centerY
                lineAvgHeight = block.boundingBox.height
                lines.add(currentLine)
            }
        }
        return lines
    }

    /** Analiza todos los bloques para encontrar los límites X de las columnas. */
    private fun detectColumns(lines: List<List<OcrBlock>>): List<Float> {
        // Obtenemos los 'left' de todos los bloques que NO son rangos iniciales
        val allBlocks = lines.flatten()
        val candidates = lines.mapNotNull { line ->
            if (line.isEmpty()) return@mapNotNull null
            // Si el primero es un rango, las columnas reales están en el resto
            val firstIsRange = RangeParser.parse(line[0].text) != null
            if (firstIsRange) line.drop(1) else line
        }.flatten().map { it.boundingBox.left }  // Float directo, sin conversión

        if (candidates.isEmpty()) return emptyList()

        // Agrupamos por proximidad para encontrar alineaciones consistentes (gutters)
        // Usamos una tolerancia basada en el ancho promedio de los bloques
        val avgWidth = allBlocks.map { it.boundingBox.width }.average().toFloat().coerceIn(20f, 100f)
        val tolerance = avgWidth * 0.4f 

        val anchors = mutableListOf<Float>()
        val sortedCandidates = candidates.sorted()
        
        if (sortedCandidates.isNotEmpty()) {
            // Mínimo siempre 2 votos para evitar que un bloque desalineado cree un gutter falso.
            // El porcentaje se reduce al 10% (era 15%) para capturar mejor columnas en tablas
            // de tamaño medio donde no todas las filas usan la columna adicional.
            val minVotes = (lines.size * 0.10).toInt().coerceAtLeast(2)

            var currentGroup = mutableListOf(sortedCandidates[0])
            for (i in 1 until sortedCandidates.size) {
                val x = sortedCandidates[i]
                if (x - currentGroup.last() < tolerance) {
                    currentGroup.add(x)
                } else {
                    if (currentGroup.size >= minVotes) {
                        anchors.add(currentGroup.average().toFloat())
                    }
                    currentGroup = mutableListOf(x)
                }
            }
            if (currentGroup.size >= minVotes) {
                anchors.add(currentGroup.average().toFloat())
            }
        }
        
        return anchors.sorted()
    }

    /** Construye una fila combinando bloques en columnas separadas por '|'. */
    private fun buildStructuredRow(line: List<OcrBlock>, gutters: List<Float>): RawRow {
        if (line.isEmpty()) return RawRow("", null)

        val firstBlock = line[0]
        val parseResult = RangeParser.parse(firstBlock.text)
        val range = parseResult?.range

        // Si el primer bloque contiene un rango, separamos el texto restante
        val remainingBlocks = mutableListOf<OcrBlock>()
        if (parseResult != null) {
            val consumed = parseResult.consumedLength
            if (consumed < firstBlock.text.length) {
                val textAfter = firstBlock.text.substring(consumed).trim()
                if (textAfter.isNotBlank()) {
                    remainingBlocks.add(firstBlock.copy(text = textAfter))
                }
            }
            remainingBlocks.addAll(line.drop(1))
        } else {
            remainingBlocks.addAll(line)
        }

        // Agrupar el resto por columnas basadas en gutters
        val columnContents = mutableMapOf<Int, MutableList<String>>()
        remainingBlocks.forEach { block ->
            val colIndex = gutters.indexOfLast { block.boundingBox.left >= it - 10f } + 1
            columnContents.getOrPut(colIndex) { mutableListOf() }.add(block.text)
        }

        val structuredText = columnContents.keys.sorted().joinToString(" | ") { idx ->
            columnContents[idx]?.joinToString(" ") ?: ""
        }

        // Confianza mínima de la fila: el bloque menos seguro marca la calidad de la entrada
        val minConfidence = line.minOf { it.confidence }

        return RawRow(structuredText, range, columnContents.mapValues { it.value.joinToString(" ") }, minConfidence)
    }

    private data class EntriesWithConfidence(
        val entries: List<TableEntry>,
        val lowConfidenceIndices: Set<Int>
    )

    private fun buildEntries(rawRows: List<RawRow>): EntriesWithConfidence {
        val entries = mutableListOf<TableEntry>()
        val lowConfidence = mutableSetOf<Int>()
        var sortOrder = 0

        for (row in rawRows) {
            if (row.range != null) {
                val idx = sortOrder
                entries.add(TableEntry(
                    minRoll = row.range.min,
                    maxRoll = row.range.max,
                    text = row.text,
                    sortOrder = sortOrder++
                ))
                if (row.minConfidence < CONFIDENCE_THRESHOLD) lowConfidence.add(idx)
            } else if (entries.isNotEmpty() && row.text.isNotBlank()) {
                val last = entries.last()
                val mergedText = mergeMultiline(last.text, row)
                entries[entries.size - 1] = last.copy(text = mergedText)
                // Si la continuación es de baja confianza, marcar la entrada padre
                if (row.minConfidence < CONFIDENCE_THRESHOLD) lowConfidence.add(last.sortOrder)
            }
        }
        return EntriesWithConfidence(entries, lowConfidence)
    }

    /** Une el texto de una fila de continuación respetando los delimitadores. */
    private fun mergeMultiline(existing: String, next: RawRow): String {
        if (!existing.contains("|") || next.columns.size <= 1) {
            return "$existing ${next.text}".trim()
        }
        
        val existingCols = existing.split("|").map { it.trim() }.toMutableList()
        val nextCols = next.columns
        
        // Intentar unir cada trozo a su columna correspondiente
        // (Esto es simplificado, asume que la alineación se mantiene)
        val resultCols = existingCols.toMutableList()
        nextCols.forEach { (idx, text) ->
            if (idx < resultCols.size) {
                resultCols[idx] = "${resultCols[idx]} $text".trim()
            } else {
                resultCols.add(text)
            }
        }
        return resultCols.joinToString(" | ")
    }

    private data class RawRow(
        val text: String,
        val range: RangeParser.ParsedRange?,
        val columns: Map<Int, String> = emptyMap(),
        val minConfidence: Float = 1.0f
    )
}
