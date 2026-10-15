package com.deckapp.feature.draw.util

import com.deckapp.core.model.SessionBoardItem

fun SessionBoardItem.getMode(): String? = displayMode

fun SessionBoardItem.getAccentColorHex(): String? {
    return if (displayMode?.startsWith("#") == true) displayMode else null
}
