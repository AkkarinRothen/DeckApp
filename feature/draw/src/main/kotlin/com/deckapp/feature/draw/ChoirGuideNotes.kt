package com.deckapp.feature.draw

/** Journal receipts remain in storage so a retried guide action cannot create a duplicate entry. */
internal object ChoirGuideNotes {
    private val receiptLine = Regex("(?m)^<!-- choir-guide:[A-Za-z0-9-]+ -->\\r?\\n?")

    fun visible(notes: String): String = notes.replace(receiptLine, "")

    fun preserveReceipts(edited: String, previous: String): String {
        val missing = receiptLine.findAll(previous)
            .map { it.value.trimEnd('\r', '\n') }
            .filterNot { it in edited }
            .distinct()
            .toList()
        if (missing.isEmpty()) return edited
        return edited.trimEnd() + (if (edited.isBlank()) "" else "\n") + missing.joinToString("\n")
    }
}
