package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import com.deckapp.core.model.HexMap
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHexMapsUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    operator fun invoke(): Flow<List<HexMap>> = hexRepository.getHexMaps()
}
