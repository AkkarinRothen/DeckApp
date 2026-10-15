package com.deckapp.core.domain.usecase

import com.deckapp.core.model.DiagonalRule
import com.deckapp.core.model.GridType
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateTacticalDistanceUseCaseTest {

    private val useCase = CalculateTacticalDistanceUseCase()

    @Test
    fun `straight movement in square grid costs 5ft per cell`() {
        val result = useCase(
            fromX = 0f,
            fromY = 0f,
            toX = 4f,
            toY = 0f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.EQUIDISTANT,
            scaleFtPerCell = 5
        )

        assertEquals(4f, result.cells, 0.001f)
        assertEquals(20, result.distanceFt)
        assertEquals(4, result.straightCells)
        assertEquals(0, result.diagonalCells)
    }

    @Test
    fun `alternating 5-10-5 rule prices first diagonal at 5ft and second at 10ft`() {
        // 1 diagonal (1,1) -> straight 0, diagonal 1 -> 1 cell = 5ft
        val diag1 = useCase(
            fromX = 0f,
            fromY = 0f,
            toX = 1f,
            toY = 1f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.ALTERNATING_5_10_5,
            scaleFtPerCell = 5
        )
        assertEquals(1f, diag1.cells, 0.001f)
        assertEquals(5, diag1.distanceFt)

        // 2 diagonals (2,2) -> straight 0, diagonal 2 -> 2 + floor(2/2) = 3 cells = 15ft
        val diag2 = useCase(
            fromX = 0f,
            fromY = 0f,
            toX = 2f,
            toY = 2f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.ALTERNATING_5_10_5,
            scaleFtPerCell = 5
        )
        assertEquals(3f, diag2.cells, 0.001f)
        assertEquals(15, diag2.distanceFt)

        // 3 diagonals (3,3) -> straight 0, diagonal 3 -> 3 + floor(3/2) = 4 cells = 20ft
        val diag3 = useCase(
            fromX = 0f,
            fromY = 0f,
            toX = 3f,
            toY = 3f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.ALTERNATING_5_10_5,
            scaleFtPerCell = 5
        )
        assertEquals(4f, diag3.cells, 0.001f)
        assertEquals(20, diag3.distanceFt)
    }

    @Test
    fun `equidistant rule treats all diagonals as 5ft`() {
        val result = useCase(
            fromX = 0f,
            fromY = 0f,
            toX = 3f,
            toY = 3f,
            gridType = GridType.SQUARE,
            diagonalRule = DiagonalRule.EQUIDISTANT,
            scaleFtPerCell = 5
        )
        assertEquals(3f, result.cells, 0.001f)
        assertEquals(15, result.distanceFt)
    }

    @Test
    fun `hex grid calculates axial steps correctly`() {
        val result = useCase(
            fromX = 2f,
            fromY = 1f,
            toX = 5f,
            toY = 3f,
            gridType = GridType.HEX_FLAT,
            scaleFtPerCell = 5
        )
        // dq = 3, dr = 2 -> max(3, 2, 5) = 5 hexes -> 25ft
        assertEquals(5f, result.cells, 0.001f)
        assertEquals(25, result.distanceFt)
    }
}
