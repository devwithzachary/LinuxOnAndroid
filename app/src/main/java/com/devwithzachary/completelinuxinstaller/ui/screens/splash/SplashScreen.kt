package com.devwithzachary.completelinuxinstaller.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devwithzachary.completelinuxinstaller.R

@Composable
fun SplashScreen(
    statusText: String = "Initializing PRoot Container..."
) {
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

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "PRoot Environment v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Progress Indicator
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.alpha(alpha)
            )
        }
    }
}
