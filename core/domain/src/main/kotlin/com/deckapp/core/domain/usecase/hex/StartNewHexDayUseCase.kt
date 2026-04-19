package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexDay
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class StartNewHexDayUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long): Long {
        val existingDays = hexRepository.getHexDays(mapId).first()
        val nextDay = (existingDays.maxOfOrNull { it.dayNumber } ?: 0) + 1
        return hexRepository.upsertHexDay(HexDay(mapId = mapId, dayNumber = nextDay))
    }
}
