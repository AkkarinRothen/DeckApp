package com.deckapp.core.domain.usecase.reference

import com.deckapp.core.domain.repository.ManualRepository
import com.deckapp.core.model.Manual
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetManualsUseCase @Inject constructor(
    private val manualRepository: ManualRepository
) {
    operator fun invoke(): Flow<List<Manual>> = manualRepository.getAllManuals()
}

class SaveManualUseCase @Inject constructor(
    private val manualRepository: ManualRepository
) {
    suspend operator fun invoke(manual: Manual): Long = manualRepository.saveManual(manual)
}

class DeleteManualUseCase @Inject constructor(
    private val manualRepository: ManualRepository
) {
    suspend operator fun invoke(manual: Manual) = manualRepository.deleteManual(manual)
}
