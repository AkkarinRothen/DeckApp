package com.deckapp.core.data.repository

import com.deckapp.core.model.*
import kotlinx.serialization.json.*

internal object AppearancePresetCodec {
    fun encode(presets: List<AppearancePreset>): String = buildJsonArray {
        presets.forEach { preset -> add(buildJsonObject {
            put("id", preset.id); put("name", preset.name); put("theme", preset.theme.name)
            put("texture", preset.appearance.texture.name); put("density", preset.appearance.density.name)
            put("accent", preset.appearance.accent.name)
        }) }
    }.toString()

    fun decode(source: String): List<AppearancePreset> = runCatching {
        Json.parseToJsonElement(source).jsonArray.mapNotNull { value -> runCatching {
            val item = value.jsonObject
            fun field(key: String) = item.getValue(key).jsonPrimitive.content
            AppearancePreset(field("id"), field("name"), AppTheme.valueOf(field("theme")),
                TableAppearance(TextureIntensity.valueOf(field("texture")), TableDensity.valueOf(field("density")),
                    ThemeAccent.valueOf(item["accent"]?.jsonPrimitive?.content ?: "THEME")))
        }.getOrNull() }
    }.getOrDefault(emptyList())
}
