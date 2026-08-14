package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Apps),
    CONTAINER("Container", Icons.Default.Upgrade),
    NETWORK("Network & DNS", Icons.Default.Dns),
    TERMINAL("Terminal", Icons.Default.Palette),
    SECURITY("Security", Icons.Default.Person),
    STORAGE("Storage & Reset", Icons.Default.Folder)
}

@Composable
fun CollapsibleSettingsCard(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: @Composable (() -> Unit)? = null,
    colors: CardColors = CardDefaults.cardColors(),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = colors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (!subtitle.isNullOrBlank() && !isExpanded) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    badge?.invoke()
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: DashboardUiState,
    backupState: BackupState = BackupState.Idle,
    terminalTheme: TerminalTheme = TerminalTheme.DRACULA,
    defaultTerminalUser: String = "root",
    terminalFontSize: Int = 13,
    terminalFontFamily: String = "Monospace",
    onSelectTheme: (String) -> Unit = {},
    onUpdateCustomTheme: (Color, Color, Color, Color, List<Color>) -> Unit = { _, _, _, _, _ -> },
    onSetTerminalFontSize: (Int) -> Unit = {},
    onSetTerminalFontFamily: (String) -> Unit = {},
    onSetDefaultTerminalUser: (String) -> Unit = {},
    onSetDnsServers: (List<String>) -> Unit = {},
    onToggleBindSdCard: () -> Unit,
    onWipeRootfsClick: () -> Unit,
    onRefreshStatusClick: () -> Unit,
    onChangeRootPassword: (String) -> Unit = {},
    onCreateUser: (String, String) -> Unit = { _, _ -> },
    onDeleteUser: (String) -> Unit = {},
    onExportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onImportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onUpgradeRootfsClick: () -> Unit = {},
    onDismissBackupStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var expandedCards by remember {
        mutableStateOf(
            mapOf(
                "upgrade" to false,
                "backup" to false,
                "dns" to false,
                "theme" to false,
                "users" to false,
                "storage" to false,
                "maintenance" to false
            )
        )
    }

    fun isCardExpanded(id: String): Boolean = expandedCards[id] ?: false
    fun toggleCard(id: String) {
        expandedCards = expandedCards.toMutableMap().apply {
            put(id, !(this[id] ?: false))
        }
    }

    val allExpanded = expandedCards.values.all { it }
    fun toggleAllExpanded() {
        val newState = !allExpanded
        expandedCards = expandedCards.keys.associateWith { newState }
    }

    var showWipeConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    var newRootPassword by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<String?>(null) }
    var changePasswordUser by remember { mutableStateOf<String?>(null) }
    var changePasswordUserNewPass by remember { mutableStateOf("") }

    var customDnsInput by remember(state.dnsServers) {
        mutableStateOf(state.dnsServers.joinToString(", "))
    }
    var showDnsSavedNotice by remember { mutableStateOf(false) }

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
        // Header & Expand/Collapse Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings & Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = { toggleAllExpanded() },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (allExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (allExpanded) "Collapse All" else "Expand All", fontSize = 12.sp)
            }
        }

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SettingsCategory.entries) { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedCategory = cat
                        if (cat != SettingsCategory.ALL) {
                            when (cat) {
                                SettingsCategory.CONTAINER -> {
                                    expandedCards = expandedCards.toMutableMap().apply {
                                        put("upgrade", true)
                                        put("backup", true)
                                    }
                                }
                                SettingsCategory.NETWORK -> {
                                    expandedCards = expandedCards.toMutableMap().apply {
                                        put("dns", true)
                                    }
                                }
                                SettingsCategory.TERMINAL -> {
                                    expandedCards = expandedCards.toMutableMap().apply {
                                        put("theme", true)
                                    }
                                }
                                SettingsCategory.SECURITY -> {
                                    expandedCards = expandedCards.toMutableMap().apply {
                                        put("users", true)
                                    }
                                }
                                SettingsCategory.STORAGE -> {
                                    expandedCards = expandedCards.toMutableMap().apply {
                                        put("storage", true)
                                        put("maintenance", true)
                                    }
                                }
                                else -> {}
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(cat.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // 1. RootFS Container Maintenance & Upgrade Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CONTAINER) {
            CollapsibleSettingsCard(
                title = "RootFS Upgrade",
                subtitle = if (state.isInstalled) {
                    val v = state.rootfsVersion
                    if (v != null) "${v.versionName} (Build ${v.versionCode})" else "v1.0.0 (Legacy)"
                } else "Not Installed",
                icon = Icons.Default.Upgrade,
                isExpanded = isCardExpanded("upgrade"),
                onToggleExpand = { toggleCard("upgrade") },
                badge = {
                    if (state.isInstalled) {
                        Surface(
                            color = if (state.isUpgradeAvailable) MaterialTheme.colorScheme.primaryContainer else Color(0xFF1E3A1E),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (state.isUpgradeAvailable) "Upgrade Available" else "Up to Date",
                                color = if (state.isUpgradeAvailable) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF4CAF50),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            ) {
                Text(
                    text = "Track which app version built your Linux File System and 'upgrade' it to the latest version here to benefit from incremental system improvements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isInstalled) {
                    val currentVer = state.rootfsVersion
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Container Build:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (currentVer != null) "${currentVer.versionName} (Build ${currentVer.versionCode})" else "v1.0.0 (Legacy Build 1)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Latest App Build:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Button(
                        onClick = onUpgradeRootfsClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = if (state.isUpgradeAvailable) {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        }
                    ) {
                        Icon(Icons.Default.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (state.isUpgradeAvailable) "Upgrade RootFS to v${BuildConfig.VERSION_NAME}" else "Re-verify & Repair RootFS")
                    }
                }
            }
        }

        // 2. 1-Tap RootFS Container Backup & Restore Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CONTAINER) {
            CollapsibleSettingsCard(
                title = "Container Backup & Restore",
                subtitle = "Export or import container archive (.tar.gz)",
                icon = Icons.Default.Upload,
                isExpanded = isCardExpanded("backup"),
                onToggleExpand = { toggleCard("backup") }
            ) {
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

        // 3. Network & DNS Configuration Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.NETWORK) {
            CollapsibleSettingsCard(
                title = "Network & DNS Configuration",
                subtitle = "DNS: " + state.dnsServers.joinToString(", "),
                icon = Icons.Default.Dns,
                isExpanded = isCardExpanded("dns"),
                onToggleExpand = { toggleCard("dns") },
                badge = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${state.dnsServers.size} DNS Active",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            ) {
                Text(
                    text = "Configure nameservers used by your Linux rootfs container for APT package downloads, web access, and CLI networking tools. Applied directly to /etc/resolv.conf.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current Active DNS Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Nameservers (/etc/resolv.conf):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = state.dnsServers.joinToString("  •  "),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Quick DNS Provider Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Preset DNS Providers
                val dnsPresets = listOf(
                    "Google (8.8.8.8, 8.8.4.4)" to listOf("8.8.8.8", "8.8.4.4"),
                    "Cloudflare (1.1.1.1, 1.0.0.1)" to listOf("1.1.1.1", "1.0.0.1"),
                    "Quad9 Secure (9.9.9.9)" to listOf("9.9.9.9", "149.112.112.112"),
                    "AdGuard AdBlock (94.140.14.14)" to listOf("94.140.14.14", "94.140.15.15"),
                    "OpenDNS (208.67.222.222)" to listOf("208.67.222.222", "208.67.220.220")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dnsPresets) { (name, ips) ->
                        val isSelected = state.dnsServers == ips
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                customDnsInput = ips.joinToString(", ")
                                onSetDnsServers(ips)
                                showDnsSavedNotice = true
                            },
                            label = { Text(name, fontSize = 12.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                Text(
                    text = "Custom DNS Nameservers:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = customDnsInput,
                    onValueChange = {
                        customDnsInput = it
                        showDnsSavedNotice = false
                    },
                    label = { Text("DNS IP Addresses (comma or space separated)") },
                    placeholder = { Text("e.g. 1.1.1.1, 8.8.8.8, 9.9.9.9") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Button(
                    onClick = {
                        val parsed = customDnsInput.split(',', ' ', '\n')
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                        if (parsed.isNotEmpty()) {
                            onSetDnsServers(parsed)
                            showDnsSavedNotice = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply DNS Configuration")
                }

                if (showDnsSavedNotice) {
                    Text(
                        text = "✓ DNS servers successfully updated in /etc/resolv.conf",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 4. Terminal Color Theme Pack Card
        var showCustomThemeDialog by remember { mutableStateOf(false) }
        var editingColorTarget by remember { mutableStateOf<String?>(null) }
        var colorHexInput by remember { mutableStateOf("") }

        var customFg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultFg) }
        var customBg by remember(terminalTheme) { mutableStateOf(terminalTheme.defaultBg) }
        var customCursor by remember(terminalTheme) { mutableStateOf(terminalTheme.cursorColor) }
        var customSelection by remember(terminalTheme) { mutableStateOf(terminalTheme.selectionColor) }
        var customAnsiColors by remember(terminalTheme) { mutableStateOf(terminalTheme.ansiColors.toMutableList()) }

        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.TERMINAL) {
            CollapsibleSettingsCard(
                title = "Terminal Appearance & Theme",
                subtitle = "Theme: ${terminalTheme.name} (${terminalFontSize}sp, $terminalFontFamily)",
                icon = Icons.Default.Palette,
                isExpanded = isCardExpanded("theme"),
                onToggleExpand = { toggleCard("theme") }
            ) {
                Text(
                    text = "Choose from standard color themes or create your own custom ANSI palette.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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

                HorizontalDivider()

                // Font Size Slider Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Terminal Font Size", fontWeight = FontWeight.SemiBold)
                        Text("${terminalFontSize} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = terminalFontSize.toFloat(),
                        onValueChange = { onSetTerminalFontSize(it.toInt()) },
                        valueRange = 10f..24f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("10sp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("14sp (Default)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("24sp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                HorizontalDivider()

                // Font Family Selector Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Terminal Font Family", fontWeight = FontWeight.SemiBold)
                    val fontFamilies = listOf("Monospace", "JetBrains Mono", "Sans Serif", "Serif", "Cursive", "Casual", "Wingdings")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(fontFamilies) { family ->
                            val isSelected = terminalFontFamily == family
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetTerminalFontFamily(family) },
                                label = { Text(family) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Theme Quick Preview Box
                val previewFontFamily = when (terminalFontFamily) {
                    "Sans Serif" -> FontFamily.SansSerif
                    "Serif" -> FontFamily.Serif
                    "Cursive" -> FontFamily.Cursive
                    "Casual" -> FontFamily(androidx.compose.ui.text.font.Typeface(android.graphics.Typeface.create("casual", android.graphics.Typeface.NORMAL)))
                    "Wingdings" -> FontFamily.Monospace
                    else -> FontFamily.Monospace
                }
                val previewFontWeight = if (terminalFontFamily == "JetBrains Mono") FontWeight.Bold else FontWeight.Normal

                val wingdingsMap = remember {
                    mapOf(
                        'a' to "✌", 'b' to "👌", 'c' to "👍", 'd' to "👎", 'e' to "👈", 'f' to "👉",
                        'g' to "👆", 'h' to "👇", 'i' to "🖐", 'j' to "☺", 'k' to "😐", 'l' to "☹",
                        'm' to "💣", 'n' to "☠", 'o' to "⚐", 'p' to "⚑", 'q' to "✈", 'r' to "☼",
                        's' to "💧", 't' to "❄", 'u' to "🕇", 'v' to "🕈", 'w' to "✠", 'x' to "✡",
                        'y' to "☸", 'z' to "☯", 'A' to "✌", 'B' to "👌", 'C' to "👍", 'D' to "👎",
                        'E' to "👈", 'F' to "👉", 'G' to "👆", 'H' to "👇", 'I' to "🖐", 'J' to "☺",
                        'K' to "😐", 'L' to "☹", 'M' to "💣", 'N' to "☠", 'O' to "⚐", 'P' to "⚑",
                        'Q' to "✈", 'R' to "☼", 'S' to "💧", 'T' to "❄", 'U' to "🕇", 'V' to "🕈",
                        'W' to "✠", 'X' to "✡", 'Y' to "☸", 'Z' to "☯",
                        '0' to "⓪", '1' to "①", '2' to "②", '3' to "③", '4' to "④",
                        '5' to "⑤", '6' to "⑥", '7' to "⑦", '8' to "⑧", '9' to "⑨",
                        ':' to "❖", '/' to "✂", '-' to "✦", '~' to "≈", '$' to "💲", '#' to "⌗",
                        '@' to "🌀", '.' to "●", ' ' to " "
                    )
                }
                fun toPreviewText(text: String): String {
                    return if (terminalFontFamily == "Wingdings") {
                        text.map { wingdingsMap[it] ?: it.toString() }.joinToString("")
                    } else text
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = terminalTheme.defaultBg
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = toPreviewText("ubuntu@localhost:~$ uname -a"),
                            color = terminalTheme.defaultFg,
                            fontFamily = previewFontFamily,
                            fontWeight = previewFontWeight,
                            fontSize = terminalFontSize.sp
                        )
                        Text(
                            text = toPreviewText("Linux localhost 6.1.0-android-proot #1 SMP PREEMPT"),
                            color = terminalTheme.ansiColors[2],
                            fontFamily = previewFontFamily,
                            fontWeight = previewFontWeight,
                            fontSize = terminalFontSize.sp
                        )
                        Text(
                            text = toPreviewText("ubuntu@localhost:~$ cat /etc/issue"),
                            color = terminalTheme.defaultFg,
                            fontFamily = previewFontFamily,
                            fontWeight = previewFontWeight,
                            fontSize = terminalFontSize.sp
                        )
                        Text(
                            text = toPreviewText("Ubuntu 26.04 LTS \\n \\l"),
                            color = terminalTheme.ansiColors[4],
                            fontFamily = previewFontFamily,
                            fontWeight = previewFontWeight,
                            fontSize = terminalFontSize.sp
                        )
                    }
                }

                // Custom Theme Creator / Editor Expand Button
                OutlinedButton(
                    onClick = { showCustomThemeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Customize Palette & Colors")
                }
            }
        }

        // Custom Theme Editor Modal Dialog
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Customize foreground, background, cursor, and ANSI 16 color palette.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text("Base Interface Colors", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        ColorSwatchPickerRow(title = "Default Foreground (Text)", color = customFg) {
                            editingColorTarget = "fg"
                            colorHexInput = TerminalTheme.colorToHex(customFg)
                        }
                        ColorSwatchPickerRow(title = "Default Background", color = customBg) {
                            editingColorTarget = "bg"
                            colorHexInput = TerminalTheme.colorToHex(customBg)
                        }
                        ColorSwatchPickerRow(title = "Cursor Color", color = customCursor) {
                            editingColorTarget = "cursor"
                            colorHexInput = TerminalTheme.colorToHex(customCursor)
                        }
                        ColorSwatchPickerRow(title = "Selection Highlight", color = customSelection) {
                            editingColorTarget = "selection"
                            colorHexInput = TerminalTheme.colorToHex(customSelection)
                        }

                        HorizontalDivider()

                        Text("ANSI 16 Color Palette", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                        val ansiLabels = listOf(
                            "0: Black", "1: Red", "2: Green", "3: Yellow",
                            "4: Blue", "5: Magenta", "6: Cyan", "7: White",
                            "8: Bright Black", "9: Bright Red", "10: Bright Green", "11: Bright Yellow",
                            "12: Bright Blue", "13: Bright Magenta", "14: Bright Cyan", "15: Bright White"
                        )

                        ansiLabels.forEachIndexed { idx, label ->
                            val color = customAnsiColors.getOrElse(idx) { Color.Gray }
                            ColorSwatchPickerRow(title = label, color = color) {
                                editingColorTarget = "ansi_$idx"
                                colorHexInput = TerminalTheme.colorToHex(color)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateCustomTheme(customFg, customBg, customCursor, customSelection, customAnsiColors)
                            onSelectTheme("custom")
                            showCustomThemeDialog = false
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

        // Color Hex Edit Dialog
        if (editingColorTarget != null) {
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

                        val previewColor = TerminalTheme.hexToColor(colorHexInput, Color.Transparent)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Preview:", fontWeight = FontWeight.SemiBold)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(previewColor)
                                    .border(1.dp, Color.White, RoundedCornerShape(6.dp))
                            )
                        }

                        Text("Quick Swatches", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        val swatches = listOf(
                            Color(0xFF000000), Color(0xFF1E1E1E), Color(0xFF282A36), Color(0xFF002B36),
                            Color(0xFFFFFFFF), Color(0xFFF8F8F2), Color(0xFF839496), Color(0xFF50FA7B),
                            Color(0xFFFF5555), Color(0xFFBD93F9), Color(0xFF8BE9FD), Color(0xFFFFB86C),
                            Color(0xFFF1FA8C), Color(0xFF6272A4), Color(0xFF2AA198), Color(0xFF268BD2)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(swatches) { swatch ->
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
                            val parsed = TerminalTheme.hexToColor(colorHexInput, Color.White)
                            when (val target = editingColorTarget) {
                                "fg" -> customFg = parsed
                                "bg" -> customBg = parsed
                                "cursor" -> customCursor = parsed
                                "selection" -> customSelection = parsed
                                else -> {
                                    if (target != null && target.startsWith("ansi_")) {
                                        val idx = target.removePrefix("ansi_").toIntOrNull() ?: 0
                                        if (idx in customAnsiColors.indices) {
                                            customAnsiColors[idx] = parsed
                                        }
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

        // 5. Users & Account Management Card
        if ((selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.SECURITY) && state.isInstalled) {
            CollapsibleSettingsCard(
                title = "User & Account Management",
                subtitle = "${state.containerUsers.size + 1} Users (${defaultTerminalUser} default)",
                icon = Icons.Default.Person,
                isExpanded = isCardExpanded("users"),
                onToggleExpand = { toggleCard("users") }
            ) {
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

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Default Terminal Login User", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Select which user account logs into interactive terminal sessions by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val loginUsers = listOf("root") + state.containerUsers
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        loginUsers.forEach { u ->
                            val isSelected = u == defaultTerminalUser
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetDefaultTerminalUser(u) },
                                label = { Text(if (u == "root") "root (Admin)" else u) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
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

        // 6. Storage & Bind Mount Options
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.STORAGE) {
            CollapsibleSettingsCard(
                title = "Storage & Mount Points",
                subtitle = if (state.bindSdCard) "SD Card Bound" else "Host Storage Isolated",
                icon = Icons.Default.Folder,
                isExpanded = isCardExpanded("storage"),
                onToggleExpand = { toggleCard("storage") }
            ) {
                var isStorageGranted by remember {
                    mutableStateOf(
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.READ_MEDIA_IMAGES
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.READ_MEDIA_VIDEO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        } else {
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                    )
                }

                val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    isStorageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_MEDIA_IMAGES
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_MEDIA_VIDEO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else {
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                }

                // Storage Information Card
                Surface(
                    color = if (isStorageGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
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
                                imageVector = if (isStorageGranted) Icons.Default.CheckCircle else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isStorageGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Device File & Storage Permission",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "LinuxOnAndroid exposes host storage (/sdcard, /storage/emulated/0, and ~/Downloads) inside the Linux container. Backup exports & imports use standard Storage Access Framework (SAF) document pickers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isStorageGranted) {
                            Text(
                                text = "✓ Storage & Media Access Enabled",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.READ_MEDIA_IMAGES,
                                                android.Manifest.permission.READ_MEDIA_VIDEO,
                                                android.Manifest.permission.READ_MEDIA_AUDIO
                                            )
                                        )
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Storage & Media Access")
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

        // 7. Maintenance & Reset Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.STORAGE) {
            CollapsibleSettingsCard(
                title = "Container Maintenance & Danger Zone",
                subtitle = "Refresh status or wipe container",
                icon = Icons.Default.Warning,
                isExpanded = isCardExpanded("maintenance"),
                onToggleExpand = { toggleCard("maintenance") }
            ) {
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
                    when (backupState) {
                        is BackupState.Processing -> "Container Backup Operation"
                        is BackupState.Success -> "Backup Succeeded"
                        is BackupState.Error -> "Backup Operation Failed"
                        else -> "Backup"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when (backupState) {
                        is BackupState.Processing -> {
                            Text(backupState.message)
                            if (backupState.progressPercent >= 0) {
                                LinearProgressIndicator(
                                    progress = { backupState.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("${backupState.progressPercent}%", style = MaterialTheme.typography.bodySmall)
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                        }
                        is BackupState.Success -> {
                            Text(backupState.message, color = Color(0xFF4CAF50))
                        }
                        is BackupState.Error -> {
                            Text(backupState.message, color = MaterialTheme.colorScheme.error)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (backupState !is BackupState.Processing) {
                    TextButton(onClick = { onDismissBackupStatus() }) {
                        Text("OK")
                    }
                }
            }
        )
    }

    // Confirmation for RootFS Wipe Dialog
    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Ubuntu Container?") },
            text = { Text("This will permanently delete your rootfs environment and all installed applications. This action cannot be undone.") },
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

    // Root Password Change Dialog
    if (showRootPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRootPasswordDialog = false },
            title = { Text("Set Root User Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new password for the system administrator ('root') account:")
                    OutlinedTextField(
                        value = newRootPassword,
                        onValueChange = { newRootPassword = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRootPassword.isNotBlank()) {
                            onChangeRootPassword(newRootPassword)
                            newRootPassword = ""
                            showRootPasswordDialog = false
                        }
                    },
                    enabled = newRootPassword.isNotBlank()
                ) {
                    Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRootPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add User Dialog
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Add New Container User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a new standard sudo user for terminal and SSH sessions:")
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
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newUserPassword.isNotBlank()) {
                            onCreateUser(newUsername.trim(), newUserPassword)
                            newUsername = ""
                            newUserPassword = ""
                            showAddUserDialog = false
                        }
                    },
                    enabled = newUsername.isNotBlank() && newUserPassword.isNotBlank()
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

    // Change Regular User Password Dialog
    changePasswordUser?.let { targetUser ->
        AlertDialog(
            onDismissRequest = { changePasswordUser = null },
            title = { Text("Change Password for '$targetUser'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new password for user '$targetUser':")
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
                Button(
                    onClick = {
                        if (changePasswordUserNewPass.isNotBlank()) {
                            onCreateUser(targetUser, changePasswordUserNewPass)
                            changePasswordUser = null
                            changePasswordUserNewPass = ""
                        }
                    },
                    enabled = changePasswordUserNewPass.isNotBlank()
                ) {
                    Text("Update Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { changePasswordUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete User Confirmation Dialog
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User '$user'?") },
            text = { Text("Are you sure you want to delete user '$user' and remove their home directory?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteUser(user)
                        userToDelete = null
                    }
                ) {
                    Text("Delete User", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
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
