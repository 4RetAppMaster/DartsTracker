package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.*
import com.example.util.UpdateCheckResult
import com.example.viewmodel.DartsViewModel
import com.example.viewmodel.UpdateUiState
import kotlinx.coroutines.launch

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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val currentTab by remember {
        derivedStateOf {
            if (pagerState.targetPage == MainTab.PLAYERS.ordinal) MainTab.PLAYERS else MainTab.PLAY
        }
    }
    var showExitMatchDialog by remember { mutableStateOf(false) }

    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val activeTournament by viewModel.tournamentState.collectAsStateWithLifecycle()
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()

    LaunchedEffect(activeMatch) {
        if (activeMatch != null && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel, context) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppOpened()
                Lifecycle.Event.ON_STOP -> {
                    val isConfigChange = (context as? Activity)?.isChangingConfigurations == true
                    if (!isConfigChange) {
                        viewModel.onAppBackgrounded()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Back handling: dismiss dialogs first if open
    BackHandler(enabled = showUpdateDialog) {
        viewModel.dismissUpdateDialog()
    }
    BackHandler(enabled = showExitMatchDialog && !showUpdateDialog) {
        showExitMatchDialog = false
    }

    // If in active match, prompt exit match
    BackHandler(enabled = activeMatch != null && !showExitMatchDialog && !showUpdateDialog) {
        showExitMatchDialog = true
    }

    // If on Players tab, swipe/animate back to Play tab
    BackHandler(enabled = pagerState.currentPage != MainTab.PLAY.ordinal && activeMatch == null && !showUpdateDialog) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(MainTab.PLAY.ordinal)
        }
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
                            // Blue rounded-square logo icon; tap to check version & GitHub updates
                            Surface(
                                onClick = { viewModel.checkForUpdates(userInitiated = true) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("darts_logo_button"),
                                shape = RoundedCornerShape(12.dp),
                                color = BrandPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_dart_logo),
                                        contentDescription = "DartsTracker Logo - Tap to check version",
                                        tint = BrandOnPrimary,
                                        modifier = Modifier.size(28.dp)
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
                Surface(
                    color = AppSurfaceElevated,
                    border = BorderStroke(1.dp, AppBorder),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_navigation_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactBottomNavItem(
                            selected = (currentTab == MainTab.PLAY),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(MainTab.PLAY.ordinal)
                                }
                            },
                            icon = if (activeTournament != null) AppIcons.EmojiEvents else AppIcons.Adjust,
                            label = if (activeTournament != null) "Tournament" else "Darts Match",
                            contentDescription = "Play",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("nav_tab_play")
                        )

                        CompactBottomNavItem(
                            selected = (currentTab == MainTab.PLAYERS),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(MainTab.PLAYERS.ordinal)
                                }
                            },
                            icon = AppIcons.People,
                            label = "Players",
                            contentDescription = "Players",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("nav_tab_players")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = (activeMatch == null),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_tab_pager")
        ) { page ->
            when (page) {
                MainTab.PLAY.ordinal -> {
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
                else -> {
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

    if (showUpdateDialog) {
        AppVersionAndUpdateDialog(
            updateUiState = updateUiState,
            onRetry = { viewModel.checkForUpdates(userInitiated = true) },
            onDismiss = { viewModel.dismissUpdateDialog() }
        )
    }
}

@Composable
private fun CompactBottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) BrandOnPrimaryContainer else TextSecondary
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                color = if (selected) BrandPrimaryContainer else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AppVersionAndUpdateDialog(
    updateUiState: UpdateUiState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentVerLabel = "Ver ${BuildConfig.VERSION_NAME}"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = BrandPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_dart_logo),
                            contentDescription = null,
                            tint = BrandOnPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "DartsTracker",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandPrimaryContainer,
                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = currentVerLabel,
                            color = BrandOnPrimaryContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .testTag("update_dialog_version_label")
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (updateUiState) {
                    is UpdateUiState.Idle,
                    is UpdateUiState.Checking -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = AppSurfaceElevated,
                            border = BorderStroke(1.dp, AppBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = BrandPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Checking GitHub for new version...",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    is UpdateUiState.Result -> {
                        when (val result = updateUiState.checkResult) {
                            is UpdateCheckResult.UpdateAvailable -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = BrandSuccessContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, BrandSuccess)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = AppIcons.SystemUpdate,
                                                contentDescription = null,
                                                tint = BrandSuccess,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "New Version Available: Ver ${result.latestVersion}",
                                                color = BrandSuccess,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = "Installed: $currentVerLabel  •  Latest: Ver ${result.latestVersion}",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "An update is available. Would you like to update now or later?",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        result.releaseNotes?.let { notes ->
                                            Text(
                                                text = notes.take(300),
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            is UpdateCheckResult.UpToDate -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = AppSurfaceElevated,
                                    border = BorderStroke(1.dp, AppBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = BrandSuccess,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "You are up to date ($currentVerLabel)",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = if (result.latestVersion != null) {
                                                    "Latest release: Ver ${result.latestVersion}"
                                                } else {
                                                    "You have the latest available version."
                                                },
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            is UpdateCheckResult.Error -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = BrandErrorContainer.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, BrandError)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.CloudOff,
                                            contentDescription = null,
                                            tint = BrandError,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Update check failed ($currentVerLabel)",
                                                color = BrandError,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = result.message,
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val updateResult = (updateUiState as? UpdateUiState.Result)?.checkResult
            when (updateResult) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateResult.releaseUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("update_download_button")
                    ) {
                        Text("Update", fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateCheckResult.Error -> {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("update_retry_button")
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }

                else -> {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("update_ok_button")
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        dismissButton = {
            val updateResult = (updateUiState as? UpdateUiState.Result)?.checkResult
            when (updateResult) {
                is UpdateCheckResult.UpdateAvailable -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text("Later", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }
                is UpdateCheckResult.Error -> {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = TextSecondary)
                    }
                }
                else -> Unit
            }
        }
    )
}
