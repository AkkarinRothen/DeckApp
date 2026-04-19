package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexActivityEntry
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LogHexActivityUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(dayId: Long, entry: HexActivityEntry) {
        val days = hexRepository.getHexDays(dayId).first()
        val day = days.find { it.id == dayId } ?: return
        hexRepository.upsertHexDay(day.copy(activitiesLog = day.activitiesLog + entry))
    }
}
