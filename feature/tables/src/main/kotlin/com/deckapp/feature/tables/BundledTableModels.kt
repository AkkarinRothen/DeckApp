package com.deckapp.feature.tables

import kotlinx.serialization.Serializable

@Serializable
data class BundledTablePack(val tables: List<BundledTable>)

@Serializable
data class BundledTable(
    val name: String,
    val description: String = "",
    val rollFormula: String = "1d6",
    val rollMode: String = "RANGE",
    val entries: List<BundledEntry>
)

@Serializable
data class BundledEntry(
    val minRoll: Int = 1,
    val maxRoll: Int = 1,
    val weight: Int = 1,
    val text: String,
    val subTableRef: String? = null
)
