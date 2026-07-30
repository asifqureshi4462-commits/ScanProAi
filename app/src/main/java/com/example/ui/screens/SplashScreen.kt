package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = LinearEasing
            )
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            )
        )
        delay(800) // Total duration ~2000 ms (1800-2200 ms)
        onSplashFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "floating_glow")
    
    // Floating animation (-8dp to 8dp)
    val floatOffset = infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    // Soft glow pulse
    val glowAlpha = infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // AI Sparkles pulse
    val sparkleScale = infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle"
    )

    // Dark Navy → Blue Premium Gradient Background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF090D16),
            Color(0xFF0F172A),
            Color(0xFF1E293B),
            Color(0xFF1E3A8A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .alpha(alpha.value),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Container with Logo, Glow & AI Sparkle Particles
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = floatOffset.value.dp)
                    .scale(scale.value)
            ) {
                // Soft Glow Backdrop
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = glowAlpha.value),
                                    Color(0xFF2563EB).copy(alpha = glowAlpha.value * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // AI Sparkle Particle 1 (Top Left)
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier
                        .offset(x = (-60).dp, y = (-50).dp)
                        .size(24.dp)
                        .scale(sparkleScale.value)
                        .alpha(glowAlpha.value)
                )

                // AI Sparkle Particle 2 (Top Right)
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFA855F7),
                    modifier = Modifier
                        .offset(x = 60.dp, y = (-40).dp)
                        .size(20.dp)
                        .scale(sparkleScale.value * 0.9f)
                        .alpha(glowAlpha.value)
                )

                // AI Sparkle Particle 3 (Bottom Right)
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .offset(x = 55.dp, y = 50.dp)
                        .size(22.dp)
                        .scale(sparkleScale.value * 1.1f)
                        .alpha(glowAlpha.value)
                )

                // Main Logo Box with Border
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color(0xFF0F172A))
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.scanpro_logo),
                        contentDescription = "ScanPro AI Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(118.dp)
                            .clip(RoundedCornerShape(26.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // App Name
            Text(
                text = "ScanPro AI",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 36.sp,
                    letterSpacing = 1.2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "AI Document Scanner & PDF Suite",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Loading Progress Bar
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF38BDF8),
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}

