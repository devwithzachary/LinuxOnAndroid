package com.devwithzachary.completelinuxinstaller.ui.screens.wizard

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.engine.DownloadState
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardConfigStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardDistroStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardInstallStep
import com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps.WizardPermissionsStep
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

enum class WizardStep(val stepNumber: Int, val title: String) {
    DISTRO(1, "Distro"),
    CONFIG(2, "Config"),
    PERMISSIONS(3, "Permissions"),
    INSTALL(4, "Install")
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var showLogSheet by remember { mutableStateOf(false) }

    var selectedDistro by remember { mutableStateOf(DistroCatalog.UBUNTU_26_04) }
    var containerName by remember { mutableStateOf(selectedDistro.name) }
    var rootPassword by remember { mutableStateOf("root") }
    var username by remember { mutableStateOf("user") }
    var userPassword by remember { mutableStateOf("user") }

    var currentStep by remember { mutableStateOf(WizardStep.DISTRO) }

    val startInstallation: () -> Unit = {
        currentStep = WizardStep.INSTALL
        onStartInstallDistro(
            selectedDistro,
            containerName.ifBlank { selectedDistro.name },
            rootPassword.ifBlank { "root" },
            username.ifBlank { "user" },
            userPassword.ifBlank { "user" }
        )
    }

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

    val animatedStepProgress by animateFloatAsState(
        targetValue = currentStep.stepNumber / WizardStep.entries.size.toFloat(),
        label = "WizardStepProgress"
    )

    val currentLogs = when (downloadState) {
        is DownloadState.Extracting -> downloadState.logs
        is DownloadState.Success -> downloadState.logs
        is DownloadState.Error -> downloadState.logs
        else -> emptyList()
    }

    val showBottomBar = when (currentStep) {
        WizardStep.DISTRO, WizardStep.CONFIG, WizardStep.PERMISSIONS -> true
        WizardStep.INSTALL -> when (downloadState) {
            is DownloadState.Success, is DownloadState.Error -> true
            is DownloadState.Extracting -> currentLogs.isNotEmpty()
            else -> false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Step ${currentStep.stepNumber} of ${WizardStep.entries.size}: ${currentStep.title}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        when {
                            currentStep == WizardStep.DISTRO && hasExistingContainers -> {
                                IconButton(onClick = onCancel) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Cancel setup"
                                    )
                                }
                            }
                            currentStep == WizardStep.CONFIG -> {
                                IconButton(onClick = { currentStep = WizardStep.DISTRO }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Previous step"
                                    )
                                }
                            }
                            currentStep == WizardStep.PERMISSIONS -> {
                                IconButton(onClick = { currentStep = WizardStep.CONFIG }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Previous step"
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                LinearProgressIndicator(
                    progress = { animatedStepProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(8.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (currentStep) {
                                WizardStep.DISTRO -> {
                                    if (hasExistingContainers) {
                                        TextButton(onClick = onCancel) {
                                            Text("Cancel")
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(onClick = { currentStep = WizardStep.CONFIG }) {
                                        Text("Next")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                WizardStep.CONFIG -> {
                                    FilledTonalButton(onClick = { currentStep = WizardStep.DISTRO }) {
                                        Text("Back")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(onClick = { currentStep = WizardStep.PERMISSIONS }) {
                                        Text("Next")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                WizardStep.PERMISSIONS -> {
                                    FilledTonalButton(onClick = { currentStep = WizardStep.CONFIG }) {
                                        Text("Back")
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Button(onClick = startInstallation) {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Install")
                                    }
                                }

                                WizardStep.INSTALL -> {
                                    when (downloadState) {
                                        is DownloadState.Success -> {
                                            if (currentLogs.isNotEmpty()) {
                                                TextButton(onClick = { showLogSheet = true }) {
                                                    Text("Setup Logs")
                                                }
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Button(onClick = onFinishWizardClick) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Terminal,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Open Dashboard")
                                            }
                                        }

                                        is DownloadState.Error -> {
                                            FilledTonalButton(
                                                onClick = {
                                                    onResetWizardState()
                                                    currentStep = WizardStep.DISTRO
                                                }
                                            ) {
                                                Text("Change Distro")
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Button(onClick = startInstallation) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Refresh,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Retry")
                                            }
                                        }

                                        is DownloadState.Extracting -> {
                                            Spacer(modifier = Modifier.weight(1f))
                                            TextButton(onClick = { showLogSheet = true }) {
                                                Text("View Full Logs (${currentLogs.size})")
                                            }
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "WizardStepAnimation"
                ) { step ->
                    when (step) {
                        WizardStep.DISTRO -> {
                            WizardDistroStep(
                                selectedDistro = selectedDistro,
                                hasExistingContainers = hasExistingContainers,
                                onSelectDistro = { selectedDistro = it },
                                onNext = { currentStep = WizardStep.CONFIG },
                                onCancel = onCancel
                            )
                        }

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

                        WizardStep.PERMISSIONS -> {
                            WizardPermissionsStep(
                                selectedDistro = selectedDistro,
                                onBack = { currentStep = WizardStep.CONFIG },
                                onStartInstall = startInstallation
                            )
                        }

                        WizardStep.INSTALL -> {
                            WizardInstallStep(
                                selectedDistro = selectedDistro,
                                downloadState = downloadState,
                                onShowLogs = { showLogSheet = true },
                                onRetry = startInstallation,
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

    if (showLogSheet) {
        LoaBottomSheet(
            onDismissRequest = { showLogSheet = false },
            title = "${selectedDistro.name} Setup Logs",
            subtitle = "${currentLogs.size} log lines"
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(currentLogs) { line ->
                        Text(
                            text = line,
                            style = LoaMonoTextStyle,
                            color = if (line.contains("ERROR", ignoreCase = true) || line.contains("FAIL", ignoreCase = true)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = { showLogSheet = false }) {
                    Text("Close")
                }
            }
        }
    }
}

@Preview(
    name = "Wizard Distro - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun WizardDistroLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        WizardScreen(
            downloadState = DownloadState.Idle,
            hasExistingContainers = true,
            onStartInstallDistro = { _, _, _, _, _ -> },
            onConfigureAccountsClick = { _, _, _ -> },
            onFinishWizardClick = {}
        )
    }
}

@Preview(
    name = "Wizard Downloading - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun WizardDownloadingDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        WizardScreen(
            downloadState = DownloadState.Downloading(
                progressPercent = 64,
                bytesDownloaded = 24L * 1024 * 1024,
                totalBytes = 38L * 1024 * 1024
            ),
            onStartInstallDistro = { _, _, _, _, _ -> },
            onConfigureAccountsClick = { _, _, _ -> },
            onFinishWizardClick = {}
        )
    }
}
