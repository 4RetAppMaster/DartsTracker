package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DartsEngine
import com.example.model.TournamentMatch
import com.example.model.TournamentType
import com.example.ui.components.PlayerAvatar
import com.example.ui.theme.*
import com.example.viewmodel.DartsViewModel

@Composable
fun TournamentScreen(
    viewModel: DartsViewModel,
    modifier: Modifier = Modifier
) {
    val tournament by viewModel.tournamentState.collectAsState()
    val t = tournament ?: return

    var showExitDialog by remember { mutableStateOf(false) }
    var selectedRoundTab by remember { mutableIntStateOf(1) }

    val allRounds = remember(t.matches) {
        t.matches.map { it.round }.distinct().sorted()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // Tournament Header Card with Exit Button
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = BrandSecondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = BrandSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = t.tournamentType.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${t.rules.gameType.displayName} • ${if (t.rules.doubleIn) "Double In" else "Straight In"} • ${if (t.rules.doubleOut) "Double Out" else "Straight Out"}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { showExitDialog = true },
                            modifier = Modifier.testTag("exit_tournament_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit Tournament",
                                tint = TextSecondary
                            )
                        }
                    }
                }
            }

            // Champion Banner if Finished
            if (t.isFinished && t.champion != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = BrandSecondaryContainer.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, BrandSecondary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BrandSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "TOURNAMENT CHAMPION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandSecondary,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = t.champion!!.name,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Button(
                                onClick = { viewModel.endTournament() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("finish_tournament_btn")
                            ) {
                                Text("Finish Tournament", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 1. ROUND ROBIN VIEW
            if (t.tournamentType == TournamentType.ROUND_ROBIN) {
                // Standings Table Header
                item {
                    Text(
                        text = "LEAGUE STANDINGS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    val standings = remember(t.matches, t.originalPlayers) {
                        DartsEngine.calculateRoundRobinStandings(t.originalPlayers, t.matches)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = AppSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(24.dp))
                                Text("PLAYER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.weight(1f))
                                Text("P", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                Text("W", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                Text("L", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                Text("PTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            }

                            HorizontalDivider(color = AppBorder, thickness = 1.dp)

                            standings.forEachIndexed { rank, row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${rank + 1}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rank == 0) BrandSecondary else TextSecondary,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = row.player.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("${row.played}", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text("${row.won}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandSuccess, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text("${row.lost}", fontSize = 12.sp, color = BrandError, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text("${row.points}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPrimary, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // Fixtures List
                item {
                    Text(
                        text = "MATCH FIXTURES (${t.matches.count { it.isCompleted }}/${t.matches.size} Completed)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(t.matches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onPlayMatch = { viewModel.launchTournamentMatch(match.id) }
                    )
                }
            }

            // 2. STRAIGHT ELIMINATION (KNOCKOUT BRACKET)
            if (t.tournamentType == TournamentType.STRAIGHT_ELIMINATION) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KNOCKOUT BRACKET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )

                        // Mix / Shuffle Players button for Straight Elimination
                        FilledTonalButton(
                            onClick = { viewModel.shuffleEliminationPlayers() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = BrandSecondaryContainer,
                                contentColor = BrandSecondary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("mix_players_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mix Players",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Round Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allRounds.forEach { roundNum ->
                            val roundName = t.matches.firstOrNull { it.round == roundNum }?.roundName ?: "Round $roundNum"
                            val isSelected = (selectedRoundTab == roundNum)
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedRoundTab = roundNum }
                                    .testTag("bracket_round_tab_$roundNum"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) BrandPrimaryContainer else AppSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) BrandPrimary else AppBorder
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = roundName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandOnPrimaryContainer else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                val roundMatches = t.matches.filter { it.round == selectedRoundTab }
                items(roundMatches, key = { it.id }) { match ->
                    MatchCard(
                        match = match,
                        onPlayMatch = { viewModel.launchTournamentMatch(match.id) }
                    )
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = AppSurface,
            title = {
                Text("Exit Tournament?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Are you sure you want to end the tournament? All tournament progress will be cleared.", color = TextSecondary, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        viewModel.endTournament()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_exit_tournament_btn")
                ) {
                    Text("Exit Tournament", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Stay in Tournament", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MatchCard(
    match: TournamentMatch,
    onPlayMatch: () -> Unit
) {
    val canPlay = !match.isCompleted && !match.isBye && match.player1Id != 0L && match.player2Id != null && match.player2Id != 0L

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (match.isCompleted) AppSurfaceElevated else AppSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (canPlay) BrandPrimary.copy(alpha = 0.8f) else AppBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = match.roundName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                if (match.isCompleted) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandSuccessContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = if (match.isBye) "BYE" else "COMPLETED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Player 1 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.player1Name,
                    fontSize = 14.sp,
                    fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Medium,
                    color = if (match.winnerId == match.player1Id) BrandSecondary else TextPrimary
                )
                if (match.winnerId == match.player1Id) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Winner", tint = BrandSecondary, modifier = Modifier.size(16.dp))
                }
            }

            Text(
                text = "vs",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                modifier = Modifier.padding(start = 2.dp)
            )

            // Player 2 Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = match.player2Name ?: "TBD",
                    fontSize = 14.sp,
                    fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Medium,
                    color = if (match.winnerId == match.player2Id) BrandSecondary else if (match.player2Name == null) TextMuted else TextPrimary
                )
                if (match.winnerId == match.player2Id && match.player2Id != null) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Winner", tint = BrandSecondary, modifier = Modifier.size(16.dp))
                }
            }

            if (canPlay) {
                Button(
                    onClick = onPlayMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .padding(top = 2.dp)
                        .testTag("play_tournament_match_${match.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play Match", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
