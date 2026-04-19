package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexDay
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHexDaysUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    operator fun invoke(mapId: Long): Flow<List<HexDay>> =
        hexRepository.getHexDays(mapId)
}
