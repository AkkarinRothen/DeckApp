package com.deckapp.core.model

data class SoloSceneContext(
    val location: String = "",
    val objective: String = "",
    val situation: String = "",
    val pendingQuestion: String = ""
)

data class SoloSceneClosure(
    val summary: String,
    val consequences: String = "",
    val pending: String = "",
    val nextTitle: String = "",
    val chaosDelta: Int = 0
)
