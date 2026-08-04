package com.devwithzachary.completelinuxinstaller.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.DashboardUiState

import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd

@Composable
fun SettingsScreen(
    state: DashboardUiState,
    onToggleBindSdCard: () -> Unit,
    onWipeRootfsClick: () -> Unit,
    onRefreshStatusClick: () -> Unit,
    onChangeRootPassword: (String) -> Unit = {},
    onCreateUser: (String, String) -> Unit = { _, _ -> },
    onDeleteUser: (String) -> Unit = {}
) {
    var showWipeConfirm by remember { mutableStateOf(false) }
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }

    var newRootPassword by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }

    var changePasswordUser by remember { mutableStateOf<String?>(null) }
    var changePasswordUserNewPass by remember { mutableStateOf("") }

    val uriHandler = LocalUriHandler.current

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
