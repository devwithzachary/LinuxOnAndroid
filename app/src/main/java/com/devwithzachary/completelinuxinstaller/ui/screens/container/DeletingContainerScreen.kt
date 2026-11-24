package com.devwithzachary.completelinuxinstaller.ui.screens.container

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.DeleteContainerState
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusBadge
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaStatusTone
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@Composable
fun DeletingContainerScreen(
    state: DeleteContainerState,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state is DeleteContainerState.Deleting) {}

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        when (state) {
            is DeleteContainerState.Deleting -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Deleting Container",
                            tone = LoaStatusTone.Error,
                            icon = Icons.Rounded.DeleteForever
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LoaProgressRing(
                            progress = null,
                            size = 96.dp,
                            strokeWidth = 10.dp,
                            indicatorColor = MaterialTheme.colorScheme.error
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = "Deleting ${state.containerName}",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        if (!state.distroName.isNullOrBlank()) {
                            LoaMonospaceChip(text = state.distroName)
                        }

                        AnimatedContent(
                            targetState = state.statusMessage,
                            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                            label = "delete_status_msg"
                        ) { targetStatus ->
                            Text(
                                text = targetStatus,
                                style = LoaMonoTextStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = LoaSegmentedSingleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = "Removing rootfs files, packages, and container storage. Please keep the app open.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            is DeleteContainerState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier.widthIn(max = 480.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LoaStatusBadge(
                            label = "Deletion Failed",
                            tone = LoaStatusTone.Error
                        )

                        Text(
                            text = "Could not delete ${state.containerName}",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = LoaSegmentedSingleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = state.errorMessage,
                                style = LoaMonoTextStyle,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Button(
                            onClick = onDismissError,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .fillMaxWidth()
                        ) {
                            Text("Return to Dashboard")
                        }
                    }
                }
            }

            is DeleteContainerState.Idle -> Unit
        }
    }
}

@Preview(
    name = "Deleting Container - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun DeletingContainerLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        DeletingContainerScreen(
            state = DeleteContainerState.Deleting(
                containerId = "ubuntu_default",
                containerName = "Ubuntu Workstation",
                distroName = "Ubuntu 26.04 LTS",
                statusMessage = "Removing /data/user/0/.../ubuntu/usr"
            ),
            onDismissError = {}
        )
    }
}

@Preview(
    name = "Deleting Error - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun DeletingContainerErrorDarkPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        DeletingContainerScreen(
            state = DeleteContainerState.Error(
                containerName = "Ubuntu Workstation",
                errorMessage = "EACCES: unable to unlink busy directory"
            ),
            onDismissError = {}
        )
    }
}
