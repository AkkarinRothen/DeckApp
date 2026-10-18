package com.deckapp.core.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** A small semantic icon set for the game's primary destinations and tools. */
object DeckIcons {
    private fun lineIcon(name: String, draw: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = draw
            )
        }.build()

    val Library by lazy { lineIcon("Deck library") {
        moveTo(4f, 7f); lineTo(4f, 19f); lineTo(16f, 19f)
        moveTo(7f, 4f); lineTo(19f, 4f); lineTo(19f, 16f); lineTo(7f, 16f); close()
        moveTo(13f, 7f); lineTo(15f, 9.5f); lineTo(13f, 12f); lineTo(11f, 9.5f); close()
    } }

    val Sessions by lazy { lineIcon("Deck sessions") {
        moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(9f, 8f); lineTo(15.5f, 12f); lineTo(9f, 16f); close()
    } }

    val Board by lazy { lineIcon("Deck board") {
        moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 20f); lineTo(3f, 20f); close()
        moveTo(12f, 4f); lineTo(12f, 20f)
        moveTo(3f, 12f); lineTo(21f, 12f)
        moveTo(5.5f, 7f); lineTo(9.5f, 7f)
        moveTo(14.5f, 16f); lineTo(18.5f, 16f)
    } }

    val Tactical by lazy { lineIcon("Deck tactical grid") {
        moveTo(3f, 3f); lineTo(21f, 3f); lineTo(21f, 21f); lineTo(3f, 21f); close()
        moveTo(9f, 3f); lineTo(9f, 21f)
        moveTo(15f, 3f); lineTo(15f, 21f)
        moveTo(3f, 9f); lineTo(21f, 9f)
        moveTo(3f, 15f); lineTo(21f, 15f)
        moveTo(11f, 12f); lineTo(13f, 12f)
    } }

    val Map by lazy { lineIcon("Deck map") {
        moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
        curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
        curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
        curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        moveTo(15.5f, 8.5f); lineTo(13.5f, 13.5f); lineTo(8.5f, 15.5f)
        lineTo(10.5f, 10.5f); close()
    } }

    val Journal by lazy { lineIcon("Deck journal") {
        moveTo(12f, 6f); curveTo(9f, 4f, 6f, 4f, 3f, 5f)
        lineTo(3f, 18f); curveTo(6f, 17f, 9f, 17f, 12f, 19f)
        curveTo(15f, 17f, 18f, 17f, 21f, 18f)
        lineTo(21f, 5f); curveTo(18f, 4f, 15f, 4f, 12f, 6f); close()
        moveTo(12f, 6f); lineTo(12f, 19f)
        moveTo(5.5f, 9f); lineTo(9f, 9.5f)
        moveTo(15f, 9.5f); lineTo(18.5f, 9f)
    } }

    val Dice by lazy { lineIcon("Deck die") {
        moveTo(12f, 2f); lineTo(20f, 7f); lineTo(18f, 18f)
        lineTo(12f, 22f); lineTo(6f, 18f); lineTo(4f, 7f); close()
        moveTo(4f, 7f); lineTo(12f, 10f); lineTo(20f, 7f)
        moveTo(12f, 2f); lineTo(12f, 10f)
        moveTo(12f, 10f); lineTo(12f, 22f)
        moveTo(6f, 18f); lineTo(12f, 10f); lineTo(18f, 18f)
    } }

    val Table by lazy { lineIcon("Deck table") {
        moveTo(3f, 3f); lineTo(21f, 3f); lineTo(21f, 21f); lineTo(3f, 21f); close()
        moveTo(3f, 8f); lineTo(21f, 8f)
        moveTo(3f, 14f); lineTo(21f, 14f)
        moveTo(9f, 8f); lineTo(9f, 21f)
        moveTo(6f, 5.5f); lineTo(6.1f, 5.5f)
    } }

    val Guide by lazy { lineIcon("Deck guide") {
        moveTo(5f, 3f); lineTo(16f, 3f); lineTo(19f, 6f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(16f, 3f); lineTo(16f, 6f); lineTo(19f, 6f)
        moveTo(8f, 10f); lineTo(9.5f, 11.5f); lineTo(12f, 8.5f)
        moveTo(14f, 10f); lineTo(16f, 10f)
        moveTo(8f, 16f); lineTo(9.5f, 17.5f); lineTo(12f, 14.5f)
        moveTo(14f, 16f); lineTo(16f, 16f)
    } }

    val Moon by lazy { lineIcon("Deck moon") {
        moveTo(18.5f, 16.5f); curveTo(10f, 18f, 6f, 12f, 8f, 4f)
        curveTo(3f, 7f, 2.5f, 14f, 7f, 18f)
        curveTo(11f, 22f, 17f, 21f, 18.5f, 16.5f); close()
    } }

    val Sun by lazy { lineIcon("Deck sun") {
        moveTo(12f, 8f); curveTo(17.5f, 8f, 17.5f, 16f, 12f, 16f)
        curveTo(6.5f, 16f, 6.5f, 8f, 12f, 8f); close()
        moveTo(12f, 2f); lineTo(12f, 5f)
        moveTo(12f, 19f); lineTo(12f, 22f)
        moveTo(2f, 12f); lineTo(5f, 12f)
        moveTo(19f, 12f); lineTo(22f, 12f)
        moveTo(5f, 5f); lineTo(7f, 7f)
        moveTo(17f, 17f); lineTo(19f, 19f)
        moveTo(19f, 5f); lineTo(17f, 7f)
        moveTo(7f, 17f); lineTo(5f, 19f)
    } }

    val Candle by lazy { lineIcon("Deck candle") {
        moveTo(9f, 11f); lineTo(15f, 11f); lineTo(15f, 20f); lineTo(9f, 20f); close()
        moveTo(12f, 11f); lineTo(12f, 9f)
        moveTo(12f, 3f); curveTo(14f, 5f, 14f, 7f, 12f, 9f)
        curveTo(10f, 7f, 10f, 5f, 12f, 3f); close()
        moveTo(6f, 21f); lineTo(18f, 21f)
    } }

    val Search by lazy { lineIcon("Deck search") {
        moveTo(10f, 3f); curveTo(14f, 3f, 17f, 6f, 17f, 10f)
        curveTo(17f, 14f, 14f, 17f, 10f, 17f)
        curveTo(6f, 17f, 3f, 14f, 3f, 10f)
        curveTo(3f, 6f, 6f, 3f, 10f, 3f); close()
        moveTo(15f, 15f); lineTo(21f, 21f)
    } }

    val Travel by lazy { lineIcon("Deck travel") {
        moveTo(12f, 3f); curveTo(14f, 3f, 14f, 6f, 12f, 6f)
        curveTo(10f, 6f, 10f, 3f, 12f, 3f); close()
        moveTo(12f, 7f); lineTo(10f, 13f); lineTo(7f, 15f)
        moveTo(12f, 8f); lineTo(16f, 11f); lineTo(19f, 11f)
        moveTo(10f, 13f); lineTo(15f, 15f); lineTo(17f, 21f)
        moveTo(10f, 13f); lineTo(7f, 21f)
    } }

    val Camp by lazy { lineIcon("Deck camp") {
        moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close()
        moveTo(12f, 3f); lineTo(12f, 20f)
        moveTo(12f, 20f); lineTo(16f, 13f); lineTo(20f, 20f)
    } }

    val Oracle by lazy { lineIcon("Deck oracle") {
        moveTo(2f, 12f); curveTo(6f, 6f, 18f, 6f, 22f, 12f)
        curveTo(18f, 18f, 6f, 18f, 2f, 12f); close()
        moveTo(12f, 9f); curveTo(16f, 9f, 16f, 15f, 12f, 15f)
        curveTo(8f, 15f, 8f, 9f, 12f, 9f); close()
        moveTo(12f, 3f); lineTo(12f, 5f)
    } }
}
