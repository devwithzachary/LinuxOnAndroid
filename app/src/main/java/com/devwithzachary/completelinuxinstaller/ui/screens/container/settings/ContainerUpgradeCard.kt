package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.BuildConfig
import com.devwithzachary.completelinuxinstaller.engine.RootfsMigrationManager
import com.devwithzachary.completelinuxinstaller.engine.UpgradeState
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContainerUpgradeCard(
    container: ContainerInstance,
    upgradeState: UpgradeState,
    onUpgradeRootfs: () -> Unit,
    onDismissUpgradeState: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rootfsVersion = remember(container.rootDirPath, upgradeState) {
        RootfsMigrationManager.readVersion(File(container.rootDirPath))
    }
    val hasPendingUpdates = remember(container.rootDirPath, rootfsVersion, upgradeState) {
        val currentCode = rootfsVersion?.versionCode ?: RootfsMigrationManager.LEGACY_VERSION_CODE
        RootfsMigrationManager.hasRootfsImprovements(currentCode, BuildConfig.VERSION_CODE)
    }
    val currentVersionLabel = when {
        rootfsVersion == null -> "Legacy v1.0.0 (Unversioned)"
        else -> "v${rootfsVersion.versionName} (Build ${rootfsVersion.versionCode})"
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
    val lastUpdatedLabel = rootfsVersion?.lastUpgradedAt?.let { dateFormat.format(Date(it)) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Upgrade, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("RootFS Upgrade & Patching", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            // Update Status Badge on its own line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (hasPendingUpdates) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                            Text(
                                text = "Update Available",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF4CAF50)))
                            Text(
                                text = "Up to Date",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Apply core runtime migrations, fix permissions, and update system scripts for this ${container.name} rootfs.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Version status panel
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Installed RootFS Build:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentVersionLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Latest App Target:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (lastUpdatedLabel != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last Patch Check:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(lastUpdatedLabel, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (upgradeState is UpgradeState.Upgrading) {
                LinearProgressIndicator(
                    progress = { upgradeState.progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(upgradeState.currentStepName, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUpgradeRootfs,
                    enabled = upgradeState !is UpgradeState.Upgrading,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasPendingUpdates) "Upgrade RootFS" else "Re-verify & Patch RootFS")
                }
            }
        }
    }

    if (upgradeState is UpgradeState.Success || upgradeState is UpgradeState.Error) {
        val isSuccess = upgradeState is UpgradeState.Success
        val logs = when (upgradeState) {
            is UpgradeState.Success -> upgradeState.logs
            is UpgradeState.Error -> upgradeState.logs
        }
        val title = if (isSuccess) "RootFS System Verified & Up to Date" else "RootFS Upgrade Error"
        val icon = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
        val iconColor = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

        AlertDialog(
            onDismissRequest = onDismissUpgradeState,
            icon = { Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp)) },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSuccess) {
                        Text(
                            text = "RootFS for ${container.name} is fully verified and running runtime build v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}). Core permissions and system files are intact.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        val errorState = upgradeState as UpgradeState.Error
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (logs.isNotEmpty()) {
                        Text("Execution Logs:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E1E1E)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(logs) { logLine ->
                                    Text(
                                        text = logLine,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (logLine.contains("FAIL") || logLine.contains("ERROR"))
                                            MaterialTheme.colorScheme.error
                                        else
                                            Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismissUpgradeState) {
                    Text("OK")
                }
            }
        )
    }
}
