package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import kotlinx.coroutines.launch

@Composable
fun ContainerAccountCard(
    container: ContainerInstance,
    containerUsers: List<String>,
    onChangeRootPassword: (password: String) -> Unit,
    onVerifyRootPassword: suspend (password: String) -> Boolean,
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDeleteUser: (username: String) -> Unit,
    onSetDefaultUser: (username: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showRootPasswordDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
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
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPwd.isBlank()) {
                            errorMessage = "Password cannot be empty"
                            return@Button
                        }
                        if (newPwd != confirmPwd) {
                            errorMessage = "Passwords do not match"
                            return@Button
                        }

                        if (hasExistingRootPassword) {
                            if (currentPwd.isBlank()) {
                                errorMessage = "Current password is required"
                                return@Button
                            }
                            isVerifying = true
                            scope.launch {
                                val valid = onVerifyRootPassword(currentPwd)
                                isVerifying = false
                                if (valid) {
                                    onChangeRootPassword(newPwd)
                                    showRootPasswordDialog = false
                                } else {
                                    errorMessage = "Current password is incorrect"
                                }
                            }
                        } else {
                            onChangeRootPassword(newPwd)
                            showRootPasswordDialog = false
                        }
                    },
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (hasExistingRootPassword) "Update Password" else "Set Password")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRootPasswordDialog = false },
                    enabled = !isVerifying
                ) { Text("Cancel") }
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
}
