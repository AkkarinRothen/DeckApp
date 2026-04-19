package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class HexSessionResources(
    val tableIds: List<Long> = emptyList(),
    val deckIds: List<Long> = emptyList(),
    val ruleIds: List<Long> = emptyList()
)
