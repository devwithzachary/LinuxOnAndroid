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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaBottomSheet
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupLogDialog(
    logs: List<String>,
    distroName: String? = null,
    title: String? = null,
    onDismiss: () -> Unit
) {
    val displayTitle = title ?: if (!distroName.isNullOrEmpty()) {
        "$distroName First Launch Setup Log"
    } else {
        stringResource(R.string.log_viewer_setup_title)
    }

    val lazyListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            lazyListState.animateScrollToItem(logs.size - 1)
        }
    }

    LoaBottomSheet(
        onDismissRequest = onDismiss,
        title = displayTitle,
        subtitle = stringResource(R.string.log_viewer_terminal_output_log)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 380.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            if (logs.isEmpty()) {
                Text(
                    text = stringResource(R.string.log_initializing),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = LoaMonoTextStyle,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logs) { line ->
                        Text(
                            text = line,
                            color = when {
                                line.startsWith("ERROR") || line.contains("E: ") -> MaterialTheme.colorScheme.error
                                line.startsWith("Get:") || line.startsWith("Executing") -> MaterialTheme.colorScheme.primary
                                line.contains("successfully") || line.startsWith("Extracted") -> MaterialTheme.colorScheme.secondary
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
                Text(stringResource(R.string.btn_close_log))
            }
        }
    }
}
