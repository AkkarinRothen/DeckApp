package com.deckapp.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RangeParserTest {

    @Test
    fun `parse simple range`() {
        val result = RangeParser.parse("1-5")
        assertNotNull(result)
        assertEquals(1, result!!.range.min)
        assertEquals(5, result.range.max)
    }

    @Test
    fun `parse range with dots`() {
        val result = RangeParser.parse("6..10")
        assertNotNull(result)
        assertEquals(6, result!!.range.min)
        assertEquals(10, result.range.max)
    }

    @Test
    fun `parse range with 'to' or 'a'`() {
        assertEquals(11, RangeParser.parse("11 a 20")?.range?.min)
        assertEquals(20, RangeParser.parse("11 to 20")?.range?.max)
    }

    @Test
    fun `parse percentile range 00 as 100`() {
        val result = RangeParser.parse("96-00")
        assertNotNull(result)
        assertEquals(96, result!!.range.min)
        assertEquals(100, result.range.max)
    }

    @Test
    fun `parse with noise prefix`() {
        val result = RangeParser.parse("  * (01-10)")
        assertNotNull(result)
        assertEquals(1, result!!.range.min)
        assertEquals(10, result.range.max)
    }

    @Test
    fun `validate integrity detects gaps`() {
        val entries = listOf(1 to 5, 7 to 10)
        val validation = RangeParser.validateIntegrity(entries)
        assert(validation.gaps.contains(6))
        assert(!validation.isValid)
    }

    @Test
    fun `validate integrity detects overlaps`() {
        val entries = listOf(1 to 5, 5 to 10)
        val validation = RangeParser.validateIntegrity(entries)
        assert(validation.overlaps.contains(5))
        assert(!validation.isValid)
    }

    @Test
    fun `infer roll formula for standard ranges`() {
        assertEquals("2d6", RangeParser.inferRollFormula(2, 12))
        assertEquals("1d20", RangeParser.inferRollFormula(1, 20))
        assertEquals("1d100", RangeParser.inferRollFormula(1, 100))
    }
}
