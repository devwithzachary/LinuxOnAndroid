package com.devwithzachary.completelinuxinstaller.ui.screens.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwareCategory
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareHubScreen(
    packages: List<SoftwarePackage>,
    onInstallPackageClick: (String) -> Unit,
    onInstallCustomPackageClick: (String) -> Unit = {},
    onLaunchPackageClick: (String) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf<SoftwareCategory?>(null) }
    var activeLogPackageId by remember { mutableStateOf<String?>(null) }
    var customPackageInput by remember { mutableStateOf("") }

    val filteredPackages = if (selectedCategory == null) {
        packages
    } else {
        packages.filter { it.category == selectedCategory }
    }

    val activePackage = packages.find { it.id == activeLogPackageId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Software & Package Hub",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Custom Package Quick Search & Install Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Install Any Apt Package",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customPackageInput,
                        onValueChange = { customPackageInput = it },
                        placeholder = { Text("e.g. ffmpeg, rustc, golang, git", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (customPackageInput.isNotBlank()) {
                                val name = customPackageInput.trim()
                                onInstallCustomPackageClick(name)
                                activeLogPackageId = "custom_${name.lowercase().replace(" ", "_")}"
                                customPackageInput = ""
                            }
                        })
                    )

                    Button(
                        onClick = {
                            if (customPackageInput.isNotBlank()) {
                                val name = customPackageInput.trim()
                                onInstallCustomPackageClick(name)
                                activeLogPackageId = "custom_${name.lowercase().replace(" ", "_")}"
                                customPackageInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Install")
                    }
                }
            }
        }

        // Category Filter Chips
        SecondaryScrollableTabRow(
            selectedTabIndex = if (selectedCategory == null) 0 else SoftwareCategory.entries.indexOf(selectedCategory) + 1,
            edgePadding = 0.dp,
            divider = {}
        ) {
            Tab(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                text = { Text("All (${packages.size})") }
            )
            SoftwareCategory.entries.forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat.displayName) }
                )
            }
        }

        // Package Cards List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredPackages) { pkg ->
                SoftwareCard(
                    pkg = pkg,
                    onInstallClick = {
                        onInstallPackageClick(pkg.id)
                        activeLogPackageId = pkg.id
                    },
                    onViewLogsClick = {
                        activeLogPackageId = pkg.id
                    },
                    onLaunchClick = { cmd ->
                        onLaunchPackageClick(cmd)
                    }
                )
            }
        }
    }

    // Large Terminal Output Popup Dialog
    activePackage?.let { pkg ->
        Dialog(
            onDismissRequest = { activeLogPackageId = null },
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

                        IconButton(onClick = { activeLogPackageId = null }) {
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
                            onClick = { activeLogPackageId = null },
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
}

@Composable
fun SoftwareCard(
    pkg: SoftwarePackage,
    onInstallClick: () -> Unit,
    onViewLogsClick: () -> Unit,
    onLaunchClick: (String) -> Unit = {}
) {
    val icon = when (pkg.iconName) {
        "DesktopWindows" -> Icons.Default.DesktopWindows
        "Code" -> Icons.Default.Code
        "Dns" -> Icons.Default.Dns
        "Terminal" -> Icons.Default.Terminal
        "Security" -> Icons.Default.Security
        "Android" -> Icons.Default.Android
        else -> Icons.Default.Apps
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pkg.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = pkg.category.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    when (pkg.status) {
                        InstallStatus.INSTALLED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Installed", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                        InstallStatus.INSTALLING -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        InstallStatus.FAILED -> {
                            Text("Failed", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        }
                        InstallStatus.NOT_INSTALLED -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Not Installed",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = pkg.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pkg.postInstallNotes != null && pkg.status == InstallStatus.INSTALLED) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: " + pkg.postInstallNotes,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pkg.status == InstallStatus.INSTALLING) {
                    Button(
                        onClick = onViewLogsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Terminal Output")
                    }
                } else if (pkg.status == InstallStatus.NOT_INSTALLED || pkg.status == InstallStatus.FAILED) {
                    Button(
                        onClick = onInstallClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1-Click Install")
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pkg.launchCommand != null) {
                            Button(
                                onClick = { onLaunchClick(pkg.launchCommand) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Service")
                            }
                        }
                        OutlinedButton(onClick = onViewLogsClick) {
                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Logs")
                        }
                    }
                }
            }
        }
    }
}
