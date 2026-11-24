package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.UpdateCheckResult
import com.devwithzachary.completelinuxinstaller.theme.TerminalTheme
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.AppUpdatesCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.BackgroundServiceCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.SystemDiagnosticsCard
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards.TerminalAppearanceCard
import com.devwithzachary.completelinuxinstaller.ui.theme.AppThemeMode
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: DashboardUiState,
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onSetAppThemeMode: (AppThemeMode) -> Unit = {},
    useDynamicColor: Boolean = true,
    onSetUseDynamicColor: (Boolean) -> Unit = {},
    terminalTheme: TerminalTheme = TerminalTheme.DEFAULT,
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
    onCheckForUpdatesClick: () -> Unit = {},
    isShowWhatsNewOnUpdateEnabled: Boolean = true,
    onSetShowWhatsNewOnUpdate: (Boolean) -> Unit = {},
    onOpenWhatsNew: () -> Unit = {}
) {
    val windowSizeClass = rememberWindowSizeClass()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (windowSizeClass.isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 1200.dp)
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        TerminalAppearanceCard(
                            appThemeMode = appThemeMode,
                            onSetAppThemeMode = onSetAppThemeMode,
                            useDynamicColor = useDynamicColor,
                            onSetUseDynamicColor = onSetUseDynamicColor,
                            terminalTheme = terminalTheme,
                            terminalFontSize = terminalFontSize,
                            terminalFontFamily = terminalFontFamily,
                            onSelectTheme = onSelectTheme,
                            onUpdateCustomTheme = onUpdateCustomTheme,
                            onSetTerminalFontSize = onSetTerminalFontSize,
                            onSetTerminalFontFamily = onSetTerminalFontFamily
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        BackgroundServiceCard(
                            isKeepAliveEnabled = isKeepAliveEnabled,
                            onToggleKeepAlive = onToggleKeepAlive,
                            isKeepScreenOnEnabled = isKeepScreenOnEnabled,
                            onSetKeepScreenOn = onSetKeepScreenOn
                        )

                        AppUpdatesCard(
                            isGitHubUpdateCheckEnabled = isGitHubUpdateCheckEnabled,
                            onSetGitHubUpdateCheckEnabled = onSetGitHubUpdateCheckEnabled,
                            isCheckingForUpdates = isCheckingForUpdates,
                            updateCheckResult = updateCheckResult,
                            onCheckForUpdatesClick = onCheckForUpdatesClick,
                            isShowWhatsNewOnUpdateEnabled = isShowWhatsNewOnUpdateEnabled,
                            onSetShowWhatsNewOnUpdate = onSetShowWhatsNewOnUpdate,
                            onOpenWhatsNew = onOpenWhatsNew
                        )

                        SystemDiagnosticsCard(
                            onGenerateDebugReport = onGenerateDebugReport
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    TerminalAppearanceCard(
                        appThemeMode = appThemeMode,
                        onSetAppThemeMode = onSetAppThemeMode,
                        useDynamicColor = useDynamicColor,
                        onSetUseDynamicColor = onSetUseDynamicColor,
                        terminalTheme = terminalTheme,
                        terminalFontSize = terminalFontSize,
                        terminalFontFamily = terminalFontFamily,
                        onSelectTheme = onSelectTheme,
                        onUpdateCustomTheme = onUpdateCustomTheme,
                        onSetTerminalFontSize = onSetTerminalFontSize,
                        onSetTerminalFontFamily = onSetTerminalFontFamily
                    )

                    BackgroundServiceCard(
                        isKeepAliveEnabled = isKeepAliveEnabled,
                        onToggleKeepAlive = onToggleKeepAlive,
                        isKeepScreenOnEnabled = isKeepScreenOnEnabled,
                        onSetKeepScreenOn = onSetKeepScreenOn
                    )

                    AppUpdatesCard(
                        isGitHubUpdateCheckEnabled = isGitHubUpdateCheckEnabled,
                        onSetGitHubUpdateCheckEnabled = onSetGitHubUpdateCheckEnabled,
                        isCheckingForUpdates = isCheckingForUpdates,
                        updateCheckResult = updateCheckResult,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        isShowWhatsNewOnUpdateEnabled = isShowWhatsNewOnUpdateEnabled,
                        onSetShowWhatsNewOnUpdate = onSetShowWhatsNewOnUpdate,
                        onOpenWhatsNew = onOpenWhatsNew
                    )

                    SystemDiagnosticsCard(
                        onGenerateDebugReport = onGenerateDebugReport
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Settings - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SettingsScreenLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreen(
            state = DashboardUiState(isInitializing = false, isInstalled = true),
            appThemeMode = AppThemeMode.LIGHT,
            useDynamicColor = false
        )
    }
}

@Preview(
    name = "Settings - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SettingsScreenDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreen(
            state = DashboardUiState(isInitializing = false, isInstalled = true),
            appThemeMode = AppThemeMode.DARK,
            useDynamicColor = true
        )
    }
}
