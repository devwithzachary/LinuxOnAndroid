package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.devwithzachary.completelinuxinstaller.R

@Composable
fun SetupLogDialog(
    logs: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF161B22),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF58A6FF)
                        )
                        Text(
                            text = stringResource(R.string.log_viewer_setup_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF8B949E)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF30363D))

                val lazyListState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) {
                        lazyListState.animateScrollToItem(logs.size - 1)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0D1117), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF21262D), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    if (logs.isEmpty()) {
                        Text(
                            text = stringResource(R.string.log_initializing),
                            color = Color(0xFF8B949E),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(logs) { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.startsWith("ERROR") || line.contains("E: ") -> Color(0xFFF85149)
                                        line.startsWith("Get:") || line.startsWith("Executing") -> Color(0xFF58A6FF)
                                        line.contains("successfully") || line.startsWith("Extracted") -> Color(0xFF3FB950)
                                        else -> Color(0xFFC9D1D9)
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636))
                    ) {
                        Text(stringResource(R.string.btn_close_log), color = Color.White)
                    }
                }
            }
        }
    }
}
