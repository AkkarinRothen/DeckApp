package com.deckapp.core.model

import com.deckapp.core.model.DrawAction
import com.deckapp.core.model.DrawMode
import kotlinx.serialization.Serializable

/** Sesión de juego activa */
data class Session(
    val id: Long = 0,
    val name: String,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val scheduledDate: Long? = null,
    val summary: String? = null,
    val showCardTitles: Boolean = true,
    val dmNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val gameSystems: List<String> = listOf("General"),
    val linkedMythicSessionId: Long? = null,
    val recentActivityCollapsed: Boolean? = null,
    val activeBoardView: SessionBoardView = SessionBoardView.MAIN,
    val boardViewLabels: SessionBoardViewLabels = SessionBoardViewLabels(),
    val hotbarActions: List<SessionHotbarAction> = emptyList(),
    val tensionClocks: List<TensionClock> = emptyList()
)

@kotlinx.serialization.Serializable
enum class SessionBoardItemType {
    TABLE,
    NOTES,
    MYTHIC,
    MAP,
    DICE,
    NPCS,
    COMBAT,
    REFERENCE,
    CLOCK,
    DECK,
    WIKI_MAP,
    MICROSETTING
}

@kotlinx.serialization.Serializable
enum class SessionBoardItemSize {
    S, M, L
}

@kotlinx.serialization.Serializable
enum class SessionBoardLayoutMode {
    GRID,
    LIST
}

@kotlinx.serialization.Serializable
enum class SessionBoardView {
    MAIN,
    EXPLORATION,
    NARRATIVE,
    COMBAT
}

@Serializable
data class SessionBoardViewLabels(
    val main: String? = null,
    val exploration: String? = null,
    val narrative: String? = null,
    val combat: String? = null,
    val viewOrder: List<SessionBoardView>? = null,
    val hiddenViews: List<SessionBoardView>? = null
) {
    fun labelFor(view: SessionBoardView): String {
        val custom = when (view) {
            SessionBoardView.MAIN -> main
            SessionBoardView.EXPLORATION -> exploration
            SessionBoardView.NARRATIVE -> narrative
            SessionBoardView.COMBAT -> combat
        }?.trim().orEmpty()
        return custom.ifBlank { view.defaultLabel() }
    }

    fun withLabel(view: SessionBoardView, label: String?): SessionBoardViewLabels {
        val normalized = label?.trim().takeUnless { it.isNullOrBlank() }
        return when (view) {
            SessionBoardView.MAIN -> copy(main = normalized)
            SessionBoardView.EXPLORATION -> copy(exploration = normalized)
            SessionBoardView.NARRATIVE -> copy(narrative = normalized)
            SessionBoardView.COMBAT -> copy(combat = normalized)
        }
    }

    fun getOrderedViews(): List<SessionBoardView> {
        val base = viewOrder ?: SessionBoardView.entries
        val hidden = hiddenViews.orEmpty()
        val filtered = base.filterNot { it in hidden }
        return if (filtered.isEmpty()) listOf(SessionBoardView.MAIN) else filtered
    }
}

fun SessionBoardView.defaultLabel(): String = when (this) {
    SessionBoardView.MAIN -> "Principal"
    SessionBoardView.EXPLORATION -> "Exploracion"
    SessionBoardView.NARRATIVE -> "Narrativa"
    SessionBoardView.COMBAT -> "Combate"
}

@kotlinx.serialization.Serializable
data class SessionBoardGroup(
    val id: Long = 0,
    val sessionId: Long,
    val boardView: SessionBoardView = SessionBoardView.MAIN,
    val name: String,
    val accentColorHex: String? = null,
    val iconName: String? = null,
    val preferredLayoutMode: SessionBoardLayoutMode = SessionBoardLayoutMode.GRID,
    val position: Int = 0,
    val isCollapsed: Boolean = false,
    val autoFilterType: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@kotlinx.serialization.Serializable
data class SessionBoardItem(
    val id: Long = 0,
    val sessionId: Long,
    val type: SessionBoardItemType,
    val resourceId: Long? = null,
    val titleOverride: String? = null,
    val position: Int = 0,
    val size: SessionBoardItemSize = SessionBoardItemSize.M,
    val displayMode: String? = null,
    val boardView: SessionBoardView = SessionBoardView.MAIN,
    val isPinned: Boolean = false,
    val isCollapsed: Boolean = false,
    val groupId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** null conserva el vínculo histórico al cuaderno; vacío es una nota propia vacía. */
    val noteContent: String? = null
)

enum class SessionStatus {
    PLANNED, ACTIVE, COMPLETED
}

/** Referencia de un mazo asignado a una sesión */
data class SessionDeckRef(
    val sessionId: Long,
    val stackId: Long,
    val drawModeOverride: DrawMode? = null,   // override del DrawMode del mazo
    val sortOrder: Int = 0
)

/**
 * Evento del log de sesión (append-only).
 * El estado actual de la sesión se reconstruye reproduciendo estos eventos.
 * Esto habilita: undo, historial de sesión y crash recovery.
 */
data class DrawEvent(
    val id: Long = 0,
    val sessionId: Long,
    val cardId: Long?,
    val action: DrawAction,
    val metadata: String = "",  // JSON opcional (ej: nombre del jugador al hacer PASS)
    val timestamp: Long = System.currentTimeMillis()
)

/** Referencia de una tabla asignada a una sesión */
data class SessionTableRef(
    val sessionId: Long,
    val tableId: Long,
    val sortOrder: Int = 0
)

/** Preset de dados guardado en un bloque de dados */
@Serializable
data class DicePreset(
    val name: String,
    val formula: String
)

/** Tipo de acción rápida en la barra Hotbar */
@Serializable
enum class HotbarActionType {
    DICE_ROLL,
    ORACLE_QUERY,
    RANDOM_TABLE,
    DRAW_CARD
}

/** Atajo o macro personalizada de la barra rápida de la mesa */
@Serializable
data class SessionHotbarAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val type: HotbarActionType,
    val payload: String, // Formula ("1d20+5"), Oracle ("50_50", "RANDOM_EVENT"), TableId ("1"), DeckId ("2")
    val iconName: String? = null,
    val colorHex: Long? = null
)

/** Starter Pack de atajos por defecto para sesiones de juego */
fun defaultHotbarActions(): List<SessionHotbarAction> = listOf(
    SessionHotbarAction(
        label = "d20",
        type = HotbarActionType.DICE_ROLL,
        payload = "1d20",
        iconName = "dice_d20",
        colorHex = 0xFF5C6BC0L
    ),
    SessionHotbarAction(
        label = "d100",
        type = HotbarActionType.DICE_ROLL,
        payload = "1d100",
        iconName = "dice_d100",
        colorHex = 0xFF42A5F5L
    ),
    SessionHotbarAction(
        label = "Oráculo 50/50",
        type = HotbarActionType.ORACLE_QUERY,
        payload = "50_50",
        iconName = "auto_awesome",
        colorHex = 0xFFAB47BCL
    ),
    SessionHotbarAction(
        label = "Evento Aleatorio",
        type = HotbarActionType.ORACLE_QUERY,
        payload = "RANDOM_EVENT",
        iconName = "bolt",
        colorHex = 0xFFFF7043L
    )
)

