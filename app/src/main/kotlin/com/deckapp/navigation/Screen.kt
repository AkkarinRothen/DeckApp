package com.deckapp.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación tipadas con kotlinx.serialization
// Cada objeto/clase corresponde a una pantalla en el NavGraph

@Serializable object LibraryRoute
@Serializable object SessionListRoute
@Serializable object SettingsRoute
@Serializable object TagManagerRoute
@Serializable object ImportRoute

@Serializable data class DeckDetailRoute(val deckId: Long)
@Serializable data class CardViewRoute(val cardId: Long, val sessionId: Long? = null)
@Serializable data class CardEditorRoute(val cardId: Long? = null, val deckId: Long)
@Serializable data class SessionRoute(val sessionId: Long)
@Serializable data class SessionSetupRoute(val preselectedDeckId: Long? = null)
@Serializable data class SessionHistoryRoute(val sessionId: Long)

// Tablas aleatorias
@Serializable object TablesListRoute
@Serializable data class TableEditorRoute(val tableId: Long = -1L)
@Serializable object TableImportRoute

// Encuentros y Combate
@Serializable object EncounterListRoute
@Serializable data class EncounterEditorRoute(val encounterId: Long = 0L)

// NPCs
@Serializable object NpcListRoute
@Serializable data class NpcEditorRoute(val npcId: Long = -1L)

// Wiki
@Serializable object WikiRoute
@Serializable data class WikiEntryRoute(val entryId: Long? = null, val categoryId: Long? = null)

// Hexploración
@Serializable object HexMapListRoute
@Serializable data class HexMapEditorRoute(val mapId: Long)
@Serializable data class HexMapSessionRoute(val mapId: Long)

// Referencias
@Serializable object ReferenceListRoute
@Serializable data class ReferenceTableEditorRoute(val tableId: Long = -1L, val prefilledSystem: String = "")
@Serializable data class RuleEditorRoute(val ruleId: Long = -1L, val prefilledSystem: String = "")
