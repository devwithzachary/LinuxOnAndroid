package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.AppUpdatesCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.BackgroundServiceCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.SystemDiagnosticsCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.TerminalAppearanceCard
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass

enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Apps),
    UPDATES("Updates", Icons.Default.CloudDownload),
    TERMINAL("Terminal", Icons.Default.Palette),
    BACKGROUND("Background", Icons.Default.PlayArrow),
    DIAGNOSTICS("Diagnostics", Icons.Default.BugReport)
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
    isGitHubUpdateCheckEnabled: Boolean = false,
    onSetGitHubUpdateCheckEnabled: (Boolean) -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    updateCheckResult: UpdateCheckResult? = null,
    onCheckForUpdatesClick: () -> Unit = {}
) {
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

    val windowSizeClass = rememberWindowSizeClass()
    val isWideScreen = windowSizeClass.isExpanded

    val updatesCard = @Composable {
        AppUpdatesCard(
            isExpanded = isCardExpanded("updates"),
            onToggleExpand = { toggleCard("updates") },
            isGitHubUpdateCheckEnabled = isGitHubUpdateCheckEnabled,
            onSetGitHubUpdateCheckEnabled = onSetGitHubUpdateCheckEnabled,
            isCheckingForUpdates = isCheckingForUpdates,
            updateCheckResult = updateCheckResult,
            onCheckForUpdatesClick = onCheckForUpdatesClick
        )
    }

    val terminalCard = @Composable {
        TerminalAppearanceCard(
            isExpanded = isCardExpanded("theme"),
            onToggleExpand = { toggleCard("theme") },
            terminalTheme = terminalTheme,
            terminalFontSize = terminalFontSize,
            terminalFontFamily = terminalFontFamily,
            onSelectTheme = onSelectTheme,
            onUpdateCustomTheme = onUpdateCustomTheme,
            onSetTerminalFontSize = onSetTerminalFontSize,
            onSetTerminalFontFamily = onSetTerminalFontFamily
        )
    }

    val backgroundCard = @Composable {
        BackgroundServiceCard(
            isExpanded = isCardExpanded("background"),
            onToggleExpand = { toggleCard("background") },
            isKeepAliveEnabled = isKeepAliveEnabled,
            onToggleKeepAlive = onToggleKeepAlive,
            isKeepScreenOnEnabled = isKeepScreenOnEnabled,
            onSetKeepScreenOn = onSetKeepScreenOn
        )
    }

    val diagnosticsCard = @Composable {
        SystemDiagnosticsCard(
            isExpanded = isCardExpanded("diagnostics"),
            onToggleExpand = { toggleCard("diagnostics") },
            onGenerateDebugReport = onGenerateDebugReport
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 1200.dp)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            if (isWideScreen && selectedCategory == SettingsCategory.ALL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        updatesCard()
                        backgroundCard()
                        diagnosticsCard()
                    }
                    Column(
                        modifier = Modifier.weight(1.1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        terminalCard()
                    }
                }
            } else {
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.UPDATES) {
                    updatesCard()
                }
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.TERMINAL) {
                    terminalCard()
                }
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.BACKGROUND) {
                    backgroundCard()
                }
                if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DIAGNOSTICS) {
                    diagnosticsCard()
                }
            }
        }
    }
}
