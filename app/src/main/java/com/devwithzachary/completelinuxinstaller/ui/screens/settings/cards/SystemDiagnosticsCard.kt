package com.devwithzachary.completelinuxinstaller.ui.screens.settings.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.components.DebugReportDialog
import com.devwithzachary.completelinuxinstaller.ui.screens.settings.CollapsibleSettingsCard
import kotlinx.coroutines.launch

@Composable
fun SystemDiagnosticsCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onGenerateDebugReport: suspend () -> String,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var showDebugReportDialog by remember { mutableStateOf(false) }
    var debugReportText by remember { mutableStateOf("") }
    var isGeneratingDebugReport by remember { mutableStateOf(false) }

    CollapsibleSettingsCard(
        title = "Diagnostics & Debug Report",
        subtitle = "Generate a technical debug report for bug reports",
        icon = Icons.Default.BugReport,
        isExpanded = isExpanded,
        onToggleExpand = onToggleExpand,
        modifier = modifier
    ) {
        Text(
            text = "Generate a technical summary of your installation (app version, device hardware, container health, storage and memory metrics) to paste into GitHub issues or Discord when asking for help.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
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
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGeneratingDebugReport,
            shape = RoundedCornerShape(10.dp)
        ) {
            if (isGeneratingDebugReport) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("Generate Debug Report")
        }
    }

    if (showDebugReportDialog) {
        DebugReportDialog(
            report = debugReportText,
            onDismiss = { showDebugReportDialog = false }
        )
    }
}
