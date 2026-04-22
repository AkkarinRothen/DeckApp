package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexDay
import javax.inject.Inject

class UpdateHexDayNotesUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(day: HexDay, notes: String) {
        hexRepository.upsertHexDay(day.copy(notes = notes))
    }
}
