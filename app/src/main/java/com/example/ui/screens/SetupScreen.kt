package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Player
import com.example.model.GameRules
import com.example.model.GameType
import com.example.model.TournamentType
import com.example.ui.theme.*
import com.example.viewmodel.DartsViewModel

private val GAME_TYPES = listOf(GameType.GAME_301, GameType.GAME_401, GameType.GAME_501)

@Composable
fun TwoLineDragHandle(
    modifier: Modifier = Modifier,
    color: Color = TextSecondary
) {
    Canvas(
        modifier = modifier
            .size(width = 20.dp, height = 14.dp)
    ) {
        val strokeWidth = 2.5.dp.toPx()
        val w = size.width
        val h = size.height
        // Top line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.25f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.25f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Bottom line
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, h * 0.75f),
            end = androidx.compose.ui.geometry.Offset(w, h * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun SetupScreen(
    viewModel: DartsViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.allPlayers.collectAsStateWithLifecycle()
    val selectedGameType by viewModel.selectedGameType.collectAsStateWithLifecycle()
    val doubleIn by viewModel.doubleIn.collectAsStateWithLifecycle()
    val doubleOut by viewModel.doubleOut.collectAsStateWithLifecycle()
    val setsToWin by viewModel.setsToWin.collectAsStateWithLifecycle()
    val legsToWinSet by viewModel.legsToWinSet.collectAsStateWithLifecycle()
    val selectedPlayerIds by viewModel.selectedPlayerIds.collectAsStateWithLifecycle()

    var showTournamentDialog by remember { mutableStateOf(false) }
    var showPlayerSelectPopup by remember { mutableStateOf(false) }

    // Intercept phone back button to dismiss popups first
    BackHandler(enabled = showPlayerSelectPopup) {
        showPlayerSelectPopup = false
    }
    BackHandler(enabled = showTournamentDialog) {
        showTournamentDialog = false
    }

    var draggingPlayerId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var playerRowHeightPx by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemSpacingPx = with(density) { 8.dp.toPx() }

    // Map selected IDs to actual Player objects in selected order (O(N + M))
    val selectedPlayersList = remember(players, selectedPlayerIds) {
        val playersById = players.associateBy { it.id }
        selectedPlayerIds.mapNotNull { playersById[it] }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            userScrollEnabled = (draggingPlayerId == null),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp)
        ) {
            // 1. Game Settings (Game Type, Format Steppers, In/Out Options)
            item {
                Text(
                    text = "1. SELECT GAME SETTINGS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Game Type Row (301, 401, 501)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GAME_TYPES.forEach { type ->
                            val isSelected = (selectedGameType == type)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.setGameType(type) }
                                    .testTag("game_type_${type.displayName}"),
                                color = if (isSelected) BrandPrimaryContainer else AppSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) BrandPrimary else AppBorder
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type.displayName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandOnPrimaryContainer else TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Match Format: Sets & Legs Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sets to Win
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = AppSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { viewModel.setSetsToWin((setsToWin - 1).coerceAtLeast(1)) },
                                    enabled = setsToWin > 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stepper_decrease_sets")
                                ) {
                                    Icon(
                                        imageVector = AppIcons.RemoveCircleOutline,
                                        contentDescription = "Decrease Sets",
                                        tint = if (setsToWin > 1) BrandPrimary else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    text = "$setsToWin ${if (setsToWin == 1) "Set" else "Sets"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )

                                IconButton(
                                    onClick = { viewModel.setSetsToWin((setsToWin + 1).coerceAtMost(10)) },
                                    enabled = setsToWin < 10,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stepper_increase_sets")
                                ) {
                                    Icon(
                                        imageVector = AppIcons.AddCircleOutline,
                                        contentDescription = "Increase Sets",
                                        tint = if (setsToWin < 10) BrandPrimary else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        // Legs to Win each Set
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = AppSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(
                                    onClick = { viewModel.setLegsToWinSet((legsToWinSet - 1).coerceAtLeast(1)) },
                                    enabled = legsToWinSet > 1,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stepper_decrease_legs")
                                ) {
                                    Icon(
                                        imageVector = AppIcons.RemoveCircleOutline,
                                        contentDescription = "Decrease Legs",
                                        tint = if (legsToWinSet > 1) BrandPrimary else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Text(
                                    text = "$legsToWinSet ${if (legsToWinSet == 1) "Leg" else "Legs"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )

                                IconButton(
                                    onClick = { viewModel.setLegsToWinSet((legsToWinSet + 1).coerceAtMost(10)) },
                                    enabled = legsToWinSet < 10,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("stepper_increase_legs")
                                ) {
                                    Icon(
                                        imageVector = AppIcons.AddCircleOutline,
                                        contentDescription = "Increase Legs",
                                        tint = if (legsToWinSet < 10) BrandPrimary else TextMuted.copy(alpha = 0.4f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // In / Out Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Double In Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setDoubleIn(!doubleIn) }
                                .testTag("toggle_double_in"),
                            color = if (doubleIn) BrandPrimaryContainer else AppSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (doubleIn) 1.5.dp else 1.dp,
                                if (doubleIn) BrandPrimary else AppBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Double In",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doubleIn) BrandOnPrimaryContainer else TextPrimary
                                )
                            }
                        }

                        // Double Out Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.setDoubleOut(!doubleOut) }
                                .testTag("toggle_double_out"),
                            color = if (doubleOut) BrandSuccessContainer.copy(alpha = 0.35f) else AppSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                if (doubleOut) 1.5.dp else 1.dp,
                                if (doubleOut) BrandSuccess else AppBorder
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Double Out",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (doubleOut) BrandSuccess else TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Player Selection Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "2. PLAYERS & ORDER (${selectedPlayerIds.size} Added)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        // Add Player icon button
                        IconButton(
                            onClick = { showPlayerSelectPopup = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("setup_add_player_popup_btn")
                        ) {
                            Icon(
                                imageVector = AppIcons.PersonAdd,
                                contentDescription = "Add / Choose Players",
                                tint = BrandPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    if (selectedPlayerIds.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearPlayerSelection() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Clear All", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Only show empty prompt if no players added to match
            if (selectedPlayersList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showPlayerSelectPopup = true }
                            .testTag("empty_players_add_prompt"),
                        shape = RoundedCornerShape(14.dp),
                        color = AppSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = BrandPrimaryContainer,
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = AppIcons.PersonAdd,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = "No Players Added to Match",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap here or the add player icon above to select players. Drag the 2-line handle to reorder.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(selectedPlayersList, key = { _, item -> item.id }) { _, player ->
                    val isDragging = (draggingPlayerId == player.id)

                    val rowModifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (size.height > 0) {
                                playerRowHeightPx = size.height.toFloat()
                            }
                        }
                        .then(if (!isDragging) Modifier.animateItem() else Modifier)
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffsetY else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = if (isDragging) 1.02f else 1f
                            shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .testTag("selected_player_row_${player.id}")

                    Surface(
                        modifier = rowModifier,
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDragging) AppSurfaceElevated else AppSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isDragging) 2.dp else 1.dp,
                            if (isDragging) BrandPrimary else AppBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // 2-Line Drag and Drop Handle (window-space tracking avoids translationY feedback loop)
                                var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                                Surface(
                                    modifier = Modifier
                                        .size(48.dp, 42.dp)
                                        .onGloballyPositioned { coords ->
                                            handleCoords = coords
                                        }
                                        .pointerInput(player.id) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                val startCoords = handleCoords
                                                if (startCoords == null || !startCoords.isAttached) return@awaitEachGesture

                                                val startIndex = viewModel.selectedPlayerIds.value.indexOf(player.id)
                                                if (startIndex == -1) return@awaitEachGesture

                                                down.consume()
                                                var lastWindowY = startCoords.localToWindow(down.position).y
                                                var currentIndex = startIndex
                                                var currentOffset = 0f

                                                draggingPlayerId = player.id
                                                dragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                                try {
                                                    while (true) {
                                                        val event = awaitPointerEvent(PointerEventPass.Main)
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                            ?: event.changes.firstOrNull()
                                                        if (change == null || !change.pressed) {
                                                            break
                                                        }

                                                        val coords = handleCoords
                                                        val currentWindowY = if (coords != null && coords.isAttached) {
                                                            coords.localToWindow(change.position).y
                                                        } else {
                                                            lastWindowY
                                                        }
                                                        val deltaY = currentWindowY - lastWindowY
                                                        lastWindowY = currentWindowY
                                                        change.consume()

                                                        val listSize = viewModel.selectedPlayerIds.value.size
                                                        if (listSize <= 1) continue

                                                        val stepPx = (if (playerRowHeightPx > 0f) playerRowHeightPx else with(density) { 64.dp.toPx() }) + itemSpacingPx
                                                        val minOffset = -currentIndex * stepPx
                                                        val maxOffset = (listSize - 1 - currentIndex) * stepPx

                                                        currentOffset = (currentOffset + deltaY).coerceIn(minOffset, maxOffset)

                                                        while (currentOffset > stepPx * 0.5f && currentIndex < listSize - 1) {
                                                            viewModel.reorderSelectedPlayers(currentIndex, currentIndex + 1)
                                                            currentIndex += 1
                                                            currentOffset -= stepPx
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }

                                                        while (currentOffset < -stepPx * 0.5f && currentIndex > 0) {
                                                            viewModel.reorderSelectedPlayers(currentIndex, currentIndex - 1)
                                                            currentIndex -= 1
                                                            currentOffset += stepPx
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        }

                                                        dragOffsetY = currentOffset
                                                    }
                                                } finally {
                                                    draggingPlayerId = null
                                                    dragOffsetY = 0f
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDragging) BrandPrimary.copy(alpha = 0.25f) else AppSurfaceElevated
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        TwoLineDragHandle(
                                            color = if (isDragging) BrandPrimary else TextSecondary
                                        )
                                    }
                                }

                                // Player Name and Stats (No P1, P2 badge)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = player.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDragging) BrandPrimary else TextPrimary
                                    )
                                    Text(
                                        text = "Record: ${player.wins}W - ${player.losses}L (${player.winRatePercent}%)",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Remove Button
                            IconButton(
                                onClick = { viewModel.removeSelectedPlayer(player.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("remove_selected_player_${player.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove player",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar (Start Match / Tournament)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val canStart = selectedPlayerIds.isNotEmpty()
                val canStartTournament = selectedPlayerIds.size >= 2

                // Primary Start Match Button
                Button(
                    onClick = { viewModel.startMatch() },
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("start_match_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary,
                        disabledContainerColor = AppSurfaceElevated,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (canStart) "Start Match (${selectedGameType.displayName} • $setsToWin ${if (setsToWin == 1) "Set" else "Sets"} • $legsToWinSet ${if (legsToWinSet == 1) "Leg" else "Legs"})" else "Select Players to Start",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tournament Button
                OutlinedButton(
                    onClick = { showTournamentDialog = true },
                    enabled = canStartTournament,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("start_tournament_modal_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (canStartTournament) BrandPrimary else AppBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandPrimary,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.EmojiEvents,
                        contentDescription = null,
                        tint = if (canStartTournament) BrandSecondary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tournament Mode (${selectedPlayerIds.size} Players)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canStartTournament) BrandPrimary else TextMuted
                    )
                }
            }
        }
    }

    // Popup Window to Select / Add Players (Name Only, Multi-select, OK button)
    if (showPlayerSelectPopup) {
        var localSelection by remember(selectedPlayerIds) {
            mutableStateOf(selectedPlayerIds.toSet())
        }
        var newPlayerInput by remember { mutableStateOf("") }
        var isAddingInline by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPlayerSelectPopup = false },
            containerColor = AppSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = AppIcons.People, contentDescription = null, tint = BrandPrimary)
                        Text("Select Players", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    IconButton(
                        onClick = { isAddingInline = !isAddingInline },
                        modifier = Modifier.size(32.dp).testTag("popup_toggle_add_new_player")
                    ) {
                        Icon(
                            imageVector = if (isAddingInline) Icons.Default.Close else AppIcons.PersonAdd,
                            contentDescription = "Create Player",
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAddingInline) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newPlayerInput,
                                onValueChange = { newPlayerInput = it },
                                placeholder = { Text("Player name...", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandPrimary,
                                    unfocusedBorderColor = AppBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("popup_new_player_input")
                            )
                            Button(
                                onClick = {
                                    if (newPlayerInput.isNotBlank()) {
                                        viewModel.addPlayer(newPlayerInput.trim())
                                        newPlayerInput = ""
                                        isAddingInline = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("popup_new_player_save")
                            ) {
                                Text("Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Select players for this match (${localSelection.size} selected):",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (players.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppSurfaceElevated
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No players created yet", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("Tap the '+' button above to add a player name.", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(players, key = { it.id }) { player ->
                                val isChecked = localSelection.contains(player.id)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            localSelection = if (isChecked) {
                                                localSelection - player.id
                                            } else {
                                                localSelection + player.id
                                            }
                                        }
                                        .testTag("popup_player_select_item_${player.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isChecked) BrandPrimaryContainer.copy(alpha = 0.5f) else AppSurfaceElevated,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isChecked) BrandPrimary else AppBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = player.name,
                                            fontSize = 15.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                            color = TextPrimary
                                        )

                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                localSelection = if (checked) {
                                                    localSelection + player.id
                                                } else {
                                                    localSelection - player.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = BrandPrimary,
                                                checkmarkColor = BrandOnPrimary,
                                                uncheckedColor = AppBorder
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSelectedPlayers(localSelection.toList())
                        showPlayerSelectPopup = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("popup_confirm_selection_btn")
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlayerSelectPopup = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Tournament Configuration Dialog
    if (showTournamentDialog) {
        var selectedTourneyType by remember { mutableStateOf(TournamentType.ROUND_ROBIN) }

        AlertDialog(
            onDismissRequest = { showTournamentDialog = false },
            containerColor = AppSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = AppIcons.EmojiEvents, contentDescription = null, tint = BrandSecondary)
                    Text("Tournament Format", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Choose the tournament system for ${selectedPlayerIds.size} participating players:",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    // Option 1: Round Robin (Everyone vs Everyone)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTourneyType = TournamentType.ROUND_ROBIN }
                            .testTag("tourney_option_round_robin"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedTourneyType == TournamentType.ROUND_ROBIN) BrandSecondaryContainer.copy(alpha = 0.4f) else AppSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedTourneyType == TournamentType.ROUND_ROBIN) BrandSecondary else AppBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selectedTourneyType == TournamentType.ROUND_ROBIN,
                                onClick = { selectedTourneyType = TournamentType.ROUND_ROBIN },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandSecondary)
                            )
                            Column {
                                Text(
                                    text = "Everyone vs Everyone",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Round-robin league table where each player battles every other player",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Option 2: Straight Elimination (Knockout)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTourneyType = TournamentType.STRAIGHT_ELIMINATION }
                            .testTag("tourney_option_elimination"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selectedTourneyType == TournamentType.STRAIGHT_ELIMINATION) BrandSecondaryContainer.copy(alpha = 0.4f) else AppSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (selectedTourneyType == TournamentType.STRAIGHT_ELIMINATION) BrandSecondary else AppBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = selectedTourneyType == TournamentType.STRAIGHT_ELIMINATION,
                                onClick = { selectedTourneyType = TournamentType.STRAIGHT_ELIMINATION },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandSecondary)
                            )
                            Column {
                                Text(
                                    text = "Straight Elimination",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Single knockout bracket where winners advance to the championship finals",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AppSurfaceElevated
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Rules: ${selectedGameType.displayName} • $setsToWin ${if (setsToWin == 1) "Set" else "Sets"} • $legsToWinSet ${if (legsToWinSet == 1) "Leg" else "Legs"} • ${if (doubleIn) "Double In" else "Straight In"} • ${if (doubleOut) "Double Out" else "Straight Out"}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rules = GameRules(
                            gameType = selectedGameType,
                            doubleIn = doubleIn,
                            doubleOut = doubleOut,
                            setsToWin = setsToWin,
                            legsToWinSet = legsToWinSet
                        )
                        viewModel.startTournament(
                            type = selectedTourneyType,
                            rules = rules,
                            playerIds = selectedPlayerIds
                        )
                        showTournamentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_start_tournament_button")
                ) {
                    Text("Launch Tournament", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTournamentDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
