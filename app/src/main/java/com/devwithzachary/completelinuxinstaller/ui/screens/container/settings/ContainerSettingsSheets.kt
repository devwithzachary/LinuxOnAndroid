package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RootPasswordSheet(
    container: ContainerInstance,
    onVerifyRootPassword: suspend (String) -> Boolean,
    onChangeRootPassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
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

    LoaBottomSheet(
        onDismissRequest = { if (!isVerifying) onDismiss() },
        title = if (hasExistingRootPassword) "Change Root Password" else "Set Root Password",
        subtitle = if (hasExistingRootPassword) {
            "Verify your current root password before updating ${container.name}."
        } else {
            "Create a root administrator password for ${container.name}."
        }
    ) {
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
                        Icon(
                            imageVector = if (showCurrentPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (showCurrentPwd) "Hide current password" else "Show current password"
                        )
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
                    Icon(
                        imageVector = if (showNewPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showNewPwd) "Hide new password" else "Show new password"
                    )
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
                    Icon(
                        imageVector = if (showConfirmPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showConfirmPwd) "Hide confirm password" else "Show confirm password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onDismiss,
                enabled = !isVerifying
            ) {
                Text("Cancel")
            }
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
                                onDismiss()
                            } else {
                                errorMessage = "Current password is incorrect"
                            }
                        }
                    } else {
                        onChangeRootPassword(newPwd)
                        onDismiss()
                    }
                },
                enabled = !isVerifying
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (hasExistingRootPassword) "Update Password" else "Set Password")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddUserSheet(
    onCreateUser: (username: String, password: String, isSudo: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var newUsername by remember { mutableStateOf("") }
    var newUserPwd by remember { mutableStateOf("") }
    var showNewUserPwd by remember { mutableStateOf(false) }
    var isSudoUser by remember { mutableStateOf(true) }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "Add Linux User",
        subtitle = "Create a user account with optional sudo access"
    ) {
        OutlinedTextField(
            value = newUsername,
            onValueChange = { input ->
                newUsername = input.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }
            },
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
                    Icon(
                        imageVector = if (showNewUserPwd) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showNewUserPwd) "Hide user password" else "Show user password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Grant Sudo Privileges",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = isSudoUser,
                onCheckedChange = { isSudoUser = it }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (newUsername.isNotBlank() && newUserPwd.isNotBlank()) {
                        onCreateUser(newUsername, newUserPwd, isSudoUser)
                        onDismiss()
                    }
                },
                enabled = newUsername.isNotBlank() && newUserPwd.isNotBlank()
            ) {
                Text("Create User")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExportExternalFileSheet(
    files: List<File>,
    onSelectFile: (File) -> Unit,
    onDismiss: () -> Unit
) {
    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "Export File from /external",
        subtitle = if (files.isEmpty()) {
            "No files found in /external to export."
        } else {
            "Select a file to save to Android storage"
        }
    ) {
        if (files.isEmpty()) {
            Text(
                text = "Place files inside /external in your container (for example: cp myfile.sh /external/), then export them here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
            ) {
                item {
                    LoaSegmentedGroup {
                        files.forEach { file ->
                            val sizeKb = (file.length() / 1024L).coerceAtLeast(1L)
                            row(
                                title = file.name,
                                subtitle = "$sizeKb KB",
                                leadingIcon = Icons.AutoMirrored.Rounded.InsertDriveFile,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { onSelectFile(file) }
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DnsConfigSheet(
    dnsServers: List<String>,
    onSetDnsServers: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var dnsInput by remember(dnsServers) { mutableStateOf(dnsServers.joinToString(", ")) }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "DNS Nameservers",
        subtitle = "Configures nameservers in /etc/resolv.conf"
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = dnsServers == listOf("1.1.1.1", "1.0.0.1"),
                onClick = {
                    val preset = listOf("1.1.1.1", "1.0.0.1")
                    onSetDnsServers(preset)
                    dnsInput = preset.joinToString(", ")
                },
                label = { Text("Cloudflare") }
            )
            FilterChip(
                selected = dnsServers == listOf("8.8.8.8", "8.8.4.4"),
                onClick = {
                    val preset = listOf("8.8.8.8", "8.8.4.4")
                    onSetDnsServers(preset)
                    dnsInput = preset.joinToString(", ")
                },
                label = { Text("Google") }
            )
            FilterChip(
                selected = dnsServers == listOf("9.9.9.9", "149.112.112.112"),
                onClick = {
                    val preset = listOf("9.9.9.9", "149.112.112.112")
                    onSetDnsServers(preset)
                    dnsInput = preset.joinToString(", ")
                },
                label = { Text("Quad9") }
            )
        }

        OutlinedTextField(
            value = dnsInput,
            onValueChange = { dnsInput = it },
            label = { Text("Custom Nameservers") },
            placeholder = { Text("e.g. 1.1.1.1, 8.8.8.8") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val servers = dnsInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (servers.isNotEmpty()) {
                        onSetDnsServers(servers)
                        onDismiss()
                    }
                }
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CodeServerPasswordSheet(
    currentPassword: String,
    onSavePassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf(currentPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "VS Code Server Password",
        subtitle = "Password for code-server over LAN or external browsers"
    ) {
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )

        OutlinedButton(
            onClick = {
                passwordInput = ContainerManager.generateSecurePassword()
                passwordVisible = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoFixHigh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Secure Password")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val trimmed = passwordInput.trim()
                    if (trimmed.isNotBlank()) {
                        onSavePassword(trimmed)
                        Toast.makeText(context, "VS Code Server password updated", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled = passwordInput.trim().isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VncPasswordSheet(
    currentPassword: String,
    userPassword: String,
    onSavePassword: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var passwordInput by remember { mutableStateOf(currentPassword) }
    var passwordVisible by remember { mutableStateOf(false) }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "VNC Server Password",
        subtitle = "Password for TigerVNC remote desktop clients"
    ) {
        OutlinedTextField(
            value = passwordInput,
            onValueChange = { passwordInput = it },
            label = { Text("VNC Password") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false
            ),
            supportingText = {
                Text("Standard VNC authentication uses up to 8 characters.")
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide VNC password" else "Show VNC password"
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    passwordInput = ContainerManager.generateSecurePassword().take(8)
                    passwordVisible = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Random (8 chars)")
            }

            OutlinedButton(
                onClick = {
                    passwordInput = userPassword
                    passwordVisible = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("User Password")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val trimmed = passwordInput.trim()
                    if (trimmed.isNotBlank()) {
                        onSavePassword(trimmed)
                        Toast.makeText(context, "VNC Server password updated", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled = passwordInput.trim().isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpgradeResultSheet(
    containerName: String,
    upgradeState: UpgradeState,
    onDismiss: () -> Unit
) {
    val isSuccess = upgradeState is UpgradeState.Success
    val logs = when (upgradeState) {
        is UpgradeState.Success -> upgradeState.logs
        is UpgradeState.Error -> upgradeState.logs
        else -> emptyList()
    }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isSuccess) "RootFS Verified & Up to Date" else "RootFS Upgrade Error"
    ) {
        LoaStatusBadge(
            label = if (isSuccess) "Success" else "Error",
            tone = if (isSuccess) LoaStatusTone.Running else LoaStatusTone.Error
        )

        if (isSuccess) {
            Text(
                text = "RootFS for $containerName is verified on runtime v${BuildConfig.VERSION_NAME} (b${BuildConfig.VERSION_CODE}).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (upgradeState is UpgradeState.Error) {
            Text(
                text = upgradeState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (logs.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { logLine ->
                        Text(
                            text = logLine,
                            style = LoaMonoTextStyle,
                            color = if (logLine.contains("FAIL") || logLine.contains("ERROR")) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    }
}
