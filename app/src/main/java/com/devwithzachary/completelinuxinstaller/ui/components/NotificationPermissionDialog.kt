package com.devwithzachary.completelinuxinstaller.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.dialog_notification_permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.dialog_notification_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDismissed = true
                        prefs.edit().putBoolean("notification_rationale_dismissed", true).apply()
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    shape = RoundedCornerShape(10.dp)
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
