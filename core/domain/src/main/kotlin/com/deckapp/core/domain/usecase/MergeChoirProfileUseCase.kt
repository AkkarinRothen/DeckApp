package com.deckapp.core.domain.usecase

import com.deckapp.core.model.ChoirProfile

/** Upgrade only vendor text; keep personal notes and never repeat the canonical appendix. */
object MergeChoirProfileUseCase {
    fun notes(existing: String, canonical: String, legacy: String?): String = when {
        ChoirProfile.fromNotes(existing) != null -> existing
        existing.isBlank() || existing == legacy -> canonical
        else -> "### Notas anteriores conservadas\n\n$existing\n\n---\n\n$canonical"
    }
}
