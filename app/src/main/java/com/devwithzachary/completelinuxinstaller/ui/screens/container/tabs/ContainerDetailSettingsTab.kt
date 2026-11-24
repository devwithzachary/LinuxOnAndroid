package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.AddUserSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.CodeServerPasswordSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.DnsConfigSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.ExportExternalFileSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.RootPasswordSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.UpgradeResultSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.container.settings.VncPasswordSheet
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import java.io.File
import java.util.Locale

@Composable
fun SettingsTabContent(
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
    onVerifyRootPassword: suspend (password: String) -> Boolean = { true },
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    onSetDnsServers: (servers: List<String>) -> Unit,
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, onComplete: (Int) -> Unit) -> Unit,
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit,
    onOpenExternalDirectory: (context: Context) -> Unit,
    onGetExternalFiles: () -> List<File>,
    onToggleWebTerminalLanAccess: (Boolean) -> Unit = {},
    onToggleCodeServerLanAccess: (Boolean) -> Unit = {},
    onSetCodeServerPassword: (String) -> Unit = {},
    onRegenerateCodeServerPassword: () -> Unit = {},
    onToggleVncLanAccess: (Boolean) -> Unit = {},
    onRegenerateVncPassword: () -> Unit = {},
    onSetVncPassword: (String) -> Unit = {},
    onDeleteContainer: () -> Unit
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    var showRootPasswordSheet by remember { mutableStateOf(false) }
    var showAddUserSheet by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<String?>(null) }

    var showExportFileSheet by remember { mutableStateOf(false) }
    var fileToExport by remember { mutableStateOf<File?>(null) }
    var externalFilesList by remember { mutableStateOf<List<File>>(emptyList()) }
    var transferStatusMessage by remember { mutableStateOf<String?>(null) }

    var showDnsSheet by remember { mutableStateOf(false) }
    var showCodeServerPasswordSheet by remember { mutableStateOf(false) }
    var showVncPasswordSheet by remember { mutableStateOf(false) }
    var showDeleteContainerDialog by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        uri?.let { onExportContainer(context.contentResolver, it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportContainer(context.contentResolver, it) }
    }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFiles(context.contentResolver, uris) { count ->
                transferStatusMessage = "Imported $count files into /external"
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && fileToExport != null) {
            val targetName = fileToExport!!.name
            onExportFile(context.contentResolver, fileToExport!!, uri) { success ->
                transferStatusMessage = if (success) {
                    "Exported $targetName"
                } else {
                    "Failed to export $targetName"
                }
                fileToExport = null
            }
        }
    }

    val rootfsVersion = remember(container.rootDirPath, upgradeState, isInspection) {
        if (isInspection) null else RootfsMigrationManager.readVersion(File(container.rootDirPath))
    }
    val hasPendingUpdates = remember(rootfsVersion, upgradeState) {
        val currentCode = rootfsVersion?.versionCode ?: RootfsMigrationManager.LEGACY_VERSION_CODE
        RootfsMigrationManager.hasRootfsImprovements(currentCode, BuildConfig.VERSION_CODE)
    }
    val currentVersionLabel = if (rootfsVersion == null) {
        "Legacy v1.0.0"
    } else {
        "v${rootfsVersion.versionName} (b${rootfsVersion.versionCode})"
    }

    val externalDisplayPath = remember(container.rootDirPath, isInspection) {
        if (isInspection) "/sdcard/Android/data/.../files/external" else container.getExternalDisplayPath(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Account & Container Identity
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoaSectionHeader(title = "Account")

                LoaSegmentedGroup {
                    row(
                        title = container.distroName,
                        subtitle = container.rootDirPath,
                        leadingIcon = Icons.Rounded.Info,
                        trailingContent = {
                            LoaMonospaceChip(text = formatSettingsStorage(container.storageUsedMb))
                        }
                    )

                    row(
                        title = "Root Password",
                        subtitle = "Change or verify the Linux root password",
                        leadingIcon = Icons.Rounded.Lock,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showRootPasswordSheet = true }
                    )

                    row(
                        title = "Add Linux User",
                        subtitle = "Create a regular or sudo account",
                        leadingIcon = Icons.Rounded.PersonAdd,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showAddUserSheet = true }
                    )

                    containerUsers.forEach { user ->
                        val isDefault = user == container.defaultUser
                        row(
                            title = user,
                            subtitle = if (isDefault) "Default login account" else "Linux user account",
                            leadingIcon = Icons.Rounded.Person,
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (isDefault) {
                                        LoaMonospaceChip(
                                            text = "default",
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        TextButton(onClick = { onSetDefaultUser(user) }) {
                                            Text("Set default")
                                        }
                                        if (user != "root") {
                                            IconButton(onClick = { userToDelete = user }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.DeleteOutline,
                                                    contentDescription = "Delete user $user",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 2. Storage
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoaSectionHeader(title = "Storage")

                LoaSegmentedGroup {
                    row(
                        title = "Shared Folder (/external)",
                        subtitle = externalDisplayPath,
                        leadingIcon = Icons.Rounded.Folder,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onOpenExternalDirectory(context) }
                    )

                    row(
                        title = "Import Files to /external",
                        subtitle = "Copy files from Android storage with POSIX permissions",
                        leadingIcon = Icons.Rounded.FileDownload,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { importFilesLauncher.launch(arrayOf("*/*")) }
                    )

                    row(
                        title = "Export File from /external",
                        subtitle = "Save files from /external back to Android storage",
                        leadingIcon = Icons.Rounded.FileUpload,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            externalFilesList = onGetExternalFiles()
                            showExportFileSheet = true
                        }
                    )

                    if (transferStatusMessage != null) {
                        row(
                            title = transferStatusMessage.orEmpty(),
                            trailingContent = {
                                IconButton(onClick = { transferStatusMessage = null }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss transfer status"
                                    )
                                }
                            }
                        )
                    }

                    row(
                        title = "Mount Host Storage (/sdcard)",
                        subtitle = "Binds Android storage into /sdcard and /mnt/sdcard",
                        leadingIcon = Icons.Rounded.SdStorage,
                        trailingContent = {
                            Switch(
                                checked = bindSdCard,
                                onCheckedChange = { onToggleBindSdCard() }
                            )
                        },
                        onClick = onToggleBindSdCard
                    )
                }
            }

            // 3. Network & Remote Access
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoaSectionHeader(title = "Network & Remote Access")

                LoaSegmentedGroup {
                    row(
                        title = "DNS Nameservers",
                        subtitle = dnsServers.joinToString(", "),
                        leadingIcon = Icons.Rounded.Dns,
                        trailingContent = {
                            LoaMonospaceChip(text = dnsServers.firstOrNull() ?: "Default")
                        },
                        onClick = { showDnsSheet = true }
                    )

                    row(
                        title = "Web Terminal LAN Access",
                        subtitle = if (container.webTerminalLanAccess) {
                            "Exposed on :8080 (or :7681) using '${container.defaultUser}'"
                        } else {
                            "Loopback only (127.0.0.1)"
                        },
                        leadingIcon = Icons.Rounded.Terminal,
                        trailingContent = {
                            Switch(
                                checked = container.webTerminalLanAccess,
                                onCheckedChange = { onToggleWebTerminalLanAccess(it) }
                            )
                        },
                        onClick = { onToggleWebTerminalLanAccess(!container.webTerminalLanAccess) }
                    )

                    row(
                        title = "VS Code Server LAN Access",
                        subtitle = if (container.codeServerLanAccess) {
                            "Exposed on :8080 with password authentication"
                        } else {
                            "Loopback only (127.0.0.1:8080)"
                        },
                        leadingIcon = Icons.Rounded.Code,
                        trailingContent = {
                            Switch(
                                checked = container.codeServerLanAccess,
                                onCheckedChange = { onToggleCodeServerLanAccess(it) }
                            )
                        },
                        onClick = { onToggleCodeServerLanAccess(!container.codeServerLanAccess) }
                    )

                    row(
                        title = "VS Code Password",
                        subtitle = container.codeServerPassword.ifBlank { "Auto-generated on LAN enable" },
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (container.codeServerPassword.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(container.codeServerPassword))
                                            Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy VS Code password"
                                        )
                                    }
                                }
                                IconButton(onClick = onRegenerateCodeServerPassword) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Regenerate VS Code password"
                                    )
                                }
                            }
                        },
                        onClick = { showCodeServerPasswordSheet = true }
                    )

                    row(
                        title = "TigerVNC LAN Access",
                        subtitle = if (container.vncLanAccess) {
                            "Listening on 0.0.0.0:5901"
                        } else {
                            "Loopback only (127.0.0.1:5901)"
                        },
                        leadingIcon = Icons.Rounded.DesktopWindows,
                        trailingContent = {
                            Switch(
                                checked = container.vncLanAccess,
                                onCheckedChange = { onToggleVncLanAccess(it) }
                            )
                        },
                        onClick = { onToggleVncLanAccess(!container.vncLanAccess) }
                    )

                    row(
                        title = "VNC Password",
                        subtitle = container.effectiveVncPassword,
                        trailingContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(container.effectiveVncPassword))
                                        Toast.makeText(context, "VNC password copied", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Copy VNC password"
                                    )
                                }
                                IconButton(onClick = onRegenerateVncPassword) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Regenerate VNC password"
                                    )
                                }
                            }
                        },
                        onClick = { showVncPasswordSheet = true }
                    )
                }
            }

            // 4. Backup & Upgrade
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoaSectionHeader(title = "Backup & Upgrade")

                LoaSegmentedGroup {
                    val isUpgrading = upgradeState is UpgradeState.Upgrading
                    row(
                        title = if (hasPendingUpdates) "Upgrade RootFS" else "Verify & Patch RootFS",
                        subtitle = if (isUpgrading) {
                            (upgradeState as UpgradeState.Upgrading).currentStepName
                        } else {
                            "$currentVersionLabel -> v${BuildConfig.VERSION_NAME} (b${BuildConfig.VERSION_CODE})"
                        },
                        leadingIcon = Icons.Rounded.SystemUpdate,
                        enabled = !isUpgrading,
                        trailingContent = {
                            if (isUpgrading) {
                                LoaProgressRing(
                                    progress = (upgradeState as UpgradeState.Upgrading).progressPercent / 100f,
                                    size = 28.dp,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                LoaStatusBadge(
                                    label = if (hasPendingUpdates) "Update" else "Up to Date",
                                    tone = if (hasPendingUpdates) LoaStatusTone.Downloading else LoaStatusTone.Running
                                )
                            }
                        },
                        onClick = onUpgradeRootfs
                    )

                    val isBackingUp = backupState is BackupState.Processing
                    row(
                        title = "Export Backup (.tar.gz)",
                        subtitle = if (isBackingUp) {
                            (backupState as BackupState.Processing).message
                        } else {
                            "Save a portable archive of this container"
                        },
                        leadingIcon = Icons.Rounded.CloudUpload,
                        enabled = !isBackingUp,
                        trailingContent = {
                            if (isBackingUp) {
                                val pct = (backupState as BackupState.Processing).progressPercent
                                LoaProgressRing(
                                    progress = if (pct >= 0) pct / 100f else null,
                                    size = 28.dp,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            val defaultName = "${container.name.lowercase().replace(" ", "_")}_backup_${System.currentTimeMillis()}.tar.gz"
                            exportBackupLauncher.launch(defaultName)
                        }
                    )

                    row(
                        title = "Restore Archive",
                        subtitle = "Restore a .tar.gz snapshot into this container",
                        leadingIcon = Icons.Rounded.CloudDownload,
                        enabled = !isBackingUp,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            importBackupLauncher.launch(
                                arrayOf("application/gzip", "application/x-gzip", "application/octet-stream", "*/*")
                            )
                        }
                    )

                    if (backupState is BackupState.Success || backupState is BackupState.Error) {
                        val isErr = backupState is BackupState.Error
                        val msg = when (backupState) {
                            is BackupState.Success -> backupState.message
                            is BackupState.Error -> backupState.message
                            else -> ""
                        }
                        row(
                            title = msg,
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LoaStatusBadge(
                                        label = if (isErr) "Failed" else "Done",
                                        tone = if (isErr) LoaStatusTone.Error else LoaStatusTone.Running
                                    )
                                    IconButton(onClick = onDismissBackupStatus) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Dismiss backup status"
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // 5. Danger Zone
            val dangerContainerColor = MaterialTheme.colorScheme.errorContainer
            val onDangerContainerColor = MaterialTheme.colorScheme.onErrorContainer
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LoaSectionHeader(title = "Danger Zone")

                LoaSegmentedGroup {
                    row(
                        title = "Delete ${container.name}",
                        subtitle = "Permanently remove rootfs, packages, accounts, and files",
                        leadingIcon = Icons.Rounded.DeleteForever,
                        containerColor = dangerContainerColor,
                        contentColor = onDangerContainerColor,
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = onDangerContainerColor
                            )
                        },
                        onClick = { showDeleteContainerDialog = true }
                    )
                }
            }
        }
    }

    if (showRootPasswordSheet) {
        RootPasswordSheet(
            container = container,
            onVerifyRootPassword = onVerifyRootPassword,
            onChangeRootPassword = onChangeRootPassword,
            onDismiss = { showRootPasswordSheet = false }
        )
    }

    if (showAddUserSheet) {
        AddUserSheet(
            onCreateUser = onCreateUser,
            onDismiss = { showAddUserSheet = false }
        )
    }

    if (showExportFileSheet) {
        ExportExternalFileSheet(
            files = externalFilesList,
            onSelectFile = { file ->
                fileToExport = file
                showExportFileSheet = false
                exportFileLauncher.launch(file.name)
            },
            onDismiss = { showExportFileSheet = false }
        )
    }

    if (showDnsSheet) {
        DnsConfigSheet(
            dnsServers = dnsServers,
            onSetDnsServers = onSetDnsServers,
            onDismiss = { showDnsSheet = false }
        )
    }

    if (showCodeServerPasswordSheet) {
        CodeServerPasswordSheet(
            currentPassword = container.codeServerPassword,
            onSavePassword = onSetCodeServerPassword,
            onDismiss = { showCodeServerPasswordSheet = false }
        )
    }

    if (showVncPasswordSheet) {
        VncPasswordSheet(
            currentPassword = container.effectiveVncPassword,
            userPassword = container.userPassword,
            onSavePassword = onSetVncPassword,
            onDismiss = { showVncPasswordSheet = false }
        )
    }

    if (upgradeState is UpgradeState.Success || upgradeState is UpgradeState.Error) {
        UpgradeResultSheet(
            containerName = container.name,
            upgradeState = upgradeState,
            onDismiss = onDismissUpgradeState
        )
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete User?") },
            text = {
                Text("Are you sure you want to delete user \"$user\" and their home directory from ${container.name}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(user)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteContainerDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteContainerDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Container?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Permanently delete \"${container.name}\" and all rootfs files?")
                    Text(
                        text = container.rootDirPath,
                        style = LoaMonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteContainerDialog = false
                        onDeleteContainer()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteContainerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatSettingsStorage(mb: Long): String {
    return if (mb >= 1024L) {
        String.format(Locale.US, "%.1f GB", mb / 1024.0)
    } else {
        "$mb MB"
    }
}

@Composable
fun ContainerDetailSettingsTab(
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
    onVerifyRootPassword: suspend (password: String) -> Boolean = { true },
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    onSetDnsServers: (servers: List<String>) -> Unit,
    onImportFiles: (contentResolver: ContentResolver, uris: List<Uri>, onComplete: (Int) -> Unit) -> Unit,
    onExportFile: (contentResolver: ContentResolver, file: File, targetUri: Uri, onComplete: (Boolean) -> Unit) -> Unit,
    onOpenExternalDirectory: (context: Context) -> Unit,
    onGetExternalFiles: () -> List<File>,
    onToggleWebTerminalLanAccess: (Boolean) -> Unit = {},
    onToggleCodeServerLanAccess: (Boolean) -> Unit = {},
    onSetCodeServerPassword: (String) -> Unit = {},
    onRegenerateCodeServerPassword: () -> Unit = {},
    onToggleVncLanAccess: (Boolean) -> Unit = {},
    onRegenerateVncPassword: () -> Unit = {},
    onSetVncPassword: (String) -> Unit = {},
    onDeleteContainer: () -> Unit
) = SettingsTabContent(
    container = container,
    bindSdCard = bindSdCard,
    dnsServers = dnsServers,
    containerUsers = containerUsers,
    upgradeState = upgradeState,
    backupState = backupState,
    onUpgradeRootfs = onUpgradeRootfs,
    onDismissUpgradeState = onDismissUpgradeState,
    onExportContainer = onExportContainer,
    onImportContainer = onImportContainer,
    onDismissBackupStatus = onDismissBackupStatus,
    onToggleBindSdCard = onToggleBindSdCard,
    onChangeRootPassword = onChangeRootPassword,
    onVerifyRootPassword = onVerifyRootPassword,
    onCreateUser = onCreateUser,
    onDeleteUser = onDeleteUser,
    onSetDefaultUser = onSetDefaultUser,
    onSetDnsServers = onSetDnsServers,
    onImportFiles = onImportFiles,
    onExportFile = onExportFile,
    onOpenExternalDirectory = onOpenExternalDirectory,
    onGetExternalFiles = onGetExternalFiles,
    onToggleWebTerminalLanAccess = onToggleWebTerminalLanAccess,
    onToggleCodeServerLanAccess = onToggleCodeServerLanAccess,
    onSetCodeServerPassword = onSetCodeServerPassword,
    onRegenerateCodeServerPassword = onRegenerateCodeServerPassword,
    onToggleVncLanAccess = onToggleVncLanAccess,
    onRegenerateVncPassword = onRegenerateVncPassword,
    onSetVncPassword = onSetVncPassword,
    onDeleteContainer = onDeleteContainer
)
