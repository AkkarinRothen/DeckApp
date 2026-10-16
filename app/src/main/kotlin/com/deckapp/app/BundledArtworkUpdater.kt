package com.deckapp.app

import android.content.Context
import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.backup.FullBackupDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Idempotent, non-destructive artwork and missing-table backfill for libraries installed before this update. */
class BundledArtworkUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val references: ReferenceRepository
) {
    suspend fun updateInstalledArtwork() {
        val packName = "choir_of_flesh.json"
        if (packName !in references.getInstalledPackNames().first()) return
        val pack = context.assets.open("starter_packs/$packName")
            .bufferedReader(Charsets.UTF_8).use { stream ->
                Json { ignoreUnknownKeys = true }.decodeFromString<FullBackupDto>(stream.readText().trimStart('\uFEFF'))
            }
        val previous = context.assets.open("starter_packs/choir_of_flesh/legacy-bestiary.json")
            .bufferedReader(Charsets.UTF_8).use {
                Json { ignoreUnknownKeys = true }.decodeFromString<FullBackupDto>(it.readText())
            }
        references.updateStarterPackArtwork(
            pack.copy(systemRules = pack.systemRules.filter {
                it.category == "Ilustraciones del manual" || it.title == "Combate, Iniciativa y Táctica"
            }),
            packName,
            previous
        )
    }
}
