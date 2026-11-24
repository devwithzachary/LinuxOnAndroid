package com.devwithzachary.completelinuxinstaller.ui.screens.container

import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.ListeningPortInfo
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.components.LogViewerDialog
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.OverviewTabContent
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.SettingsTabContent
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.SoftwareTabContent
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import java.io.File
import kotlinx.coroutines.launch

enum class ContainerDetailTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Rounded.Analytics),
    SOFTWARE("Software", Icons.Rounded.Apps),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(
    container: ContainerInstance,
    metrics: SystemResourceMetrics = SystemResourceMetrics(),
    packages: List<SoftwarePackage> = emptyList(),
    initialTab: ContainerDetailTab = ContainerDetailTab.OVERVIEW,
    isVncInstalled: Boolean = false,
    isNginxInstalled: Boolean = false,
    isSshInstalled: Boolean = false,
    isCodeServerInstalled: Boolean = false,
    isWebTerminalInstalled: Boolean = false,
    sshPort: Int = 2222,
    bindSdCard: Boolean = true,
    dnsServers: List<String> = listOf("8.8.8.8", "1.1.1.1"),
    containerUsers: List<String> = emptyList(),
    upgradeState: UpgradeState = UpgradeState.Idle,
    backupState: BackupState = BackupState.Idle,
    onBack: () -> Unit,
    onOpenTerminal: (containerId: String) -> Unit = {},
    onSetDefault: (containerId: String) -> Unit,
    onKillProcess: (pid: Int) -> Unit,
    onRunPresetCommand: (command: String) -> Unit,
    onInstallPackage: (packageId: String, containerId: String) -> Unit,
    onUninstallPackage: (packageId: String, containerId: String) -> Unit = { _, _ -> },
    onInstallCustomPackage: (packageName: String, containerId: String) -> Unit,
    onDeleteContainer: (containerId: String) -> Unit = {},
    onUpgradeRootfs: (containerId: String) -> Unit = {},
    onDismissUpgradeState: () -> Unit = {},
    onExportContainer: (contentResolver: ContentResolver, uri: Uri, containerId: String) -> Unit = { _, _, _ -> },
    onImportContainer: (contentResolver: ContentResolver, uri: Uri, containerId: String) -> Unit = { _, _, _ -> },
    onDismissBackupStatus: () -> Unit = {},
    onToggleBindSdCard: () -> Unit = {},
    onChangeRootPassword: (password: String, containerId: String) -> Unit = { _, _ -> },
    onVerifyRootPassword: suspend (password: String, containerId: String) -> Boolean = { _, _ -> true },
    onCreateUser: (username: String, password: String, isSudo: Boolean, containerId: String) -> Unit = { _, _, _, _ -> },
    onDeleteUser: (username: String, containerId: String) -> Unit = { _, _ -> },
    onSetDefaultUser: (username: String, containerId: String) -> Unit = { _, _ -> },
    onSetDnsServers: (servers: List<String>, containerId: String) -> Unit = { _, _ -> },
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, containerId: String, onComplete: (Int) -> Unit) -> Unit = { _, _, _, _ -> },
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenExternalDirectory: (context: Context, containerId: String) -> Unit = { _, _ -> },
    onGetExternalFiles: (containerId: String) -> List<File> = { emptyList() },
    onToggleWebTerminalLanAccess: (containerId: String, enabled: Boolean) -> Unit = { _, _ -> },
    onToggleCodeServerLanAccess: (containerId: String, enabled: Boolean) -> Unit = { _, _ -> },
    onSetCodeServerPassword: (password: String, containerId: String) -> Unit = { _, _ -> },
    onRegenerateCodeServerPassword: (containerId: String) -> Unit = {},
    onToggleVncLanAccess: (containerId: String, enabled: Boolean) -> Unit = { _, _ -> },
    onRegenerateVncPassword: (containerId: String) -> Unit = {},
    onSetVncPassword: (password: String, containerId: String) -> Unit = { _, _ -> },
    onRefreshMetrics: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal,
        pageCount = { ContainerDetailTab.entries.size }
    )

    LaunchedEffect(Unit) {
        onRefreshMetrics()
    }

    LaunchedEffect(initialTab) {
        if (pagerState.currentPage != initialTab.ordinal) {
            pagerState.scrollToPage(initialTab.ordinal)
        }
    }

    var activeLogPackageId by remember { mutableStateOf<String?>(null) }
    val activePackage = packages.find { it.id == activeLogPackageId }

    val vncInstalled = remember(container.rootDirPath, packages, isVncInstalled) {
        SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/vncserver") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/tigervncserver") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/startxfce4") ||
            packages.find { it.id == "xfce_desktop" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isVncInstalled)
    }

    val nginxInstalled = remember(container.rootDirPath, packages, isNginxInstalled) {
        SoftwarePackage.isBinaryPresent(container.rootDir, "usr/sbin/nginx") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/nginx") ||
            packages.find { it.id == "nginx_web" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isNginxInstalled)
    }

    val sshInstalled = remember(container.rootDirPath, packages, isSshInstalled) {
        SoftwarePackage.isBinaryPresent(container.rootDir, "usr/sbin/sshd") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/sshd") ||
            packages.find { it.id == "openssh_server" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isSshInstalled)
    }

    val codeServerInstalled = remember(container.rootDirPath, packages, isCodeServerInstalled) {
        SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/code-server") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/local/bin/code-server") ||
            packages.find { it.id == "code_server" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isCodeServerInstalled)
    }

    val webTerminalInstalled = remember(container.rootDirPath, packages, isWebTerminalInstalled) {
        SoftwarePackage.isBinaryPresent(container.rootDir, "usr/bin/ttyd") ||
            SoftwarePackage.isBinaryPresent(container.rootDir, "usr/local/bin/ttyd") ||
            packages.find { it.id == "web_terminal" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isWebTerminalInstalled)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = container.name,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = container.distroName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back to dashboard"
                            )
                        }
                    },
                    actions = {
                        if (container.isDefault) {
                            LoaMonospaceChip(
                                text = "default",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        } else {
                            TextButton(onClick = { onSetDefault(container.id) }) {
                                Text("Set default")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                ) {
                    ContainerDetailTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tab.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            key = { ContainerDetailTab.entries[it].name }
        ) { page ->
            when (ContainerDetailTab.entries[page]) {
                ContainerDetailTab.OVERVIEW -> {
                    OverviewTabContent(
                        container = container,
                        metrics = metrics,
                        sshPort = sshPort,
                        isVncInstalled = vncInstalled,
                        isNginxInstalled = nginxInstalled,
                        isSshInstalled = sshInstalled,
                        isCodeServerInstalled = codeServerInstalled,
                        isWebTerminalInstalled = webTerminalInstalled,
                        onOpenTerminal = { onOpenTerminal(container.id) },
                        onKillProcess = onKillProcess,
                        onRunPresetCommand = onRunPresetCommand,
                        onNavigateToSoftwareTab = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(ContainerDetailTab.SOFTWARE.ordinal)
                            }
                        },
                        onRefreshMetrics = onRefreshMetrics
                    )
                }

                ContainerDetailTab.SOFTWARE -> {
                    SoftwareTabContent(
                        container = container,
                        packages = packages,
                        onInstallPackage = { pkgId -> onInstallPackage(pkgId, container.id) },
                        onUninstallPackage = { pkgId -> onUninstallPackage(pkgId, container.id) },
                        onInstallCustomPackage = { pkgName -> onInstallCustomPackage(pkgName, container.id) },
                        onViewLogs = { pkgId -> activeLogPackageId = pkgId },
                        onRunPresetCommand = onRunPresetCommand
                    )
                }

                ContainerDetailTab.SETTINGS -> {
                    SettingsTabContent(
                        container = container,
                        bindSdCard = bindSdCard,
                        dnsServers = dnsServers,
                        containerUsers = containerUsers,
                        upgradeState = upgradeState,
                        backupState = backupState,
                        onUpgradeRootfs = { onUpgradeRootfs(container.id) },
                        onDismissUpgradeState = onDismissUpgradeState,
                        onExportContainer = { cr, uri -> onExportContainer(cr, uri, container.id) },
                        onImportContainer = { cr, uri -> onImportContainer(cr, uri, container.id) },
                        onDismissBackupStatus = onDismissBackupStatus,
                        onToggleBindSdCard = onToggleBindSdCard,
                        onChangeRootPassword = { pwd -> onChangeRootPassword(pwd, container.id) },
                        onVerifyRootPassword = { pwd -> onVerifyRootPassword(pwd, container.id) },
                        onCreateUser = { u, p, sudo -> onCreateUser(u, p, sudo, container.id) },
                        onDeleteUser = { u -> onDeleteUser(u, container.id) },
                        onSetDefaultUser = { u -> onSetDefaultUser(u, container.id) },
                        onSetDnsServers = { s -> onSetDnsServers(s, container.id) },
                        onImportFiles = { cr, uris, onComplete -> onImportFiles(cr, uris, container.id, onComplete) },
                        onExportFile = { cr, file, targetUri, onComplete -> onExportFile(cr, file, targetUri, onComplete) },
                        onOpenExternalDirectory = { ctx -> onOpenExternalDirectory(ctx, container.id) },
                        onGetExternalFiles = { onGetExternalFiles(container.id) },
                        onToggleWebTerminalLanAccess = { enabled -> onToggleWebTerminalLanAccess(container.id, enabled) },
                        onToggleCodeServerLanAccess = { enabled -> onToggleCodeServerLanAccess(container.id, enabled) },
                        onSetCodeServerPassword = { pwd -> onSetCodeServerPassword(pwd, container.id) },
                        onRegenerateCodeServerPassword = { onRegenerateCodeServerPassword(container.id) },
                        onToggleVncLanAccess = { enabled -> onToggleVncLanAccess(container.id, enabled) },
                        onRegenerateVncPassword = { onRegenerateVncPassword(container.id) },
                        onSetVncPassword = { pwd -> onSetVncPassword(pwd, container.id) },
                        onDeleteContainer = { onDeleteContainer(container.id) }
                    )
                }
            }
        }
    }

    if (activePackage != null) {
        LogViewerDialog(
            pkg = activePackage,
            onDismiss = { activeLogPackageId = null }
        )
    }
}

private val previewContainer = ContainerInstance(
    id = "ubuntu_default",
    name = "Ubuntu Workstation",
    distroId = "ubuntu",
    distroName = "Ubuntu 26.04 LTS (arm64)",
    rootDirPath = "/data/user/0/com.devwithzachary.completelinuxinstaller/files/ubuntu",
    isDefault = true,
    storageUsedMb = 1480L,
    defaultUser = "zachary"
)

private val previewMetrics = SystemResourceMetrics(
    containerMemoryUsedMb = 384L,
    systemTotalRamMb = 8192L,
    systemAvailableRamMb = 4800L,
    storageUsedMb = 1480L,
    storageTotalBytes = 128L * 1024 * 1024 * 1024,
    storageAvailableBytes = 42L * 1024 * 1024 * 1024,
    listeningPorts = listOf(
        ListeningPortInfo(port = 2222, serviceName = "OpenSSH Server", isWebAccessible = false),
        ListeningPortInfo(port = 8080, serviceName = "VS Code Server", isWebAccessible = true)
    ),
    isSessionRunning = true
)

private val previewPackages = listOf(
    SoftwarePackage(
        id = "openssh_server",
        name = "OpenSSH Server",
        category = SoftwareCategory.NETWORKING,
        description = "Secure remote shell and SFTP server.",
        iconName = "Security",
        installCommand = "apt-get install -y openssh-server",
        status = InstallStatus.INSTALLED,
        launchCommand = "service ssh start"
    ),
    SoftwarePackage(
        id = "xfce_desktop",
        name = "XFCE Desktop",
        category = SoftwareCategory.DESKTOP_GUI,
        description = "Lightweight graphical desktop over TigerVNC.",
        iconName = "DesktopWindows",
        installCommand = "apt-get install -y xfce4 tigervnc-standalone-server",
        status = InstallStatus.NOT_INSTALLED,
        isBeta = true
    )
)

@Preview(
    name = "Container Overview - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ContainerOverviewLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        ContainerDetailScreen(
            container = previewContainer,
            metrics = previewMetrics,
            packages = previewPackages,
            initialTab = ContainerDetailTab.OVERVIEW,
            isSshInstalled = true,
            onBack = {},
            onSetDefault = {},
            onKillProcess = {},
            onRunPresetCommand = {},
            onInstallPackage = { _, _ -> },
            onInstallCustomPackage = { _, _ -> }
        )
    }
}

@Preview(
    name = "Container Overview - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContainerOverviewDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        ContainerDetailScreen(
            container = previewContainer,
            metrics = previewMetrics,
            packages = previewPackages,
            initialTab = ContainerDetailTab.OVERVIEW,
            isSshInstalled = true,
            onBack = {},
            onSetDefault = {},
            onKillProcess = {},
            onRunPresetCommand = {},
            onInstallPackage = { _, _ -> },
            onInstallCustomPackage = { _, _ -> }
        )
    }
}

@Preview(
    name = "Container Software - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContainerSoftwareDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        ContainerDetailScreen(
            container = previewContainer,
            metrics = previewMetrics,
            packages = previewPackages,
            initialTab = ContainerDetailTab.SOFTWARE,
            onBack = {},
            onSetDefault = {},
            onKillProcess = {},
            onRunPresetCommand = {},
            onInstallPackage = { _, _ -> },
            onInstallCustomPackage = { _, _ -> }
        )
    }
}

@Preview(
    name = "Container Settings - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ContainerSettingsLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        ContainerDetailScreen(
            container = previewContainer,
            metrics = previewMetrics,
            packages = previewPackages,
            initialTab = ContainerDetailTab.SETTINGS,
            containerUsers = listOf("root", "zachary"),
            onBack = {},
            onSetDefault = {},
            onKillProcess = {},
            onRunPresetCommand = {},
            onInstallPackage = { _, _ -> },
            onInstallCustomPackage = { _, _ -> }
        )
    }
}
