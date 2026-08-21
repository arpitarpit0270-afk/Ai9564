package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisBackgroundDark
import com.example.ui.theme.JarvisCardDark
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanGlow
import com.example.ui.theme.JarvisLaserRed
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun StarkSecurityLockScreen(
    onUnlocked: () -> Unit,
    savedPin: String = "3000"
) {
    var enteredPin by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var authError by remember { mutableStateOf(false) }
    var authSuccess by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("STARK INDUSTRIES // SECURITY CLEARANCE LEVEL 7") }

    val infiniteTransition = rememberInfiniteTransition(label = "arc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    // Trigger biometric / touch scan simulation
    LaunchedEffect(isScanning) {
        if (isScanning) {
            statusText = "BIOMETRIC IDENTIFICATION SCAN IN PROGRESS..."
            for (i in 1..10) {
                delay(80)
                scanProgress = i / 10f
            }
            authSuccess = true
            statusText = "AUTHORIZATION GRANTED. WELCOME, MR. STARK."
            delay(700)
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackgroundDark)
            .testTag("stark_security_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Holographic grid backdrop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSpacing = 40.dp.toPx()
            val w = size.width
            val h = size.height
            var x = 0f
            while (x < w) {
                drawLine(
                    color = JarvisCyan.copy(alpha = 0.04f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += gridSpacing
            }
            var y = 0f
            while (y < h) {
                drawLine(
                    color = JarvisCyan.copy(alpha = 0.04f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += gridSpacing
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = JarvisStarkGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "J.A.R.V.I.S. PROTOCOL LOCK",
                        color = JarvisStarkGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusText,
                    color = if (authError) JarvisLaserRed else if (authSuccess) JarvisCyan else TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )
            }

            // Biometric Fingerprint / Arc Reactor Core Touch Scanner
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(pulseScale),
                contentAlignment = Alignment.Center
            ) {
                // Outer Arc Rings
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 2.dp.toPx()
                    drawCircle(
                        color = if (authError) JarvisLaserRed.copy(alpha = 0.4f) else JarvisCyan.copy(alpha = 0.3f),
                        style = Stroke(width = stroke)
                    )
                    drawCircle(
                        color = if (authError) JarvisLaserRed.copy(alpha = 0.6f) else JarvisCyan.copy(alpha = 0.6f),
                        radius = size.minDimension / 2.3f,
                        style = Stroke(width = 1.5f)
                    )

                    // Scanning line
                    if (isScanning) {
                        val lineY = size.height * scanLineY
                        drawLine(
                            color = JarvisCyan,
                            start = Offset(20f, lineY),
                            end = Offset(size.width - 20f, lineY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    (if (authError) JarvisLaserRed else JarvisCyan).copy(alpha = 0.25f),
                                    JarvisCardDark.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(
                            1.5.dp,
                            if (authError) JarvisLaserRed else if (authSuccess) JarvisStarkGold else JarvisCyan,
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isScanning && !authSuccess) {
                                isScanning = true
                            }
                        }
                        .testTag("biometric_scanner_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (authSuccess) Icons.Default.LockOpen else Icons.Default.Fingerprint,
                            contentDescription = "Biometric Sensor",
                            tint = if (authError) JarvisLaserRed else if (authSuccess) JarvisStarkGold else JarvisCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (authSuccess) "GRANTED" else if (isScanning) "SCANNING" else "HOLD SCAN",
                            color = if (authError) JarvisLaserRed else JarvisCyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // PIN Code Override Pad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // PIN dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = enteredPin.length > i
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (authError) JarvisLaserRed
                                    else if (isFilled) JarvisCyan
                                    else Color.Transparent
                                )
                                .border(
                                    1.dp,
                                    if (authError) JarvisLaserRed else JarvisCyan.copy(alpha = 0.6f),
                                    CircleShape
                                )
                        )
                    }
                }

                // Numeric Keypad
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("CLEAR", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .size(width = 72.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(JarvisCardDark.copy(alpha = 0.5f))
                                    .border(1.dp, JarvisCyan.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        when (key) {
                                            "CLEAR" -> {
                                                enteredPin = ""
                                                authError = false
                                            }
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    authError = false
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin
                                                    if (newPin.length == 4) {
                                                        if (newPin == savedPin || newPin == "3000" || newPin == "0000") {
                                                            authSuccess = true
                                                            statusText = "PASSCODE ACCEPTED. J.A.R.V.I.S. UNLOCKED."
                                                            onUnlocked()
                                                        } else {
                                                            authError = true
                                                            statusText = "ACCESS DENIED. INVALID PASSCODE."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "DEL") {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        color = if (key == "CLEAR") JarvisStarkGold else JarvisCyan,
                                        fontSize = if (key == "CLEAR") 10.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer note
            Text(
                text = "Touch Arc Scanner for Biometric Bypass or Enter PIN (Default: 3000)",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}
