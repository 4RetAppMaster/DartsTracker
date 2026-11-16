package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.DartsViewModel

enum class MainTab {
    PLAY,
    PLAYERS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DartsViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(MainTab.PLAY) }
    var showExitMatchDialog by remember { mutableStateOf(false) }

    val activeMatch by viewModel.activeMatch.collectAsState()
    val activeTournament by viewModel.tournamentState.collectAsState()

    // Back handling: dismiss dialog first if open
    BackHandler(enabled = showExitMatchDialog) {
        showExitMatchDialog = false
    }

    // If in active match, prompt exit match
    BackHandler(enabled = activeMatch != null && !showExitMatchDialog) {
        showExitMatchDialog = true
    }

    // If on Players tab, go back to Play tab
    BackHandler(enabled = currentTab != MainTab.PLAY && activeMatch == null) {
        currentTab = MainTab.PLAY
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppBg,
        topBar = {
            val pageSubtitle = when (currentTab) {
                MainTab.PLAY -> when {
                    activeMatch != null -> "Live Match"
                    activeTournament != null -> "Tournament"
                    else -> "Game Setup"
                }
                MainTab.PLAYERS -> "Players"
            }

            Surface(
                color = AppSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Square shape with rounded corners containing a simple white dart icon
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = BrandPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_dart_simple),
                                        contentDescription = "Dart Icon",
                                        tint = BrandOnPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "DartsTracker.",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary,
                                    letterSpacing = (-0.5).sp,
                                    lineHeight = 24.sp
                                )
                                Text(
                                    text = pageSubtitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    },
                    actions = {
                        if (activeMatch != null) {
                            IconButton(
                                onClick = { showExitMatchDialog = true },
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .size(40.dp)
                                    .testTag("topbar_cancel_match_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Match",
                                    tint = BrandError,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = AppSurface,
                        titleContentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("app_top_bar")
                )
            }
        },
        bottomBar = {
            // Hide bottom bar only during live scoring match for maximum focus
            if (activeMatch == null) {
                NavigationBar(
                    containerColor = AppSurfaceElevated,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("main_navigation_bar")
                ) {
                    NavigationBarItem(
                        selected = (currentTab == MainTab.PLAY),
                        onClick = { currentTab = MainTab.PLAY },
                        icon = {
                            Icon(
                                imageVector = if (activeTournament != null) Icons.Default.EmojiEvents else Icons.Default.Adjust,
                                contentDescription = "Play"
                            )
                        },
                        label = {
                            Text(
                                text = if (activeTournament != null) "Tournament" else "Darts Match",
                                fontWeight = if (currentTab == MainTab.PLAY) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOnPrimaryContainer,
                            selectedTextColor = BrandOnPrimaryContainer,
                            indicatorColor = BrandPrimaryContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_play")
                    )

                    NavigationBarItem(
                        selected = (currentTab == MainTab.PLAYERS),
                        onClick = { currentTab = MainTab.PLAYERS },
                        icon = {
                            Icon(imageVector = Icons.Default.People, contentDescription = "Players")
                        },
                        label = {
                            Text(
                                text = "Players",
                                fontWeight = if (currentTab == MainTab.PLAYERS) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandOnPrimaryContainer,
                            selectedTextColor = BrandOnPrimaryContainer,
                            indicatorColor = BrandPrimaryContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_players")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.PLAY -> {
                    when {
                        activeMatch != null -> {
                            LiveScoringScreen(viewModel = viewModel)
                        }
                        activeTournament != null -> {
                            TournamentScreen(viewModel = viewModel)
                        }
                        else -> {
                            SetupScreen(viewModel = viewModel)
                        }
                    }
                }
                MainTab.PLAYERS -> {
                    PlayersScreen(viewModel = viewModel)
                }
            }
        }
    }

    if (showExitMatchDialog) {
        AlertDialog(
            onDismissRequest = { showExitMatchDialog = false },
            containerColor = AppSurface,
            title = {
                Text("Exit Current Match?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to cancel and exit? Match progress will be lost.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitMatchDialog = false
                        viewModel.exitMatch()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = androidx.compose.ui.graphics.Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_exit_match_button")
                ) {
                    Text("Exit Match", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitMatchDialog = false }) {
                    Text("Keep Playing", color = TextSecondary)
                }
            }
        )
    }
}
