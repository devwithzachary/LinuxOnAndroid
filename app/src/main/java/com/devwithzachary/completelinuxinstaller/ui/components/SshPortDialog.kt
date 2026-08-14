package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SshPortDialog(
    initialPort: Int = 2222,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var portText by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    val parsedPort = portText.trim().toIntOrNull()
    val isValid = parsedPort != null && parsedPort in 1..65535
    val isPrivileged = parsedPort != null && parsedPort < 1024

    val presetPorts = listOf(2222, 8022, 2022, 22)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Start SSH Server",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configure the listening TCP port for the OpenSSH daemon. Ports \u2265 1024 (e.g. 2222) are recommended.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetPorts.forEach { port ->
                        val isSelected = portText == port.toString()
                        FilterChip(
                            selected = isSelected,
                            onClick = { portText = port.toString() },
                            label = {
                                Text(
                                    text = if (port == 2222) "2222 (Default)" else port.toString(),
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }

                // Port Input Field
                OutlinedTextField(
                    value = portText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        if (filtered.length <= 5) {
                            portText = filtered
                        }
                    },
                    label = { Text("SSH Port") },
                    placeholder = { Text("2222") },
                    singleLine = true,
                    isError = portText.isNotBlank() && !isValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (portText.isNotEmpty()) {
                            IconButton(onClick = { portText = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    supportingText = {
                        if (portText.isNotBlank() && !isValid) {
                            Text(
                                text = "Port must be between 1 and 65535",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (isValid && isPrivileged) {
                            Text(
                                text = "Note: Ports < 1024 may require root permissions.",
                                color = Color(0xFFFFA726)
                            )
                        } else {
                            Text("Standard Linux range: 1024 - 65535")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Connection Command Preview Box
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Connect command:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ssh <username>@<device-ip> -p ${if (isValid) parsedPort else "PORT"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedPort?.let { port ->
                        if (port in 1..65535) {
                            onConfirm(port)
                        }
                    }
                },
                enabled = isValid
            ) {
                Text("Start SSH Server")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
