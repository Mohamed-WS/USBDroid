package com.usbdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Usb
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.PrimaryCyan
import com.usbdroid.ui.theme.Background
import com.usbdroid.ui.theme.OnBackground
import com.usbdroid.ui.theme.OnSurfaceVariant
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigateToHome: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = EaseOutBack),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Usb,
                contentDescription = "USBDroid Logo",
                tint = PrimaryCyan,
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "USBDroid",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryCyan,
                textAlign = TextAlign.Center,
                modifier = Modifier.scale(scale)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Universal USB Hardware Control Center",
                fontSize = 14.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animated pulse dots
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { index ->
                    val delay = index * 200
                    val animatedAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, delayMillis = delay),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = PrimaryCyan.copy(alpha = animatedAlpha),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }
    }
}
