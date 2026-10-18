package com.deckapp.feature.draw.tactical

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Stable pin keys keep existing saved maps compatible. */
internal object TacticalPinIcons {
    private fun icon(name: String, draw: PathBuilder.() -> Unit) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
        }.build()

    val Info by lazy { icon("Pin info") {
        moveTo(12f, 2f); curveTo(17.5f, 2f, 22f, 6.5f, 22f, 12f)
        curveTo(22f, 17.5f, 17.5f, 22f, 12f, 22f)
        curveTo(6.5f, 22f, 2f, 17.5f, 2f, 12f)
        curveTo(2f, 6.5f, 6.5f, 2f, 12f, 2f); close()
        moveTo(12f, 10f); lineTo(12f, 17f)
        moveTo(12f, 7f); lineTo(12f, 7.2f)
    } }
    val Door by lazy { icon("Pin door") {
        moveTo(5f, 3f); lineTo(19f, 3f); lineTo(19f, 21f); lineTo(5f, 21f); close()
        moveTo(8f, 6f); lineTo(16f, 6f); lineTo(16f, 21f); lineTo(8f, 21f); close()
        moveTo(13.8f, 14f); lineTo(14f, 14f)
    } }
    val Trap by lazy { icon("Pin trap") {
        moveTo(12f, 3f); lineTo(22f, 20f); lineTo(2f, 20f); close()
        moveTo(12f, 9f); lineTo(12f, 15f)
        moveTo(12f, 18f); lineTo(12f, 18.2f)
    } }
    val Chest by lazy { icon("Pin chest") {
        moveTo(3f, 10f); lineTo(21f, 10f); lineTo(21f, 20f); lineTo(3f, 20f); close()
        moveTo(3f, 10f); lineTo(5f, 5f); lineTo(19f, 5f); lineTo(21f, 10f)
        moveTo(10f, 10f); lineTo(10f, 15f); lineTo(14f, 15f); lineTo(14f, 10f)
    } }
    val Skull by lazy { icon("Pin skull") {
        moveTo(12f, 3f); curveTo(17f, 3f, 20f, 6f, 20f, 11f)
        curveTo(20f, 14f, 18f, 16f, 17f, 17f)
        lineTo(17f, 21f); lineTo(7f, 21f); lineTo(7f, 17f)
        curveTo(6f, 16f, 4f, 14f, 4f, 11f)
        curveTo(4f, 6f, 7f, 3f, 12f, 3f); close()
        moveTo(8f, 12f); lineTo(9f, 12f)
        moveTo(15f, 12f); lineTo(16f, 12f)
        moveTo(10f, 17f); lineTo(14f, 17f)
    } }
    val Secret by lazy { icon("Pin secret") {
        moveTo(8f, 3f); curveTo(11f, 3f, 13f, 5f, 13f, 8f)
        curveTo(13f, 11f, 11f, 13f, 8f, 13f)
        curveTo(5f, 13f, 3f, 11f, 3f, 8f)
        curveTo(3f, 5f, 5f, 3f, 8f, 3f); close()
        moveTo(11.5f, 11.5f); lineTo(21f, 21f)
        moveTo(18f, 18f); lineTo(20f, 16f)
    } }
    val Stairs by lazy { icon("Pin stairs") {
        moveTo(3f, 20f); lineTo(8f, 20f); lineTo(8f, 15f)
        lineTo(13f, 15f); lineTo(13f, 10f)
        lineTo(18f, 10f); lineTo(18f, 5f); lineTo(21f, 5f)
        moveTo(3f, 5f); lineTo(3f, 9f)
    } }
    val Portal by lazy { icon("Pin portal") {
        moveTo(12f, 2f); curveTo(17f, 2f, 21f, 6f, 21f, 12f)
        curveTo(21f, 18f, 17f, 22f, 12f, 22f)
        curveTo(7f, 22f, 3f, 18f, 3f, 12f)
        curveTo(3f, 6f, 7f, 2f, 12f, 2f); close()
        moveTo(12f, 6f); curveTo(15f, 6f, 17f, 8f, 17f, 12f)
        curveTo(17f, 16f, 15f, 18f, 12f, 18f)
        curveTo(9f, 18f, 7f, 16f, 7f, 12f)
        curveTo(7f, 8f, 9f, 6f, 12f, 6f); close()
    } }

    fun forName(name: String): ImageVector = when (name.lowercase()) {
        "door" -> Door
        "trap" -> Trap
        "chest" -> Chest
        "skull" -> Skull
        "secret" -> Secret
        "stairs" -> Stairs
        "portal", "map" -> Portal
        else -> Info
    }
}

/** Draws the same distinct concepts at map scale, without font-dependent Unicode glyphs. */
internal fun DrawScope.drawTacticalPinSymbol(name: String, center: Offset, radius: Float) {
    val r = radius * 0.55f
    val width = (radius * 0.16f).coerceAtLeast(1f)
    val stroke = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(center.x + x * r, center.y + y * r)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(Color.White, p(x1, y1), p(x2, y2), width, cap = StrokeCap.Round)
    when (name.lowercase()) {
        "trap" -> {
            val shape = Path().apply { moveTo(p(0f, -1f).x, p(0f, -1f).y)
                lineTo(p(1f, 0.9f).x, p(1f, 0.9f).y)
                lineTo(p(-1f, 0.9f).x, p(-1f, 0.9f).y); close() }
            drawPath(shape, Color.White, style = stroke)
            line(0f, -0.35f, 0f, 0.25f)
            drawCircle(Color.White, width * 0.55f, p(0f, 0.55f))
        }
        "door" -> {
            drawRect(Color.White, p(-0.75f, -1f), Size(1.5f * r, 2f * r), style = stroke)
            line(-0.35f, -0.65f, -0.35f, 1f)
            drawCircle(Color.White, width * 0.55f, p(0.35f, 0.15f))
        }
        "stairs" -> {
            line(-1f, 0.9f, -0.35f, 0.9f); line(-0.35f, 0.9f, -0.35f, 0.25f)
            line(-0.35f, 0.25f, 0.3f, 0.25f); line(0.3f, 0.25f, 0.3f, -0.4f)
            line(0.3f, -0.4f, 1f, -0.4f)
        }
        "portal", "map" -> {
            drawCircle(Color.White, r, center, style = stroke)
            drawCircle(Color.White, r * 0.55f, center, style = stroke)
        }
        "chest" -> {
            drawRect(Color.White, p(-1f, -0.3f), Size(2f * r, 1.2f * r), style = stroke)
            line(-1f, -0.3f, -0.7f, -0.85f); line(-0.7f, -0.85f, 0.7f, -0.85f)
            line(0.7f, -0.85f, 1f, -0.3f); line(0f, -0.3f, 0f, 0.3f)
        }
        "skull" -> {
            drawCircle(Color.White, r * 0.85f, p(0f, -0.2f), style = stroke)
            drawCircle(Color.White, width * 0.65f, p(-0.35f, -0.25f))
            drawCircle(Color.White, width * 0.65f, p(0.35f, -0.25f))
            line(-0.35f, 0.65f, 0.35f, 0.65f)
        }
        "secret" -> {
            drawCircle(Color.White, r * 0.43f, p(-0.45f, -0.45f), style = stroke)
            line(-0.15f, -0.15f, 0.9f, 0.9f); line(0.45f, 0.45f, 0.8f, 0.1f)
        }
        else -> {
            drawCircle(Color.White, r, center, style = stroke)
            drawCircle(Color.White, width * 0.6f, p(0f, -0.4f))
            line(0f, -0.05f, 0f, 0.55f)
        }
    }
}
