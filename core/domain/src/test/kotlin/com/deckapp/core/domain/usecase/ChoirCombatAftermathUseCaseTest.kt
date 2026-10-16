package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ChoirAttributes
import com.deckapp.core.model.ChoirCharacter
import org.junit.Assert.*
import org.junit.Test

class ChoirCombatAftermathUseCaseTest {

    private val baseChar = ChoirCharacter(
        attributes = ChoirAttributes(intelligence = 14, constitution = 12), // INT mod +1, CON mod 0
        medicalSupplies = 2,
        trauma = 1,
        xp = 5
    )

    @Test
    fun `uninjured character receives xp and loot without suffering trauma or spending supplies`() {
        val request = ChoirAftermathRequest(
            character = baseChar.copy(major = 0, minor = 0),
            spendMedicalSupply = true,
            xpGained = 2,
            lootNotes = "Daga ceremonial"
        )
        val result = ChoirCombatAftermathUseCase.resolve(request)

        assertEquals(2, result.updatedCharacter.medicalSupplies)
        assertEquals(0, result.traumaGained)
        assertEquals(1, result.updatedCharacter.trauma)
        assertEquals(7, result.updatedCharacter.xp)
        assertTrue(result.updatedCharacter.personalNotes.contains("Daga ceremonial"))
        assertFalse(result.medicalSupplySpent)
        assertTrue(result.treatmentSucceeded)
    }

    @Test
    fun `successful treatment with medical supply prevents minor injuries from festering`() {
        val injured = baseChar.copy(major = 1, minor = 3, medicalSupplies = 2)
        val request = ChoirAftermathRequest(
            character = injured,
            spendMedicalSupply = true,
            intRollResult = 12, // >= 10: Success
            xpGained = 3
        )
        val result = ChoirCombatAftermathUseCase.resolve(request)

        assertTrue(result.medicalSupplySpent)
        assertEquals(1, result.updatedCharacter.medicalSupplies)
        assertTrue(result.treatmentSucceeded)
        assertEquals(0, result.minorFesteredToMajor)
        // Trauma gained is only from the 1 major injury
        assertEquals(1, result.traumaGained)
        assertEquals(2, result.updatedCharacter.trauma) // 1 previous + 1 gained
        assertEquals(0, result.updatedCharacter.major)   // Cleared
        assertEquals(0, result.updatedCharacter.minor)   // Cleared
        assertEquals(8, result.updatedCharacter.xp)
    }

    @Test
    fun `failed treatment causes minor injuries to fester to major and increase trauma`() {
        val injured = baseChar.copy(major = 1, minor = 2, medicalSupplies = 0)
        val request = ChoirAftermathRequest(
            character = injured,
            spendMedicalSupply = false,
            conRollResult = 8, // < 10: Failure
            xpGained = 1
        )
        val result = ChoirCombatAftermathUseCase.resolve(request)

        assertFalse(result.medicalSupplySpent)
        assertFalse(result.treatmentSucceeded)
        assertEquals(2, result.minorFesteredToMajor)
        // 1 major + 2 festered minor = 3 major -> 3 trauma gained
        assertEquals(3, result.traumaGained)
        assertEquals(4, result.updatedCharacter.trauma) // 1 previous + 3 gained
        assertEquals(0, result.updatedCharacter.major)   // Cleared
        assertEquals(0, result.updatedCharacter.minor)   // Cleared
    }
}
