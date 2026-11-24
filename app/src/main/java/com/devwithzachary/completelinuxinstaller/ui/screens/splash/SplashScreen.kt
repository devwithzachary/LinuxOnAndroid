package com.devwithzachary.completelinuxinstaller.ui.screens.splash

import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaMonospaceChip
import com.devwithzachary.completelinuxinstaller.ui.designsystem.LoaProgressRing
import com.devwithzachary.completelinuxinstaller.ui.theme.LinuxOnAndroidTheme
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaMonoTextStyle
import com.devwithzachary.completelinuxinstaller.ui.theme.LoaSegmentedSingleShape

@Composable
fun SplashScreen(
    statusText: String = stringResource(R.string.splash_init_verifying_binaries),
    initSlow: Boolean = false,
    elapsedSeconds: Int = 0,
    onRetry: () -> Unit = {},
    onContinueAnyway: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    val archLabel = when {
        primaryAbi.contains("arm64") || primaryAbi.contains("aarch64") -> "ARM64"
        primaryAbi.contains("x86_64") || primaryAbi.contains("amd64") -> "x86_64"
        primaryAbi.contains("v7") || primaryAbi.contains("arm") -> "ARMv7"
        else -> primaryAbi
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_logo),
                    contentDescription = "Linux on Android Logo",
                    modifier = Modifier
                        .size(96.dp)
                        .scale(scale)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Linux on Android",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Complete Linux Installer",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                LoaMonospaceChip(text = "PRoot | $archLabel")

                Spacer(modifier = Modifier.height(48.dp))

                LoaProgressRing(
                    progress = null,
                    size = 56.dp,
                    strokeWidth = 6.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    style = LoaMonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(alpha)
                )

                if (initSlow) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = LoaSegmentedSingleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = stringResource(R.string.splash_init_slow_warning, elapsedSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = onRetry) {
                            Text(stringResource(R.string.splash_retry))
                        }
                        Button(onClick = onContinueAnyway) {
                            Text(stringResource(R.string.splash_continue_anyway))
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Splash - Light",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun SplashScreenLightPreview() {
    LinuxOnAndroidTheme(darkTheme = false, dynamicColor = false) {
        SplashScreen()
    }
}

@Preview(
    name = "Splash Slow - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun SplashScreenDarkSlowPreview() {
    LinuxOnAndroidTheme(darkTheme = true, dynamicColor = false) {
        SplashScreen(
            initSlow = true,
            elapsedSeconds = 12
        )
    }
}
