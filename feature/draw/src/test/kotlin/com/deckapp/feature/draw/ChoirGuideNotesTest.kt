package com.deckapp.feature.draw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoirGuideNotesTest {
    private val receipt = "<!-- choir-guide:abc-123 -->"

    @Test fun `reading a note hides only guide receipts`() {
        val stored = "# Mi apunte\n<!-- comentario propio -->\n$receipt\nSigue la historia"
        val visible = ChoirGuideNotes.visible(stored)

        assertEquals("# Mi apunte\n<!-- comentario propio -->\nSigue la historia", visible)
        assertFalse(receipt in visible)
    }

    @Test fun `editing visible text retains receipt needed for retry deduplication`() {
        val stored = "Nota original\n$receipt"
        val edited = ChoirGuideNotes.preserveReceipts("Nota corregida", stored)

        assertEquals("Nota corregida\n$receipt", edited)
        assertEquals("Nota corregida\n", ChoirGuideNotes.visible(edited))
    }

    @Test fun `already present receipt is never appended twice`() {
        val stored = "Nota\n$receipt"
        assertEquals(stored, ChoirGuideNotes.preserveReceipts(stored, stored))
        assertEquals(1, Regex("choir-guide:abc-123").findAll(stored).count())
        assertTrue(receipt in ChoirGuideNotes.preserveReceipts("Cambio", stored))
    }
}
