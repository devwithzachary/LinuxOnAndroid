package com.devwithzachary.completelinuxinstaller.ui.screens.container

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.devwithzachary.completelinuxinstaller.engine.SystemResourceMetrics
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.components.ActiveProcessTableCard
import com.devwithzachary.completelinuxinstaller.ui.components.DashboardGaugesCard
import com.devwithzachary.completelinuxinstaller.ui.components.LogViewerDialog
import com.devwithzachary.completelinuxinstaller.ui.components.NetworkListenerCard
import com.devwithzachary.completelinuxinstaller.ui.components.SoftwareCard
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
            File(container.rootDir, "usr/bin/vncserver").exists() ||
            File(container.rootDir, "usr/bin/tigervncserver").exists() ||
            File(container.rootDir, "usr/bin/startxfce4").exists() ||
            packages.find { it.id == "xfce_desktop" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isVncInstalled)
        }

        val nginxInstalled = remember(container.rootDirPath, packages, isNginxInstalled) {
            File(container.rootDir, "usr/sbin/nginx").exists() ||
            File(container.rootDir, "usr/bin/nginx").exists() ||
            packages.find { it.id == "nginx_web" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isNginxInstalled)
        }

        val sshInstalled = remember(container.rootDirPath, packages, isSshInstalled) {
            File(container.rootDir, "usr/sbin/sshd").exists() ||
            File(container.rootDir, "usr/bin/sshd").exists() ||
            packages.find { it.id == "openssh_server" }?.status == InstallStatus.INSTALLED ||
            (container.isDefault && isSshInstalled)
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

@Composable
private fun OverviewTabContent(
    container: ContainerInstance,
    metrics: SystemResourceMetrics,
    sshPort: Int,
    isVncInstalled: Boolean,
    isNginxInstalled: Boolean,
    isSshInstalled: Boolean,
    onKillProcess: (pid: Int) -> Unit,
    onRunPresetCommand: (command: String) -> Unit,
    onNavigateToSoftwareTab: () -> Unit,
    onRefreshMetrics: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var servicePrompt by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Live System Resources & Gauges Card
        DashboardGaugesCard(
            metrics = metrics.copy(storageUsedMb = container.storageUsedMb)
        )

        // 2. One-Touch Container Services
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RocketLaunch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Container Services & Launchers",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Launch background servers and graphical desktop sessions inside this rootfs container.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // VNC Launcher
                    ServiceLauncherButton(
                        icon = Icons.Default.DesktopWindows,
                        label = "VNC",
                        isInstalled = isVncInstalled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isVncInstalled) {
                                val vncPkg = SoftwarePackage.getPresets().find { it.id == "xfce_desktop" }
                                vncPkg?.launchCommand?.let { onRunPresetCommand(it) }
                            } else {
                                servicePrompt = Pair("TigerVNC & XFCE Desktop", "xfce_desktop")
                            }
                        }
                    )

                    // NGINX Launcher
                    ServiceLauncherButton(
                        icon = Icons.Default.Public,
                        label = "NGINX",
                        isInstalled = isNginxInstalled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isNginxInstalled) {
                                val nginxPkg = SoftwarePackage.getPresets().find { it.id == "nginx_web" }
                                nginxPkg?.launchCommand?.let { onRunPresetCommand(it) }
                            } else {
                                servicePrompt = Pair("NGINX Web Server", "nginx_web")
                            }
                        }
                    )

                    // SSH Launcher
                    ServiceLauncherButton(
                        icon = Icons.Default.VpnKey,
                        label = "SSH",
                        isInstalled = isSshInstalled,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSshInstalled) {
                                val sshPkg = SoftwarePackage.getPresets(sshPort).find { it.id == "openssh_server" }
                                sshPkg?.launchCommand?.let { onRunPresetCommand(it) }
                            } else {
                                servicePrompt = Pair("OpenSSH Server", "openssh_server")
                            }
                        }
                    )
                }
            }
        }

        // 3. Live Active Container Processes Table (`ps aux`)
        ActiveProcessTableCard(
            processes = metrics.processes,
            onKillProcess = onKillProcess
        )

        // 4. Open Listening Ports Card
        NetworkListenerCard(
            ports = metrics.listeningPorts,
            onRefresh = onRefreshMetrics
        )
    }

    if (servicePrompt != null) {
        val (serviceTitle, _) = servicePrompt!!
        AlertDialog(
            onDismissRequest = { servicePrompt = null },
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("Service Not Installed") },
            text = {
                Text("$serviceTitle is not installed in ${container.name}.\n\nWould you like to open the Software tab to install it?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        servicePrompt = null
                        onNavigateToSoftwareTab()
                    }
                ) {
                    Text("Go to Software")
                }
            },
            dismissButton = {
                TextButton(onClick = { servicePrompt = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ServiceLauncherButton(
    icon: ImageVector,
    label: String,
    isInstalled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isInstalled) {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SoftwareTabContent(
    container: ContainerInstance,
    packages: List<SoftwarePackage>,
    onInstallPackage: (packageId: String) -> Unit,
    onInstallCustomPackage: (packageName: String) -> Unit,
    onViewLogs: (packageId: String) -> Unit,
    onRunPresetCommand: (command: String) -> Unit
) {
    var selectedSoftwareCategory by remember { mutableStateOf<SoftwareCategory?>(null) }
    var customPackageInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredPackages = packages.filter { pkg ->
        selectedSoftwareCategory == null || pkg.category == selectedSoftwareCategory
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Custom Package Quick Search & Install Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Install Custom Package",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Enter any package name to download and configure directly into ${container.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPackageInput,
                            onValueChange = { customPackageInput = it },
                            placeholder = { Text("e.g. htop, neofetch, git, vim", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (customPackageInput.isNotBlank()) {
                                    onInstallCustomPackage(customPackageInput.trim())
                                    keyboardController?.hide()
                                    customPackageInput = ""
                                }
                            })
                        )
                        Button(
                            onClick = {
                                if (customPackageInput.isNotBlank()) {
                                    onInstallCustomPackage(customPackageInput.trim())
                                    keyboardController?.hide()
                                    customPackageInput = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Install")
                        }
                    }
                }
            }
        }

        // 2. Category Filter Tabs
        item {
            SecondaryScrollableTabRow(
                selectedTabIndex = if (selectedSoftwareCategory == null) 0 else SoftwareCategory.entries.indexOf(selectedSoftwareCategory) + 1,
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(
                    selected = selectedSoftwareCategory == null,
                    onClick = { selectedSoftwareCategory = null },
                    text = { Text("All (${packages.size})") }
                )
                SoftwareCategory.entries.forEach { cat ->
                    Tab(
                        selected = selectedSoftwareCategory == cat,
                        onClick = { selectedSoftwareCategory = cat },
                        text = { Text(cat.displayName) }
                    )
                }
            }
        }

        // 3. Preset Software Cards
        items(filteredPackages, key = { it.id }) { pkg ->
            SoftwareCard(
                pkg = pkg,
                onInstallClick = { onInstallPackage(pkg.id) },
                onViewLogsClick = { onViewLogs(pkg.id) },
                onLaunchClick = { cmd -> onRunPresetCommand(cmd) },
                onUpgradeClick = { onInstallPackage(pkg.id) }
            )
        }
    }
}

@Composable
private fun SettingsTabContent(
    container: ContainerInstance,
    bindSdCard: Boolean,
    dnsServers: List<String>,
    containerUsers: List<String>,
    upgradeState: UpgradeState,
    backupState: BackupState,
    onUpgradeRootfs: () -> Unit,
    onDismissUpgradeState: () -> Unit,
    onExportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onImportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onDismissBackupStatus: () -> Unit,
    onToggleBindSdCard: () -> Unit,
    onChangeRootPassword: (password: String) -> Unit,
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    onSetDnsServers: (servers: List<String>) -> Unit,
    onDeleteContainer: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dialog & input states
    var showDeleteContainerDialog by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<String?>(null) }

    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        uri?.let { onExportContainer(context.contentResolver, it) }
    }

    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportContainer(context.contentResolver, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Container Identity & Path Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Container Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("Distribution: ${container.distroName}", fontSize = 13.sp)
                Text("Filesystem Path: ${container.rootDirPath}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Storage Used: ${container.storageUsedMb} MB", fontSize = 13.sp)
            }
        }

        // 2. RootFS Upgrade Mechanism Card
        val rootfsVersion = remember(container.rootDirPath, upgradeState) {
            RootfsMigrationManager.readVersion(File(container.rootDirPath))
        }
        val hasPendingUpdates = remember(container.rootDirPath, rootfsVersion, upgradeState) {
            val currentCode = rootfsVersion?.versionCode ?: RootfsMigrationManager.LEGACY_VERSION_CODE
            RootfsMigrationManager.hasRootfsImprovements(currentCode, BuildConfig.VERSION_CODE)
        }
        val currentVersionLabel = when {
            rootfsVersion == null -> "Legacy v1.0.0 (Unversioned)"
            else -> "v${rootfsVersion.versionName} (Build ${rootfsVersion.versionCode})"
        }
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
        val lastUpdatedLabel = rootfsVersion?.lastUpgradedAt?.let { dateFormat.format(Date(it)) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Upgrade, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("RootFS Upgrade & Patching", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Update Status Badge on its own line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasPendingUpdates) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                                Text(
                                    text = "Update Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                                Text(
                                    text = "Up to Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Apply core runtime migrations, fix permissions, and update system scripts for this ${container.name} rootfs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Version status panel
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Installed RootFS Build:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentVersionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Latest App Target:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (lastUpdatedLabel != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Patch Check:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(lastUpdatedLabel, fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (upgradeState is UpgradeState.Upgrading) {
                    LinearProgressIndicator(
                        progress = { upgradeState.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(upgradeState.currentStepName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onUpgradeRootfs,
                        enabled = upgradeState !is UpgradeState.Upgrading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasPendingUpdates) "Upgrade RootFS" else "Re-verify & Patch RootFS")
                    }
                }
            }
        }

        // 3. Container Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Backup & Restore Container", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Export this rootfs container to a portable .tar.gz archive or restore a previous snapshot directly into this container.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (backupState is BackupState.Processing) {
                    LinearProgressIndicator(
                        progress = { if (backupState.progressPercent >= 0) backupState.progressPercent / 100f else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(backupState.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = "${container.name.lowercase().replace(" ", "_")}_backup_${System.currentTimeMillis()}.tar.gz"
                            exportLauncher.launch(defaultName)
                        },
                        enabled = backupState !is BackupState.Processing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup")
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/octet-stream", "*/*"))
                        },
                        enabled = backupState !is BackupState.Processing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Archive")
                    }
                }
            }
        }

        // 4. Storage Mount Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Storage Mount Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Configure host filesystem bind-mounts into this container (/sdcard, /storage/emulated/0, Downloads, Documents).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mount Host Storage (/sdcard)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Binds Android storage into /sdcard and /mnt/sdcard", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = bindSdCard,
                        onCheckedChange = { onToggleBindSdCard() }
                    )
                }
            }
        }

        // 5. User & Account Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("User & Account Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Manage Linux accounts, passwords, and sudo access inside ${container.name}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRootPasswordDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Root Password", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showAddUserDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add User", fontSize = 12.sp)
                    }
                }

                // User List
                if (containerUsers.isNotEmpty()) {
                    Text("Installed User Accounts:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        containerUsers.forEach { user ->
                            val isDefault = user == container.defaultUser
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(user, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (isDefault) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                            Text(
                                                text = "DEFAULT LOGIN",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!isDefault) {
                                        OutlinedButton(
                                            onClick = { onSetDefaultUser(user) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Set Default", fontSize = 11.sp)
                                        }
                                    }

                                    if (user != "root" && !isDefault) {
                                        IconButton(
                                            onClick = { userToDelete = user },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete user", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Network & DNS Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Network & DNS Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Configures nameservers in /etc/resolv.conf for package downloads and internet access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Active Nameservers: ${dnsServers.joinToString(", ")}",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = dnsServers == listOf("1.1.1.1", "1.0.0.1"),
                        onClick = { onSetDnsServers(listOf("1.1.1.1", "1.0.0.1")) },
                        label = { Text("Cloudflare", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = dnsServers == listOf("8.8.8.8", "8.8.4.4"),
                        onClick = { onSetDnsServers(listOf("8.8.8.8", "8.8.4.4")) },
                        label = { Text("Google", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = dnsServers == listOf("9.9.9.9", "149.112.112.112"),
                        onClick = { onSetDnsServers(listOf("9.9.9.9", "149.112.112.112")) },
                        label = { Text("Quad9", fontSize = 11.sp) }
                    )
                }

                OutlinedButton(
                    onClick = { showCustomDnsDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Custom DNS Servers")
                }
            }
        }

        // 7. Danger Zone: Delete Container Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Permanently delete ${container.name} and all of its installed packages, files, user accounts, and rootfs storage from your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showDeleteContainerDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Container")
                }
            }
        }
    }

    // Dialogs
    if (showDeleteContainerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContainerDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            },
            title = { Text("Delete Container?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to permanently delete \"${container.name}\"? All rootfs files inside ${container.rootDirPath} will be completely removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteContainerDialog = false
                        onDeleteContainer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContainerDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRootPasswordDialog) {
        var rootPwd by remember { mutableStateOf("") }
        var showPwd by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showRootPasswordDialog = false },
            title = { Text("Set Root Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter new root password for ${container.name}:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = rootPwd,
                        onValueChange = { rootPwd = it },
                        singleLine = true,
                        visualTransformation = if (showPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPwd = !showPwd }) {
                                Icon(if (showPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rootPwd.isNotBlank()) {
                            onChangeRootPassword(rootPwd)
                            showRootPasswordDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRootPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddUserDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newUserPwd by remember { mutableStateOf("") }
        var isSudoUser by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Add New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' || c == '-' } },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserPwd,
                        onValueChange = { newUserPwd = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Sudo Privileges", fontSize = 13.sp)
                        Switch(checked = isSudoUser, onCheckedChange = { isSudoUser = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newUserPwd.isNotBlank()) {
                            onCreateUser(newUsername, newUserPwd, isSudoUser)
                            showAddUserDialog = false
                        }
                    }
                ) { Text("Create User") }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (userToDelete != null) {
        val user = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User?") },
            text = { Text("Are you sure you want to delete user \"$user\" and their home directory from ${container.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showCustomDnsDialog) {
        var dnsInput by remember { mutableStateOf(dnsServers.joinToString(", ")) }
        AlertDialog(
            onDismissRequest = { showCustomDnsDialog = false },
            title = { Text("Custom Nameservers") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter comma-separated IP addresses for /etc/resolv.conf:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = dnsInput,
                        onValueChange = { dnsInput = it },
                        placeholder = { Text("e.g. 1.1.1.1, 8.8.8.8") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val servers = dnsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        if (servers.isNotEmpty()) {
                            onSetDnsServers(servers)
                            showCustomDnsDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDnsDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (upgradeState is UpgradeState.Success || upgradeState is UpgradeState.Error) {
        val isSuccess = upgradeState is UpgradeState.Success
        val logs = when (upgradeState) {
            is UpgradeState.Success -> upgradeState.logs
            is UpgradeState.Error -> upgradeState.logs
            else -> emptyList()
        }
        val title = if (isSuccess) "RootFS System Verified & Up to Date" else "RootFS Upgrade Error"
        val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
        val iconColor = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

        AlertDialog(
            onDismissRequest = onDismissUpgradeState,
            icon = { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp)) },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSuccess) {
                        Text(
                            text = "RootFS for ${container.name} is fully verified and running runtime build v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}). Core permissions and system files are intact.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val errorState = upgradeState as UpgradeState.Error
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (logs.isNotEmpty()) {
                        Text("Execution Logs:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E)
                        ) {
                            val logScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .verticalScroll(logScrollState)
                            ) {
                                logs.forEach { line ->
                                    Text(
                                        text = line,
                                        color = Color(0xFFD4D4D4),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissUpgradeState) {
                    Text("Done")
                }
            }
        )
    }
}
