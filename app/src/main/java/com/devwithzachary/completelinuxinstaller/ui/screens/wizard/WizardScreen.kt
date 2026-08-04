package com.devwithzachary.completelinuxinstaller.ui.screens.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.engine.DownloadState

@Composable
fun WizardScreen(
    downloadState: DownloadState,
    onStartInstallClick: () -> Unit,
    onConfigureAccountsClick: (rootPassword: String, username: String, userPassword: String) -> Unit,
    onFinishWizardClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    var showLogDialog by remember { mutableStateOf(false) }

    var rootPassword by remember { mutableStateOf("root") }
    var username by remember { mutableStateOf("ubuntu") }
    var userPassword by remember { mutableStateOf("ubuntu") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (downloadState) {
                    is DownloadState.Success -> {
                        // Step 2: Account & Security Configuration Page
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Account & Password Setup",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Configure your Linux root administrator password and create a regular user for secure SSH logins and sudo privileges.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = rootPassword,
                                    onValueChange = { rootPassword = it },
                                    label = { Text("Root User Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("Regular Username (Sudo)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = userPassword,
                                    onValueChange = { userPassword = it },
                                    label = { Text("Regular User Password") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onConfigureAccountsClick(
                                    rootPassword.ifBlank { "root" },
                                    username.ifBlank { "ubuntu" },
                                    userPassword.ifBlank { "ubuntu" }
                                )
                                onFinishWizardClick()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Account & Open Dashboard")
                        }

                        if (downloadState.logs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showLogDialog = true },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View Setup Logs")
                            }
                        }
                    }

                    else -> {
                        // Step 1: Clean First Page (Header + Features + Download Button)
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Complete Linux Installer",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        val annotatedLinkString = buildAnnotatedString {
                            append("Part of the LinuxonAndroid project (")
                            pushStringAnnotation(tag = "URL", annotation = "https://linuxonandroid.com")
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append("linuxonandroid.com")
                            }
                            pop()
                            append("). Run a full Linux distribution on your phone without root access.")
                        }

                        ClickableText(
                            text = annotatedLinkString,
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            onClick = { offset ->
                                annotatedLinkString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        try {
                                            uriHandler.openUri(annotation.item)
                                        } catch (_: Exception) {}
                                    }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Feature Explanation Cards
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureInfoRow(
                                icon = Icons.Default.Lock,
                                title = "No Root Required",
                                description = "Uses PRoot user-space virtualization to safely execute Linux binaries inside an isolated app sandbox."
                            )
                            FeatureInfoRow(
                                icon = Icons.Default.Storage,
                                title = "Official Ubuntu 26.04 LTS Base",
                                description = "Downloads a minimal official Ubuntu image (~30 MB) with APT package manager support."
                            )
                            FeatureInfoRow(
                                icon = Icons.Default.Terminal,
                                title = "CLI & Developer Tools",
                                description = "Install and run Python, Git, GCC, Curl, Node.js, and command-line utilities directly on your phone."
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        when (val state = downloadState) {
                            is DownloadState.Idle -> {
                                Button(
                                    onClick = onStartInstallClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download & Setup Ubuntu 26.04")
                                }
                            }

                            is DownloadState.Downloading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                    )
                                    Text(
                                        text = "Downloading: ${state.progressPercent}% (${state.bytesDownloaded / (1024 * 1024)} MB)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is DownloadState.Extracting -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    if (state.logs.isNotEmpty()) {
                                        OutlinedButton(
                                            onClick = { showLogDialog = true },
                                            modifier = Modifier.padding(top = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Expand Full Setup Logs (${state.logs.size} lines)")
                                        }
                                    }
                                }
                            }

                            is DownloadState.Error -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                if (state.logs.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { showLogDialog = true },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("View Failure Logs")
                                    }
                                }

                                Button(
                                    onClick = onStartInstallClick,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Retry Download")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Installation Log Viewer Popup
    if (showLogDialog) {
        val currentLogs = when (downloadState) {
            is DownloadState.Extracting -> downloadState.logs
            is DownloadState.Success -> downloadState.logs
            is DownloadState.Error -> downloadState.logs
            else -> emptyList()
        }

        Dialog(
            onDismissRequest = { showLogDialog = false },
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
                            Text(
                                text = "Ubuntu First Launch Setup Log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(onClick = { showLogDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF8B949E)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF30363D))

                    val lazyListState = rememberLazyListState()
                    LaunchedEffect(currentLogs.size) {
                        if (currentLogs.isNotEmpty()) {
                            lazyListState.animateScrollToItem(currentLogs.size - 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF0D1117), shape = RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        if (currentLogs.isEmpty()) {
                            Text(
                                text = "Initializing installation processes...",
                                color = Color(0xFF8B949E),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        } else {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(currentLogs) { line ->
                                    Text(
                                        text = line,
                                        color = when {
                                            line.startsWith("ERROR") || line.contains("E: ") -> Color(0xFFF85149)
                                            line.startsWith("Get:") || line.startsWith("Executing") -> Color(0xFF58A6FF)
                                            line.contains("successfully") || line.startsWith("Extracted") -> Color(0xFF3FB950)
                                            else -> Color(0xFFC9D1D9)
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showLogDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                        ) {
                            Text("Close Log", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureInfoRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
