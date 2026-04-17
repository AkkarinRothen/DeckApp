package com.deckapp.core.domain.usecase

import com.deckapp.core.model.OcrBlock
import com.deckapp.core.model.TableEntry
import javax.inject.Inject
import kotlin.math.abs

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
        val layout = analyzeLayout(blocks, expectedTableCount)
        return layout.mapNotNull { (cluster, anchors) -> 
            processWithAnchors(cluster, anchors)
        }
    }

    /**
     * Paso 1: Fragmentar los bloques en tablas candidatas y detectar sus columnas (anchors).
     */
    fun analyzeLayout(blocks: List<OcrBlock>, expectedTableCount: Int = 0): List<Pair<List<OcrBlock>, List<Float>>> {
        if (blocks.isEmpty()) return emptyList()

        val cleanBlocks = blocks.filter { !isSeparatorNoise(it.text) }
        val segmentedBlocks = splitMultiLineBlocks(cleanBlocks)

        val clusters = if (expectedTableCount == 1) {
            listOf(segmentedBlocks)
        } else {
            clusterBlocks(segmentedBlocks)
        }

        return clusters.map { cluster ->
            val sortedBlocks = cluster.sortedBy { it.boundingBox.centerY }
            val lines = groupIntoLines(sortedBlocks)
            // Aquí simplificamos: el pre-mapeo visual se suele hacer sobre la tabla principal
            // Si hay múltiples clusters, tomamos sus gutters.
            cluster to detectColumns(lines)
        }
    }

    /**
     * Paso 2: Procesar un cluster usando columnas (anchors) específicas (manuales o detectadas).
     */
    fun processWithAnchors(cluster: List<OcrBlock>, anchors: List<Float>): AnalysisResult? {
        val sortedBlocks = cluster.sortedBy { it.boundingBox.centerY }
        val lines = groupIntoLines(sortedBlocks)
        return processTableGroup(lines, anchors)
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
        val thresholdX = avgWidth * 3.5f
        val thresholdY = avgHeight * 3.5f

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
        return tableGroups.mapNotNull { linesInGroup ->
            val gutters = detectColumns(linesInGroup)
            processTableGroup(linesInGroup, gutters)
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

    private fun processTableGroup(lines: List<List<OcrBlock>>, columnGutters: List<Float>): AnalysisResult? {
        if (lines.isEmpty()) return null
        
        // 1. Intentar encontrar un posible título
        val titleCandidate = detectTitle(lines)

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

        // Usar una estrategia de cubos verticales (bins).
        // Dos bloques pertenecen a la misma línea si su solapamiento vertical es > 30%
        // de la altura del más pequeño.
        val processed = mutableSetOf<Int>()
        val blocks = sortedBlocks.sortedBy { it.boundingBox.top }

        for (i in blocks.indices) {
            if (i in processed) continue
            
            val currentLine = mutableListOf(blocks[i])
            processed.add(i)
            
            var lineTop = blocks[i].boundingBox.top
            var lineBottom = blocks[i].boundingBox.bottom

            // Buscar otros bloques que traslapen significativamente con este "carril" vertical
            for (j in i + 1 until blocks.size) {
                if (j in processed) continue
                val target = blocks[j]
                
                val overlap = calculateOverlap(lineTop, lineBottom, target.boundingBox.top, target.boundingBox.bottom)
                val targetHeight = target.boundingBox.height
                val lineHeight = lineBottom - lineTop
                
                // Si el solapamiento es mayor al 40% de cualquiera de las dos alturas,
                // consideramos que están en la misma línea visual.
                if (overlap > minOf(targetHeight, lineHeight) * 0.4f) {
                    currentLine.add(target)
                    processed.add(j)
                    // Expandir ligeramente el carril de la línea si el bloque es más alto
                    lineTop = minOf(lineTop, target.boundingBox.top)
                    lineBottom = maxOf(lineBottom, target.boundingBox.bottom)
                }
            }
            lines.add(currentLine)
        }
        
        // Re-ordenar las líneas por su posición Y media (por si el sorting inicial falló)
        // y ordenar bloques dentro de cada línea por X.
        return lines.sortedBy { it.map { b -> b.boundingBox.centerY }.average() }
            .map { it.sortedBy { b -> b.boundingBox.left }.toMutableList() }
    }

    private fun calculateOverlap(s1: Float, e1: Float, s2: Float, e2: Float): Float {
        val start = maxOf(s1, s2)
        val end = minOf(e1, e2)
        return if (end > start) end - start else 0f
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
            // Reducimos el umbral de "votos" para detectar columnas
            // En tablas con muchas filas vacías, 10% puede ser mucho si la columna es opcional.
            // Bajamos a un mínimo de 1 voto si la tabla es pequeña, o 8% para tablas largas.
            val minVotes = (lines.size * 0.08).toInt().coerceAtLeast(1)

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
                var textAfter = firstBlock.text.substring(consumed).trimStart()
                
                // Limpiar separadores comunes que quedan tras el rango (ej: "1. Texto" -> "Texto")
                textAfter = textAfter.trimStart('.', ':', ')', '-', ' ', '–', '—').trimStart()

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
            // Usamos un offset de seguridad para que bloques ligeramente a la izquierda
            // de un gutter caigan en la columna correcta.
            val colIndex = gutters.indexOfLast { block.boundingBox.left >= it - 8f } + 1
            columnContents.getOrPut(colIndex) { mutableListOf() }.add(block.text)
        }

        val structuredText = columnContents.keys.sorted().joinToString(" | ") { idx ->
            columnContents[idx]?.joinToString(" ") ?: ""
        }

        // Confianza mínima de la fila: el bloque menos seguro marca la calidad de la entrada
        val minConfidence = line.minOf { it.confidence }

        return RawRow(structuredText, range, columnContents.mapValues { it.value.joinToString(" ") }, minConfidence)
    }

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

    data class EntriesWithConfidence(
        val entries: List<TableEntry>,
        val lowConfidenceIndices: Set<Int>
    )

    data class RawRow(
        val text: String,
        val range: RangeParser.ParsedRange?,
        val columns: Map<Int, String> = emptyMap(),
        val minConfidence: Float = 1.0f
    )
}
