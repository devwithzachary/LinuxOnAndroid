package com.devwithzachary.completelinuxinstaller.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaSegmentedRow

@Composable
fun FeatureInfoRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    LoaSegmentedRow(
        title = title,
        subtitle = description,
        leadingIcon = icon,
        modifier = modifier
    )
}
