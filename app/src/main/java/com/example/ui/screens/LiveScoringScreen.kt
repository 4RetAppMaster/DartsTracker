package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.CheckoutCalculator
import com.example.model.*
import com.example.ui.components.ConfettiPopOverlay
import com.example.ui.theme.*
import com.example.viewmodel.ActiveMatchState
import com.example.viewmodel.DartsViewModel
import java.util.Locale

private val DART_NUMBER_ROWS = listOf(
    listOf(1, 2, 3, 4, 5),
    listOf(6, 7, 8, 9, 10),
    listOf(11, 12, 13, 14, 15),
    listOf(16, 17, 18, 19, 20),
    listOf(0, 25, 50)
)
private val DartKeyShape = RoundedCornerShape(8.dp)
private val KeyBorderDefault = BorderStroke(1.dp, AppBorder)
private val SlotShape = RoundedCornerShape(4.dp)

@Composable
fun LiveScoringScreen(
    viewModel: DartsViewModel,
    modifier: Modifier = Modifier
) {
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val match = activeMatch ?: return

    var selectedMultiplier by remember(match.tournamentMatchId, match.currentSet, match.currentLeg) {
        mutableStateOf(Multiplier.SINGLE)
    }

    // Hardware Back reverts a false win when any winning popup is displayed
    BackHandler(enabled = match.pendingSubWin != null || match.isMatchOver) {
        viewModel.revertFalseWin()
    }

    val playersListState = rememberLazyListState()

    // When currentPlayer changes and there are many players, scroll to keep active player in view without reordering
    LaunchedEffect(match.currentPlayerIndex) {
        if (match.players.size > 2) {
            playersListState.animateScrollToItem(match.currentPlayerIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        // Top Match Info Bar: Sets & Legs info (no undo button)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Match format badges
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BrandPrimaryContainer
                ) {
                    Text(
                        text = "Set ${match.currentSet}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandOnPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AppSurfaceElevated
                ) {
                    Text(
                        text = "Leg ${match.currentLeg}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "•  ${match.rules.gameType.displayName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }
        }

        // Scrollable Players Section (Original Order, scrolls when there are many players)
        LazyColumn(
            state = playersListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Bust Feedback Alert Banner
            if (match.isBustFeedback) {
                item(key = "bust_feedback") {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = BrandErrorContainer,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandError.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Bust",
                                    tint = BrandError,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "BUST! Turn score reset.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandOnErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Display players in original order, active player highlighted
            itemsIndexed(match.players, key = { _, p -> p.player.id }) { idx, pState ->
                val isActive = (idx == match.currentPlayerIndex)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isActive) BrandPrimaryContainer.copy(alpha = 0.35f) else AppSurface,
                    border = androidx.compose.foundation.BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) BrandPrimary else AppBorder
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // LEFT COLUMN: Remaining Points (top) | Player Name (bottom)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = "${pState.currentScore}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isActive) BrandPrimary else TextPrimary
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = pState.player.name,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) BrandPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (match.rules.doubleIn && !pState.hasDoubleInAchieved) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = BrandSecondaryContainer
                                    ) {
                                        Text(
                                            text = "Needs DI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // CENTER COLUMN: D1 D2 D3 (top) | Round Points (bottom)
                        Column(
                            modifier = Modifier.weight(1.1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // D1 D2 D3 Slots (Fixed width so card never resizes or shifts)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                for (slotIndex in 0..2) {
                                    val dart = if (isActive) {
                                        match.currentDarts.getOrNull(slotIndex)
                                    } else {
                                        pState.turns.lastOrNull()?.darts?.getOrNull(slotIndex)
                                    }
                                    val isCurrentSlot = isActive && (slotIndex == match.currentDarts.size)

                                    Surface(
                                        modifier = Modifier
                                            .width(34.dp)
                                            .height(20.dp),
                                        shape = SlotShape,
                                        color = when {
                                            dart != null -> BrandSuccessContainer.copy(alpha = 0.5f)
                                            isCurrentSlot -> BrandPrimary.copy(alpha = 0.15f)
                                            else -> AppSurfaceElevated
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            when {
                                                dart != null -> BrandSuccess.copy(alpha = 0.6f)
                                                isCurrentSlot -> BrandPrimary
                                                else -> AppBorder
                                            }
                                        )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = dart?.label ?: "D${slotIndex + 1}",
                                                fontSize = 10.sp,
                                                fontWeight = if (dart != null || isCurrentSlot) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    dart != null -> BrandSuccess
                                                    isCurrentSlot -> BrandPrimary
                                                    else -> TextMuted
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Total points in that round (without "Last:" prefix)
                            val roundPoints = if (isActive) {
                                if (match.currentDarts.isNotEmpty()) {
                                    match.currentDarts.sumOf { it.points }
                                } else {
                                    pState.turns.lastOrNull()?.totalPoints ?: 0
                                }
                            } else {
                                pState.turns.lastOrNull()?.totalPoints ?: 0
                            }

                            Surface(
                                shape = SlotShape,
                                color = if (isActive && match.currentDarts.isNotEmpty()) BrandPrimaryContainer else AppSurfaceElevated
                            ) {
                                Text(
                                    text = "$roundPoints pts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive && match.currentDarts.isNotEmpty()) BrandOnPrimaryContainer else TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // RIGHT COLUMN: 3-Dart Average + Set / Leg Wins
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Avg ${String.format(Locale.US, "%.1f", pState.threeDartAverage)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) BrandPrimary else TextSecondary,
                                maxLines = 1
                            )
                            Text(
                                text = "S ${pState.setsWon} • L ${pState.legsWon}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pState.setsWon > 0 || pState.legsWon > 0) BrandSecondary else TextMuted,
                                maxLines = 1,
                                modifier = Modifier.testTag("player_sets_legs_${pState.player.id}")
                            )
                        }
                    }
                }
            }
        }

        // Fixed Dart Pad at the bottom (bigger D1-D3 boxes, Double/Triple/Backspace on right)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
        ) {
            DartByDartInterface(
                match = match,
                selectedMultiplier = selectedMultiplier,
                onSelectMultiplier = { selectedMultiplier = it },
                viewModel = viewModel
            )
        }
    }

    // 1. Sub-Win Popup (Won a Leg or Set, not the whole game yet) -> Green Popup + Confetti + Back + OK
    match.pendingSubWin?.let { subWin ->
        WinPopupWithConfetti(
            isGreenIntermediateWin = true,
            titleText = subWin.title,
            headlineText = "${subWin.winnerName} wins this ${if (subWin.isSetWin) "set" else "leg"}!",
            confirmLabel = "OK",
            onBackRevert = { viewModel.revertFalseWin() },
            onConfirm = { viewModel.confirmSubWinAndAdvance() },
            burstKey = "${subWin.title}_${subWin.winnerPlayerId}_${match.turnHistory.size}",
            statsContent = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppSurfaceElevated,
                    border = BorderStroke(1.dp, BrandSuccess.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Sets Won", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${subWin.setsWon} / ${match.rules.setsToWin}",
                                color = BrandSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Legs Won in Set", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${subWin.legsWon} / ${match.rules.legsToWinSet}",
                                color = BrandSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Up Next", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "Set ${subWin.nextSet} • Leg ${subWin.nextLeg}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        )
    }

    // 2. Match Winner Popup (Tournament intermediate match -> Green Popup -> Continue returns to match selector; Whole Game -> Gold Popup -> Continue)
    if (match.isMatchOver && match.winner != null) {
        val winner = match.winner!!
        val isIntermediateTournamentMatch = remember(match.tournamentMatchId, winner.player.id) {
            viewModel.hasNextTournamentMatch(match)
        }

        WinPopupWithConfetti(
            isGreenIntermediateWin = isIntermediateTournamentMatch,
            titleText = "MATCH WON!",
            headlineText = "${winner.player.name} is the Winner!",
            confirmLabel = "Continue",
            onBackRevert = { viewModel.revertFalseWin() },
            onConfirm = { viewModel.confirmMatchWinAndExit() },
            burstKey = "MATCH_${winner.player.id}_${match.turnHistory.size}",
            statsContent = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isIntermediateTournamentMatch) BrandSuccess.copy(alpha = 0.45f) else AppBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Final Sets Won", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${winner.setsWon} / ${match.rules.setsToWin}",
                                color = if (isIntermediateTournamentMatch) BrandSuccess else BrandPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3-Dart Average", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = String.format(Locale.US, "%.1f", winner.threeDartAverage),
                                color = if (isIntermediateTournamentMatch) BrandSuccess else BrandPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Darts Thrown", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${winner.totalDartsThrown}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Highest Turn Score", color = TextSecondary, fontSize = 13.sp)
                            Text(
                                text = "${winner.highestTurnScore}",
                                color = BrandSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun WinPopupWithConfetti(
    isGreenIntermediateWin: Boolean,
    titleText: String,
    headlineText: String,
    confirmLabel: String,
    onBackRevert: () -> Unit,
    onConfirm: () -> Unit,
    burstKey: Any,
    statsContent: @Composable ColumnScope.() -> Unit
) {
    val accentColor = if (isGreenIntermediateWin) BrandSuccess else BrandSecondary
    val accentContainer = if (isGreenIntermediateWin) BrandSuccessContainer else BrandSecondaryContainer
    val cardBackground = if (isGreenIntermediateWin) Color(0xFF102A1C) else AppSurface

    Dialog(
        onDismissRequest = { /* Force Back or Confirm action */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            // Confetti popping BEHIND the popup window
            ConfettiPopOverlay(
                modifier = Modifier.fillMaxSize(),
                burstKey = burstKey
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 380.dp)
                    .testTag("win_popup_card"),
                shape = RoundedCornerShape(24.dp),
                color = cardBackground,
                border = BorderStroke(2.dp, accentColor),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = accentContainer,
                        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGreenIntermediateWin) Icons.Default.CheckCircle else AppIcons.EmojiEvents,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = titleText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = headlineText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    statsContent()

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onBackRevert,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("winner_dialog_back_button"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AppBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppSurfaceElevated,
                                contentColor = TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = AppIcons.Undo,
                                contentDescription = "Revert",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Revert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1.35f)
                                .height(48.dp)
                                .testTag("winner_dialog_confirm_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGreenIntermediateWin) BrandSuccess else BrandPrimary,
                                contentColor = if (isGreenIntermediateWin) Color(0xFF042211) else BrandOnPrimary
                            )
                        ) {
                            Text(
                                text = confirmLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DartByDartInterface(
    match: ActiveMatchState,
    selectedMultiplier: Multiplier,
    onSelectMultiplier: (Multiplier) -> Unit,
    viewModel: DartsViewModel
) {
    val currentPlayer = match.currentPlayer
    val currentDarts = match.currentDarts

    // Calculate remaining points dynamically based on throws in the current turn
    val turnPoints = currentDarts.sumOf { it.points }
    val remainingScore = (currentPlayer?.currentScore ?: 0) - turnPoints

    val checkoutSuggestion = remember(remainingScore, match.rules.doubleOut) {
        if (remainingScore > 1) {
            CheckoutCalculator.getSuggestedCheckout(remainingScore, match.rules.doubleOut)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Suggested Darts / Checkout Bar (Above numbers keypad - only middle-aligned targets, bigger size)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            color = if (checkoutSuggestion != null) BrandSuccessContainer.copy(alpha = 0.25f) else AppSurface,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (checkoutSuggestion != null) BrandSuccess.copy(alpha = 0.5f) else AppBorder
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (checkoutSuggestion != null) {
                    // Suggested target pills only, middle aligned and prominent
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val darts = checkoutSuggestion.split(" • ")
                        darts.forEach { dartText ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BrandSuccess,
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = dartText.trim(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "${currentPlayer?.player?.name ?: "Player"}'s Turn  •  $remainingScore remaining",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }

        // Keypad Section (Left: 5 columns of numbers; Right: Double, Triple, Erase)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Numbers Grid (Left Column taking ~77% of width)
            Column(
                modifier = Modifier.weight(4.2f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                DART_NUMBER_ROWS.forEachIndexed { rowIndex, rowNums ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (rowIndex == 4) {
                            // Row with 0, 25, 50
                            Surface(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp)
                                    .clip(DartKeyShape)
                                    .clickable {
                                        viewModel.addDartThrow(DartThrow(0, Multiplier.MISS))
                                        onSelectMultiplier(Multiplier.SINGLE)
                                    }
                                    .testTag("dart_num_0"),
                                shape = DartKeyShape,
                                color = AppSurface,
                                border = KeyBorderDefault
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "0",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1.75f)
                                    .height(44.dp)
                                    .clip(DartKeyShape)
                                    .clickable {
                                        val isDouble = selectedMultiplier == Multiplier.DOUBLE
                                        viewModel.addDartThrow(DartThrow(25, if (isDouble) Multiplier.DOUBLE else Multiplier.SINGLE))
                                        onSelectMultiplier(Multiplier.SINGLE)
                                    }
                                    .testTag("dart_num_25"),
                                shape = DartKeyShape,
                                color = AppSurface,
                                border = KeyBorderDefault
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "25",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1.75f)
                                    .height(44.dp)
                                    .clip(DartKeyShape)
                                    .clickable {
                                        viewModel.addDartThrow(DartThrow(25, Multiplier.DOUBLE))
                                        onSelectMultiplier(Multiplier.SINGLE)
                                    }
                                    .testTag("dart_num_50"),
                                shape = DartKeyShape,
                                color = AppSurface,
                                border = KeyBorderDefault
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "50",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        } else {
                            // Rows 1-4 (1 to 20)
                            rowNums.forEach { num ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(DartKeyShape)
                                        .clickable {
                                            viewModel.addDartThrow(DartThrow(num, selectedMultiplier))
                                            onSelectMultiplier(Multiplier.SINGLE)
                                        }
                                        .testTag("dart_num_$num"),
                                    shape = DartKeyShape,
                                    color = AppSurface,
                                    border = KeyBorderDefault
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$num",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Right Action Column: "Double", "Triple", Delete / Erase Button
            Column(
                modifier = Modifier
                    .weight(1.35f)
                    .height(240.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                val isDouble = (selectedMultiplier == Multiplier.DOUBLE)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(DartKeyShape)
                        .clickable {
                            onSelectMultiplier(if (isDouble) Multiplier.SINGLE else Multiplier.DOUBLE)
                        }
                        .testTag("multiplier_DOUBLE"),
                    shape = DartKeyShape,
                    color = if (isDouble) BrandSecondary else AppSurface,
                    border = BorderStroke(
                        if (isDouble) 2.dp else 1.dp,
                        if (isDouble) BrandSecondary else AppBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Double",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDouble) Color.White else TextPrimary
                        )
                    }
                }

                val isTriple = (selectedMultiplier == Multiplier.TRIPLE)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(DartKeyShape)
                        .clickable {
                            onSelectMultiplier(if (isTriple) Multiplier.SINGLE else Multiplier.TRIPLE)
                        }
                        .testTag("multiplier_TRIPLE"),
                    shape = DartKeyShape,
                    color = if (isTriple) BrandPrimary else AppSurface,
                    border = BorderStroke(
                        if (isTriple) 2.dp else 1.dp,
                        if (isTriple) BrandPrimary else AppBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Triple",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isTriple) Color.White else TextPrimary
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(DartKeyShape)
                        .clickable { viewModel.removeLastDartThrow() }
                        .testTag("dart_erase_button"),
                    shape = DartKeyShape,
                    color = BrandError,
                    border = BorderStroke(1.dp, BrandError)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.Backspace,
                            contentDescription = "Erase Dart",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
