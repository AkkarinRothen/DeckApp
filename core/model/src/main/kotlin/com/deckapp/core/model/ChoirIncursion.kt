package com.deckapp.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChoirNodeState { UNEXPLORED, CURRENT, CLEARED, FLED }

@Serializable
enum class ChoirNodeType {
    ENTRANCE,       // Punto de Entrada / Campamento Base (Nodo 0)
    EXPLORATION,    // Yermo de Carne / Vestigios
    HAZARD,         // Aberración ambiental / Trampa viva
    COMBAT,         // Encuentro de criaturas del Coro
    RELIC_CACHE,    // Alijo sagrado / Gracia
    BOSS_CHAMBER    // Núcleo del Escatón / Jefe (Nodo 8)
}

@Serializable
enum class ChoirIncursionStatus {
    ACTIVE,
    EXTRACTED_SUCCESS,
    ABANDONED_OR_WIPED
}

@Serializable
data class ChoirIncursionNode(
    val index: Int,
    val title: String,
    val nodeType: ChoirNodeType = ChoirNodeType.EXPLORATION,
    val state: ChoirNodeState = if (index == 0) ChoirNodeState.CURRENT else ChoirNodeState.UNEXPLORED,
    val description: String = "",
    val eventOrEncounter: String = "",
    val lootFound: String = "",
    val songDelta: Int = 1
)

@Serializable
data class ChoirIncursion(
    val id: String = "incursion_1",
    val title: String = "Incursión al Páramo de Tendones",
    val currentNodeIndex: Int = 0,
    val songLevel: Int = 0, // Canción de la Carne / Tensión: 0..10
    val status: ChoirIncursionStatus = ChoirIncursionStatus.ACTIVE,
    val rationsConsumed: Int = 0,
    val lootCollected: String = "",
    val nodes: List<ChoirIncursionNode> = defaultNodes(),
    val incursionNotes: String = ""
) {
    val currentNode: ChoirIncursionNode? get() = nodes.getOrNull(currentNodeIndex)
    val isFinished: Boolean get() = status != ChoirIncursionStatus.ACTIVE
    val canExtract: Boolean get() = currentNodeIndex > 0 && (currentNode?.state == ChoirNodeState.CLEARED || currentNode?.state == ChoirNodeState.FLED)

    companion object {
        fun defaultNodes(): List<ChoirIncursionNode> = listOf(
            ChoirIncursionNode(0, "Punto de Entrada · Campamento Base", ChoirNodeType.ENTRANCE, ChoirNodeState.CURRENT, "Umbral del asentamiento hacia la zona infectada por el Coro."),
            ChoirIncursionNode(1, "Nodo 1: Muralla de Huesos Rotos", ChoirNodeType.EXPLORATION, description = "Vestigios de barricadas humanas cubiertas por venas palpitantes."),
            ChoirIncursionNode(2, "Nodo 2: Charcas de Bilis y Limo", ChoirNodeType.HAZARD, description = "Gases asfixiantes y terreno digestivo inestable."),
            ChoirIncursionNode(3, "Nodo 3: Emboscada de Despojos", ChoirNodeType.COMBAT, description = "Ecos de pasos acelerados; criaturas carroñeras de la carne."),
            ChoirIncursionNode(4, "Nodo 4: Cripta del Monasterio Caído", ChoirNodeType.RELIC_CACHE, description = "Estatua mutilada que aún conserva una lámpara con aceite bendito."),
            ChoirIncursionNode(5, "Nodo 5: El Corredor de Espinas", ChoirNodeType.HAZARD, description = "Costillas curvadas que forman un túnel claustrofóbico."),
            ChoirIncursionNode(6, "Nodo 6: Nidal de Quistes", ChoirNodeType.COMBAT, description = "Masa de quistes que laten al unísono de la Canción."),
            ChoirIncursionNode(7, "Nodo 7: Puesto de Escucha del Coro", ChoirNodeType.EXPLORATION, description = "Antena biológica que emite susurros que erosionan la mente."),
            ChoirIncursionNode(8, "Nodo 8: Altar Central del Escatón", ChoirNodeType.BOSS_CHAMBER, description = "Cámara final donde la melodía se vuelve ensordecedora.")
        )
    }
}

@Serializable
data class ChoirSettlement(
    val name: String = "Bastión de Ceniza",
    val morale: Int = 5,           // 0..10
    val defense: Int = 5,          // 0..10
    val stockRations: Int = 6,
    val stockMedicalSupplies: Int = 3,
    val population: Int = 14,
    val facilities: List<String> = listOf("Enfermería de Campaña", "Almacén de Provisiones", "Campana Bendita"),
    val activeRumors: List<String> = emptyList(),
    val log: List<String> = emptyList()
)
