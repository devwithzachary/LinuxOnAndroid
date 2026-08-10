package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.ui.BackupState
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

@Composable
fun SettingsScreen(
    state: DashboardUiState,
    backupState: BackupState = BackupState.Idle,
    onToggleBindSdCard: () -> Unit,
    onWipeRootfsClick: () -> Unit,
    onRefreshStatusClick: () -> Unit,
    onChangeRootPassword: (String) -> Unit = {},
    onCreateUser: (String, String) -> Unit = { _, _ -> },
    onDeleteUser: (String) -> Unit = {},
    onExportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onImportContainer: (android.content.ContentResolver, android.net.Uri) -> Unit = { _, _ -> },
    onDismissBackupStatus: () -> Unit = {}
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    var showWipeConfirm by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    var newRootPassword by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }

    var changePasswordUser by remember { mutableStateOf<String?>(null) }
    var changePasswordUserNewPass by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        if (uri != null) {
            onExportContainer(contentResolver, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportContainer(contentResolver, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Configuration",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 1-Tap RootFS Container Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Container Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Create a 1-tap backup archive (.tar.gz) of your complete Linux rootfs environment or restore an existing container archive from device storage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val defaultName = "linux_on_android_backup_${System.currentTimeMillis() / 1000}.tar.gz"
                            exportLauncher.launch(defaultName)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = state.isInstalled && backupState is BackupState.Idle,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Container")
                    }

                    OutlinedButton(
                        onClick = { showImportConfirm = true },
                        modifier = Modifier.weight(1f),
                        enabled = backupState is BackupState.Idle,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import Container")
                    }
                }
            }
        }

        // Users & Account Management Card
        if (state.isInstalled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "User & Account Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Root User Password", fontWeight = FontWeight.SemiBold)
                            Text("Set or reset the system administrator (root) password.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { showRootPasswordDialog = true }) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Change")
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Regular Users (${state.containerUsers.size})", fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = { showAddUserDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add User")
                        }
                    }

                    if (state.containerUsers.isEmpty()) {
                        Text(
                            text = "No non-root users found. Add a user to connect via SSH safely.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.containerUsers.forEach { user ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(user, fontWeight = FontWeight.Bold)
                                                Text("Sudo User (/bin/bash)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = {
                                                changePasswordUser = user
                                                changePasswordUserNewPass = ""
                                            }) {
                                                Icon(Icons.Default.Lock, contentDescription = "Change Password", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { onDeleteUser(user) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Storage & Bind Mount Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Storage & Mount Points",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bind Mount /sdcard", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Expose external Android storage inside /sdcard in PRoot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.bindSdCard,
                        onCheckedChange = { onToggleBindSdCard() }
                    )
                }
            }
        }

        // Maintenance & Reset Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Container Maintenance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = onRefreshStatusClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh System & Storage Status")
                }

                Button(
                    onClick = { showWipeConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Wipe & Reset Ubuntu RootFS")
                }
            }
        }
    }

    // Confirmation for Container Import
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("Import RootFS Container?") },
            text = { Text("Restoring a backup container will replace your current active RootFS and all installed software. Are you sure you want to select a backup file to restore?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/x-tar", "*/*"))
                    }
                ) {
                    Text("Proceed & Select Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Processing / Success / Error Modal Dialog
    if (backupState !is BackupState.Idle) {
        AlertDialog(
            onDismissRequest = {
                if (backupState !is BackupState.Processing) {
                    onDismissBackupStatus()
                }
            },
            title = {
                Text(
                    text = when (backupState) {
                        is BackupState.Processing -> "Container Backup Operation"
                        is BackupState.Success -> "Backup Operation Complete"
                        is BackupState.Error -> "Backup Error"
                        else -> ""
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    when (backupState) {
                        is BackupState.Processing -> {
                            if (backupState.progressPercent >= 0) {
                                LinearProgressIndicator(
                                    progress = { (backupState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = backupState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${backupState.progressPercent}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                CircularProgressIndicator()
                                Text(
                                    text = backupState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        is BackupState.Success -> {
                            Text(
                                text = backupState.message,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        is BackupState.Error -> {
                            Text(
                                text = backupState.message,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                if (backupState !is BackupState.Processing) {
                    TextButton(onClick = onDismissBackupStatus) {
                        Text("OK")
                    }
                }
            }
        )
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text("Wipe Ubuntu Container?") },
            text = { Text("Are you sure you want to completely delete the Ubuntu rootfs image and all installed Linux software? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWipeConfirm = false
                        onWipeRootfsClick()
                    }
                ) {
                    Text("Wipe Container", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRootPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showRootPasswordDialog = false },
            title = { Text("Change Root Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new system administrator password for root:")
                    OutlinedTextField(
                        value = newRootPassword,
                        onValueChange = { newRootPassword = it },
                        label = { Text("New Root Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newRootPassword.isNotBlank()) {
                            onChangeRootPassword(newRootPassword)
                            newRootPassword = ""
                            showRootPasswordDialog = false
                        }
                    }
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRootPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Create New User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add a non-root system user account with sudo privileges:")
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newUserPassword,
                        onValueChange = { newUserPassword = it },
                        label = { Text("User Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.isNotBlank() && newUserPassword.isNotBlank()) {
                            onCreateUser(newUsername, newUserPassword)
                            newUsername = ""
                            newUserPassword = ""
                            showAddUserDialog = false
                        }
                    }
                ) {
                    Text("Create User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (changePasswordUser != null) {
        val targetUser = changePasswordUser!!
        AlertDialog(
            onDismissRequest = { changePasswordUser = null },
            title = { Text("Change Password for '$targetUser'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a new password for user $targetUser:")
                    OutlinedTextField(
                        value = changePasswordUserNewPass,
                        onValueChange = { changePasswordUserNewPass = it },
                        label = { Text("New Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (changePasswordUserNewPass.isNotBlank()) {
                            onCreateUser(targetUser, changePasswordUserNewPass)
                            changePasswordUserNewPass = ""
                            changePasswordUser = null
                        }
                    }
                ) {
                    Text("Save Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { changePasswordUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
