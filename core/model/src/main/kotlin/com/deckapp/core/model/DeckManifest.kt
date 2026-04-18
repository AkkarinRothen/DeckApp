package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeckManifest(
    val name: String,
    val description: String = "",
    val drawMode: String = "TOP",
    val aspectRatio: String = "STANDARD",
    val backImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val cards: List<CardManifest> = emptyList(),
    val exportVersion: Int = 1
)

@Serializable
data class CardManifest(
    val title: String,
    val suit: String? = null,
    val value: String? = null,
    val dmNotes: String? = null,
    val faces: List<FaceManifest> = emptyList()
)

@Serializable
data class FaceManifest(
    val name: String,
    val fileName: String?,
    val contentMode: String = "CARDS_TTRPG"
)
