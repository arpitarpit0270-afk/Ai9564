package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.JarvisReactorState
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisStarkGold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    state: JarvisReactorState,
    audioRmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 210.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")

    // Continuous clockwise rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == JarvisReactorState.THINKING) 3000 else 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Counter-clockwise rotation for inner layer
    val counterRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )

    // Core pulse breathing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (state == JarvisReactorState.LISTENING) 800 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dynamic state accent colors
    val coreColor = when (state) {
        JarvisReactorState.STANDBY -> JarvisCyan
        JarvisReactorState.LISTENING -> JarvisGreen
        JarvisReactorState.THINKING -> JarvisStarkGold
        JarvisReactorState.SPEAKING -> JarvisCyanBright
        JarvisReactorState.EXECUTING -> JarvisOrange
    }

    val stateLabel = when (state) {
        JarvisReactorState.STANDBY -> "STANDBY // TAP TO SPEAK"
        JarvisReactorState.LISTENING -> "LISTENING // SPEAK NOW"
        JarvisReactorState.THINKING -> "AI PROCESSING..."
        JarvisReactorState.SPEAKING -> "J.A.R.V.I.S. TRANSMITTING"
        JarvisReactorState.EXECUTING -> "EXECUTING COMMAND"
    }

    val dynamicAudioScale = 1.0f + (audioRmsLevel * 0.25f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sizeDp)
                .testTag("arc_reactor_core")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            // Ambient Outer Glow Ripples
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale * dynamicAudioScale)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2

                // Outer ambient radial glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = if (state == JarvisReactorState.LISTENING || state == JarvisReactorState.SPEAKING) 0.35f else 0.18f),
                            coreColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )

                // Holographic circular tech ring 1
                drawCircle(
                    color = coreColor.copy(alpha = 0.4f),
                    radius = maxRadius * 0.92f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f, 5f, 15f), 0f)
                    )
                )

                // Holographic circular tech ring 2
                drawCircle(
                    color = coreColor.copy(alpha = 0.6f),
                    radius = maxRadius * 0.78f,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Arc segments (12 segments around reactor ring)
                val segments = 12
                val segRadius = maxRadius * 0.68f
                for (i in 0 until segments) {
                    val angleRad = Math.toRadians((i * (360.0 / segments) + rotationAngle).toDouble())
                    val x = center.x + (segRadius * cos(angleRad)).toFloat()
                    val y = center.y + (segRadius * sin(angleRad)).toFloat()
                    drawCircle(
                        color = coreColor,
                        radius = if (i % 3 == 0) 3.5.dp.toPx() else 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                // Audio reactive wave ripples if speaking or listening
                if (audioRmsLevel > 0.05f) {
                    val waveRadius = maxRadius * (0.8f + audioRmsLevel * 0.2f)
                    drawCircle(
                        color = coreColor.copy(alpha = (audioRmsLevel * 0.7f).coerceIn(0.1f, 0.8f)),
                        radius = waveRadius,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // Inner Core Arc Reactor Art image
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(sizeDp * 0.62f)
                    .clip(CircleShape)
                    .rotate(counterRotationAngle * 0.2f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.jarvis_reactor_core),
                    contentDescription = "Jarvis Reactor Hologram",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Center glowing energy nucleus
                Box(
                    modifier = Modifier
                        .size(sizeDp * 0.22f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White,
                                    coreColor,
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reactor State Status Badge
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(coreColor.copy(alpha = 0.15f))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = stateLabel,
                color = coreColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }
    }
}
