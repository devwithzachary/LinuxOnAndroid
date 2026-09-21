package com.devwithzachary.completelinuxinstaller.ui.screens.container.tabs

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
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
    onDeleteContainer: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Dialog & input states
    var showDeleteContainerDialog by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showCustomDnsDialog by remember { mutableStateOf(false) }
    var showExportFileDialog by remember { mutableStateOf(false) }
    var fileToExport by remember { mutableStateOf<File?>(null) }
    var externalFilesList by remember { mutableStateOf<List<File>>(emptyList()) }
    var transferStatusMessage by remember { mutableStateOf<String?>(null) }
    var userToDelete by remember { mutableStateOf<String?>(null) }

    val importFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportFiles(context.contentResolver, uris) { count ->
                transferStatusMessage = "Successfully imported $count file(s) to /external"
                externalFilesList = onGetExternalFiles()
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        if (uri != null && fileToExport != null) {
            onExportFile(context.contentResolver, fileToExport!!, uri) { success ->
                transferStatusMessage = if (success) "Exported ${fileToExport!!.name} successfully" else "Failed to export ${fileToExport!!.name}"
                fileToExport = null
            }
        }
    }

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

        // 4. Shared External Storage (/external) Card
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
                    Icon(Icons.Default.FolderShared, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Shared External Storage (/external)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "This dedicated Android storage folder is automatically mounted into ${container.name} at /external. Because it resides in app-specific storage, all file types (.sh, .zip, scripts, code, binaries) have full POSIX read, write, and execute permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clickable directory box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenExternalDirectory(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = container.getExternalDisplayPath(context),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tap to open folder in File Manager",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Import / Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { importFilesLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Files", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            externalFilesList = onGetExternalFiles()
                            showExportFileDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export File", fontSize = 13.sp)
                    }
                }

                if (transferStatusMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = transferStatusMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { transferStatusMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

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
        val hasExistingRootPassword = container.hasRootPassword
        var currentPwd by remember { mutableStateOf("") }
        var newPwd by remember { mutableStateOf("") }
        var confirmPwd by remember { mutableStateOf("") }
        var showCurrentPwd by remember { mutableStateOf(false) }
        var showNewPwd by remember { mutableStateOf(false) }
        var showConfirmPwd by remember { mutableStateOf(false) }
        var isVerifying by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { if (!isVerifying) showRootPasswordDialog = false },
            title = { Text(if (hasExistingRootPassword) "Change Root Password" else "Set Root Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (hasExistingRootPassword)
                            "Verify your current root password before setting a new one for ${container.name}:"
                        else
                            "Create a root password for ${container.name}:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (hasExistingRootPassword) {
                        OutlinedTextField(
                            value = currentPwd,
                            onValueChange = {
                                currentPwd = it
                                errorMessage = null
                            },
                            label = { Text("Current Root Password") },
                            singleLine = true,
                            enabled = !isVerifying,
                            visualTransformation = if (showCurrentPwd) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false
                            ),
                            trailingIcon = {
                                IconButton(onClick = { showCurrentPwd = !showCurrentPwd }) {
                                    Icon(if (showCurrentPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPwd,
                        onValueChange = {
                            newPwd = it
                            errorMessage = null
                        },
                        label = { Text("New Root Password") },
                        singleLine = true,
                        enabled = !isVerifying,
                        visualTransformation = if (showNewPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showNewPwd = !showNewPwd }) {
                                Icon(if (showNewPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPwd,
                        onValueChange = {
                            confirmPwd = it
                            errorMessage = null
                        },
                        label = { Text("Confirm New Password") },
                        singleLine = true,
                        enabled = !isVerifying,
                        visualTransformation = if (showConfirmPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPwd = !showConfirmPwd }) {
                                Icon(if (showConfirmPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hasExistingRootPassword && currentPwd.isBlank()) {
                            errorMessage = "Current password cannot be blank"
                            return@Button
                        }
                        if (newPwd.isBlank()) {
                            errorMessage = "New password cannot be blank"
                            return@Button
                        }
                        if (newPwd != confirmPwd) {
                            errorMessage = "New passwords do not match"
                            return@Button
                        }
                        scope.launch {
                            isVerifying = true
                            if (hasExistingRootPassword) {
                                val isValid = onVerifyRootPassword(currentPwd)
                                if (!isValid) {
                                    errorMessage = "Current root password is incorrect"
                                    isVerifying = false
                                    return@launch
                                }
                            }
                            onChangeRootPassword(newPwd)
                            isVerifying = false
                            showRootPasswordDialog = false
                        }
                    },
                    enabled = !isVerifying && (!hasExistingRootPassword || currentPwd.isNotBlank()) && newPwd.isNotBlank() && confirmPwd.isNotBlank()
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verifying...")
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRootPasswordDialog = false },
                    enabled = !isVerifying
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddUserDialog) {
        var newUsername by remember { mutableStateOf("") }
        var newUserPwd by remember { mutableStateOf("") }
        var showNewUserPwd by remember { mutableStateOf(false) }
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
                        visualTransformation = if (showNewUserPwd) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false
                        ),
                        trailingIcon = {
                            IconButton(onClick = { showNewUserPwd = !showNewUserPwd }) {
                                Icon(if (showNewUserPwd) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
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

    if (showExportFileDialog) {
        AlertDialog(
            onDismissRequest = { showExportFileDialog = false },
            icon = {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Export File from /external", fontWeight = FontWeight.Bold) },
            text = {
                if (externalFilesList.isEmpty()) {
                    Text(
                        "No files found in /external to export.\n\nPlace or generate files inside /external in your container (e.g. cp myfile.sh /external/), then export them to Android storage here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Select a file to save to Android storage:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        externalFilesList.forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        fileToExport = file
                                        showExportFileDialog = false
                                        exportFileLauncher.launch(file.name)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        val sizeKb = (file.length() / 1024L).coerceAtLeast(1L)
                                        Text("${sizeKb} KB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportFileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
