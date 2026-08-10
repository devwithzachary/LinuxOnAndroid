package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

@Composable
fun SettingsScreen(
    state: DashboardUiState,
    backupState: BackupState = BackupState.Idle,
    terminalTheme: TerminalTheme = TerminalTheme.DRACULA,
    onSelectTheme: (String) -> Unit = {},
    onUpdateCustomTheme: (Color, Color, Color, Color, List<Color>) -> Unit = { _, _, _, _, _ -> },
    onToggleBindSdCard: () -> Unit,
    onWipeRootfsClick: () -> Unit,
    onRefreshStatusClick: () -> Unit,
    onChangeRootPassword: (String) -> Unit = {},
    onCreateUser: (String, String) -> Unit = { _, _ -> },
    onDeleteUser: (String) -> Unit = {},
    onExportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onImportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onDismissBackupStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var showWipeConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    var newRootPassword by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }

    var changePasswordUser by remember { mutableStateOf<String?>(null) }
    var changePasswordUserNewPass by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        if (uri != null) {
            onExportContainer(contentResolver, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportContainer(contentResolver, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Configuration",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 1-Tap RootFS Container Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Container Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create a 1-tap backup archive (.tar.gz) of your complete Linux rootfs environment or restore an existing container archive from device storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = "linux_on_android_backup_${System.currentTimeMillis() / 1000}.tar.gz"
                            exportLauncher.launch(defaultName)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = state.isInstalled && backupState is BackupState.Idle,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Container")
                    }

                    OutlinedButton(
                        onClick = { showImportConfirm = true },
                        modifier = Modifier.weight(1f),
                        enabled = backupState is BackupState.Idle,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Container")
                    }
                }
            }
        }

        // Terminal Color Theme Pack Card
        var showCustomThemeDialog by remember { mutableStateOf(false) }
        var editingColorTarget by remember { mutableStateOf<String?>(null) }
        var colorHexInput by remember { mutableStateOf("") }

        var customFg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultFg) }
        var customBg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultBg) }
        var customCursor by remember(terminalTheme) { mutableStateOf(terminalTheme.cursorColor) }
        var customSelection by remember(terminalTheme) { mutableStateOf(terminalTheme.selectionColor) }
        var customAnsiColors by remember(terminalTheme) { mutableStateOf(terminalTheme.ansiColors.toMutableList()) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Terminal Color Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose from standard color themes or create your own custom ANSI palette.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Horizontal Preset Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    val allThemes = TerminalTheme.PRESETS + listOf(
                        TerminalTheme(
                            id = "custom",
                            name = "Custom",
                            defaultFg = customFg,
                            defaultBg = customBg,
                            cursorColor = customCursor,
                            selectionColor = customSelection,
                            ansiColors = customAnsiColors
                        )
                    )

                    items(allThemes) { theme ->
                        val isSelected = terminalTheme.id == theme.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (theme.id == "custom" && isSelected) {
                                    showCustomThemeDialog = true
                                } else {
                                    onSelectTheme(theme.id)
                                }
                            },
                            label = { Text(theme.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(theme.defaultBg)
                                        .border(1.dp, theme.defaultFg, CircleShape)
                                )
                            },
                            trailingIcon = if (theme.id == "custom") {
                                {
                                    IconButton(
                                        onClick = { showCustomThemeDialog = true },
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Custom Theme", modifier = Modifier.size(12.dp))
                                    }
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Interactive Live Terminal Preview Box
                Surface(
                    color = terminalTheme.defaultBg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, terminalTheme.selectionColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "root@ubuntu:~# ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = terminalTheme.ansiColors.getOrElse(2) { Color.Green }
                            )
                            Text(
                                text = "neofetch",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = terminalTheme.defaultFg
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OS: ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = terminalTheme.ansiColors.getOrElse(6) { Color.Cyan }
                            )
                            Text(
                                text = "Ubuntu 26.04 LTS (ARM64)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = terminalTheme.defaultFg
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kernel: ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = terminalTheme.ansiColors.getOrElse(3) { Color.Yellow }
                            )
                            Text(
                                text = "6.1.0-linuxonandroid",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = terminalTheme.defaultFg
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Terminal: ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = terminalTheme.ansiColors.getOrElse(5) { Color.Magenta }
                            )
                            Text(
                                text = "LinuxOnAndroid PTY ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = terminalTheme.defaultFg
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp, 14.dp)
                                    .background(terminalTheme.cursorColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Palette 16 Color Dots Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            terminalTheme.ansiColors.take(16).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                            }
                        }
                    }
                }

                if (terminalTheme.id == "custom") {
                    Button(
                        onClick = { showCustomThemeDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Palette & Colors")
                    }
                }
            }
        }

        // Custom Theme Editor Dialog
        if (showCustomThemeDialog) {
            AlertDialog(
                onDismissRequest = { showCustomThemeDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Custom Theme Creator")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tap any color swatch to edit its Hex color value.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text("Base Interface Colors", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        ColorSwatchPickerRow("Foreground (Text)", customFg) {
                            editingColorTarget = "fg"
                            colorHexInput = TerminalTheme.colorToHex(customFg)
                        }

                        ColorSwatchPickerRow("Background (Canvas)", customBg) {
                            editingColorTarget = "bg"
                            colorHexInput = TerminalTheme.colorToHex(customBg)
                        }

                        ColorSwatchPickerRow("Cursor Color", customCursor) {
                            editingColorTarget = "cursor"
                            colorHexInput = TerminalTheme.colorToHex(customCursor)
                        }

                        ColorSwatchPickerRow("Selection Highlight", customSelection) {
                            editingColorTarget = "selection"
                            colorHexInput = TerminalTheme.colorToHex(customSelection)
                        }

                        HorizontalDivider()

                        Text("ANSI 16 Color Palette", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        val colorNames = listOf(
                            "0: Black", "1: Red", "2: Green", "3: Yellow",
                            "4: Blue", "5: Magenta", "6: Cyan", "7: White",
                            "8: Bright Black", "9: Bright Red", "10: Bright Green", "11: Bright Yellow",
                            "12: Bright Blue", "13: Bright Magenta", "14: Bright Cyan", "15: Bright White"
                        )

                        colorNames.forEachIndexed { index, name ->
                            val color = customAnsiColors.getOrElse(index) { Color.White }
                            ColorSwatchPickerRow(name, color) {
                                editingColorTarget = "ansi_$index"
                                colorHexInput = TerminalTheme.colorToHex(color)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCustomThemeDialog = false
                            onUpdateCustomTheme(customFg, customBg, customCursor, customSelection, customAnsiColors)
                        }
                    ) {
                        Text("Save Theme")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Inner Hex Picker Dialog
        editingColorTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { editingColorTarget = null },
                title = { Text("Edit Hex Color") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = colorHexInput,
                            onValueChange = { colorHexInput = it },
                            label = { Text("Hex Code (e.g. #FF0055)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Preview:", fontWeight = FontWeight.SemiBold)
                            val parsed = TerminalTheme.hexToColor(colorHexInput, Color.Gray)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(parsed)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            )
                        }

                        Text("Quick Swatches", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        val quickSwatches = listOf(
                            Color(0xFF282A36), Color(0xFF002B36), Color(0xFF272822), Color(0xFF0D0221),
                            Color(0xFFFF5555), Color(0xFF50FA7B), Color(0xFFF1FA8C), Color(0xFFBD93F9),
                            Color(0xFFFF79C6), Color(0xFF8BE9FD), Color(0xFF00FF9F), Color(0xFFFF0055)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            quickSwatches.take(6).forEach { swatch ->
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .clickable { colorHexInput = TerminalTheme.colorToHex(swatch) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newColor = TerminalTheme.hexToColor(colorHexInput, Color.White)
                            when {
                                target == "fg" -> customFg = newColor
                                target == "bg" -> customBg = newColor
                                target == "cursor" -> customCursor = newColor
                                target == "selection" -> customSelection = newColor
                                target.startsWith("ansi_") -> {
                                    val idx = target.removePrefix("ansi_").toIntOrNull() ?: 0
                                    if (idx in 0..15) {
                                        val newList = customAnsiColors.toMutableList()
                                        newList[idx] = newColor
                                        customAnsiColors = newList
                                    }
                                }
                            }
                            editingColorTarget = null
                        }
                    ) {
                        Text("Apply Color")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingColorTarget = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Users & Account Management Card
        if (state.isInstalled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "User & Account Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Root User Password", fontWeight = FontWeight.SemiBold)
                            Text("Set or reset the system administrator (root) password.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { showRootPasswordDialog = true }) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Regular Users (${state.containerUsers.size})", fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = { showAddUserDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add User")
                        }
                    }

                    if (state.containerUsers.isEmpty()) {
                        Text(
                            text = "No non-root users found. Add a user to connect via SSH safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.containerUsers.forEach { user ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(user, fontWeight = FontWeight.Bold)
                                                Text("Sudo User (/bin/bash)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                changePasswordUser = user
                                                changePasswordUserNewPass = ""
                                            }) {
                                                Icon(Icons.Default.Lock, contentDescription = "Change Password", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { onDeleteUser(user) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Storage & Bind Mount Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Storage & Mount Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val isStorageGranted = remember {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        android.os.Environment.isExternalStorageManager()
                    } else {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                }

                // Storage Permission Explanation Card
                Surface(
                    color = if (isStorageGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isStorageGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isStorageGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Device File & Storage Permission",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isStorageGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Text(
                            text = "LinuxOnAndroid requires All Files Access permission to expose your host storage (/sdcard, /storage/emulated/0, and ~/Downloads) inside the Linux container, and to save/restore container backup archives. Without this permission, /sdcard will appear empty.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isStorageGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )

                        if (isStorageGranted) {
                            Text(
                                text = "✓ All Files Storage Access Enabled",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        try {
                                            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Storage Access")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bind Mount /sdcard & Downloads", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Expose host storage inside /sdcard, /storage/emulated/0, and ~/Downloads in PRoot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.bindSdCard,
                        onCheckedChange = { onToggleBindSdCard() }
                    )
                }
            }
        }

        // Maintenance & Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Container Maintenance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onRefreshStatusClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh System & Storage Status")
                }

                Button(
                    onClick = { showWipeConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe & Reset Ubuntu RootFS")
                }
            }
        }
    }

    // Confirmation for Container Import
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import RootFS Container?") },
            text = { Text("Restoring a backup container will replace your current active RootFS and all installed software. Are you sure you want to select a backup file to restore?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/x-tar", "*/*"))
                    }
                ) {
                    Text("Proceed & Select Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Processing / Success / Error Modal Dialog
    if (backupState !is BackupState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (backupState !is BackupState.Processing) {
                    onDismissBackupStatus()
                }
            },
            title = {
                Text(
                    text = when (backupState) {
                        is BackupState.Processing -> "Container Backup Operation"
                        is BackupState.Success -> "Backup Operation Complete"
                        is BackupState.Error -> "Backup Error"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    when (backupState) {
                        is BackupState.Processing -> {
                            if (backupState.progressPercent >= 0) {
                                LinearProgressIndicator(
                                    progress = { (backupState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = backupState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${backupState.progressPercent}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                CircularProgressIndicator()
                                Text(
                                    text = backupState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        is BackupState.Success -> {
                            Text(
                                text = backupState.message,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is BackupState.Error -> {
                            Text(
                                text = backupState.message,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (backupState !is BackupState.Processing) {
                    TextButton(onClick = onDismissBackupStatus) {
                        Text("OK")
                    }
                }
            }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Ubuntu Container?") },
            text = { Text("Are you sure you want to completely delete the Ubuntu rootfs image and all installed Linux software? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirm = false
                        onWipeRootfsClick()
                    }
                ) {
                    Text("Wipe Container", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRootPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRootPasswordDialog = false },
            title = { Text("Change Root Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new system administrator password for root:")
                    OutlinedTextField(
                        value = newRootPassword,
                        onValueChange = { newRootPassword = it },
                        label = { Text("New Root Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newRootPassword.isNotBlank()) {
                            onChangeRootPassword(newRootPassword)
                            newRootPassword = ""
                            showRootPasswordDialog = false
                        }
                    }
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRootPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Create New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a non-root system user account with sudo privileges:")
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserPassword,
                        onValueChange = { newUserPassword = it },
                        label = { Text("User Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.isNotBlank() && newUserPassword.isNotBlank()) {
                            onCreateUser(newUsername, newUserPassword)
                            newUsername = ""
                            newUserPassword = ""
                            showAddUserDialog = false
                        }
                    }
                ) {
                    Text("Create User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (changePasswordUser != null) {
        val targetUser = changePasswordUser!!
        AlertDialog(
            onDismissRequest = { changePasswordUser = null },
            title = { Text("Change Password for '$targetUser'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new password for user $targetUser:")
                    OutlinedTextField(
                        value = changePasswordUserNewPass,
                        onValueChange = { changePasswordUserNewPass = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (changePasswordUserNewPass.isNotBlank()) {
                            onCreateUser(targetUser, changePasswordUserNewPass)
                            changePasswordUserNewPass = ""
                            changePasswordUser = null
                        }
                    }
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { changePasswordUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ColorSwatchPickerRow(
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = TerminalTheme.colorToHex(color),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            )
        }
    }
}
