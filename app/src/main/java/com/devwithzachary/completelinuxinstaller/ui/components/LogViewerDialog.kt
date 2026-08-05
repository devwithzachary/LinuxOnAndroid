package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage

@Composable
fun LogViewerDialog(
    pkg: SoftwarePackage,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF)
                        )
                        Column {
                            Text(
                                text = pkg.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Terminal Output Log",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF8B949E)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF8B949E)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF30363D))

                // Status Ribbon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0D1117), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (pkg.status) {
                            InstallStatus.INSTALLING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF58A6FF)
                                )
                                Text(
                                    text = "INSTALLING PACKAGES...",
                                    color = Color(0xFF58A6FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            InstallStatus.INSTALLED -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF3FB950), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "INSTALLATION COMPLETE",
                                    color = Color(0xFF3FB950),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            InstallStatus.FAILED -> {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF85149), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "INSTALLATION FAILED",
                                    color = Color(0xFFF85149),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                            InstallStatus.NOT_INSTALLED -> {
                                Text(
                                    text = "NOT INSTALLED",
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "UTF-8",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Terminal Logs Box
                val scrollState = rememberLazyListState()
                val logLines = pkg.installLogs.lines()

                LaunchedEffect(logLines.size) {
                    if (logLines.isNotEmpty()) {
                        scrollState.animateScrollToItem(logLines.size - 1)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp)),
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(logLines) { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    text = line,
                                    color = when {
                                        line.contains("ERROR", ignoreCase = true) || line.contains("Failed", ignoreCase = true) -> Color(0xFFF85149)
                                        line.contains("Setting up", ignoreCase = true) || line.contains("Unpacking", ignoreCase = true) -> Color(0xFF79C0FF)
                                        line.contains("Processing triggers", ignoreCase = true) -> Color(0xFFD2A8FF)
                                        line.contains("completed successfully", ignoreCase = true) -> Color(0xFF56D364)
                                        else -> Color(0xFFC9D1D9)
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Footer Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close Log Viewer")
                    }
                }
            }
        }
    }
}
