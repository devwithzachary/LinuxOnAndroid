package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ContainerNetworkCard(
    dnsServers: List<String>,
    onSetDnsServers: (servers: List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDnsDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
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
}
