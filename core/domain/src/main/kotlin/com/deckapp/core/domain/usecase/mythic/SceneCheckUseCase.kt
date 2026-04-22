package com.deckapp.core.domain.usecase.mythic

import javax.inject.Inject

enum class SceneCheckResult { NORMAL, ALTERED, INTERRUPTED }

/**
 * Chequeo de escena de Mythic.
 * Tira 1d10 contra el Factor de Caos.
 */
class SceneCheckUseCase @Inject constructor() {

    operator fun invoke(chaosFactor: Int, roll: Int): SceneCheckResult {
        return when {
            roll > chaosFactor -> SceneCheckResult.NORMAL
            roll % 2 == 0 -> SceneCheckResult.ALTERED
            else -> SceneCheckResult.INTERRUPTED
        }
    }
}
