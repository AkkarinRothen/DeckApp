package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexPoi
import javax.inject.Inject

class AddHexPoiUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(poi: HexPoi): Long = hexRepository.upsertHexPoi(poi)
}
