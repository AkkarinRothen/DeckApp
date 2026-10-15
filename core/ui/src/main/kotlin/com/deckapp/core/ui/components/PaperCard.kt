package com.deckapp.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.AppTheme
import androidx.compose.ui.geometry.Offset
import kotlin.random.Random
import com.deckapp.core.ui.theme.LocalAppTheme
import com.deckapp.core.ui.theme.LocalTableAppearance

/**
 * Modificador para aplicar una textura de papel de fondo continua y optimizada.
 *
 * @param theme Tema visual actual (se toma de [LocalAppTheme] si no se especifica)
 * @param tintColor Color de fondo base de la superficie
 * @param alpha Opacidad de la textura
 */
@Composable
fun Modifier.paperBackground(
    theme: AppTheme = LocalAppTheme.current,
    tintColor: Color = MaterialTheme.colorScheme.surface,
    alpha: Float? = null
): Modifier {
    val multiplier = LocalTableAppearance.current.texture.multiplier
    if (multiplier == 0f) return this.background(tintColor)
    val textureAlpha = alpha ?: when (theme) {
        AppTheme.DARK -> 0.06f
        AppTheme.LIGHT -> 0.08f
        AppTheme.PARCHMENT, AppTheme.PARCHMENT_NIGHT -> 0.18f
    }
    return this.drawWithCache {
        val random = Random(417)
        val count = (size.width * size.height / (12.dp.toPx() * 12.dp.toPx())).toInt().coerceIn(0, 2400)
        val points = List(count) { Offset(random.nextFloat() * size.width, random.nextFloat() * size.height) }
        val dark = theme == AppTheme.DARK || theme == AppTheme.PARCHMENT_NIGHT
        val parchment = theme == AppTheme.PARCHMENT || theme == AppTheme.PARCHMENT_NIGHT
        val ink = if (dark) Color(0xFFF2DCC0) else Color(0xFF5D432D)
        val opacity = (textureAlpha * multiplier).coerceIn(0f, 0.5f)
        onDrawBehind {
            drawRect(tintColor)
            if (parchment) drawRect(Brush.verticalGradient(listOf(
                ink.copy(alpha = opacity * 0.15f), Color.Transparent, ink.copy(alpha = opacity * 0.22f))))
            points.forEachIndexed { index, point ->
                if (theme == AppTheme.DARK) {
                    drawCircle(ink, radius = (if (index % 3 == 0) 0.7f else 0.35f).dp.toPx(),
                        center = point, alpha = opacity)
                } else {
                    val length = (if (parchment) 3 + index % 7 else 1 + index % 3).dp.toPx()
                    drawLine(ink.copy(alpha = opacity * (if (parchment) 0.65f else 0.45f)),
                        point, point + Offset(length, if (parchment) length * 0.2f else 0f), strokeWidth = 0.4.dp.toPx())
                }
            }
        }
    }
}

/**
 * Tarjeta temática con textura táctil de papel adaptada a Dark, Light y Parchment.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    elevation: Dp = 1.dp,
    border: BorderStroke? = null,
    textureAlpha: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalAppTheme.current
    val effectiveBorder = border ?: deckCardBorder()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = if (contentColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else contentColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = effectiveBorder
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .paperBackground(
                    theme = theme,
                    tintColor = containerColor,
                    alpha = textureAlpha
                )
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(
                            interactionSource = interactionSource,
                            indication = ripple(),
                            onClick = { onClick?.invoke() },
                            onLongClick = onLongClick
                        )
                    } else Modifier
                ),
            content = content
        )
    }
}

/**
 * Superficie con textura de papel para modales, paneles y hojas emergentes.
 */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    elevation: Dp = 0.dp,
    border: BorderStroke? = null,
    textureAlpha: Float? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalAppTheme.current
    val effectiveBorder = border ?: if (shape != RectangleShape) deckCardBorder() else null

    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = if (contentColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else contentColor,
        tonalElevation = elevation,
        shadowElevation = elevation,
        border = effectiveBorder
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .paperBackground(theme = theme, tintColor = color, alpha = textureAlpha)
        ) {
            content()
        }
    }
}
