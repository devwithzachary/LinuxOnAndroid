package com.devwithzachary.completelinuxinstaller.ui.screens.container

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.components.LogViewerDialog
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.OverviewTabContent
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.SettingsTabContent
import com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs.SoftwareTabContent
import java.io.File
import kotlinx.coroutines.launch

enum class ContainerDetailTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Analytics),
    SOFTWARE("Software", Icons.Default.Apps),
    SETTINGS("Settings", Icons.Default.Settings)
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
    onInstallCustomPackage: (packageName: String, containerId: String) -> Unit,
    onDeleteContainer: (containerId: String) -> Unit = {},
    onUpgradeRootfs: (containerId: String) -> Unit = {},
    onDismissUpgradeState: () -> Unit = {},
    onExportContainer: (contentResolver: ContentResolver, uri: Uri, containerId: String) -> Unit = { _, _, _ -> },
    onImportContainer: (contentResolver: ContentResolver, uri: Uri, containerId: String) -> Unit = { _, _, _ -> },
    onDismissBackupStatus: () -> Unit = {},
    onToggleBindSdCard: () -> Unit = {},
    onChangeRootPassword: (password: String, containerId: String) -> Unit = { _, _ -> },
    onCreateUser: (username: String, password: String, isSudo: Boolean, containerId: String) -> Unit = { _, _, _, _ -> },
    onDeleteUser: (username: String, containerId: String) -> Unit = { _, _ -> },
    onSetDefaultUser: (username: String, containerId: String) -> Unit = { _, _ -> },
    onSetDnsServers: (servers: List<String>, containerId: String) -> Unit = { _, _ -> },
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, containerId: String, onComplete: (Int) -> Unit) -> Unit = { _, _, _, _ -> },
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit = { _, _, _, _ -> },
    onOpenExternalDirectory: (context: Context, containerId: String) -> Unit = { _, _ -> },
    onGetExternalFiles: (containerId: String) -> List<File> = { emptyList() },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Container Top Header Card (Sticky Header)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Containers")
                        }
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color(container.colorHex), shape = CircleShape)
                        )
                        Column {
                            Text(
                                text = container.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = container.distroName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (container.isDefault) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text(
                                text = "DEFAULT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSetDefault(container.id) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Set Default", fontSize = 11.sp)
                        }
                    }
                }

                // Three Tabs: Overview, Software, and Settings with sliding indicator
                PrimaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent,
                    divider = {}
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
                                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

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

        // Horizontal Pager with continuous sliding animation and swipe gesture support
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
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
                        onCreateUser = { u, p, sudo -> onCreateUser(u, p, sudo, container.id) },
                        onDeleteUser = { u -> onDeleteUser(u, container.id) },
                        onSetDefaultUser = { u -> onSetDefaultUser(u, container.id) },
                        onSetDnsServers = { s -> onSetDnsServers(s, container.id) },
                        onImportFiles = { cr, uris, onComplete -> onImportFiles(cr, uris, container.id, onComplete) },
                        onExportFile = { cr, file, targetUri, onComplete -> onExportFile(cr, file, targetUri, onComplete) },
                        onOpenExternalDirectory = { ctx -> onOpenExternalDirectory(ctx, container.id) },
                        onGetExternalFiles = { onGetExternalFiles(container.id) },
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
