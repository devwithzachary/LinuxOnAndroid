package com.devwithzachary.completelinuxinstaller.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedBottomShape
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedMiddleShape
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedTopShape

enum class LoaSegmentedPosition {
    Single,
    Top,
    Middle,
    Bottom
}

fun loaSegmentedShape(position: LoaSegmentedPosition): Shape {
    return when (position) {
        LoaSegmentedPosition.Single -> LoaSegmentedSingleShape
        LoaSegmentedPosition.Top -> LoaSegmentedTopShape
        LoaSegmentedPosition.Middle -> LoaSegmentedMiddleShape
        LoaSegmentedPosition.Bottom -> LoaSegmentedBottomShape
    }
}

fun loaSegmentedPositionFor(index: Int, count: Int): LoaSegmentedPosition {
    return when {
        count <= 1 -> LoaSegmentedPosition.Single
        index <= 0 -> LoaSegmentedPosition.Top
        index >= count - 1 -> LoaSegmentedPosition.Bottom
        else -> LoaSegmentedPosition.Middle
    }
}

class LoaSegmentedGroupScope {
    internal val items = mutableListOf<@Composable (shape: Shape) -> Unit>()

    fun item(content: @Composable (shape: Shape) -> Unit) {
        items.add(content)
    }

    fun row(
        title: String,
        modifier: Modifier = Modifier,
        subtitle: String? = null,
        leadingIcon: ImageVector? = null,
        trailingContent: (@Composable () -> Unit)? = null,
        containerColor: Color? = null,
        contentColor: Color? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null
    ) {
        items.add { shape ->
            LoaSegmentedRow(
                title = title,
                modifier = modifier,
                subtitle = subtitle,
                leadingIcon = leadingIcon,
                trailingContent = trailingContent,
                shape = shape,
                containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
                enabled = enabled,
                onClick = onClick
            )
        }
    }
}

@Composable
fun LoaSegmentedGroup(
    modifier: Modifier = Modifier,
    content: LoaSegmentedGroupScope.() -> Unit
) {
    val scope = LoaSegmentedGroupScope().apply(content)
    val count = scope.items.size
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        scope.items.forEachIndexed { index, itemContent ->
            itemContent(loaSegmentedShape(loaSegmentedPositionFor(index, count)))
        }
    }
}

@Composable
fun LoaSegmentedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    shape: Shape = LoaSegmentedSingleShape,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val resolvedContentColor = if (enabled) {
        contentColor
    } else {
        contentColor.copy(alpha = 0.38f)
    }
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = resolvedContentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = iconColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            shape = shape,
            color = containerColor,
            contentColor = resolvedContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = rowContent
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            contentColor = resolvedContentColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = rowContent
        )
    }
}
