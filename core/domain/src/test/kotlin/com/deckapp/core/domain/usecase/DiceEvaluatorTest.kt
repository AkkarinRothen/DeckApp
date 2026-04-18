package com.deckapp.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class DiceEvaluatorTest {

    @Test
    fun `evaluate standard dice formula`() {
        val formula = "1d6"
        val result = DiceEvaluator.evaluate(formula)
        assert(result in 1..6)
    }

    @Test
    fun `evaluate formula with modifier`() {
        val formula = "2d4+2"
        val result = DiceEvaluator.evaluate(formula)
        assert(result in 4..10)
    }

    @Test
    fun `evaluate formula with negative modifier`() {
        val formula = "1d10-5"
        val result = DiceEvaluator.evaluate(formula)
        assert(result in -4..5)
    }

    @Test
    fun `get range of standard formula`() {
        val formula = "2d8+3"
        val (min, max) = DiceEvaluator.getRange(formula)
        assertEquals(5, min)
        assertEquals(19, max)
    }

    @Test
    fun `evaluate fixed number`() {
        assertEquals(10, DiceEvaluator.evaluate("10"))
    }

    @Test
    fun `evaluate empty or invalid formula returns 1`() {
        assertEquals(1, DiceEvaluator.evaluate(""))
        assertEquals(1, DiceEvaluator.evaluate("invalid"))
    }
}
