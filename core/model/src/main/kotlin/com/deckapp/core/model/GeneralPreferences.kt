package com.deckapp.core.model

enum class StartPage { LIBRARY, SESSIONS, REFERENCES }

data class GeneralPreferences(
    val startPage: StartPage = StartPage.LIBRARY,
    val keepScreenOn: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val textScale: Float = 1f
)
