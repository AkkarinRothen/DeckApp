package com.deckapp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta oscura — la app usa dark theme por defecto (mesas de juego con poca luz)
private val DeckDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF21005D),
    primaryContainer = Color(0xFF4A0080),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xFF003731),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    error = Color(0xFFCF6679)
)

private val DeckLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF018786),
    onSecondary = Color.White,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

/**
 * Tema principal de DeckApp.
 * [forceDarkTheme] permite forzar dark mode independiente de la configuración del sistema.
 * Por defecto usa dark theme (mesas de juego suelen tener poca luz).
 */
@Composable
fun DeckAppTheme(
    darkTheme: Boolean = true, // Dark por defecto
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DeckDarkColorScheme else DeckLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DeckAppTypography,
        content = content
    )
}
