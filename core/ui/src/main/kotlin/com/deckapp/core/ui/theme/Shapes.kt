package com.deckapp.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.AppTheme

/** Fields use extraSmall, actions small, cards medium and dialogs extraLarge. */
fun deckShapes(theme: AppTheme): Shapes = when (theme) {
    AppTheme.PARCHMENT, AppTheme.PARCHMENT_NIGHT -> Shapes(
        extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
        medium = RoundedCornerShape(10.dp), large = RoundedCornerShape(14.dp),
        extraLarge = RoundedCornerShape(18.dp)
    )
    AppTheme.DARK, AppTheme.LIGHT -> Shapes(
        extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )
}
