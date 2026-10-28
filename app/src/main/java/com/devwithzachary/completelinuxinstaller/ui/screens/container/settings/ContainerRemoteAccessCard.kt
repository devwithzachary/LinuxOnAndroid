package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.ContainerManager
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance

@Composable
fun ContainerRemoteAccessCard(
    container: ContainerInstance,
    onToggleWebTerminalLanAccess: (Boolean) -> Unit,
    onToggleCodeServerLanAccess: (Boolean) -> Unit,
    onSetCodeServerPassword: (String) -> Unit,
    onRegenerateCodeServerPassword: () -> Unit,
    onToggleVncLanAccess: (Boolean) -> Unit,
    onRegenerateVncPassword: () -> Unit,
    onSetVncPassword: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCodeServerPasswordDialog by remember { mutableStateOf(false) }
    var showVncPasswordDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Remote Access & LAN Sharing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "By default, all network services bind only to loopback (127.0.0.1) so they can only be reached from your phone's browser or apps. Enable LAN sharing to access them from other devices on your Wi-Fi network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Web Terminal (ttyd)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Web Terminal (ttyd) LAN Sharing", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = if (container.webTerminalLanAccess)
                            "Exposed to Wi-Fi on port 8080 (or 7681). Login required using '${container.defaultUser}' and container password."
                        else
                            "Loopback only (http://127.0.0.1:8080). Safe for public Wi-Fi networks.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = container.webTerminalLanAccess,
                    onCheckedChange = { onToggleWebTerminalLanAccess(it) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // VS Code Server (code-server)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("VS Code Server LAN Access", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (container.codeServerLanAccess)
                                "Exposed to Wi-Fi on port 8080 with password authentication required."
                            else
                                "Loopback only (http://127.0.0.1:8080). Passwordless on this phone.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = container.codeServerLanAccess,
                        onCheckedChange = { onToggleCodeServerLanAccess(it) }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (container.codeServerLanAccess) "LAN Access Password" else "VS Code Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = container.codeServerPassword.ifBlank { "Not set (auto-generated on LAN enable)" },
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showCodeServerPasswordDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Set Password", modifier = Modifier.size(18.dp))
                            }
                            if (container.codeServerPassword.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(container.codeServerPassword))
                                        Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(
                                onClick = { onRegenerateCodeServerPassword() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Regenerate Password", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // TigerVNC Server
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("TigerVNC Server LAN Access", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (container.vncLanAccess)
                                "Listening on 0.0.0.0:5901. Accessible across LAN using VNC password."
                            else
                                "Loopback only (127.0.0.1:5901). Only local VNC viewers can connect.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = container.vncLanAccess,
                        onCheckedChange = { onToggleVncLanAccess(it) }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "VNC Password",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = container.effectiveVncPassword,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showVncPasswordDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Set VNC Password", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(container.effectiveVncPassword))
                                    Toast.makeText(context, "VNC password copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Password", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { onRegenerateVncPassword() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Generate New Password", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCodeServerPasswordDialog) {
        var passwordInput by remember { mutableStateOf(container.codeServerPassword) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showCodeServerPasswordDialog = false },
            title = {
                Text(
                    text = "Set VS Code Server Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configure a custom password for code-server when accessed over LAN or from external browsers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Secure Password", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = passwordInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSetCodeServerPassword(trimmed)
                            Toast.makeText(context, "VS Code Server password updated", Toast.LENGTH_SHORT).show()
                            showCodeServerPasswordDialog = false
                        }
                    },
                    enabled = passwordInput.trim().isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodeServerPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showVncPasswordDialog) {
        var passwordInput by remember { mutableStateOf(container.effectiveVncPassword) }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showVncPasswordDialog = false },
            title = {
                Text(
                    text = "Set VNC Server Password",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configure a custom password for TigerVNC remote desktop access (AVNC, bVNC, RealVNC).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                            Text(
                                text = "Standard VNC authentication typically uses up to 8 characters.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
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
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Random (8 chars)", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                passwordInput = container.userPassword
                                passwordVisible = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("User Password", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = passwordInput.trim()
                        if (trimmed.isNotBlank()) {
                            onSetVncPassword(trimmed)
                            Toast.makeText(context, "VNC Server password updated", Toast.LENGTH_SHORT).show()
                            showVncPasswordDialog = false
                        }
                    },
                    enabled = passwordInput.trim().isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVncPasswordDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
