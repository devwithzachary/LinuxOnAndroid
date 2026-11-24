package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = "Start SSH Server",
        subtitle = "Configure the listening TCP port for the OpenSSH daemon. Ports >= 1024 (e.g. 2222) are recommended."
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetPorts.forEach { port ->
                val isSelected = portText == port.toString()
                FilterChip(
                    selected = isSelected,
                    onClick = { portText = port.toString() },
                    label = {
                        Text(
                            text = if (port == 2222) "2222 (Default)" else port.toString(),
                            style = LoaMonoTextStyle
                        )
                    }
                )
            }
        }

        OutlinedTextField(
            value = portText,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() }
                if (filtered.length <= 5) {
                    portText = filtered
                }
            },
            label = { Text("SSH Port") },
            placeholder = { Text("2222", style = LoaMonoTextStyle) },
            textStyle = LoaMonoTextStyle,
            singleLine = true,
            isError = portText.isNotBlank() && !isValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                if (portText.isNotEmpty()) {
                    IconButton(onClick = { portText = "" }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear port"
                        )
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
                        color = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Text("Standard Linux range: 1024 - 65535")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Connect command:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ssh <username>@<device-ip> -p ${if (isValid) parsedPort else "PORT"}",
                    style = LoaMonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.widthIn(max = 560.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 320.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    parsedPort?.let { port ->
                        if (port in 1..65535) {
                            onConfirm(port)
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 320.dp)
            ) {
                Text("Start SSH Server")
            }
        }
    }
}
