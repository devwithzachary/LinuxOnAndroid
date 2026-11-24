package com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone

@Composable
fun WizardPermissionsStep(
    selectedDistro: DistroDefinition,
    onBack: () -> Unit,
    onStartInstall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current

    fun checkStoragePermission(): Boolean {
        if (isInspection) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var isStorageGranted by remember { mutableStateOf(checkStoragePermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        isStorageGranted = checkStoragePermission()
    }

    val supportsRuntimeNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var isNotificationGranted by remember {
        mutableStateOf(
            if (isInspection || !supportsRuntimeNotif) true
            else ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotificationGranted = granted
    }

    val requestStorage: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "System Permissions",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Optional permissions for mounting Android storage and keeping background sessions alive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LoaSectionHeader(title = "Device Access")

            LoaSegmentedGroup {
                row(
                    title = "Device Storage (/sdcard)",
                    subtitle = "Allow ${selectedDistro.name} to read and write host files in /sdcard",
                    leadingIcon = Icons.Rounded.Folder,
                    trailingContent = {
                        if (isStorageGranted) {
                            LoaStatusBadge(
                                label = "Granted",
                                tone = LoaStatusTone.Running
                            )
                        } else {
                            FilledTonalButton(onClick = requestStorage) {
                                Text("Grant")
                            }
                        }
                    },
                    onClick = if (!isStorageGranted) requestStorage else null
                )

                if (supportsRuntimeNotif) {
                    row(
                        title = "Background Notifications",
                        subtitle = "Show active terminal and server status in your notification shade",
                        leadingIcon = Icons.Rounded.Notifications,
                        trailingContent = {
                            if (isNotificationGranted) {
                                LoaStatusBadge(
                                    label = "Granted",
                                    tone = LoaStatusTone.Running
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                ) {
                                    Text("Enable")
                                }
                            }
                        },
                        onClick = if (!isNotificationGranted) {
                            { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
