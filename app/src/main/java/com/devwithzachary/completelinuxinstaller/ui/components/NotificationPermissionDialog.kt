package com.devwithzachary.completelinuxinstaller.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.devwithzachary.completelinuxinstaller.R

@Composable
fun NotificationPermissionRationaleHandler(
    onPermissionGranted: () -> Unit = {}
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE) }
    var isDismissed by remember {
        mutableStateOf(prefs.getBoolean("notification_rationale_dismissed", false))
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            onPermissionGranted()
        }
    }

    if (!hasPermission && !isDismissed) {
        AlertDialog(
            onDismissRequest = {
                isDismissed = true
                prefs.edit().putBoolean("notification_rationale_dismissed", true).apply()
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            iconContentColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_notification_permission_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_notification_permission_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDismissed = true
                        prefs.edit().putBoolean("notification_rationale_dismissed", true).apply()
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) {
                    Text(stringResource(R.string.dialog_notification_permission_grant))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        isDismissed = true
                        prefs.edit().putBoolean("notification_rationale_dismissed", true).apply()
                    }
                ) {
                    Text(stringResource(R.string.dialog_notification_permission_not_now))
                }
            }
        )
    }
}
