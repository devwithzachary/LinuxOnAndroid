package com.devwithzachary.completelinuxinstaller.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import androidx.compose.material.icons.filled.Info
import com.devwithzachary.completelinuxinstaller.ui.screens.about.AboutScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.dashboard.DashboardScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.hub.SoftwareHubScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.SettingsScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.splash.SplashScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.WizardScreen

enum class AppScreen(val title: String) {
    SPLASH("Splash"),
    DASHBOARD("Dashboard"),
    WIZARD("Installer Wizard"),
    TERMINAL("Terminal"),
    SOFTWARE_HUB("Software Hub"),
    SETTINGS("Settings"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val isSessionRunning by viewModel.isSessionRunning.collectAsStateWithLifecycle()

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
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (!isInstalled) "Complete Linux Installer" else currentScreen.title,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            },
            bottomBar = {
                if (isInstalled && currentScreen != AppScreen.WIZARD) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.DASHBOARD,
                            onClick = { currentScreen = AppScreen.DASHBOARD },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                            label = { Text("Dashboard") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.TERMINAL,
                            onClick = { currentScreen = AppScreen.TERMINAL },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = "Terminal") },
                            label = { Text("Terminal") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SOFTWARE_HUB,
                            onClick = { currentScreen = AppScreen.SOFTWARE_HUB },
                            icon = { Icon(Icons.Default.Apps, contentDescription = "Hub") },
                            label = { Text("Software") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SETTINGS,
                            onClick = { currentScreen = AppScreen.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.ABOUT,
                            onClick = { currentScreen = AppScreen.ABOUT },
                            icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                            label = { Text("About") }
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
                        SettingsScreen(
                            state = dashboardState,
                            onToggleBindSdCard = { viewModel.toggleBindSdCard() },
                            onWipeRootfsClick = { viewModel.wipeRootfs() },
                            onRefreshStatusClick = { viewModel.refreshStatus() },
                            onChangeRootPassword = { pass -> viewModel.changeRootPassword(pass) },
                            onCreateUser = { user, pass -> viewModel.createUser(user, pass) },
                            onDeleteUser = { user -> viewModel.deleteUser(user) }
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
