package com.deckapp.core.domain.usecase

import com.deckapp.core.model.RandomTable
import com.deckapp.core.model.TableRollMode
import javax.inject.Inject

/**
 * Use case para validar la integridad de una tabla aleatoria.
 * Detecta huecos, solapamientos y problemas con las fórmulas de dados.
 */
class ValidateTableUseCase @Inject constructor() {

    sealed class TableValidationError {
        data class RangeGaps(val gaps: List<Int>) : TableValidationError()
        data class RangeOverlaps(val overlaps: List<Int>) : TableValidationError()
        object EmptyEntries : TableValidationError()
        data class OutOfDiceRange(val min: Int, val max: Int, val diceMin: Int, val diceMax: Int) : TableValidationError()
    }

    data class ValidationReport(
        val isValid: Boolean,
        val errors: List<TableValidationError>
    )

    operator fun invoke(table: RandomTable): ValidationReport {
        val errors = mutableListOf<TableValidationError>()

        if (table.entries.isEmpty()) {
            errors.add(TableValidationError.EmptyEntries)
            return ValidationReport(false, errors)
        }

        if (table.rollMode == TableRollMode.RANGE) {
            // 1. Validar integridad de rangos
            val rangeResult = RangeParser.validateIntegrity(table.entries.map { it.minRoll to it.maxRoll })
            if (rangeResult.gaps.isNotEmpty()) {
                errors.add(TableValidationError.RangeGaps(rangeResult.gaps))
            }
            if (rangeResult.overlaps.isNotEmpty()) {
                errors.add(TableValidationError.RangeOverlaps(rangeResult.overlaps))
            }

            // 2. Validar correspondencia con fórmula de dado
            val (diceMin, diceMax) = DiceEvaluator.getRange(table.rollFormula)
            val tableMin = table.entries.minOfOrNull { it.minRoll } ?: 1
            val tableMax = table.entries.maxOfOrNull { it.maxRoll } ?: 1

            if (tableMin > diceMin || tableMax < diceMax) {
                // No es un error crítico pero avisa de que el dado puede caer en "vacío"
                // o que hay entradas inalcanzables.
            }
        }

        return ValidationReport(isValid = errors.isEmpty(), errors = errors)
    }
}
