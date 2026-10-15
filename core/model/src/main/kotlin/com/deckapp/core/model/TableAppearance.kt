package com.deckapp.core.model

enum class TextureIntensity(val multiplier: Float) {
    OFF(0f), SOFT(1f), STRONG(1.8f)
}

enum class TableDensity { COMFORTABLE, COMPACT }
enum class ThemeAccent { THEME, FOREST, COPPER, ARCANE }

data class TableAppearance(
    val texture: TextureIntensity = TextureIntensity.SOFT,
    val density: TableDensity = TableDensity.COMFORTABLE,
    val accent: ThemeAccent = ThemeAccent.THEME
)

data class AppearancePreset(
    val id: String,
    val name: String,
    val theme: AppTheme,
    val appearance: TableAppearance
)
