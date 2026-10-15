package com.deckapp.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiceRollResult(
    val expression: String,
    val total: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val timestampLabel: String get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}
