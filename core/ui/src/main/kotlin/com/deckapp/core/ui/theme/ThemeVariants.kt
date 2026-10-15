package com.deckapp.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.deckapp.core.model.ThemeAccent

val DeckParchmentNightColorScheme = DeckDarkColorScheme.copy(
    primary = Color(0xFFE6B18A), onPrimary = Color(0xFF40230F),
    primaryContainer = Color(0xFF603D25), onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD6C087), onSecondary = Color(0xFF382B09),
    secondaryContainer = Color(0xFF514220), onSecondaryContainer = Color(0xFFF6E3B1),
    tertiary = Color(0xFFA6C9A3), onTertiary = Color(0xFF17351B),
    tertiaryContainer = Color(0xFF2E4C30), onTertiaryContainer = Color(0xFFC2E6BF),
    background = Color(0xFF17120F), onBackground = Color(0xFFF1E5D3),
    surface = Color(0xFF211A15), onSurface = Color(0xFFF1E5D3),
    surfaceVariant = Color(0xFF3B3027), onSurfaceVariant = Color(0xFFD3C3B0),
    surfaceDim = Color(0xFF17120F), surfaceBright = Color(0xFF483B30),
    surfaceContainerLowest = Color(0xFF110D0A), surfaceContainerLow = Color(0xFF261E18),
    surfaceContainer = Color(0xFF2D241D), surfaceContainerHigh = Color(0xFF352B23),
    surfaceContainerHighest = Color(0xFF40342A),
    outline = Color(0xFFA99580), outlineVariant = Color(0xFF5B493A),
    inverseSurface = Color(0xFFF1E5D3), inverseOnSurface = Color(0xFF352B23),
    inversePrimary = Color(0xFF81512F), surfaceTint = Color(0xFFE6B18A), scrim = Color(0xFF100B08)
)

fun ColorScheme.withAccent(accent: ThemeAccent, dark: Boolean): ColorScheme {
    if (accent == ThemeAccent.THEME) return this
    val palette = when (accent) {
        ThemeAccent.FOREST -> if (dark) listOf(0xFFA6D5AE, 0xFF11391F, 0xFF285135, 0xFFC1F2C9)
            else listOf(0xFF28613B, 0xFFFFFFFF, 0xFFC1EAC8, 0xFF072B15)
        ThemeAccent.COPPER -> if (dark) listOf(0xFFF3B58B, 0xFF4B260B, 0xFF6B3D1D, 0xFFFFDBC3)
            else listOf(0xFF864B24, 0xFFFFFFFF, 0xFFFFDBC3, 0xFF321300)
        else -> if (dark) listOf(0xFFD5BAFF, 0xFF3D176A, 0xFF553084, 0xFFEDDCFF)
            else listOf(0xFF70429A, 0xFFFFFFFF, 0xFFEDDCFF, 0xFF290A49)
    }.map { Color(it) }
    return copy(primary = palette[0], onPrimary = palette[1], primaryContainer = palette[2],
        onPrimaryContainer = palette[3], surfaceTint = palette[0])
}
