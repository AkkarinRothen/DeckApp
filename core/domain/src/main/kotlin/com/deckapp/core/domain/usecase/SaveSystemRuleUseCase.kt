package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.SystemRule
import javax.inject.Inject

class SaveSystemRuleUseCase @Inject constructor(
    private val repository: ReferenceRepository
) {
    suspend operator fun invoke(rule: SystemRule): Result<Long> = runCatching {
        require(rule.title.isNotBlank()) { "El título de la regla no puede estar vacío." }
        repository.saveSystemRule(rule.copy(lastUpdated = System.currentTimeMillis()))
    }
}
