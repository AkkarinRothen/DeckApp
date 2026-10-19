package com.deckapp.core.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Define un "conector" para una herramienta de la sesión.
 * Permite que cada feature registre su propio Tab en la mesa de juego
 * sin que :feature:draw tenga que depender de ella directamente.
 */
data class SessionToolConnector(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val alignment: SessionToolAlignment,
    val content: @Composable (SessionToolContext) -> Unit
)

enum class SessionToolAlignment {
    START, END
}

/**
 * Contexto proporcionado por la Mesa de Juego a cada herramienta.
 * Contiene callbacks comunes y datos compartidos.
 */
data class SessionToolContext(
    val sessionId: Long,
    val onNavigateTo: (Any) -> Unit,
    val onShowDiceRoller: () -> Unit,
    val onRollTable: (Long) -> Unit = {},
    val onDrawCard: (Long) -> Unit = {},
    val onNpcClick: (Long) -> Unit = {},
    val onWikiClick: (Long) -> Unit = {},
    val onStartEncounter: (Long) -> Unit = {},
    val onRollDice: (String) -> Unit = {},
    val onPrepareSceneForBoard: (Long) -> Unit = {},
    val isSidebarOpen: Boolean = false,
    val onCardClick: (Long) -> Unit = {},
)

/**
 * Conector para la vista central de la mesa de juego.
 */
data class SessionCentralConnector(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val content: @Composable (SessionToolContext) -> Unit
)

object SessionToolIds {
    const val TABLES = "tables"
    const val NPCS = "npcs"
    const val WIKI = "wiki"
    const val PLAN = "plan"
    const val COMBAT = "combat"
    const val NOTES = "notes"
    const val REFERENCE = "reference"
    const val DECKS = "decks"
    const val JOURNAL = "journal"
    const val MYTHIC_CONTEXT = "mythic_context"
    const val CLOCKS = "clocks"
    const val IMPROV = "improv"
}
