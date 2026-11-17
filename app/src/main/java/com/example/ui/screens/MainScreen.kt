package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.core.content.FileProvider
import java.io.File
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
            onStartUpdate = { version, url -> viewModel.startAutomaticUpdate(version, url) },
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
    onStartUpdate: (latestVersion: String, releaseUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentVerLabel = "Ver ${BuildConfig.VERSION_NAME}"

    var pendingInstallApkPath by remember { mutableStateOf<String?>(null) }
    var launchedInstallForPath by remember { mutableStateOf<String?>(null) }
    var needsInstallPermission by remember { mutableStateOf(false) }
    var installLaunchError by remember { mutableStateOf<String?>(null) }

    fun launchApkInstaller(ctx: Context, apkPath: String) {
        try {
            val apkFile = File(apkPath)
            val apkUri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(installIntent)
            installLaunchError = null
        } catch (e: Exception) {
            installLaunchError = e.localizedMessage ?: "Unable to launch package installer"
        }
    }

    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val canInstall = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
        if (canInstall) {
            needsInstallPermission = false
            pendingInstallApkPath?.let { path ->
                launchApkInstaller(context, path)
            }
        } else {
            needsInstallPermission = true
        }
    }

    fun triggerInstall(apkPath: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstallApkPath = apkPath
            needsInstallPermission = true
            try {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
                unknownSourcesLauncher.launch(settingsIntent)
            } catch (e: Exception) {
                installLaunchError = e.localizedMessage ?: "Unable to open install permission settings"
            }
        } else {
            needsInstallPermission = false
            launchApkInstaller(context, apkPath)
        }
    }

    LaunchedEffect(updateUiState) {
        if (updateUiState is UpdateUiState.ReadyToInstall &&
            launchedInstallForPath != updateUiState.apkFilePath
        ) {
            launchedInstallForPath = updateUiState.apkFilePath
            triggerInstall(updateUiState.apkFilePath)
        } else if (updateUiState !is UpdateUiState.ReadyToInstall) {
            launchedInstallForPath = null
            needsInstallPermission = false
            installLaunchError = null
        }
    }

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

                    is UpdateUiState.Downloading -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_downloading_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = AppSurfaceElevated,
                            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Downloading Ver ${updateUiState.latestVersion}...",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    updateUiState.progressPercent?.let { pct ->
                                        Text(
                                            text = "$pct%",
                                            color = BrandPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                val progressPct = updateUiState.progressPercent
                                if (progressPct != null) {
                                    LinearProgressIndicator(
                                        progress = { progressPct / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BrandPrimary,
                                        trackColor = AppBorder
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = BrandPrimary,
                                        trackColor = AppBorder
                                    )
                                }
                                Text(
                                    text = "The installer will open automatically once downloaded.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    is UpdateUiState.ReadyToInstall -> {
                        val hasError = installLaunchError != null
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_ready_card"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasError) {
                                BrandErrorContainer.copy(alpha = 0.45f)
                            } else {
                                BrandSuccessContainer.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(1.dp, if (hasError) BrandError else BrandSuccess)
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
                                        tint = if (hasError) BrandError else BrandSuccess,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Update Ready (Ver ${updateUiState.latestVersion})",
                                        color = if (hasError) BrandError else BrandSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Text(
                                    text = when {
                                        installLaunchError != null -> installLaunchError!!
                                        needsInstallPermission ->
                                            "Allow installing unknown apps in Settings to finish updating automatically."
                                        else -> "Opening Android installer..."
                                    },
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    is UpdateUiState.DownloadFailed -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("update_download_failed_card"),
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
                                        text = "Update download failed (Ver ${updateUiState.latestVersion})",
                                        color = BrandError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = updateUiState.message,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
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
                                            text = "An update is available. Would you like to download and install it now or later?",
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
            when (updateUiState) {
                is UpdateUiState.Downloading -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_cancel_download_button")
                    ) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }

                is UpdateUiState.ReadyToInstall -> {
                    Button(
                        onClick = { triggerInstall(updateUiState.apkFilePath) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("update_install_now_button")
                    ) {
                        Text(
                            text = if (needsInstallPermission) "Allow & Install" else "Install Now",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is UpdateUiState.DownloadFailed -> {
                    Button(
                        onClick = {
                            onStartUpdate(updateUiState.latestVersion, updateUiState.releaseUrl)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("update_retry_download_button")
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }

                is UpdateUiState.Result -> {
                    when (val updateResult = updateUiState.checkResult) {
                        is UpdateCheckResult.UpdateAvailable -> {
                            Button(
                                onClick = {
                                    onStartUpdate(updateResult.latestVersion, updateResult.releaseUrl)
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
            when (updateUiState) {
                is UpdateUiState.ReadyToInstall -> {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text("Later", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                }

                is UpdateUiState.DownloadFailed -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUiState.releaseUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.testTag("update_open_browser_button")
                        ) {
                            Text("Browser", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("update_later_button")
                        ) {
                            Text("Later", color = TextSecondary)
                        }
                    }
                }

                is UpdateUiState.Result -> {
                    when (updateUiState.checkResult) {
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

                else -> Unit
            }
        }
    )
}
