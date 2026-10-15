package com.deckapp.feature.draw.components.blocks

import com.deckapp.core.model.TableEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TableEntriesSearchTest {
    private val entries = listOf(
        TableEntry(id = 1, text = "Un dragón rojo protege el tesoro"),
        TableEntry(id = 2, text = "Un dragón azul duerme en la cueva"),
        TableEntry(id = 3, text = "La guardia protege el tesoro: 1d6+2 monedas")
    )

    @Test fun `encuentra texto con tildes usando mayusculas sin tildes`() {
        assertEquals(listOf(entries[0], entries[1]), filterTableEntries(entries, "DRAGON"))
    }

    @Test fun `varias palabras deben coincidir en la misma entrada`() {
        assertEquals(listOf(entries[0]), filterTableEntries(entries, "  tesoro\n dragon  "))
    }

    @Test fun `una busqueda vacia conserva todas las entradas y su orden`() {
        assertEquals(entries, filterTableEntries(entries, " \t "))
    }

    @Test fun `las formulas se buscan como texto literal`() {
        assertEquals(listOf(entries[2]), filterTableEntries(entries, "1d6+2"))
        assertEquals(emptyList<TableEntry>(), filterTableEntries(entries, "[1d6+2]"))
    }
}
