package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Player
import com.example.ui.theme.*
import com.example.viewmodel.DartsViewModel
import kotlinx.coroutines.launch

@Composable
fun PlayersScreen(
    viewModel: DartsViewModel,
    modifier: Modifier = Modifier
) {
    val players by viewModel.allPlayers.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<Player?>(null) }
    var playerToDelete by remember { mutableStateOf<Player?>(null) }
    var playerToReset by remember { mutableStateOf<Player?>(null) }

    // Intercept back button for dialogs
    BackHandler(enabled = showBackupDialog) {
        showBackupDialog = false
    }
    BackHandler(enabled = showAddDialog) {
        showAddDialog = false
    }
    BackHandler(enabled = playerToEdit != null) {
        playerToEdit = null
    }
    BackHandler(enabled = playerToDelete != null) {
        playerToDelete = null
    }
    BackHandler(enabled = playerToReset != null) {
        playerToReset = null
    }

    val filteredPlayers = remember(players, searchQuery) {
        if (searchQuery.isBlank()) players
        else players.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
    ) {
        // Header Row: "Players" and "Add Player" button (No floating + button)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Players",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${players.size} Registered",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = BrandOnPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_player_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Add Player",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Search Bar (if more than 3 players)
        if (players.size > 3) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search players...", color = TextMuted, fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = AppBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = AppSurface,
                        unfocusedContainerColor = AppSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_search_bar")
                )
            }
        }

        if (filteredPlayers.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonSearch,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No players registered" else "No matching players found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (searchQuery.isBlank()) {
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Player", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredPlayers, key = { it.id }) { player ->
                PlayerCompactCard(
                    player = player,
                    onEdit = { playerToEdit = player },
                    onReset = { playerToReset = player },
                    onDelete = { playerToDelete = player }
                )
            }
        }

        // Export / Import Database Button at the bottom of the list
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showBackupDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_import_db_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AppSurface,
                    contentColor = BrandPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SyncAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export / Import Player Database",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Add Player Dialog
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = AppSurface,
            title = {
                Text("Add New Player", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter player's name:", fontSize = 13.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g. Michael Smith", color = TextMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = AppBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_player_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            viewModel.addPlayer(nameInput)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("add_player_dialog_save")
                ) {
                    Text("Add Player", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Player Dialog
    playerToEdit?.let { player ->
        var editNameInput by remember { mutableStateOf(player.name) }
        AlertDialog(
            onDismissRequest = { playerToEdit = null },
            containerColor = AppSurface,
            title = {
                Text("Edit Player Name", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Update player's name:", fontSize = 13.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = AppBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_player_dialog_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameInput.isNotBlank()) {
                            viewModel.editPlayer(player, editNameInput)
                            playerToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("edit_player_dialog_save")
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Player Confirmation Dialog
    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            containerColor = AppSurface,
            title = {
                Text("Delete Player?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${player.name}\"? All their records will be permanently removed.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(player)
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_player_button")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Reset Player Stats Dialog
    playerToReset?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToReset = null },
            containerColor = AppSurface,
            title = {
                Text("Reset Win-Loss Record?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Reset stats for \"${player.name}\" to 0W - 0L (0%)?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPlayerStats(player.id)
                        playerToReset = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary, contentColor = BrandOnPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_reset_player_button")
                ) {
                    Text("Reset Stats", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToReset = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Export / Import Backup Dialog
    if (showBackupDialog) {
        PlayerBackupDialog(
            viewModel = viewModel,
            playerCount = players.size,
            onDismiss = { showBackupDialog = false }
        )
    }
}

/**
 * Dialog to Export or Import the entire Player Database in JSON format.
 */
@Composable
private fun PlayerBackupDialog(
    viewModel: DartsViewModel,
    playerCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Export, 1 = Import
    var exportedJson by remember { mutableStateOf("") }
    var copyNotice by remember { mutableStateOf(false) }

    var importJsonText by remember { mutableStateOf("") }
    var replaceAll by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isImportSuccess by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        exportedJson = viewModel.exportPlayersJson()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Player Database Backup",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, AppBorder, RoundedCornerShape(10.dp)),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { selectedTab = 0 }
                            .testTag("backup_tab_export"),
                        color = if (selectedTab == 0) BrandPrimaryContainer else AppSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Export ($playerCount)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) BrandOnPrimaryContainer else TextSecondary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clickable { selectedTab = 1 }
                            .testTag("backup_tab_import"),
                        color = if (selectedTab == 1) BrandPrimaryContainer else AppSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Import DB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) BrandOnPrimaryContainer else TextSecondary
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    // EXPORT TAB
                    Text(
                        text = "Export $playerCount players with all match stats and win-loss records as JSON.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(exportedJson))
                                copyNotice = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_copy_export_json"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimary,
                                contentColor = BrandOnPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (copyNotice) "Copied!" else "Copy JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TEXT, exportedJson)
                                    putExtra(Intent.EXTRA_TITLE, "Darts Players DB")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Players DB JSON"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_share_export_json"),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppSurface,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // JSON Preview Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = 180.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = AppBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            item {
                                Text(
                                    text = exportedJson.ifBlank { "No players to export." },
                                    fontSize = 11.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    // IMPORT TAB
                    Text(
                        text = "Paste player JSON or backup data below to restore or add players.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste JSON Data:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    importJsonText = clip.text
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp), tint = BrandPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Clipboard", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = {
                            importJsonText = it
                            importStatusMessage = null
                        },
                        placeholder = { Text("[{\"name\": \"Player 1\", \"wins\": 2, ...}]", color = TextMuted, fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("import_json_textfield"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = AppBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = AppBg,
                            unfocusedContainerColor = AppBg
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )

                    // Replace vs Merge option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { replaceAll = !replaceAll }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replaceAll,
                            onCheckedChange = { replaceAll = it },
                            colors = CheckboxDefaults.colors(checkedColor = BrandPrimary),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (replaceAll) "Replace entire database" else "Merge with existing players",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (replaceAll) BrandError else TextPrimary
                            )
                            Text(
                                text = if (replaceAll) "Deletes all current players and replaces with import" else "Updates matching names, adds new players",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Status / feedback message
                    importStatusMessage?.let { status ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isImportSuccess) BrandSuccessContainer.copy(alpha = 0.5f) else BrandErrorContainer.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isImportSuccess) BrandSuccess else BrandError
                            )
                        ) {
                            Text(
                                text = status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isImportSuccess) BrandSuccess else BrandError,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (importJsonText.isBlank()) {
                                importStatusMessage = "Please paste valid JSON text first."
                                isImportSuccess = false
                                return@Button
                            }
                            isImporting = true
                            coroutineScope.launch {
                                val res = viewModel.importPlayersJson(importJsonText, replaceAll)
                                isImporting = false
                                if (res.isSuccess) {
                                    val count = res.getOrNull() ?: 0
                                    isImportSuccess = true
                                    importStatusMessage = "Successfully imported $count player(s)!"
                                    importJsonText = ""
                                } else {
                                    isImportSuccess = false
                                    importStatusMessage = "Import failed: ${res.exceptionOrNull()?.message}"
                                }
                            }
                        },
                        enabled = !isImporting && importJsonText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_perform_import"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = BrandOnPrimary
                        )
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandOnPrimary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Importing...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Database", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Compact Player Card showing player name, W-L-% under it, and edit/reset/trash icons.
 */
@Composable
private fun PlayerCompactCard(
    player: Player,
    onEdit: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("player_card_${player.id}"),
        shape = RoundedCornerShape(12.dp),
        color = AppSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, AppBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player name and W-L-% directly under it
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = player.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${player.wins}W - ${player.losses}L - ${player.winRatePercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }

            // Edit, Reset, Trash icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("player_edit_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Player",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("player_reset_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Stats",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("player_delete_${player.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Player",
                        tint = BrandError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
