package com.deckapp.core.domain.usecase

import com.deckapp.core.domain.repository.CardRepository
import com.deckapp.core.model.Tag
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Gestiona la obtención o creación de etiquetas para evitar duplicados globales por nombre.
 * Útil tanto para Mazos como para Cartas y Tablas.
 */
class GetOrCreateTagUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    /**
     * Busca una etiqueta existente por nombre (ignora mayúsculas/minúsculas).
     * Si no existe, la crea y devuelve la nueva instancia con su ID.
     */
    suspend operator fun invoke(name: String): Tag {
        val trimmed = name.trim()
        if (trimmed.isBlank()) throw IllegalArgumentException("El nombre del tag no puede estar vacío")

        val allTags = cardRepository.getAllTags().first()
        val existing = allTags.find { it.name.equals(trimmed, ignoreCase = true) }
        
        if (existing != null) return existing
        
        val newTag = Tag(name = trimmed)
        val id = cardRepository.saveTag(newTag)
        return newTag.copy(id = id)
    }
}
