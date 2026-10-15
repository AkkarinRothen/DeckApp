package com.deckapp.core.domain.usecase

import com.deckapp.core.model.DiagonalRule
import com.deckapp.core.model.GridType
import com.deckapp.core.model.TacticalTerrainType
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class TacticalDistanceResult(
    val cells: Float,
    val distanceFt: Int,
    val straightCells: Int = 0,
    val diagonalCells: Int = 0,
    val difficultCellsCount: Int = 0,
    val isBlockedByWall: Boolean = false,
    val hazardCellsCount: Int = 0
)

/**
 * Calcula la distancia táctica entre celdas según el tipo de grilla, la regla de diagonales
 * y los obstáculos / terreno difícil presentes en la trayectoria.
 * Inspirado en la implementación de gridDistance de Atlas VTT.
 */
class CalculateTacticalDistanceUseCase @Inject constructor() {

    operator fun invoke(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        gridType: GridType = GridType.SQUARE,
        diagonalRule: DiagonalRule = DiagonalRule.ALTERNATING_5_10_5,
        scaleFtPerCell: Int = 5,
        terrainMap: Map<String, TacticalTerrainType> = emptyMap()
    ): TacticalDistanceResult {
        val baseResult = when (gridType) {
            GridType.HEX_FLAT, GridType.HEX_POINTY -> {
                val dq = (toX - fromX).roundToInt()
                val dr = (toY - fromY).roundToInt()
                val steps = max(abs(dq), max(abs(dr), abs(dq + dr)))
                TacticalDistanceResult(
                    cells = steps.toFloat(),
                    distanceFt = steps * scaleFtPerCell,
                    straightCells = steps,
                    diagonalCells = 0
                )
            }
            GridType.NONE -> {
                val dx = toX - fromX
                val dy = toY - fromY
                val euclid = hypot(dx, dy)
                TacticalDistanceResult(
                    cells = euclid,
                    distanceFt = (euclid * scaleFtPerCell).roundToInt()
                )
            }
            GridType.SQUARE -> {
                val dx = abs(toX - fromX).roundToInt()
                val dy = abs(toY - fromY).roundToInt()
                val straight = abs(dx - dy)
                val diagonal = min(dx, dy)

                val cells: Float = when (diagonalRule) {
                    DiagonalRule.EUCLIDEAN -> hypot(dx.toFloat(), dy.toFloat())
                    DiagonalRule.ALTERNATING_5_10_5 -> (straight + diagonal + floor(diagonal / 2f))
                    DiagonalRule.EQUIDISTANT -> (straight + diagonal).toFloat()
                }

                val distanceFt = when (diagonalRule) {
                    DiagonalRule.EUCLIDEAN -> (hypot(dx.toFloat(), dy.toFloat()) * scaleFtPerCell).roundToInt()
                    DiagonalRule.ALTERNATING_5_10_5 -> {
                        ((straight + diagonal + floor(diagonal / 2f)) * scaleFtPerCell).roundToInt()
                    }
                    DiagonalRule.EQUIDISTANT -> (straight + diagonal) * scaleFtPerCell
                }

                TacticalDistanceResult(
                    cells = cells,
                    distanceFt = distanceFt,
                    straightCells = straight,
                    diagonalCells = diagonal
                )
            }
        }

        if (terrainMap.isEmpty()) {
            return baseResult
        }

        val pathCells = getPathCells(fromX, fromY, toX, toY)
        var difficultCount = 0
        var isBlocked = false
        var hazardCount = 0

        for ((col, row) in pathCells) {
            val terrain = terrainMap["$col,$row"] ?: continue
            when (terrain) {
                TacticalTerrainType.WALL -> isBlocked = true
                TacticalTerrainType.DIFFICULT -> difficultCount++
                TacticalTerrainType.HAZARD -> hazardCount++
                TacticalTerrainType.NORMAL -> Unit
            }
        }

        val extraCells = difficultCount.toFloat()
        val extraFt = difficultCount * scaleFtPerCell

        return baseResult.copy(
            cells = baseResult.cells + extraCells,
            distanceFt = baseResult.distanceFt + extraFt,
            difficultCellsCount = difficultCount,
            isBlockedByWall = isBlocked,
            hazardCellsCount = hazardCount
        )
    }

    private fun getPathCells(fromX: Float, fromY: Float, toX: Float, toY: Float): List<Pair<Int, Int>> {
        val result = linkedSetOf<Pair<Int, Int>>()
        val dist = hypot(toX - fromX, toY - fromY)
        if (dist < 0.05f) return emptyList()
        val steps = max(1, (dist * 4).roundToInt())
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val cx = floor(fromX + (toX - fromX) * t).toInt()
            val cy = floor(fromY + (toY - fromY) * t).toInt()
            result.add(cx to cy)
        }
        return result.toList()
    }
}
