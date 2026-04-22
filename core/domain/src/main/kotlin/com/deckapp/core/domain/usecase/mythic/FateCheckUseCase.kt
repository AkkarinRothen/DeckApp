package com.deckapp.core.domain.usecase.mythic

import com.deckapp.core.model.FateResult
import com.deckapp.core.model.ProbabilityLevel
import javax.inject.Inject

/**
 * Núcleo del Oráculo Mythic 2e.
 * Determina el resultado de una pregunta al destino basándose en probabilidad y caos.
 */
class FateCheckUseCase @Inject constructor() {

    // Matriz fate chart [probabilidad.ordinal][chaosFactor-1] = umbral Yes
    private val fateChart = arrayOf(
        intArrayOf(95, 97, 99, 99, 99, 99, 99, 99, 99),  // CERTAIN
        intArrayOf(75, 83, 90, 95, 97, 99, 99, 99, 99),  // NEARLY_CERTAIN
        intArrayOf(55, 65, 75, 83, 90, 95, 97, 99, 99),  // VERY_LIKELY
        intArrayOf(35, 45, 55, 65, 75, 83, 90, 95, 97),  // LIKELY
        intArrayOf(25, 35, 45, 55, 65, 75, 83, 90, 95),  // FIFTY_FIFTY
        intArrayOf(15, 25, 35, 45, 55, 65, 75, 83, 90),  // UNLIKELY
        intArrayOf(10, 15, 25, 35, 45, 55, 65, 75, 83),  // VERY_UNLIKELY
        intArrayOf(5, 10, 15, 25, 35, 45, 55, 65, 75),   // NEARLY_IMPOSSIBLE
        intArrayOf(3, 5, 10, 15, 25, 35, 45, 55, 65)     // IMPOSSIBLE
    )

    /**
     * @param probability Nivel de probabilidad elegido
     * @param chaosFactor Factor de Caos actual (1-9)
     * @param roll Tirada 1-100
     * @return Par con el resultado del oráculo y si se disparó un evento aleatorio
     */
    operator fun invoke(probability: ProbabilityLevel, chaosFactor: Int, roll: Int): Pair<FateResult, Boolean> {
        val safeCf = chaosFactor.coerceIn(1, 9)
        val threshold = fateChart[probability.ordinal][safeCf - 1]

        // Reglas Mythic 2e para Excepcionales:
        // Except Yes: si roll <= 1/5 del threshold
        // Except No: si roll > threshold + 4/5 de lo que queda hasta 100
        val exceptYesThreshold = maxOf(1, threshold / 5)
        val exceptNoThreshold = threshold + ((100 - threshold) * 4 / 5)

        val result = when {
            roll <= exceptYesThreshold -> FateResult.EXCEPTIONAL_YES
            roll <= threshold -> FateResult.YES
            roll > exceptNoThreshold -> FateResult.EXCEPTIONAL_NO
            else -> FateResult.NO
        }

        // Eventos aleatorios: dobles (11, 22... 99) y roll <= Factor de Caos
        // Nota: en Mythic 2e el disparador es dobles si roll <= Chaos Factor
        val isDoubles = roll % 11 == 0 && roll != 100
        val isRandomEvent = isDoubles && (roll / 11) <= safeCf

        return result to isRandomEvent
    }
}
