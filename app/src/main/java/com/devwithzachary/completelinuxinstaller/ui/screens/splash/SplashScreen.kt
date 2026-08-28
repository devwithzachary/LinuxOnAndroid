package com.devwithzachary.completelinuxinstaller.ui.screens.splash

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.devwithzachary.completelinuxinstaller.R

@Composable
fun SplashScreen(
    statusText: String = stringResource(R.string.splash_init_verifying_binaries),
    initSlow: Boolean = false,
    elapsedSeconds: Int = 0,
    onRetry: () -> Unit = {},
    onContinueAnyway: () -> Unit = {}
) {
    val view = LocalView.current
    val window = remember(view) { (view.context as? Activity)?.window }
    val insetsController = remember(window, view) { window?.let { WindowCompat.getInsetsController(it, view) } }

    DisposableEffect(window, insetsController) {
        val prevStatusBarColor = window?.statusBarColor
        val prevLightStatusBars = insetsController?.isAppearanceLightStatusBars

        window?.statusBarColor = android.graphics.Color.parseColor("#1E1E2E")
        insetsController?.isAppearanceLightStatusBars = false

        onDispose {
            if (window != null && prevStatusBarColor != null) {
                window.statusBarColor = prevStatusBarColor
            }
            if (insetsController != null && prevLightStatusBars != null) {
                insetsController.isAppearanceLightStatusBars = prevLightStatusBars
            }
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val primaryAbi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    val archLabel = when {
        primaryAbi.contains("arm64") || primaryAbi.contains("aarch64") -> "ARM64"
        primaryAbi.contains("x86_64") || primaryAbi.contains("amd64") -> "x86_64"
        primaryAbi.contains("v7") || primaryAbi.contains("arm") -> "ARMv7"
        else -> primaryAbi
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E1E2E),
                        Color(0xFF11111B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated App Logo Circle Badge
            Surface(
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale),
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 12.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_logo),
                        contentDescription = "Linux on Android Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "Linux on Android",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Complete Linux Installer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Architecture & PRoot Container Environment Badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3FB950))
                    )
                    Text(
                        text = "PRoot Container • $archLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Progress Indicator
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Dynamic Step-by-Step Status message
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFBAC2DE),
                modifier = Modifier.alpha(alpha)
            )

            if (initSlow) {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0xFFF38BA8).copy(alpha = 0.12f),
                    contentColor = Color(0xFFF38BA8)
                ) {
                    Text(
                        text = stringResource(R.string.splash_init_slow_warning, elapsedSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
