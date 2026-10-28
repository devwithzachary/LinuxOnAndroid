package com.devwithzachary.completelinuxinstaller.ui.screens.container.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devwithzachary.completelinuxinstaller.model.ContainerInstance
import com.devwithzachary.completelinuxinstaller.ui.BackupState

@Composable
fun ContainerBackupCard(
    container: ContainerInstance,
    backupState: BackupState,
    onExportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onImportContainer: (contentResolver: ContentResolver, uri: Uri) -> Unit,
    onDismissBackupStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/gzip")
    ) { uri ->
        uri?.let { onExportContainer(context.contentResolver, it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onImportContainer(context.contentResolver, it) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Backup & Restore Container", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "Export this rootfs container to a portable .tar.gz archive or restore a previous snapshot directly into this container.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (backupState is BackupState.Processing) {
                LinearProgressIndicator(
                    progress = { if (backupState.progressPercent >= 0) backupState.progressPercent / 100f else 0f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(backupState.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val defaultName = "${container.name.lowercase().replace(" ", "_")}_backup_${System.currentTimeMillis()}.tar.gz"
                        exportLauncher.launch(defaultName)
                    },
                    enabled = backupState !is BackupState.Processing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Backup")
                }

                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/gzip", "application/x-gzip", "application/octet-stream", "*/*"))
                    },
                    enabled = backupState !is BackupState.Processing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Archive")
                }
            }
        }
    }
}
