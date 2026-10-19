package com.deckapp.feature.draw.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.blocks.*
import com.deckapp.feature.draw.util.getMode
import com.deckapp.feature.mythic.MythicSessionUiState

@Composable
internal fun BoardBlockContent(
    item: SessionBoardItem,
    meta: BoardBlockMeta,
    latestTableRoll: TableRollResult?,
    linkedClock: TensionClock?,
    linkedMythicSession: MythicSession?,
    mythicState: MythicSessionUiState?,
    linkedHexMapId: Long?,
    linkedMapName: String?,
    linkedMapTileCount: Int,
    linkedMapPoiCount: Int,
    linkedMapDayNumber: Int?,
    linkedMapPartyLocation: String?,
    locationName: String?,
    locationImagePath: String?,
    hexTiles: List<HexTile> = emptyList(),
    hexPois: List<HexPoi> = emptyList(),
    partyLocation: Pair<Int, Int>? = null,
    onMoveParty: (Int, Int) -> Unit = { _, _ -> },
    onExploreTile: (HexTile) -> Unit = {},
    onReconnoiterTile: (HexTile) -> Unit = {},
    onMapTile: (HexTile) -> Unit = {},
    locationMapPins: List<MapPin> = emptyList(),
    npcs: List<Npc> = emptyList(),
    wikiEntries: List<WikiEntry> = emptyList(),
    onUpdateWikiEntry: (WikiEntry) -> Unit = {},
    linkedWikiEntry: WikiEntry? = null,
    referenceFocusTitle: String?,
    referencePreview: String?,
    referencePinnedCount: Int,
    referenceSearchQuery: String,
    referenceSearchCount: Int,
    wikiEntryCount: Int,
    compactInFolder: Boolean,
    npcNames: List<String>,
    tableNames: List<String>,
    onOpen: () -> Unit,
    onTagClicked: (String) -> Unit,
    onDiceClicked: (String, Boolean, Boolean, String?) -> Unit,
    onRollTable: (Long) -> Unit,
    onTickClock: (Long, Int) -> Unit,
    onUpdateClockDescriptionAndProgress: (Long, String, Int) -> Unit,
    recentDiceRolls: List<DiceRollResult>,
    onUpdateDicePresets: (Long, List<DicePreset>) -> Unit,
    handByDeck: Map<Long, List<Card>>,
    deckCardCounts: Map<Long, Int>,
    deckAspectRatios: Map<Long, CardAspectRatio>,
    deckBackImages: Map<Long, String?>,
    onDrawCardFromDeck: (Long) -> Unit,
    onShuffleDeck: (Long) -> Unit,
    onShuffleOnly: (Long) -> Unit = onShuffleDeck,
    onShuffleDiscards: (Long) -> Unit = onShuffleDeck,
    piledByDeck: Map<Long, Int> = emptyMap(),
    onResetDeck: (Long) -> Unit,
    onDiscardCard: (Long) -> Unit,
    onRevealCard: (Long) -> Unit,
    sessionId: Long,
    npcCount: Int,
    activeEncounter: Encounter?,
    playerParticipants: List<PlayerInitiativeParticipant>,
    permanentPlayers: List<Npc>,
    onApplyDamage: (Long, Int) -> Unit,
    onNextTurn: () -> Unit,
    onEndEncounter: () -> Unit,
    onAddPlayer: (String, Int) -> Unit,
    onRemovePlayer: (String) -> Unit,
    onSavePermanentPlayer: (Npc) -> Unit,
    onDeletePermanentPlayer: (Npc) -> Unit,
    onToggleCondition: (Long, Condition) -> Unit,
    onUpdateNpcHp: (Long, Int) -> Unit,
    onNpcClick: (Long) -> Unit,
    onWikiClick: (Long) -> Unit,
    onCardClick: (Long) -> Unit = {},
    onRotateCard: (Long) -> Unit = {},
    notesContent: String = "",
    onUpdateNotes: (String) -> Unit = {},
    onSendToNotes: ((tableName: String, resultText: String) -> Unit)? = null,
    onRecordCombatNote: ((String) -> Unit)? = null,
    tableFormula: String? = null,
    tableRecentRolls: List<TableRollResult> = emptyList(),
    randomTable: RandomTable? = null,
    pinnedTableRoll: TableRollResult? = null,
    onPinTableRoll: ((TableRollResult) -> Unit)? = null,
    onUnpinTableRoll: (() -> Unit)? = null,
    boardMythicChaos: Int = 5,
    boardMythicProbability: ProbabilityLevel = ProbabilityLevel.FIFTY_FIFTY,
    boardMythicLastResult: BoardMythicFateResult? = null,
    boardMythicHistory: List<BoardMythicFateResult> = emptyList(),
    boardMythicLastEvent: String? = null,
    onSetBoardMythicProbability: (ProbabilityLevel) -> Unit = {},
    onUpdateBoardMythicChaos: (Int) -> Unit = {},
    onRollBoardMythicFate: (ProbabilityLevel) -> Unit = {},
    onAskBoardMythicQuestion: (ProbabilityLevel, String) -> Unit = { _, _ -> },
    onRollBoardMythicEvent: () -> Unit = {},
    onAppendToNotes: (String) -> Unit = {},
    onGenerateMicrosetting: ((Long) -> Unit)? = null,
    onRotateMicrosettingSlot: ((Long, MicrosettingSlotType) -> Unit)? = null,
    onRedrawMicrosettingSlot: ((Long, MicrosettingSlotType) -> Unit)? = null
) {
    when (item.type) {
        SessionBoardItemType.TABLE -> TableBlockContent(
            item = item,
            meta = meta,
            latestTableRoll = latestTableRoll,
            tableFormula = tableFormula,
            recentRolls = tableRecentRolls,
            randomTable = randomTable,
            pinnedTableRoll = pinnedTableRoll,
            onPinTableRoll = onPinTableRoll,
            onUnpinTableRoll = onUnpinTableRoll,
            compactInFolder = compactInFolder,
            npcNames = npcNames,
            tableNames = tableNames,
            hasActiveEncounter = activeEncounter != null,
            onTagClicked = onTagClicked,
            onDiceClicked = { formula -> onDiceClicked(formula, false, false, null) },
            onRollTable = onRollTable,
            onSendToNotes = onSendToNotes,
            onRecordCombatNote = onRecordCombatNote,
            onOpenTable = onOpen
        )
        SessionBoardItemType.NOTES -> NoteBlockContent(
            item = item,
            meta = meta,
            compactInFolder = compactInFolder,
            notesContent = notesContent,
            onUpdateNotes = onUpdateNotes,
            npcNames = npcNames,
            tableNames = tableNames,
            onTagClicked = onTagClicked,
            onDiceClicked = { formula -> onDiceClicked(formula, false, false, null) },
            lastMythicResult = boardMythicLastResult,
            lastDiceResult = recentDiceRolls.firstOrNull(),
            onGenerateSpark = {
                val a = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.actionKeywords.random()
                val s = com.deckapp.core.domain.usecase.mythic.MythicReferenceData.subjectKeywords.random()
                "$a · $s"
            }
        )
        SessionBoardItemType.CLOCK -> ClockBlockContent(
            clock = linkedClock,
            compactInFolder = compactInFolder,
            onTickClock = onTickClock,
            onUpdateClockDescriptionAndProgress = onUpdateClockDescriptionAndProgress
        )
        SessionBoardItemType.MAP -> MapBlockContent(
            linkedHexMapId = linkedHexMapId,
            linkedMapName = linkedMapName,
            linkedMapTileCount = linkedMapTileCount,
            linkedMapPoiCount = linkedMapPoiCount,
            linkedMapDayNumber = linkedMapDayNumber,
            linkedMapPartyLocation = linkedMapPartyLocation,
            tiles = hexTiles,
            pois = hexPois,
            partyLocation = partyLocation,
            compactInFolder = compactInFolder,
            onOpenMap = onOpen,
            onMoveParty = onMoveParty,
            onExploreTile = onExploreTile,
            onReconnoiterTile = onReconnoiterTile,
            onMapTile = onMapTile
        )
        SessionBoardItemType.WIKI_MAP -> LocationMapBlockContent(
            locationName = locationName,
            imagePath = locationImagePath,
            mapPins = locationMapPins,
            npcs = npcs,
            wikiEntries = wikiEntries,
            compactInFolder = compactInFolder,
            size = item.size,
            onDiceClicked = { formula -> onDiceClicked(formula, false, false, null) },
            onRollTable = onRollTable,
            onOpen = onOpen,
            onUpdateWikiEntry = onUpdateWikiEntry,
            linkedWikiEntry = linkedWikiEntry,
            onOpenNpc = onNpcClick,
            onOpenWiki = onWikiClick
        )
        SessionBoardItemType.MYTHIC -> MythicBlockContent(
            item = item,
            session = linkedMythicSession,
            mythicState = mythicState,
            compactInFolder = compactInFolder,
            boardChaos = boardMythicChaos,
            boardOdds = boardMythicProbability,
            lastResult = boardMythicLastResult,
            recentHistory = boardMythicHistory,
            lastRandomEvent = boardMythicLastEvent,
            onOddsChanged = onSetBoardMythicProbability,
            onChaosChanged = onUpdateBoardMythicChaos,
            onRollFateCheck = onRollBoardMythicFate,
            onAskQuestion = onAskBoardMythicQuestion,
            onRollRandomEvent = onRollBoardMythicEvent,
            onSendToNotes = onAppendToNotes,
            onOpen = onOpen
        )
        SessionBoardItemType.REFERENCE -> ReferenceBlockContent(
            item = item,
            focusTitle = referenceFocusTitle,
            preview = referencePreview,
            pinnedCount = referencePinnedCount,
            searchQuery = referenceSearchQuery,
            searchCount = referenceSearchCount,
            searchPreview = null,
            wikiEntryCount = wikiEntryCount,
            compactInFolder = compactInFolder,
            onOpen = onOpen
        )
        SessionBoardItemType.DICE -> DiceBlockContent(
            item = item,
            meta = meta,
            recentDiceRolls = recentDiceRolls,
            compactInFolder = compactInFolder,
            onDiceClicked = onDiceClicked,
            onUpdatePresets = { newPresets -> onUpdateDicePresets(item.id, newPresets) }
        )
        SessionBoardItemType.DECK -> {
            val deckId = item.resourceId ?: 0L
            DeckBlockContent(
                item = item,
                meta = meta,
                cards = handByDeck[deckId] ?: emptyList(),
                availableCount = deckCardCounts[deckId] ?: 0,
                discardCount = piledByDeck[deckId] ?: 0,
                aspectRatio = deckAspectRatios[deckId] ?: CardAspectRatio.STANDARD,
                backImagePath = deckBackImages[deckId],
                sessionId = sessionId,
                compactInFolder = compactInFolder,
                onDraw = { onDrawCardFromDeck(deckId) },
                onShuffle = { onShuffleOnly(deckId) },
                onShuffleDiscards = { onShuffleDiscards(deckId) },
                onReset = { onResetDeck(deckId) },
                onDiscard = onDiscardCard,
                onRevealCard = onRevealCard,
                onCardClick = onCardClick,
                onRotateCard = onRotateCard
            )
        }
        SessionBoardItemType.NPCS -> {
            val mode = item.getMode()
            val featuredNpcId = item.resourceId
            val featuredNpc = npcs.find { it.id == featuredNpcId }
            val openAction = if (mode == "featured" && featuredNpc != null) {
                { onNpcClick(featuredNpc.id) }
            } else {
                onOpen
            }
            NpcBlockContent(
                item = item,
                npcCount = npcCount,
                npcNames = npcNames,
                npcs = npcs,
                compactInFolder = compactInFolder,
                onDiceClicked = { formula: String -> onDiceClicked(formula, false, false, null) },
                onUpdateNpcHp = onUpdateNpcHp,
                onNpcClick = onNpcClick,
                onOpen = openAction
            )
        }
        SessionBoardItemType.COMBAT -> CombatBlockContent(
            item = item,
            meta = meta,
            activeEncounter = activeEncounter,
            playerParticipants = playerParticipants,
            permanentPlayers = permanentPlayers,
            compactInFolder = compactInFolder,
            onApplyDamage = onApplyDamage,
            onNextTurn = onNextTurn,
            onEndEncounter = onEndEncounter,
            onAddPlayer = onAddPlayer,
            onRemovePlayer = onRemovePlayer,
            onSavePermanentPlayer = onSavePermanentPlayer,
            onDeletePermanentPlayer = onDeletePermanentPlayer,
            onOpenTracker = onOpen,
            npcs = npcs,
            onDiceClicked = { formula: String -> onDiceClicked(formula, false, false, null) },
            onToggleCondition = onToggleCondition
        )
        SessionBoardItemType.MICROSETTING -> MicrosettingBlockContent(
            item = item,
            meta = meta,
            compactInFolder = compactInFolder,
            onOpenBuilder = onOpen,
            onGenerate = { onGenerateMicrosetting?.invoke(item.id) ?: onOpen() },
            onRotateSlot = { slot -> onRotateMicrosettingSlot?.invoke(item.id, slot) },
            onRedrawSlot = { slot -> onRedrawMicrosettingSlot?.invoke(item.id, slot) },
            onSendToNotes = { cluster ->
                onAppendToNotes(MicrosettingCodec.formatMarkdown(cluster))
            },
            onOpenCard = onCardClick
        )
    }
}

