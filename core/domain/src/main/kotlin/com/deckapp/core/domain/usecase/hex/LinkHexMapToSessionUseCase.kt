package com.deckapp.core.domain.usecase.hex

import com.deckapp.core.domain.repository.HexRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LinkHexMapToSessionUseCase @Inject constructor(
    private val hexRepository: HexRepository
) {
    suspend operator fun invoke(mapId: Long, sessionId: Long?) {
        val map = hexRepository.getHexMap(mapId).first() ?: return
        hexRepository.upsertHexMap(map.copy(sessionId = sessionId))
    }
}
