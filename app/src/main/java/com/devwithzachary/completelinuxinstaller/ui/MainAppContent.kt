package com.devwithzachary.completelinuxinstaller.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.ui.screens.about.AboutScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.container.ContainerDetailScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.container.DeletingContainerScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.dashboard.DashboardScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.SettingsScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.splash.SplashScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.terminal.TerminalScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.welcome.WelcomeScreen
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.WizardScreen

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.util.DesktopKeyHandler
import com.devwithzachary.completelinuxinstaller.ui.util.rememberWindowSizeClass

enum class AppScreen(val titleRes: Int) {
    SPLASH(R.string.app_name),
    WELCOME(R.string.app_name),
    DASHBOARD(R.string.nav_dashboard),
    WIZARD(R.string.app_title),
    TERMINAL(R.string.nav_terminal),
    SETTINGS(R.string.nav_settings),
    ABOUT(R.string.nav_about)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val windowSizeClass = rememberWindowSizeClass()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val deleteContainerState by viewModel.deleteContainerState.collectAsStateWithLifecycle()
    val hasSeenWelcome by viewModel.hasSeenWelcome.collectAsStateWithLifecycle()
    val packages by viewModel.packages.collectAsStateWithLifecycle()
    val requestedScreen by viewModel.requestedScreen.collectAsStateWithLifecycle()
    val systemMetrics by viewModel.systemMetrics.collectAsStateWithLifecycle()
    val containerMetrics by viewModel.containerMetrics.collectAsStateWithLifecycle()

    val isInitializing = dashboardState.isInitializing
    val isInstalled = dashboardState.isInstalled
    val splashDismissed = dashboardState.splashDismissed
    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var selectedContainerTarget by remember { mutableStateOf<Pair<String, com.devwithzachary.completelinuxinstaller.ui.screens.container.ContainerDetailTab>?>(null) }

    LaunchedEffect(requestedScreen) {
        requestedScreen?.let { target ->
            currentScreen = target
            viewModel.clearRequestedScreen()
        }
    }

    // Sync screen navigation state when initialization completes or installation status is confirmed
    LaunchedEffect(isInitializing, isInstalled, splashDismissed, hasSeenWelcome) {
        if (!isInitializing) {
            if (!isInstalled && !splashDismissed) {
                if (!hasSeenWelcome) {
                    currentScreen = AppScreen.WELCOME
                } else {
                    currentScreen = AppScreen.WIZARD
                }
            } else if (currentScreen == AppScreen.SPLASH || currentScreen == AppScreen.WELCOME || currentScreen == AppScreen.WIZARD) {
                currentScreen = AppScreen.DASHBOARD
            }
        }
    }

    if (isInitializing || currentScreen == AppScreen.SPLASH) {
        SplashRoute(viewModel, dashboardState)
    } else if (deleteContainerState !is DeleteContainerState.Idle) {
        DeletingContainerScreen(
            state = deleteContainerState,
            onDismissError = {
                viewModel.dismissDeleteContainerError()
                selectedContainerTarget = null
                currentScreen = AppScreen.DASHBOARD
            }
        )
    } else {
        val isTerminal = currentScreen == AppScreen.TERMINAL
        val isFullscreenScreen = currentScreen == AppScreen.WIZARD || currentScreen == AppScreen.WELCOME

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { keyEvent ->
                    DesktopKeyHandler.handleGlobalShortcut(
                        event = keyEvent,
                        onNavigate = { target ->
                            if (target == AppScreen.TERMINAL && !dashboardState.isRunning) {
                                viewModel.startTerminalSession()
                            }
                            selectedContainerTarget = null
                            currentScreen = target
                        },
                        onNewTerminalTab = {
                            val container = dashboardState.containers.find { it.id == dashboardState.defaultContainerId }
                                ?: dashboardState.containers.firstOrNull()
                            viewModel.createNewTab(container?.id, container?.defaultUser ?: "root", null)
                        },
                        onCloseTerminalTab = {
                            viewModel.closeActiveTab()
                        },
                        onNextTerminalTab = {
                            viewModel.nextTab()
                        },
                        onPrevTerminalTab = {
                            viewModel.prevTab()
                        },
                        currentScreen = currentScreen
                    )
                }
        ) {
            Scaffold(
                containerColor = if (isTerminal) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.background,
                contentWindowInsets = if (isTerminal) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
                bottomBar = {
                    val isImeVisible = WindowInsets.isImeVisible
                    if (windowSizeClass.isCompact && (isInstalled || splashDismissed) && !isFullscreenScreen && !(currentScreen == AppScreen.TERMINAL && isImeVisible)) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = {
                                    selectedContainerTarget = null
                                    currentScreen = AppScreen.DASHBOARD
                                },
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = if (windowSizeClass.isCompact) innerPadding.calculateBottomPadding() else 0.dp)
                ) {
                    if (windowSizeClass.isMediumOrExpanded && (isInstalled || splashDismissed) && !isFullscreenScreen) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(top = if (isTerminal) 0.dp else innerPadding.calculateTopPadding()),
                            containerColor = if (isTerminal) Color(0xFF242424) else NavigationRailDefaults.ContainerColor,
                            contentColor = if (isTerminal) Color.White else MaterialTheme.colorScheme.onSurface,
                            header = {
                                IconButton(
                                    onClick = {
                                        selectedContainerTarget = null
                                        currentScreen = AppScreen.DASHBOARD
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_launcher_logo),
                                        contentDescription = stringResource(R.string.app_title),
                                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                                        tint = Color.Unspecified
                                    )
                                }
                            }
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            NavigationRailItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = {
                                    selectedContainerTarget = null
                                    currentScreen = AppScreen.DASHBOARD
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Home,
                                        contentDescription = stringResource(R.string.nav_dashboard)
                                    )
                                },
                                label = { Text(stringResource(R.string.nav_dashboard)) }
                            )
                            NavigationRailItem(
                                selected = currentScreen == AppScreen.TERMINAL,
                                onClick = { currentScreen = AppScreen.TERMINAL },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (dashboardState.isRunning) {
                                                Badge(containerColor = Color(0xFF4CAF50))
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Terminal,
                                            contentDescription = stringResource(R.string.nav_terminal)
                                        )
                                    }
                                },
                                label = { Text(stringResource(R.string.nav_terminal)) }
                            )
                            NavigationRailItem(
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
                            NavigationRailItem(
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

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(top = if (isTerminal) 0.dp else innerPadding.calculateTopPadding()),
                        color = if (isTerminal) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.background
                    ) {
                        when (currentScreen) {
                    AppScreen.SPLASH -> {
                        SplashRoute(viewModel, dashboardState)
                    }

                    AppScreen.WELCOME -> {
                        WelcomeScreen(
                            onGetStarted = {
                                viewModel.completeWelcome()
                                currentScreen = AppScreen.WIZARD
                            },
                            hasExistingContainers = isInstalled || dashboardState.containers.isNotEmpty(),
                            onBack = if (isInstalled || dashboardState.containers.isNotEmpty()) {
                                { currentScreen = AppScreen.ABOUT }
                            } else null
                        )
                    }

                    AppScreen.WIZARD -> {
                        WizardScreen(
                            downloadState = downloadState,
                            hasExistingContainers = isInstalled || dashboardState.containers.isNotEmpty(),
                            onStartInstallDistro = { distroDef, containerName, rootPass, user, userPass ->
                                viewModel.installDistro(distroDef, containerName, rootPass, user, userPass)
                            },
                            onConfigureAccountsClick = { rootPass, user, userPass ->
                                viewModel.configureWizardAccounts(rootPass, user, userPass)
                            },
                            onFinishWizardClick = {
                                viewModel.resetWizardState()
                                currentScreen = AppScreen.DASHBOARD
                            },
                            onCancel = {
                                viewModel.resetWizardState()
                                currentScreen = AppScreen.DASHBOARD
                            }
                        )
                    }

                    AppScreen.DASHBOARD -> {
                        val activeDetailContainer = selectedContainerTarget?.let { (id, _) ->
                            dashboardState.containers.find { it.id == id }
                        }

                        if (activeDetailContainer != null) {
                            BackHandler {
                                selectedContainerTarget = null
                            }
                            val sshPort by viewModel.sshPort.collectAsStateWithLifecycle()
                            val initialTab = selectedContainerTarget?.second ?: com.devwithzachary.completelinuxinstaller.ui.screens.container.ContainerDetailTab.OVERVIEW
                            val upgradeState by viewModel.upgradeState.collectAsStateWithLifecycle()
                            val dnsServers by viewModel.dnsServers.collectAsStateWithLifecycle()
                            val detailPackages = remember(activeDetailContainer.id, packages) {
                                viewModel.getPackagesForContainer(activeDetailContainer.id)
                            }
                            val activeContainerMetrics = containerMetrics[activeDetailContainer.id]
                                ?: SystemResourceMetrics(storageUsedMb = activeDetailContainer.storageUsedMb)
                            ContainerDetailScreen(
                                container = activeDetailContainer,
                                metrics = activeContainerMetrics,
                                packages = detailPackages,
                                initialTab = initialTab,
                                isVncInstalled = dashboardState.isVncInstalled,
                                isNginxInstalled = dashboardState.isNginxInstalled,
                                isSshInstalled = dashboardState.isSshInstalled,
                                sshPort = sshPort,
                                bindSdCard = dashboardState.bindSdCard,
                                dnsServers = dnsServers,
                                containerUsers = viewModel.getContainerUsers(activeDetailContainer.id),
                                upgradeState = upgradeState,
                                backupState = backupState,
                                onBack = { selectedContainerTarget = null },
                                onOpenTerminal = { containerId ->
                                    viewModel.startTerminalSessionForContainer(containerId)
                                    currentScreen = AppScreen.TERMINAL
                                },
                                onSetDefault = { containerId ->
                                    viewModel.setDefaultContainer(containerId)
                                },
                                onKillProcess = { pid -> viewModel.killProcess(pid) },
                                onRunPresetCommand = { cmd ->
                                    viewModel.sendTerminalCommand(cmd, activeDetailContainer.id)
                                    currentScreen = AppScreen.TERMINAL
                                },
                                onInstallPackage = { pkgId, containerId ->
                                    viewModel.installSoftwarePackage(pkgId, containerId)
                                },
                                onInstallCustomPackage = { pkgName, containerId ->
                                    viewModel.installCustomPackage(pkgName, containerId)
                                },
                                onDeleteContainer = { containerId ->
                                    viewModel.deleteContainer(containerId) {
                                        selectedContainerTarget = null
                                    }
                                },
                                onUpgradeRootfs = { containerId ->
                                    viewModel.upgradeRootfs(containerId)
                                },
                                onDismissUpgradeState = {
                                    viewModel.dismissUpgradeState()
                                },
                                onExportContainer = { cr, uri, containerId ->
                                    viewModel.exportContainer(cr, uri, containerId)
                                },
                                onImportContainer = { cr, uri, containerId ->
                                    viewModel.importContainer(cr, uri, containerId)
                                },
                                onDismissBackupStatus = {
                                    viewModel.dismissBackupStatus()
                                },
                                onToggleBindSdCard = {
                                    viewModel.toggleBindSdCard()
                                },
                                onChangeRootPassword = { pwd, containerId ->
                                    viewModel.changeRootPassword(pwd, containerId)
                                },
                                onCreateUser = { u, p, sudo, containerId ->
                                    viewModel.createUser(u, p, sudo, containerId)
                                },
                                onDeleteUser = { u, containerId ->
                                    viewModel.deleteUser(u, containerId)
                                },
                                onSetDefaultUser = { u, containerId ->
                                    viewModel.setContainerDefaultUser(u, containerId)
                                },
                                onSetDnsServers = { s, containerId ->
                                    viewModel.setDnsServers(s, containerId)
                                },
                                onRefreshMetrics = {
                                    viewModel.triggerMetricsRefresh()
                                }
                            )
                        } else {
                            DashboardScreen(
                                state = dashboardState,
                                metrics = systemMetrics,
                                containerMetrics = containerMetrics,
                                onInstallClick = {
                                    viewModel.resetWizardState()
                                    currentScreen = AppScreen.WIZARD
                                },
                                onOpenTerminalClick = {
                                    viewModel.startTerminalSession()
                                    currentScreen = AppScreen.TERMINAL
                                },
                                onOpenContainerTerminalClick = { containerId ->
                                    viewModel.startTerminalSessionForContainer(containerId)
                                    currentScreen = AppScreen.TERMINAL
                                },
                                onContainerClick = { containerId, tab ->
                                    selectedContainerTarget = Pair(containerId, tab)
                                },
                                onSetDefaultContainerClick = { containerId ->
                                    viewModel.setDefaultContainer(containerId)
                                }
                            )
                        }
                    }

                    AppScreen.TERMINAL -> {
                        val defaultUser by viewModel.defaultTerminalUser.collectAsState()
                        val fontSize by viewModel.terminalFontSize.collectAsState()
                        val fontFamily by viewModel.terminalFontFamily.collectAsState()
                        val isKeepScreenOnEnabled by viewModel.isKeepScreenOnEnabled.collectAsState()
                        TerminalScreen(
                            terminalBridge = viewModel.terminalBridge,
                            containers = dashboardState.containers,
                            defaultContainerId = dashboardState.defaultContainerId,
                            onStartSession = { viewModel.startTerminalSession() },
                            onStopSession = { viewModel.stopTerminalSession() },
                            onCreateTab = { containerId, user, title -> viewModel.createNewTab(containerId, user, title) },
                            onSwitchTab = { sessionId -> viewModel.switchTab(sessionId) },
                            onCloseTab = { sessionId -> viewModel.closeTab(sessionId) },
                            onRenameTab = { sessionId, newTitle -> viewModel.renameTab(sessionId, newTitle) },
                            defaultLoginUser = defaultUser,
                            fontSizeSp = fontSize,
                            fontFamilyName = fontFamily,
                            isKeepScreenOnEnabled = isKeepScreenOnEnabled
                        )
                    }

                    AppScreen.SETTINGS -> {
                        val terminalTheme by viewModel.terminalTheme.collectAsState()
                        val fontSize by viewModel.terminalFontSize.collectAsState()
                        val fontFamily by viewModel.terminalFontFamily.collectAsState()
                        val isKeepAliveEnabled by viewModel.isKeepAliveEnabled.collectAsState()
                        val isKeepScreenOnEnabled by viewModel.isKeepScreenOnEnabled.collectAsState()
                        val isGitHubUpdateCheckEnabled by viewModel.isGitHubUpdateCheckEnabled.collectAsStateWithLifecycle()
                        val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsStateWithLifecycle()
                        val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
                        SettingsScreen(
                            state = dashboardState,
                            terminalTheme = terminalTheme,
                            terminalFontSize = fontSize,
                            terminalFontFamily = fontFamily,
                            onSelectTheme = { themeId -> viewModel.setTerminalTheme(themeId) },
                            onUpdateCustomTheme = { fg, bg, cursor, sel, ansi ->
                                viewModel.updateCustomTheme(fg, bg, cursor, sel, ansi)
                            },
                            onSetTerminalFontSize = { size -> viewModel.setTerminalFontSize(size) },
                            onSetTerminalFontFamily = { family -> viewModel.setTerminalFontFamily(family) },
                            onGenerateDebugReport = { viewModel.generateDebugReport() },
                            isKeepAliveEnabled = isKeepAliveEnabled,
                            onToggleKeepAlive = { viewModel.toggleKeepAlive() },
                            isKeepScreenOnEnabled = isKeepScreenOnEnabled,
                            onSetKeepScreenOn = { enabled -> viewModel.setKeepScreenOnEnabled(enabled) },
                            isGitHubUpdateCheckEnabled = isGitHubUpdateCheckEnabled,
                            onSetGitHubUpdateCheckEnabled = { enabled -> viewModel.setGitHubUpdateCheckEnabled(enabled) },
                            isCheckingForUpdates = isCheckingForUpdates,
                            updateCheckResult = updateCheckResult,
                            onCheckForUpdatesClick = { viewModel.checkForGitHubUpdates(manual = true) }
                        )
                    }

                    AppScreen.ABOUT -> {
                        val isCheckingForUpdates by viewModel.isCheckingForUpdates.collectAsStateWithLifecycle()
                        val updateCheckResult by viewModel.updateCheckResult.collectAsStateWithLifecycle()
                        AboutScreen(
                            onCheckForUpdatesClick = { viewModel.checkForGitHubUpdates(manual = true) },
                            isCheckingForUpdates = isCheckingForUpdates,
                            updateCheckResult = updateCheckResult,
                            onViewAppOverview = { currentScreen = AppScreen.WELCOME }
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
private fun SplashRoute(
    viewModel: MainViewModel,
    dashboardState: DashboardUiState
) {
    SplashScreen(
        initSlow = dashboardState.isInitSlow,
        elapsedSeconds = (dashboardState.initElapsedMs / 1000).toInt(),
        onRetry = { viewModel.retryInit() },
        onContinueAnyway = { viewModel.dismissSplash() }
    )
}
