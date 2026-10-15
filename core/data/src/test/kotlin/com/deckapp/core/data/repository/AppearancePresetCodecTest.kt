package com.deckapp.core.data.repository

import com.deckapp.core.model.*
import org.junit.Assert.*
import org.junit.Test

class AppearancePresetCodecTest {
    private val preset = AppearancePreset("campaign-1", "El bosque \"olvidado\"\nCampaña", AppTheme.PARCHMENT_NIGHT,
        TableAppearance(TextureIntensity.STRONG, TableDensity.COMPACT, ThemeAccent.COPPER))

    @Test fun `round trip preserves campaign name and every appearance option`() {
        assertEquals(listOf(preset), AppearancePresetCodec.decode(AppearancePresetCodec.encode(listOf(preset))))
    }

    @Test fun `malformed entry does not discard other campaigns`() {
        val encoded = AppearancePresetCodec.encode(listOf(preset))
        assertEquals(listOf(preset), AppearancePresetCodec.decode("[{\"theme\":\"UNKNOWN\"},${encoded.drop(1)}"))
    }

    @Test fun `old preset without accent uses theme accent`() {
        val encoded = AppearancePresetCodec.encode(listOf(preset)).replace(",\"accent\":\"COPPER\"", "")
        assertEquals(ThemeAccent.THEME, AppearancePresetCodec.decode(encoded).single().appearance.accent)
    }

    @Test fun `corrupt document returns empty list`() {
        assertTrue(AppearancePresetCodec.decode("broken").isEmpty())
    }
}
