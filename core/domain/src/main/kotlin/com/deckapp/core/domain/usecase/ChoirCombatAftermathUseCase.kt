package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ChoirCharacter
import kotlin.random.Random

data class ChoirAftermathRequest(
    val character: ChoirCharacter,
    val spendMedicalSupply: Boolean,
    val intRollResult: Int? = null,
    val conRollResult: Int? = null,
    val targetNumber: Int = 10,
    val xpGained: Int = 0,
    val lootNotes: String = ""
)

data class ChoirAftermathResult(
    val updatedCharacter: ChoirCharacter,
    val medicalSupplySpent: Boolean,
    val treatmentSucceeded: Boolean,
    val minorFesteredToMajor: Int,
    val traumaGained: Int,
    val previousMajor: Int,
    val previousMinor: Int,
    val summaryReport: String
)

/**
 * Procedimiento oficial de Cierre de Combate (Aftermath pp. 60-61).
 *
 * 1. Si no está herido: no hay supuración de heridas, se añade XP y botín.
 * 2. Si está herido: se puede gastar 1 suministro médico para chequeo de Inteligencia (NO 10 Promedio).
 * 3. Si falla o no hay suministros: chequeo de Constitución (NO 10 Promedio).
 * 4. Fallo: las heridas menores supuran y se convierten en heridas mayores.
 * 5. Por cada herida mayor: se gana +1 Trauma.
 * 6. Se limpian las heridas de combate (Clear Injuries).
 */
object ChoirCombatAftermathUseCase {
    fun resolve(
        request: ChoirAftermathRequest,
        d20: () -> Int = { Random.nextInt(1, 21) }
    ): ChoirAftermathResult {
        val original = request.character
        val prevMajor = original.major
        val prevMinor = original.minor

        if (prevMajor == 0 && prevMinor == 0) {
            val updated = original.copy(
                xp = original.xp + request.xpGained.coerceAtLeast(0),
                personalNotes = if (request.lootNotes.isNotBlank()) {
                    (original.personalNotes.trimEnd() + "\n- Botín de combate: ${request.lootNotes.trim()}").trim()
                } else original.personalNotes,
                undoState = ChoirCharacterCodec.snapshot(original)
            )
            return ChoirAftermathResult(
                updatedCharacter = updated,
                medicalSupplySpent = false,
                treatmentSucceeded = true,
                minorFesteredToMajor = 0,
                traumaGained = 0,
                previousMajor = 0,
                previousMinor = 0,
                summaryReport = "Sin heridas que tratar. +${request.xpGained} XP ganados."
            )
        }

        var supplySpent = false
        var currentSupplies = original.medicalSupplies
        var treatmentSuccess = false

        if (request.spendMedicalSupply && currentSupplies > 0) {
            supplySpent = true
            currentSupplies -= 1
            val intMod = ChoirCharacterRules.modifier(original.attributes.intelligence)
            val roll = request.intRollResult ?: (d20() + intMod)
            if (roll >= request.targetNumber) {
                treatmentSuccess = true
            }
        }

        if (!treatmentSuccess) {
            val conMod = ChoirCharacterRules.modifier(original.attributes.constitution)
            val roll = request.conRollResult ?: (d20() + conMod)
            if (roll >= request.targetNumber) {
                treatmentSuccess = true
            }
        }

        val festered = if (!treatmentSuccess) prevMinor else 0
        val totalMajorBeforeClear = (prevMajor + festered).coerceAtMost(4)
        val traumaGained = totalMajorBeforeClear

        val updated = original.copy(
            medicalSupplies = currentSupplies,
            trauma = original.trauma + traumaGained,
            major = 0,
            minor = 0,
            xp = original.xp + request.xpGained.coerceAtLeast(0),
            personalNotes = if (request.lootNotes.isNotBlank()) {
                (original.personalNotes.trimEnd() + "\n- Botín de combate: ${request.lootNotes.trim()}").trim()
            } else original.personalNotes,
            undoState = ChoirCharacterCodec.snapshot(original)
        )

        val report = buildString {
            append("### Consecuencias de Combate (Aftermath)\n")
            if (supplySpent) append("- Se gastó 1 Suministro Médico (restantes: $currentSupplies).\n")
            if (treatmentSuccess) {
                append("- Tratamiento exitoso: Las heridas menores fueron estabilizadas sin infectarse.\n")
            } else {
                append("- Tratamiento fallido: $festered heridas menores supuraron a mayores.\n")
            }
            if (traumaGained > 0) {
                append("- Trauma adquirido: +$traumaGained (Total Trauma: ${updated.trauma}).\n")
            }
            append("- Heridas de combate limpiadas.\n")
            if (request.xpGained > 0) append("- XP adquirido: +${request.xpGained} (Total XP: ${updated.xp}).\n")
            if (request.lootNotes.isNotBlank()) append("- Botín registrado: ${request.lootNotes.trim()}.\n")
        }

        return ChoirAftermathResult(
            updatedCharacter = updated,
            medicalSupplySpent = supplySpent,
            treatmentSucceeded = treatmentSuccess,
            minorFesteredToMajor = festered,
            traumaGained = traumaGained,
            previousMajor = prevMajor,
            previousMinor = prevMinor,
            summaryReport = report
        )
    }
}
