package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.components.DebugReportDialog
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSectionHeader
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup
import kotlinx.coroutines.launch

@Composable
fun SystemDiagnosticsCard(
    onGenerateDebugReport: suspend () -> String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showDebugReportDialog by remember { mutableStateOf(false) }
    var debugReportText by remember { mutableStateOf("") }
    var isGeneratingDebugReport by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LoaSectionHeader(title = "Diagnostics")

        LoaSegmentedGroup {
            row(
                title = "Generate Debug Report",
                subtitle = "Collect app version, hardware, container health, and memory metrics",
                leadingIcon = Icons.Rounded.BugReport,
                enabled = !isGeneratingDebugReport,
                trailingContent = {
                    if (isGeneratingDebugReport) {
                        LoaProgressRing(
                            progress = null,
                            size = 24.dp,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = {
                    coroutineScope.launch {
                        isGeneratingDebugReport = true
                        try {
                            debugReportText = onGenerateDebugReport()
                            showDebugReportDialog = true
                        } finally {
                            isGeneratingDebugReport = false
                        }
                    }
                }
            )
        }
    }

    if (showDebugReportDialog) {
        DebugReportDialog(
            report = debugReportText,
            onDismiss = { showDebugReportDialog = false }
        )
    }
}
