package com.devwithzachary.completelinuxinstaller.ui.screens.settings

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.components.DebugReportDialog
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalFonts
import kotlinx.coroutines.launch

enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Apps),
    UPDATES("Updates", Icons.Default.CloudDownload),
    TERMINAL("Terminal", Icons.Default.Palette),
    BACKGROUND("Background", Icons.Default.PlayArrow),
    DIAGNOSTICS("Diagnostics", Icons.Default.BugReport)
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
    terminalTheme: TerminalTheme = TerminalTheme.DRACULA,
    terminalFontSize: Int = 13,
    terminalFontFamily: String = "Monospace",
    onSelectTheme: (String) -> Unit = {},
    onUpdateCustomTheme: (Color, Color, Color, Color, List<Color>) -> Unit = { _, _, _, _, _ -> },
    onSetTerminalFontSize: (Int) -> Unit = {},
    onSetTerminalFontFamily: (String) -> Unit = {},
    onGenerateDebugReport: suspend () -> String = { "" },
    isKeepAliveEnabled: Boolean = true,
    onToggleKeepAlive: () -> Unit = {},
    isKeepScreenOnEnabled: Boolean = true,
    onSetKeepScreenOn: (Boolean) -> Unit = {},
    isGitHubUpdateCheckEnabled: Boolean = true,
    onSetGitHubUpdateCheckEnabled: (Boolean) -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    updateCheckResult: UpdateCheckResult? = null,
    onCheckForUpdatesClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDebugReportDialog by remember { mutableStateOf(false) }
    var debugReportText by remember { mutableStateOf("") }
    var isGeneratingDebugReport by remember { mutableStateOf(false) }

    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var expandedCards by remember {
        mutableStateOf(
            mapOf(
                "updates" to true,
                "theme" to true,
                "background" to false,
                "diagnostics" to false
            )
        )
    }

    fun isCardExpanded(id: String) = expandedCards[id] ?: false
    fun toggleCard(id: String) {
        expandedCards = expandedCards.toMutableMap().apply {
            this[id] = !(this[id] ?: false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Settings Category Filter Tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(SettingsCategory.entries) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // 1. Updates & Release Channel Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.UPDATES) {
            CollapsibleSettingsCard(
                title = stringResource(R.string.github_updates_card_title),
                subtitle = stringResource(R.string.github_updates_card_subtitle),
                icon = Icons.Default.CloudDownload,
                isExpanded = isCardExpanded("updates"),
                onToggleExpand = { toggleCard("updates") },
                badge = {
                    if (updateCheckResult is UpdateCheckResult.UpdateAvailable) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = updateCheckResult.release.tagName,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            ) {
                Text(
                    text = stringResource(R.string.github_updates_setting_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSetGitHubUpdateCheckEnabled(!isGitHubUpdateCheckEnabled) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.github_updates_setting_title),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isGitHubUpdateCheckEnabled) "Automatic startup & background release checks active" else "Automatic notifications disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isGitHubUpdateCheckEnabled,
                        onCheckedChange = { onSetGitHubUpdateCheckEnabled(it) }
                    )
                }

                Button(
                    onClick = onCheckForUpdatesClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCheckingForUpdates,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isCheckingForUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isCheckingForUpdates) stringResource(R.string.github_updates_status_checking)
                        else stringResource(R.string.github_updates_btn_check)
                    )
                }

                if (updateCheckResult is UpdateCheckResult.UpToDate) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E3A1E),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.github_updates_status_up_to_date, updateCheckResult.currentVersion),
                                color = Color(0xFF4CAF50),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (updateCheckResult is UpdateCheckResult.Error) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(
                                text = stringResource(R.string.github_updates_status_error, updateCheckResult.message),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Terminal Appearance & Theme Pack Card
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
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Custom Theme",
                                            modifier = Modifier.size(12.dp)
                                        )
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
                        Text(
                            "${terminalFontSize} sp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                        Text(
                            "10sp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "14sp (Default)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "24sp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                // Font Family Selector Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Terminal Font Family", fontWeight = FontWeight.SemiBold)
                    val fontFamilies = TerminalFonts.AVAILABLE_FONTS
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
                                    {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                val previewFontFamily = TerminalFonts.getComposeFontFamily(terminalFontFamily)
                val previewFontWeight = FontWeight.Normal

                fun toPreviewText(text: String): String {
                    return if (terminalFontFamily == "CyberGlyphs") {
                        com.devwithzachary.completelinuxinstaller.theme.CyberGlyphs.transformText(text)
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
                            text = toPreviewText("user@localhost:~$ uname -a"),
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
                            text = toPreviewText("user@localhost:~$ cat /etc/issue"),
                            color = terminalTheme.defaultFg,
                            fontFamily = previewFontFamily,
                            fontWeight = previewFontWeight,
                            fontSize = terminalFontSize.sp
                        )
                        Text(
                            text = toPreviewText("LinuxOnAndroid Environment \\n \\l"),
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

                        Text(
                            "Base Interface Colors",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

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

                        Text(
                            "ANSI 16 Color Palette",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

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

        // 3. Background Execution & Power Management Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.BACKGROUND) {
            CollapsibleSettingsCard(
                title = stringResource(R.string.setting_keep_alive_title),
                subtitle = if (isKeepAliveEnabled) "Foreground Service & WakeLock Active" else "Standard Background Limits",
                icon = Icons.Default.PlayArrow,
                isExpanded = isCardExpanded("background"),
                onToggleExpand = { toggleCard("background") },
                badge = {
                    Surface(
                        color = if (isKeepAliveEnabled) Color(0xFF1E3A1E) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isKeepAliveEnabled) "WakeLock Active" else "Disabled",
                            color = if (isKeepAliveEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            ) {
                Text(
                    text = "Controls whether LinuxOnAndroid keeps background processes alive when the app is minimized or the screen is turned off.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Foreground Service & WakeLock", fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.setting_keep_alive_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isKeepAliveEnabled,
                        onCheckedChange = { onToggleKeepAlive() }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.setting_keep_screen_on_title),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            stringResource(R.string.setting_keep_screen_on_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isKeepScreenOnEnabled,
                        onCheckedChange = { onSetKeepScreenOn(it) }
                    )
                }

                if (isKeepAliveEnabled) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "🛡️ Protection Status:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "• Partial CPU WakeLock held during active terminal & server sessions\n• Ongoing low-priority notification with quick actions\n• Resilient against Android Doze and Phantom Process Killer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    var isNotifGranted by remember {
                        mutableStateOf(
                            androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        )
                    }
                    val notifLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        isNotifGranted = granted
                    }

                    if (!isNotifGranted) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Notification Permission Disabled",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Text(
                                    text = "Android 13+ requires notification permission to display the foreground service status and prevent background terminations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Button(
                                    onClick = { notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Grant Notification Permission")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Diagnostics & Debug Report Card
        if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DIAGNOSTICS) {
            CollapsibleSettingsCard(
                title = "Diagnostics & Debug Report",
                subtitle = "Generate a technical debug report for bug reports",
                icon = Icons.Default.BugReport,
                isExpanded = isCardExpanded("diagnostics"),
                onToggleExpand = { toggleCard("diagnostics") }
            ) {
                Text(
                    text = "Generate a technical summary of your installation (app version, device hardware, container health, storage and memory metrics) to paste into GitHub issues or Discord when asking for help.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            isGeneratingDebugReport = true
                            try {
                                debugReportText = onGenerateDebugReport()
                                showDebugReportDialog = true
                            } finally {
                                isGeneratingDebugReport = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingDebugReport,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isGeneratingDebugReport) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Debug Report")
                }
            }
        }
    }

    // Debug Report Dialog
    if (showDebugReportDialog) {
        DebugReportDialog(
            report = debugReportText,
            onDismiss = { showDebugReportDialog = false }
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = TerminalTheme.colorToHex(color),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            )
        }
    }
}
