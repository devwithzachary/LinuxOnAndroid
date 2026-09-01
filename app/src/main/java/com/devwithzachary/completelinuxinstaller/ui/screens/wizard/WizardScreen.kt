package com.devwithzachary.completelinuxinstaller.ui.screens.wizard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.components.SetupLogDialog

enum class WizardStep(val stepNumber: Int, val title: String) {
    DISTRO(1, "Distro"),
    CONFIG(2, "Config"),
    PERMISSIONS(3, "Permissions"),
    INSTALL(4, "Install")
}

@Composable
fun WizardScreen(
    downloadState: DownloadState,
    hasExistingContainers: Boolean = false,
    onStartInstallDistro: (distroDef: DistroDefinition, containerName: String, rootPassword: String, username: String, userPassword: String) -> Unit,
    onConfigureAccountsClick: (rootPassword: String, username: String, userPassword: String) -> Unit,
    onFinishWizardClick: () -> Unit,
    onCancel: () -> Unit = onFinishWizardClick
) {
    val scrollState = rememberScrollState()
    var showLogDialog by remember { mutableStateOf(false) }

    var selectedDistro by remember { mutableStateOf(DistroCatalog.UBUNTU_26_04) }
    var containerName by remember { mutableStateOf(selectedDistro.name) }
    var rootPassword by remember { mutableStateOf("root") }
    var username by remember { mutableStateOf("user") }
    var userPassword by remember { mutableStateOf("user") }

    var currentStep by remember { mutableStateOf(WizardStep.DISTRO) }

    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            currentStep == WizardStep.CONFIG -> currentStep = WizardStep.DISTRO
            currentStep == WizardStep.PERMISSIONS -> currentStep = WizardStep.CONFIG
            currentStep == WizardStep.DISTRO && hasExistingContainers -> onCancel()
            currentStep == WizardStep.INSTALL && downloadState is DownloadState.Success -> onFinishWizardClick()
            currentStep == WizardStep.INSTALL && downloadState is DownloadState.Error && hasExistingContainers -> onCancel()
        }
    }

    // Auto-advance to Install step if installation is underway or completed; reset to DISTRO if Idle
    LaunchedEffect(downloadState) {
        if (downloadState !is DownloadState.Idle) {
            currentStep = WizardStep.INSTALL
        } else {
            currentStep = WizardStep.DISTRO
        }
    }

    LaunchedEffect(selectedDistro) {
        containerName = selectedDistro.name
        username = if (selectedDistro.id == "ubuntu_26_04") "ubuntu" else "user"
        userPassword = if (selectedDistro.id == "ubuntu_26_04") "ubuntu" else "user"
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Step Progress Indicator
        WizardStepIndicator(currentStep = currentStep)

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WizardStepAnimation"
                ) { step ->
                    when (step) {
                        // Step 1: Choose Linux Distribution
                        WizardStep.DISTRO -> {
                            DistroSelectionStep(
                                selectedDistro = selectedDistro,
                                hasExistingContainers = hasExistingContainers,
                                onSelectDistro = { selectedDistro = it },
                                onNext = { currentStep = WizardStep.CONFIG },
                                onCancel = onCancel
                            )
                        }

                        // Step 2: Container & Account Configuration
                        WizardStep.CONFIG -> {
                            ContainerConfigStep(
                                selectedDistro = selectedDistro,
                                containerName = containerName,
                                onContainerNameChange = { containerName = it },
                                rootPassword = rootPassword,
                                onRootPasswordChange = { rootPassword = it },
                                username = username,
                                onUsernameChange = { username = it },
                                userPassword = userPassword,
                                onUserPasswordChange = { userPassword = it },
                                onBack = { currentStep = WizardStep.DISTRO },
                                onNext = { currentStep = WizardStep.PERMISSIONS }
                            )
                        }

                        // Step 3: Device Permissions (Storage & Notifications)
                        WizardStep.PERMISSIONS -> {
                            PermissionsStep(
                                selectedDistro = selectedDistro,
                                onBack = { currentStep = WizardStep.CONFIG },
                                onStartInstall = {
                                    currentStep = WizardStep.INSTALL
                                    onStartInstallDistro(
                                        selectedDistro,
                                        containerName.ifBlank { selectedDistro.name },
                                        rootPassword.ifBlank { "root" },
                                        username.ifBlank { "user" },
                                        userPassword.ifBlank { "user" }
                                    )
                                }
                            )
                        }

                        // Step 4: Download, Archive Extraction & Completion
                        WizardStep.INSTALL -> {
                            InstallationStep(
                                selectedDistro = selectedDistro,
                                downloadState = downloadState,
                                onShowLogs = { showLogDialog = true },
                                onRetry = {
                                    onStartInstallDistro(
                                        selectedDistro,
                                        containerName.ifBlank { selectedDistro.name },
                                        rootPassword.ifBlank { "root" },
                                        username.ifBlank { "user" },
                                        userPassword.ifBlank { "user" }
                                    )
                                },
                                onFinish = onFinishWizardClick
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        val currentLogs = when (downloadState) {
            is DownloadState.Extracting -> downloadState.logs
            is DownloadState.Success -> downloadState.logs
            is DownloadState.Error -> downloadState.logs
            else -> emptyList()
        }

        SetupLogDialog(
            logs = currentLogs,
            distroName = selectedDistro.name,
            onDismiss = { showLogDialog = false }
        )
    }
}

@Composable
private fun WizardStepIndicator(currentStep: WizardStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WizardStep.values().forEachIndexed { index, step ->
            val isCurrent = step == currentStep
            val isCompleted = step.stepNumber < currentStep.stepNumber

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isCompleted -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "${step.stepNumber}",
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = step.title,
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (index < WizardStep.values().size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    color = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DistroSelectionStep(
    selectedDistro: DistroDefinition,
    hasExistingContainers: Boolean,
    onSelectDistro: (DistroDefinition) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Step 1: Choose Distribution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select a Linux distribution to install inside PRoot on your Android device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

        DistroCatalog.ALL_DISTROS.forEach { distro ->
            val isSelected = distro.id == selectedDistro.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDistro(distro) },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(distro.colorHex), shape = CircleShape)
                            )
                            Text(
                                text = distro.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Badge(containerColor = MaterialTheme.colorScheme.surface) {
                            Text("~${distro.expectedSizeMb} MB", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }

                    Text(
                        text = distro.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Next: Configure Container")
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        if (hasExistingContainers) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cancel & Return to Dashboard")
            }
        }
    }
}

@Composable
private fun ContainerConfigStep(
    selectedDistro: DistroDefinition,
    containerName: String,
    onContainerNameChange: (String) -> Unit,
    rootPassword: String,
    onRootPasswordChange: (String) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    userPassword: String,
    onUserPasswordChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Step 2: Container Configuration",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Set a custom display name for this container and configure credentials.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Selected Distro Summary Chip
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(selectedDistro.colorHex), shape = CircleShape)
                )
                Text(
                    text = "Distro: ${selectedDistro.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

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
                    value = containerName,
                    onValueChange = onContainerNameChange,
                    label = { Text("Container Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rootPassword,
                    onValueChange = onRootPasswordChange,
                    label = { Text(stringResource(R.string.label_root_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text(stringResource(R.string.label_regular_username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = userPassword,
                    onValueChange = onUserPasswordChange,
                    label = { Text(stringResource(R.string.label_regular_password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next: Permissions")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PermissionsStep(
    selectedDistro: DistroDefinition,
    onBack: () -> Unit,
    onStartInstall: () -> Unit
) {
    val context = LocalContext.current
    var isStorageGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_VIDEO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        isStorageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VIDEO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Step 3: System Permissions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Configure device permissions to allow file mounting and uninterrupted background execution.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Storage Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isStorageGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isStorageGranted) Icons.Default.CheckCircle else Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (isStorageGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Device Storage & SDCard Mount",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Text(
                    text = "Linux containers mount /sdcard and Downloads directly. Granting storage access lets container applications read and write your host files.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isStorageGranted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                        Text("Storage Permission Granted", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_MEDIA_IMAGES,
                                        android.Manifest.permission.READ_MEDIA_VIDEO,
                                        android.Manifest.permission.READ_MEDIA_AUDIO
                                    )
                                )
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Grant Storage Access")
                    }
                }
            }
        }

        // Notification Permission Card (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            var isNotificationGranted by remember {
                mutableStateOf(
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                )
            }

            val notificationLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted ->
                isNotificationGranted = granted
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNotificationGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isNotificationGranted) Icons.Default.CheckCircle else Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isNotificationGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Background Execution Notifications",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Text(
                        text = "Allows the foreground service to show active terminal sessions and servers in your notification shade, preventing process kills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isNotificationGranted) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Text("Notification Permission Granted", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enable Notifications")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }

            Button(
                onClick = onStartInstall,
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Install")
            }
        }
    }
}

@Composable
private fun InstallationStep(
    selectedDistro: DistroDefinition,
    downloadState: DownloadState,
    onShowLogs: () -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        when (downloadState) {
            is DownloadState.Success -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color(0xFF4CAF50)
                )

                Text(
                    text = "Installation Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${selectedDistro.name} rootfs has been installed and configured successfully. You can now launch a terminal session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Terminal & Dashboard")
                }

                if (downloadState.logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onShowLogs,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_view_setup_logs))
                    }
                }
            }

            is DownloadState.Downloading -> {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Downloading ${selectedDistro.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                LinearProgressIndicator(
                    progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                )

                Text(
                    text = "Downloading: ${downloadState.progressPercent}% (${downloadState.bytesDownloaded / (1024 * 1024)} MB / ~${downloadState.totalBytes / (1024 * 1024)} MB)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            is DownloadState.Extracting -> {
                CircularProgressIndicator(modifier = Modifier.size(44.dp))

                Text(
                    text = "Extracting & Configuring ${selectedDistro.name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = downloadState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                if (downloadState.logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onShowLogs,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Expand Logs (${downloadState.logs.size} lines)")
                    }
                }
            }

            is DownloadState.Error -> {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Installation Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = downloadState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                if (downloadState.logs.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onShowLogs,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_view_failure_logs))
                    }
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry Installation")
                }
            }

            is DownloadState.Idle -> {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
                Text("Initializing setup...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
