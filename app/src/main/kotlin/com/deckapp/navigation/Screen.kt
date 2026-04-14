package com.deckapp.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación tipadas con kotlinx.serialization
// Cada objeto/clase corresponde a una pantalla en el NavGraph

@Serializable object LibraryRoute
@Serializable object SessionListRoute
@Serializable object SettingsRoute
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
