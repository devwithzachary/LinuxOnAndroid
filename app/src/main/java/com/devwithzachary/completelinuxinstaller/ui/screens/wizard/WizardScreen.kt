package com.devwithzachary.completelinuxinstaller.ui.screens.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.components.SetupLogDialog
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardConfigStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardDistroStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardInstallStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardPermissionsStep

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
    onCancel: () -> Unit = onFinishWizardClick,
    onResetWizardState: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showLogDialog by remember { mutableStateOf(false) }

    var selectedDistro by remember { mutableStateOf(DistroCatalog.UBUNTU_26_04) }
    var containerName by remember { mutableStateOf(selectedDistro.name) }
    var rootPassword by remember { mutableStateOf("root") }
    var username by remember { mutableStateOf("user") }
    var userPassword by remember { mutableStateOf("user") }

    var currentStep by remember { mutableStateOf(WizardStep.DISTRO) }

    BackHandler(enabled = true) {
        when {
            currentStep == WizardStep.CONFIG -> currentStep = WizardStep.DISTRO
            currentStep == WizardStep.PERMISSIONS -> currentStep = WizardStep.CONFIG
            currentStep == WizardStep.DISTRO && hasExistingContainers -> onCancel()
            currentStep == WizardStep.INSTALL && downloadState is DownloadState.Success -> onFinishWizardClick()
            currentStep == WizardStep.INSTALL && downloadState is DownloadState.Error -> {
                onResetWizardState()
                currentStep = WizardStep.DISTRO
            }
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
        Column(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        modifier = Modifier.fillMaxWidth(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "WizardStepAnimation"
                    ) { step ->
                        when (step) {
                            // Step 1: Choose Linux Distribution
                            WizardStep.DISTRO -> {
                                WizardDistroStep(
                                    selectedDistro = selectedDistro,
                                    hasExistingContainers = hasExistingContainers,
                                    onSelectDistro = { selectedDistro = it },
                                    onNext = { currentStep = WizardStep.CONFIG },
                                    onCancel = onCancel
                                )
                            }

                            // Step 2: Container & Account Configuration
                            WizardStep.CONFIG -> {
                                WizardConfigStep(
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
                                WizardPermissionsStep(
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
                                WizardInstallStep(
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
                                    onSelectDifferentDistro = {
                                        onResetWizardState()
                                        currentStep = WizardStep.DISTRO
                                    },
                                    onCancel = if (hasExistingContainers) onCancel else null,
                                    onFinish = onFinishWizardClick
                                )
                            }
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
        WizardStep.entries.forEachIndexed { index, step ->
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
                    modifier = Modifier.size(32.dp)
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
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

            if (index < WizardStep.entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (isCompleted) Color(0xFF4CAF50).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
