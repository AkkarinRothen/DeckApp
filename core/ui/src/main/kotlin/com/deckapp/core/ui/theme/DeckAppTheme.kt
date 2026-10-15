package com.deckapp.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.deckapp.core.model.AppTheme
import com.deckapp.core.model.TableAppearance

// Paleta oscura optimizada para OLED (True Black)
val DeckDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF21005D),
    primaryContainer = Color(0xFF4A0080),
    onPrimaryContainer = Color(0xFFF0DBFF),
    secondary = Color(0xFF03DAC5),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004D45),
    onSecondaryContainer = Color(0xFFA4F4E5),
    tertiary = Color(0xFFE8C47C),
    onTertiary = Color(0xFF3D2D00),
    tertiaryContainer = Color(0xFF554015),
    onTertiaryContainer = Color(0xFFFFDEA1),
    background = Color(0xFF000000), // Negro puro para OLED
    surface = Color(0xFF121212),    // Superficies muy oscuras
    surfaceVariant = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceDim = Color(0xFF101010),
    surfaceBright = Color(0xFF383838),
    surfaceContainerLowest = Color(0xFF080808),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHighest = Color(0xFF303030),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFC5C0CA),
    outline = Color(0xFF96909B),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE8E3EB),
    inverseOnSurface = Color(0xFF302D33),
    inversePrimary = Color(0xFF6B3DA3),
    surfaceTint = Color(0xFFBB86FC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black
)

val DeckLightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF006A69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7EFEB),
    onSecondaryContainer = Color(0xFF00201F),
    tertiary = Color(0xFF795900),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA1),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    surfaceVariant = Color(0xFFF4EFF4),
    surfaceContainerHigh = Color(0xFFEBE6EB),
    surfaceDim = Color(0xFFDED8DE),
    surfaceBright = Color(0xFFFFFBFE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F3F8),
    surfaceContainer = Color(0xFFF1ECF1),
    surfaceContainerHighest = Color(0xFFE5E0E5),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFD0BCFF),
    surfaceTint = Color(0xFF6200EE),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black
)

// Paleta Pergamino inspirada en grimorios y manuales clásicos TTRPG
val DeckParchmentColorScheme = lightColorScheme(
    primary = Color(0xFF7B1E2B),          // Borgoña / Vino tinto medieval
    onPrimary = Color(0xFFFBF8F0),        // Blanco papiro
    primaryContainer = Color(0xFFF2D6D9), // Borgoña suave
    onPrimaryContainer = Color(0xFF3B0B12),
    secondary = Color(0xFF80600F),        // Bronce con contraste para texto blanco
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF6E8C3),
    onSecondaryContainer = Color(0xFF422F00),
    tertiary = Color(0xFF3D6B52),         // Verde bosque antiguo
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4E7DC),
    onTertiaryContainer = Color(0xFF0F2E1E),
    background = Color(0xFFF4EEDD),       // Crema pergamino cálido
    onBackground = Color(0xFF2B2118),     // Tinta carbón / sepia profundo
    surface = Color(0xFFEBE1C8),          // Papiro envejecido
    onSurface = Color(0xFF2B2118),
    surfaceVariant = Color(0xFFE2D6BA),
    onSurfaceVariant = Color(0xFF5C4F43),
    surfaceContainerHigh = Color(0xFFDFD2B4),
    surfaceDim = Color(0xFFDED3BA),
    surfaceBright = Color(0xFFFFF8E9),
    surfaceContainerLowest = Color(0xFFFFFBF2),
    surfaceContainerLow = Color(0xFFF5ECD8),
    surfaceContainer = Color(0xFFEBE1C8),
    surfaceContainerHighest = Color(0xFFD7C9AA),
    inverseSurface = Color(0xFF352C23),
    inverseOnSurface = Color(0xFFF5ECD8),
    inversePrimary = Color(0xFFE9A8AF),
    surfaceTint = Color(0xFF7B1E2B),
    outline = Color(0xFF9E8E7C),
    outlineVariant = Color(0xFFD5C7B0),
    error = Color(0xFF9E2A2B),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0xFF21170F)
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme.DARK }
val LocalTableAppearance = staticCompositionLocalOf { TableAppearance() }

/**
 * Tema principal de DeckApp con soporte para [AppTheme].
 */
@Composable
fun DeckAppTheme(
    appTheme: AppTheme = AppTheme.DARK,
    tableAppearance: TableAppearance = TableAppearance(),
    content: @Composable () -> Unit
) {
    val (colorScheme, typography) = when (appTheme) {
        AppTheme.DARK -> DeckDarkColorScheme to DeckAppTypography
        AppTheme.LIGHT -> DeckLightColorScheme to DeckAppTypography
        AppTheme.PARCHMENT -> DeckParchmentColorScheme to DeckParchmentTypography
        AppTheme.PARCHMENT_NIGHT -> DeckParchmentNightColorScheme to DeckParchmentTypography
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme, LocalTableAppearance provides tableAppearance) {
        MaterialTheme(
            colorScheme = colorScheme.withAccent(tableAppearance.accent,
                appTheme == AppTheme.DARK || appTheme == AppTheme.PARCHMENT_NIGHT),
            typography = typography,
            shapes = deckShapes(appTheme),
            content = content
        )
    }
}

/**
 * Sobrecarga por retrocompatibilidad con [darkTheme].
 */
@Composable
fun DeckAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    DeckAppTheme(
        appTheme = if (darkTheme) AppTheme.DARK else AppTheme.LIGHT,
        content = content
    )
}
