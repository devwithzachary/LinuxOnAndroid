package com.devwithzachary.completelinuxinstaller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.screens.about.AboutScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.dashboard.DashboardScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.hub.SoftwareHubScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.SettingsScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.splash.SplashScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.WizardScreen

enum class AppScreen(val titleRes: Int) {
    SPLASH(R.string.app_name),
    DASHBOARD(R.string.nav_dashboard),
    WIZARD(R.string.app_title),
    TERMINAL(R.string.nav_terminal),
    SOFTWARE_HUB(R.string.hub_title),
    SETTINGS(R.string.nav_settings),
    ABOUT(R.string.nav_about)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()

    val isInitializing = dashboardState.isInitializing
    val isInstalled = dashboardState.isInstalled
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

    // Sync screen navigation state when initialization completes
    LaunchedEffect(isInitializing, isInstalled) {
        if (!isInitializing) {
            if (!isInstalled) {
                currentScreen = AppScreen.WIZARD
            } else if (currentScreen == AppScreen.SPLASH) {
                currentScreen = AppScreen.DASHBOARD
            }
        }
    }

    if (isInitializing || currentScreen == AppScreen.SPLASH) {
        SplashScreen()
    } else {
        Scaffold(
            bottomBar = {
                val isImeVisible = WindowInsets.isImeVisible
                if (isInstalled && currentScreen != AppScreen.WIZARD && !(currentScreen == AppScreen.TERMINAL && isImeVisible)) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.DASHBOARD,
                            onClick = { currentScreen = AppScreen.DASHBOARD },
                            icon = {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = stringResource(R.string.nav_dashboard)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_dashboard)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.TERMINAL,
                            onClick = { currentScreen = AppScreen.TERMINAL },
                            icon = {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = stringResource(R.string.nav_terminal)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_terminal)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SOFTWARE_HUB,
                            onClick = { currentScreen = AppScreen.SOFTWARE_HUB },
                            icon = {
                                Icon(
                                    Icons.Default.Apps,
                                    contentDescription = stringResource(R.string.nav_software)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_software)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SETTINGS,
                            onClick = { currentScreen = AppScreen.SETTINGS },
                            icon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = stringResource(R.string.nav_settings)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_settings)) }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.ABOUT,
                            onClick = { currentScreen = AppScreen.ABOUT },
                            icon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = stringResource(R.string.nav_about)
                                )
                            },
                            label = { Text(stringResource(R.string.nav_about)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier.padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (currentScreen) {
                    AppScreen.SPLASH -> {
                        SplashScreen()
                    }

                    AppScreen.WIZARD -> {
                        WizardScreen(
                            downloadState = downloadState,
                            onStartInstallClick = {
                                viewModel.installUbuntu()
                            },
                            onConfigureAccountsClick = { rootPass, user, userPass ->
                                viewModel.configureWizardAccounts(rootPass, user, userPass)
                            },
                            onFinishWizardClick = { currentScreen = AppScreen.DASHBOARD }
                        )
                    }

                    AppScreen.DASHBOARD -> {
                        DashboardScreen(
                            state = dashboardState,
                            onInstallClick = { currentScreen = AppScreen.WIZARD },
                            onOpenTerminalClick = {
                                viewModel.startTerminalSession()
                                currentScreen = AppScreen.TERMINAL
                            },
                            onStopSessionClick = { viewModel.stopTerminalSession() },
                            onRunPresetClick = { cmd ->
                                viewModel.sendTerminalCommand(cmd)
                                currentScreen = AppScreen.TERMINAL
                            }
                        )
                    }

                    AppScreen.TERMINAL -> {
                        TerminalScreen(
                            terminalBridge = viewModel.terminalBridge,
                            onStartSession = { viewModel.startTerminalSession() },
                            onStopSession = { viewModel.stopTerminalSession() }
                        )
                    }

                    AppScreen.SOFTWARE_HUB -> {
                        SoftwareHubScreen(
                            packages = packages,
                            onInstallPackageClick = { pkgId ->
                                viewModel.installSoftwarePackage(pkgId)
                            },
                            onInstallCustomPackageClick = { packageName ->
                                viewModel.installCustomPackage(packageName)
                            },
                            onLaunchPackageClick = { cmd ->
                                viewModel.sendTerminalCommand(cmd)
                                currentScreen = AppScreen.TERMINAL
                            }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        val terminalTheme by viewModel.terminalTheme.collectAsState()
                        SettingsScreen(
                            state = dashboardState,
                            backupState = backupState,
                            terminalTheme = terminalTheme,
                            onSelectTheme = { themeId -> viewModel.setTerminalTheme(themeId) },
                            onUpdateCustomTheme = { fg, bg, cursor, sel, ansi ->
                                viewModel.updateCustomTheme(fg, bg, cursor, sel, ansi)
                            },
                            onToggleBindSdCard = { viewModel.toggleBindSdCard() },
                            onWipeRootfsClick = { viewModel.wipeRootfs() },
                            onRefreshStatusClick = { viewModel.refreshStatus() },
                            onChangeRootPassword = { pass -> viewModel.changeRootPassword(pass) },
                            onCreateUser = { user, pass -> viewModel.createUser(user, pass) },
                            onDeleteUser = { user -> viewModel.deleteUser(user) },
                            onExportContainer = { cr, uri -> viewModel.exportContainer(cr, uri) },
                            onImportContainer = { cr, uri -> viewModel.importContainer(cr, uri) },
                            onDismissBackupStatus = { viewModel.dismissBackupStatus() }
                        )
                    }

                    AppScreen.ABOUT -> {
                        AboutScreen()
                    }
                }
            }
        }
    }
}
