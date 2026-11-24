package com.devwithzachary.completelinuxinstaller.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class LoaStatusTone(val defaultIcon: ImageVector) {
    Running(Icons.Rounded.PlayCircle),
    Stopped(Icons.Rounded.StopCircle),
    Downloading(Icons.Rounded.Downloading),
    Warning(Icons.Rounded.Warning),
    Error(Icons.Rounded.Error)
}

@Composable
fun LoaStatusBadge(
    label: String,
    tone: LoaStatusTone,
    modifier: Modifier = Modifier,
    icon: ImageVector = tone.defaultIcon
) {
    val containerColor = when (tone) {
        LoaStatusTone.Running -> MaterialTheme.colorScheme.secondaryContainer
        LoaStatusTone.Stopped -> MaterialTheme.colorScheme.surfaceVariant
        LoaStatusTone.Downloading -> MaterialTheme.colorScheme.primaryContainer
        LoaStatusTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer
        LoaStatusTone.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (tone) {
        LoaStatusTone.Running -> MaterialTheme.colorScheme.onSecondaryContainer
        LoaStatusTone.Stopped -> MaterialTheme.colorScheme.onSurfaceVariant
        LoaStatusTone.Downloading -> MaterialTheme.colorScheme.onPrimaryContainer
        LoaStatusTone.Warning -> MaterialTheme.colorScheme.onTertiaryContainer
        LoaStatusTone.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
