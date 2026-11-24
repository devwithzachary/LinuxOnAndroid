package com.devwithzachary.completelinuxinstaller.ui.screens.wizard.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.model.DistroCatalog
import com.devwithzachary.completelinuxinstaller.model.DistroDefinition
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedGroup

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WizardDistroStep(
    selectedDistro: DistroDefinition,
    hasExistingContainers: Boolean,
    onSelectDistro: (DistroDefinition) -> Unit,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Choose a Linux Distribution",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Select a distribution to install inside a rootless PRoot container.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LoaSegmentedGroup {
            DistroCatalog.ALL_DISTROS.forEach { distro ->
                val isSelected = distro.id == selectedDistro.id
                item { shape ->
                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val secondaryTextColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val chipContainer = if (isSelected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    }

                    Surface(
                        onClick = { onSelectDistro(distro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = shape,
                        color = containerColor,
                        contentColor = contentColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    itemVerticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = distro.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (distro.tag.isNotBlank()) {
                                        LoaMonospaceChip(
                                            text = distro.tag,
                                            containerColor = chipContainer,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = distro.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = secondaryTextColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LoaMonospaceChip(
                                        text = "${distro.formattedDownloadSize} dl",
                                        containerColor = chipContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    LoaMonospaceChip(
                                        text = "${distro.formattedInstalledSize} disk",
                                        containerColor = chipContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isSelected) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.RadioButtonUnchecked
                                },
                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
