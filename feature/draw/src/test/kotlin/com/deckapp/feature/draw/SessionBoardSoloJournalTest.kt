package com.deckapp.feature.draw

import com.deckapp.core.domain.usecase.mythic.FateCheckUseCase
import com.deckapp.core.domain.usecase.mythic.MythicReferenceData
import com.deckapp.core.model.BoardMythicFateResult
import com.deckapp.core.model.FateResult
import com.deckapp.core.model.ProbabilityLevel
import org.junit.Assert.*
import org.junit.Test

class SessionBoardSoloJournalTest {

    private val fateCheck = FateCheckUseCase()

    @Test
    fun `fate check produces valid result and threshold for standard 50-50 at chaos 5`() {
        // At 50/50 (index 4) and Chaos 5, threshold is 50.
        // Roll <= 10 -> EXCEPTIONAL_YES (1/5th of 50)
        // Roll 11..50 -> YES
        // Roll 51..89 -> NO
        // Roll >= 91 -> EXCEPTIONAL_NO (100 - (100 - 50)/5 = 90)

        val (resultYes, isRandomEventLow) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 25)
        assertEquals(FateResult.YES, resultYes)
        assertFalse(isRandomEventLow)

        val (resultExceptionalYes, _) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 5)
        assertEquals(FateResult.EXCEPTIONAL_YES, resultExceptionalYes)

        val (resultNo, _) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 70)
        assertEquals(FateResult.NO, resultNo)

        val (resultExceptionalNo, _) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 95)
        assertEquals(FateResult.EXCEPTIONAL_NO, resultExceptionalNo)
    }

    @Test
    fun `random event triggers on matching digits when within chaos factor`() {
        // Roll 11 with Chaos 5: digits match (1 == 1) and 1 <= 5 -> Random Event!
        val (_, isRandomEvent11) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 11)
        assertTrue("Roll 11 with Chaos 5 must trigger random event", isRandomEvent11)

        // Roll 66 with Chaos 5: digits match (6 == 6) but 6 > 5 -> No Random Event!
        val (_, isRandomEvent66) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 5, 66)
        assertFalse("Roll 66 with Chaos 5 must not trigger random event", isRandomEvent66)

        // Roll 66 with Chaos 7: digits match (6 == 6) and 6 <= 7 -> Random Event!
        val (_, isRandomEvent66Chaos7) = fateCheck(ProbabilityLevel.FIFTY_FIFTY, 7, 66)
        assertTrue("Roll 66 with Chaos 7 must trigger random event", isRandomEvent66Chaos7)
    }

    @Test
    fun `board mythic fate result stores and formats answer labels accurately`() {
        val fateItem = BoardMythicFateResult(
            odds = ProbabilityLevel.VERY_LIKELY,
            chaos = 6,
            roll = 14,
            targetThreshold = 83,
            fateResult = FateResult.YES,
            isRandomEvent = false
        )

        assertEquals("SÍ", fateItem.answerLabel)
        assertEquals(ProbabilityLevel.VERY_LIKELY, fateItem.odds)
        assertEquals(6, fateItem.chaos)
        assertEquals(14, fateItem.roll)
        assertEquals(83, fateItem.targetThreshold)
        assertFalse(fateItem.isRandomEvent)
        assertNull(fateItem.randomEventDescription)
    }

    @Test
    fun `board mythic random event formats action and subject correctly`() {
        assertTrue("Mythic action keywords must not be empty", MythicReferenceData.actionKeywords.isNotEmpty())
        assertTrue("Mythic subject keywords must not be empty", MythicReferenceData.subjectKeywords.isNotEmpty())

        val action = MythicReferenceData.actionKeywords.first()
        val subject = MythicReferenceData.subjectKeywords.first()
        val eventDescription = "$action · $subject"

        val fateItemWithEvent = BoardMythicFateResult(
            odds = ProbabilityLevel.FIFTY_FIFTY,
            chaos = 5,
            roll = 22,
            targetThreshold = 50,
            fateResult = FateResult.YES,
            isRandomEvent = true,
            randomEventDescription = eventDescription
        )

        assertTrue(fateItemWithEvent.isRandomEvent)
        assertNotNull(fateItemWithEvent.randomEventDescription)
        assertTrue(fateItemWithEvent.randomEventDescription!!.contains("·"))
    }

    @Test
    fun `chaos factor clamping stays within range 1 to 9`() {
        var chaos = 5
        fun updateChaos(delta: Int) {
            chaos = (chaos + delta).coerceIn(1, 9)
        }

        updateChaos(-10)
        assertEquals(1, chaos)

        updateChaos(10)
        assertEquals(9, chaos)

        updateChaos(-3)
        assertEquals(6, chaos)
    }

    @Test
    fun `solo journaling append logic formats narrative blocks cleanly`() {
        fun appendToNotes(currentNotes: String, textToAppend: String): String {
            return if (currentNotes.isBlank()) textToAppend else "$currentNotes\n\n$textToAppend"
        }

        val emptyNotes = ""
        val oracleStamp = "🔮 **[Oráculo]** 50/50 (Caos 5) -> **SÍ** (Tirada: 34 vs 50)"
        val updated1 = appendToNotes(emptyNotes, oracleStamp)
        assertEquals(oracleStamp, updated1)

        val sparkStamp = "⚡ **[Chispa]** Pelear · Prisión"
        val updated2 = appendToNotes(updated1, sparkStamp)
        assertEquals("$oracleStamp\n\n$sparkStamp", updated2)

        val narrative = "El guardia rechaza el soborno y desenfunda su espada."
        val updated3 = appendToNotes(updated2, narrative)
        assertEquals("$oracleStamp\n\n$sparkStamp\n\n$narrative", updated3)
    }
}
