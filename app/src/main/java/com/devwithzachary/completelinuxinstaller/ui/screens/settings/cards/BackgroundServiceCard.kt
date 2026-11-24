package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup

@Composable
fun BackgroundServiceCard(
    isKeepAliveEnabled: Boolean,
    onToggleKeepAlive: () -> Unit,
    isKeepScreenOnEnabled: Boolean,
    onSetKeepScreenOn: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current

    val keepAliveTitle = stringResource(R.string.setting_keep_alive_title)
    val keepScreenOnTitle = stringResource(R.string.setting_keep_screen_on_title)
    val keepScreenOnDesc = stringResource(R.string.setting_keep_screen_on_desc)

    val needsNotificationPermission = !isInspection && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var isNotifGranted by remember {
        mutableStateOf(
            if (isInspection || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                true
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isNotifGranted = granted
    }

    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = "Background & System")

        LoaSegmentedGroup {
            row(
                title = keepAliveTitle,
                subtitle = if (isKeepAliveEnabled) {
                    "Active: holds partial CPU WakeLock during sessions"
                } else {
                    "Disabled: subject to Android background limits"
                },
                leadingIcon = Icons.Rounded.Shield,
                trailingContent = {
                    Switch(
                        checked = isKeepAliveEnabled,
                        onCheckedChange = { onToggleKeepAlive() }
                    )
                },
                onClick = onToggleKeepAlive
            )

            row(
                title = keepScreenOnTitle,
                subtitle = keepScreenOnDesc,
                leadingIcon = Icons.Rounded.BrightnessHigh,
                trailingContent = {
                    Switch(
                        checked = isKeepScreenOnEnabled,
                        onCheckedChange = onSetKeepScreenOn
                    )
                },
                onClick = { onSetKeepScreenOn(!isKeepScreenOnEnabled) }
            )

            if (needsNotificationPermission && !isNotifGranted) {
                row(
                    title = "Notification Permission Required",
                    subtitle = "Tap to allow foreground service status notifications",
                    leadingIcon = Icons.Rounded.NotificationsOff,
                    containerColor = errorContainer,
                    contentColor = onErrorContainer,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = onErrorContainer
                        )
                    },
                    onClick = {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }
        }
    }
}
