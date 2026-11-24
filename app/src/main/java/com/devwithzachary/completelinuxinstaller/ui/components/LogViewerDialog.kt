package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.model.InstallStatus
import com.devwithzachary.completelinuxinstaller.model.SoftwarePackage
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerDialog(
    pkg: SoftwarePackage,
    onDismiss: () -> Unit
) {
    val statusLabel = when (pkg.status) {
        InstallStatus.UNINSTALLING -> "Uninstalling"
        InstallStatus.INSTALLING -> stringResource(R.string.log_status_installing)
        InstallStatus.INSTALLED -> stringResource(R.string.log_status_complete)
        InstallStatus.FAILED -> stringResource(R.string.log_status_failed)
        InstallStatus.NOT_INSTALLED -> stringResource(R.string.log_status_not_installed)
    }
    val statusTone = when (pkg.status) {
        InstallStatus.INSTALLING, InstallStatus.UNINSTALLING -> LoaStatusTone.Downloading
        InstallStatus.INSTALLED -> LoaStatusTone.Running
        InstallStatus.FAILED -> LoaStatusTone.Error
        InstallStatus.NOT_INSTALLED -> LoaStatusTone.Stopped
    }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = pkg.name,
        subtitle = stringResource(R.string.log_viewer_terminal_output_log)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoaStatusBadge(label = statusLabel, tone = statusTone)
                if (pkg.status == InstallStatus.INSTALLING || pkg.status == InstallStatus.UNINSTALLING) {
                    LoaProgressRing(
                        progress = null,
                        size = 20.dp,
                        strokeWidth = 3.dp
                    )
                }
            }

            LoaMonospaceChip(text = "UTF-8")
        }

        val scrollState = rememberLazyListState()
        val logLines = pkg.installLogs.lines().filter { it.isNotBlank() }

        LaunchedEffect(logLines.size) {
            if (logLines.isNotEmpty()) {
                scrollState.animateScrollToItem(logLines.size - 1)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 380.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            if (logLines.isEmpty()) {
                Text(
                    text = stringResource(R.string.log_initializing),
                    style = LoaMonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logLines) { line ->
                        Text(
                            text = line,
                            color = when {
                                line.contains("ERROR", ignoreCase = true) ||
                                    line.contains("Failed", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                line.contains("Setting up", ignoreCase = true) ||
                                    line.contains("Unpacking", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                                line.contains("Processing triggers", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
                                line.contains("completed successfully", ignoreCase = true) -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            style = LoaMonoTextStyle
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(R.string.btn_close_log_viewer))
            }
        }
    }
}
