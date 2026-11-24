package com.devwithzachary.completelinuxinstaller.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LoaProgressRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 8.dp,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    centerContent: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (progress != null) {
            val animatedProgress by animateFloatAsState(
                targetValue = progress.coerceIn(0f, 1f),
                label = "LoaProgressRingProgress"
            )
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = indicatorColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                strokeCap = StrokeCap.Round
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = indicatorColor,
                trackColor = trackColor,
                strokeWidth = strokeWidth,
                strokeCap = StrokeCap.Round
            )
        }

        if (centerContent != null) {
            centerContent()
        }
    }
}
