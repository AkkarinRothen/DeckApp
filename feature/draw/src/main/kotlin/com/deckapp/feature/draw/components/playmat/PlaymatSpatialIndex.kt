package com.deckapp.feature.draw.components.playmat

import androidx.compose.ui.geometry.Rect
import com.deckapp.core.model.PlaymatSurface
import kotlin.math.floor

/** Lightweight, transient spatial lookup for rendering and selection. */
internal class PlaymatSpatialIndex(surface: PlaymatSurface) {
    internal enum class Kind { CARD, TOKEN, CLOCK, NOTE, CHIP, DECK, DISCARD }

    internal data class Entry(
        val kind: Kind,
        val id: String,
        val bounds: Rect,
        val order: Int
    )

    private val entries: List<Entry> = buildList {
        surface.cards.forEachIndexed { index, card ->
            val radians = Math.toRadians(card.rotationDegrees.toDouble())
            val halfWidth = (120f * kotlin.math.abs(kotlin.math.cos(radians)).toFloat() + 180f * kotlin.math.abs(kotlin.math.sin(radians)).toFloat()) / 2f
            val halfHeight = (120f * kotlin.math.abs(kotlin.math.sin(radians)).toFloat() + 180f * kotlin.math.abs(kotlin.math.cos(radians)).toFloat()) / 2f
            val centerX = card.x + 60f
            val centerY = card.y + 90f
            add(Entry(Kind.CARD, "card:${card.cardId}", Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight), index))
        }
        surface.tokens.forEachIndexed { index, token ->
            add(Entry(Kind.TOKEN, "token:${token.id}", Rect(token.x, token.y, token.x + 64f, token.y + 82f), index))
        }
        surface.clocks.forEachIndexed { index, clock ->
            add(Entry(Kind.CLOCK, "clock:${clock.id}", Rect(clock.x, clock.y, clock.x + 76f, clock.y + 100f), index))
        }
        surface.stickyNotes.forEachIndexed { index, note ->
            add(Entry(Kind.NOTE, "note:${note.id}", Rect(note.x, note.y, note.x + 100f, note.y + 100f), index))
        }
        surface.tableChips.forEachIndexed { index, chip ->
            add(Entry(Kind.CHIP, "chip:${chip.id}", Rect(chip.x, chip.y, chip.x + 180f, chip.y + 48f), index))
        }
        surface.decks.forEachIndexed { index, deck ->
            add(Entry(Kind.DECK, "deck:${deck.stackId}", Rect(deck.x, deck.y, deck.x + 120f, deck.y + 180f), index * 2 + 1))
            val discardX = deck.discardX ?: deck.x + 150f
            val discardY = deck.discardY ?: deck.y
            add(Entry(Kind.DISCARD, "discard:${deck.stackId}", Rect(discardX, discardY, discardX + 120f, discardY + 180f), index * 2))
        }
    }

    private val bucketEntries = mutableMapOf<Long, MutableList<Entry>>()
    private val buckets: Map<Long, List<Entry>> = run {
        entries.forEach { entry ->
            val left = floor(entry.bounds.left / CELL_SIZE).toInt()
            val right = floor(entry.bounds.right / CELL_SIZE).toInt()
            val top = floor(entry.bounds.top / CELL_SIZE).toInt()
            val bottom = floor(entry.bounds.bottom / CELL_SIZE).toInt()
            for (x in left..right) for (y in top..bottom) {
                bucketEntries.getOrPut(key(x, y)) { mutableListOf() }.add(entry)
            }
        }
        bucketEntries
    }

    fun query(bounds: Rect): List<Entry> {
        val left = floor(bounds.left / CELL_SIZE).toInt()
        val right = floor(bounds.right / CELL_SIZE).toInt()
        val top = floor(bounds.top / CELL_SIZE).toInt()
        val bottom = floor(bounds.bottom / CELL_SIZE).toInt()
        val candidates = LinkedHashSet<Entry>()
        for (x in left..right) for (y in top..bottom) {
            buckets[key(x, y)].orEmpty().forEach { entry ->
                if (entry.bounds.overlaps(bounds)) candidates.add(entry)
            }
        }
        return candidates.toList()
    }

    fun hitTest(x: Float, y: Float): Entry? {
        val point = Rect(x, y, x + 0.01f, y + 0.01f)
        val candidates = query(point)
        val primaryHit = Kind.values().filter { it != Kind.DECK && it != Kind.DISCARD }.firstNotNullOfOrNull { kind ->
            candidates.asSequence().filter { it.kind == kind }.maxByOrNull { it.order }
        }
        val deckHit = candidates.asSequence().filter { it.kind == Kind.DECK || it.kind == Kind.DISCARD }.maxByOrNull { it.order }
        return (primaryHit ?: deckHit)?.takeIf { x >= it.bounds.left && x <= it.bounds.right && y >= it.bounds.top && y <= it.bounds.bottom }
    }

    companion object {
        private const val CELL_SIZE = 256f
        private fun key(x: Int, y: Int): Long = (x.toLong() shl 32) xor (y.toLong() and 0xffffffffL)
    }
}
