package com.deckapp.feature.draw.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deckapp.core.model.*
import com.deckapp.feature.draw.components.blocks.*
import com.deckapp.feature.mythic.MythicSessionUiState
import com.deckapp.feature.draw.util.getAccentColorHex
import com.deckapp.feature.draw.util.getMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardBlockCard(
    item: SessionBoardItem,
    boardGroups: List<SessionBoardGroup>,
    tableName: String?,
    tableCategory: String? = null,
    tableFormula: String? = null,
    tableRecentRolls: List<TableRollResult> = emptyList(),
    latestTableRoll: TableRollResult?,
    recentDiceRolls: List<DiceRollResult> = emptyList(),
    notesPreview: String,
    notesContent: String = "",
    isSavingNotes: Boolean = false,
    onUpdateNotes: (String) -> Unit = {},
    npcCount: Int,
    activeEncounter: Encounter?,
    playerParticipants: List<PlayerInitiativeParticipant> = emptyList(),
    permanentPlayers: List<Npc> = emptyList(),
    onApplyDamage: (Long, Int) -> Unit = { _, _ -> },
    onNextTurn: () -> Unit = {},
    onEndEncounter: () -> Unit = {},
    onAddPlayer: (String, Int) -> Unit = { _, _ -> },
    onRemovePlayer: (String) -> Unit = {},
    onSavePermanentPlayer: (Npc) -> Unit = {},
    onDeletePermanentPlayer: (Npc) -> Unit = {},
    onToggleCondition: (Long, Condition) -> Unit = { _, _ -> },
    wikiEntryCount: Int,
    referenceFocusTitle: String?,
    referencePreview: String?,
    referencePinnedCount: Int,
    referenceSearchQuery: String,
    referenceSearchCount: Int,
    referenceSearchPreview: String?,
    linkedMapName: String?,
    linkedMapTileCount: Int,
    linkedMapPoiCount: Int,
    linkedMapDayNumber: Int?,
    linkedMapPartyLocation: String?,
    linkedMythicSession: MythicSession?,
    mythicState: MythicSessionUiState? = null,
    linkedHexMapId: Long?,
    locationName: String? = null,
    locationImagePath: String? = null,
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
    tensionClocks: List<TensionClock> = emptyList(),
    deckNames: Map<Long, String> = emptyMap(),
    handByDeck: Map<Long, List<Card>> = emptyMap(),
    deckCardCounts: Map<Long, Int> = emptyMap(),
    deckAspectRatios: Map<Long, CardAspectRatio> = emptyMap(),
    deckBackImages: Map<Long, String?> = emptyMap(),
    onDrawCardFromDeck: (Long) -> Unit = {},
    onShuffleDeck: (Long) -> Unit = {},
    onResetDeck: (Long) -> Unit = {},
    onDiscardCard: (Long) -> Unit = {},
    onRevealCard: (Long) -> Unit = {},
    onCardClick: (Long) -> Unit = {},
    sessionId: Long = 0L,
    onTickClock: (Long, Int) -> Unit = { _, _ -> },
    onUpdateClockDescriptionAndProgress: (Long, String, Int) -> Unit = { _, _, _ -> },
    isEditing: Boolean,
    compactInFolder: Boolean,
    isDragTarget: Boolean,
    isMergeTarget: Boolean = false,
    npcNames: List<String> = emptyList(),
    tableNames: List<String> = emptyList(),
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onConfigClick: () -> Unit,
    onQuickAction: () -> Unit = {},
    onTogglePin: () -> Unit = {},
    onToggleCollapse: () -> Unit = {},
    onTagClicked: (String) -> Unit = {},
    onDiceClicked: (String, Boolean, Boolean, String?) -> Unit = { _, _, _, _ -> },
    onUpdateNpcHp: (Long, Int) -> Unit = { _, _ -> },
    onNpcClick: (Long) -> Unit = {},
    onWikiClick: (Long) -> Unit = {},
    onUpdateDicePresets: (Long, List<DicePreset>) -> Unit = { _, _ -> },
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDragMove: (Long, Offset) -> Unit,
    onDragEnd: () -> Unit,
    onRollTable: (Long) -> Unit,
    onBoundsChanged: (Rect) -> Unit,
    dragEnabled: Boolean = true,
    onSendToNotes: ((tableName: String, resultText: String) -> Unit)? = null,
    onRecordCombatNote: ((String) -> Unit)? = null,
    pinnedTableRoll: TableRollResult? = null,
    onPinTableRoll: ((TableRollResult) -> Unit)? = null,
    onUnpinTableRoll: (() -> Unit)? = null,
    spreadResult: CardSpreadResult? = null,
    onDrawSpread: ((CardSpread) -> Unit)? = null,
    onClearSpread: (() -> Unit)? = null,
    onSaveSpreadToNotes: ((CardSpreadResult) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val linkedClock = if (item.type == SessionBoardItemType.CLOCK) {
        tensionClocks.find { it.id == item.resourceId }
    } else null

    val linkedWikiEntry = if (item.type == SessionBoardItemType.WIKI_MAP) {
        wikiEntries.find { it.id == item.resourceId }
    } else null

    val meta = boardBlockMeta(
        item = item,
        tableName = tableName,
        tableCategory = tableCategory,
        latestTableRoll = latestTableRoll,
        notesPreview = notesPreview,
        npcCount = npcCount,
        activeEncounter = activeEncounter,
        linkedMapDayNumber = linkedMapDayNumber,
        linkedMapPartyLocation = linkedMapPartyLocation,
        linkedMythicSession = linkedMythicSession,
        linkedHexMapId = linkedHexMapId,
        linkedClock = linkedClock,
        deckNames = deckNames,
        handByDeck = handByDeck,
        deckCardCounts = deckCardCounts,
        locationName = locationName
    )

    val customAccentColor = remember(item.displayMode) {
        item.getAccentColorHex()?.let { hex ->
            try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                null
            }
        }
    }

    var offset by remember { mutableStateOf(Offset.Zero) }
    val isDragged = offset != Offset.Zero

    val scale by animateFloatAsState(
        targetValue = if (isDragged) 1.05f else 1f,
        label = "scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragged) 12.dp else 0.dp,
        label = "elevation"
    )

    var showExpanded by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(isEditing) { if (isEditing) showExpanded = false }
    var contentHeightPx by remember(item.id) { mutableIntStateOf(0) }
    var showPlayMenu by remember(item.id) { mutableStateOf(false) }
    val density = LocalDensity.current

    val blockContent: @Composable () -> Unit = {
        BoardBlockContent(
            item = if (showExpanded) item.copy(size = SessionBoardItemSize.L, isCollapsed = false) else item,
            meta = meta,
            latestTableRoll = latestTableRoll,
            linkedClock = linkedClock,
            linkedMythicSession = linkedMythicSession,
            mythicState = mythicState,
            linkedHexMapId = linkedHexMapId,
            linkedMapName = linkedMapName,
            linkedMapTileCount = linkedMapTileCount,
            linkedMapPoiCount = linkedMapPoiCount,
            linkedMapDayNumber = linkedMapDayNumber,
            linkedMapPartyLocation = linkedMapPartyLocation,
            locationName = locationName,
            locationImagePath = locationImagePath,
            hexTiles = hexTiles,
            hexPois = hexPois,
            partyLocation = partyLocation,
            onMoveParty = onMoveParty,
            onExploreTile = onExploreTile,
            onReconnoiterTile = onReconnoiterTile,
            onMapTile = onMapTile,
            locationMapPins = locationMapPins,
            npcs = npcs,
            wikiEntries = wikiEntries,
            onUpdateWikiEntry = onUpdateWikiEntry,
            linkedWikiEntry = linkedWikiEntry,
            referenceFocusTitle = referenceFocusTitle,
            referencePreview = referencePreview,
            referencePinnedCount = referencePinnedCount,
            referenceSearchQuery = referenceSearchQuery,
            referenceSearchCount = referenceSearchCount,
            wikiEntryCount = wikiEntryCount,
            compactInFolder = if (showExpanded) false else compactInFolder,
            npcNames = npcNames,
            tableNames = tableNames,
            onOpen = { showExpanded = false; onOpen() },
            onTagClicked = { tag -> showExpanded = false; onTagClicked(tag) },
            onDiceClicked = onDiceClicked,
            onRollTable = onRollTable,
            onTickClock = onTickClock,
            onUpdateClockDescriptionAndProgress = onUpdateClockDescriptionAndProgress,
            recentDiceRolls = recentDiceRolls,
            onUpdateDicePresets = onUpdateDicePresets,
            handByDeck = handByDeck,
            deckCardCounts = deckCardCounts,
            deckAspectRatios = deckAspectRatios,
            deckBackImages = deckBackImages,
            onDrawCardFromDeck = onDrawCardFromDeck,
            onShuffleDeck = onShuffleDeck,
            onResetDeck = onResetDeck,
            onDiscardCard = onDiscardCard,
            onRevealCard = onRevealCard,
            sessionId = sessionId,
            npcCount = npcCount,
            activeEncounter = activeEncounter,
            playerParticipants = playerParticipants,
            permanentPlayers = permanentPlayers,
            onApplyDamage = onApplyDamage,
            onNextTurn = onNextTurn,
            onEndEncounter = onEndEncounter,
            onAddPlayer = onAddPlayer,
            onRemovePlayer = onRemovePlayer,
            onSavePermanentPlayer = onSavePermanentPlayer,
            onDeletePermanentPlayer = onDeletePermanentPlayer,
            onToggleCondition = onToggleCondition,
            onUpdateNpcHp = onUpdateNpcHp,
            onNpcClick = { id -> showExpanded = false; onNpcClick(id) },
            onWikiClick = { id -> showExpanded = false; onWikiClick(id) },
            onCardClick = { id -> showExpanded = false; onCardClick(id) },
            notesContent = notesContent,
            onUpdateNotes = onUpdateNotes,
            onSendToNotes = onSendToNotes,
            onRecordCombatNote = onRecordCombatNote
        )
    }

    Surface(
        onClick = {
            if (!isEditing) {
                if (item.type == SessionBoardItemType.NOTES && item.noteContent != null) showExpanded = true
                else onOpen()
            }
        },
        enabled = !isEditing,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isDragTarget || isMergeTarget || item.isPinned) 2.dp else 1.dp,
            color = when {
                isMergeTarget -> MaterialTheme.colorScheme.tertiary
                isDragTarget -> MaterialTheme.colorScheme.primary
                item.isPinned -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        ),
        tonalElevation = elevation,
        modifier = modifier
            .onGloballyPositioned { onBoundsChanged(it.boundsInRoot()) }
            .graphicsLayer {
                translationX = offset.x
                translationY = offset.y
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (dragEnabled && isEditing) {
                    Modifier.pointerInput(item.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart() },
                            onDragEnd = {
                                offset = Offset.Zero
                                onDragEnd()
                            },
                            onDragCancel = {
                                offset = Offset.Zero
                                onDragEnd()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offset += dragAmount
                                onDragMove(item.id, change.position + offset)
                            }
                        )
                    }
                } else Modifier
            )
            .animateContentSize()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = (customAccentColor ?: meta.color).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = meta.icon,
                                contentDescription = null,
                                tint = customAccentColor ?: meta.color,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = meta.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (meta.subtitle.isNotBlank() && item.size != SessionBoardItemSize.S) {
                                Text(
                                    text = meta.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (isEditing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Arrastrar para reordenar",
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            IconButton(onClick = onConfigClick) {
                                Icon(Icons.Default.Settings, null, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = onRemove) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.type == SessionBoardItemType.NOTES && isSavingNotes) {
                                Icon(
                                    Icons.Default.CloudSync,
                                    contentDescription = "Guardando...",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            if (item.isPinned) {
                                Icon(
                                    Icons.Outlined.PushPin,
                                    null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            IconButton(onClick = onToggleCollapse) {
                                Icon(
                                    imageVector = if (item.isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = if (item.isCollapsed) "Expandir bloque" else "Contraer bloque",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Summary
                if (item.isCollapsed || isEditing) {
                    Text(
                        text = boardBlockSummary(item.type, meta, latestTableRoll, linkedClock, activeEncounter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Content (only shown when expanded and not editing)
                if (!item.isCollapsed && !isEditing) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (showExpanded) {
                            Spacer(Modifier.height(with(density) { contentHeightPx.toDp() }))
                        } else {
                            Box(Modifier.onSizeChanged { contentHeightPx = it.height }) {
                                blockContent()
                            }
                        }
                    }
                }

                // Footer (if needed)
                if (meta.footnote.isNotBlank() && item.size == SessionBoardItemSize.L && !compactInFolder) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = meta.footnote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        )
                        if (meta.accentNote != null) {
                            Text(
                                text = meta.accentNote,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = meta.color
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExpanded && !isEditing) {
        BoardBlockExpandedDialog(title = meta.title, onDismiss = { showExpanded = false }) {
            blockContent()
        }
    }
}

@Composable
fun AddBoardPlaceholder(onOpenPicker: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable { onOpenPicker() },
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Agregar bloque",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}