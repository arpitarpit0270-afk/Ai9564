package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisDarkSurfaceVariant
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.concurrent.Executors

@Composable
fun LiveCameraVisionHud(
    assistantName: String,
    onClose: () -> Unit,
    onAnalyzeFrame: (prompt: String, bitmap: Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var queryText by remember { mutableStateOf("Explain what this object is, identify text, parts, and details.") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // HUD Scanline & Radar Animations
    val infiniteTransition = rememberInfiniteTransition(label = "hudScan")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    val gyroRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gyroRotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Live CameraX Video Stream Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        Log.e("LiveCameraVisionHud", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            capture
                        )
                        camera?.cameraControl?.enableTorch(isTorchEnabled)
                    } catch (e: Exception) {
                        Log.e("LiveCameraVisionHud", "Camera update failed", e)
                    }
                }, ContextCompat.getMainExecutor(previewView.context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Futuristic Cybernetic HUD Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // Central Targeting Box
            val boxW = w * 0.72f
            val boxH = h * 0.45f
            val left = cx - (boxW / 2f)
            val top = cy - (boxH / 2f)
            val right = left + boxW
            val bottom = top + boxH

            // Corner Brackets
            val cornerLen = 28f
            val strokeW = 3f

            // Top-Left
            drawLine(JarvisCyanBright, Offset(left, top), Offset(left + cornerLen, top), strokeWidth = strokeW)
            drawLine(JarvisCyanBright, Offset(left, top), Offset(left, top + cornerLen), strokeWidth = strokeW)

            // Top-Right
            drawLine(JarvisCyanBright, Offset(right, top), Offset(right - cornerLen, top), strokeWidth = strokeW)
            drawLine(JarvisCyanBright, Offset(right, top), Offset(right, top + cornerLen), strokeWidth = strokeW)

            // Bottom-Left
            drawLine(JarvisCyanBright, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth = strokeW)
            drawLine(JarvisCyanBright, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth = strokeW)

            // Bottom-Right
            drawLine(JarvisCyanBright, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth = strokeW)
            drawLine(JarvisCyanBright, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth = strokeW)

            // Center Crosshairs
            val crossSize = 14f
            drawLine(JarvisCyan.copy(alpha = 0.8f), Offset(cx - crossSize, cy), Offset(cx + crossSize, cy), strokeWidth = 2f)
            drawLine(JarvisCyan.copy(alpha = 0.8f), Offset(cx, cy - crossSize), Offset(cx, cy + crossSize), strokeWidth = 2f)

            // Central Reticle Circle
            drawCircle(
                color = JarvisCyanBright.copy(alpha = 0.35f),
                radius = 42f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            )

            // Moving Horizontal Scanline
            val scanYPos = top + (boxH * scanLineY)
            drawLine(
                color = JarvisCyanBright.copy(alpha = pulseAlpha),
                start = Offset(left, scanYPos),
                end = Offset(right, scanYPos),
                strokeWidth = 2.5f
            )
        }

        // 3. Top HUD Status Bar & Controls
        Surface(
            color = JarvisDarkBg.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .border(1.dp, JarvisBorderCyan.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(JarvisGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$assistantName // LIVE OPTICAL HUD",
                                color = JarvisCyanBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "REAL-TIME VISION SENSORS ACTIVE",
                            color = JarvisCyanDim,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Torch / Flashlight Toggle
                    IconButton(
                        onClick = {
                            isTorchEnabled = !isTorchEnabled
                            camera?.cameraControl?.enableTorch(isTorchEnabled)
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchEnabled) JarvisStarkGold else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Flip Camera (Front / Back)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close HUD",
                            tint = JarvisRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 4. Telemetry Readout Badges (Left & Right HUD metrics)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(JarvisDarkSurface.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .border(1.dp, JarvisCyanDim.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "LENS: ${if (lensFacing == CameraSelector.LENS_FACING_BACK) "OPTICAL REAR" else "FRONT SENSOR"}\nFPS: 30.0\nRES: 1080P FHD\nISO: AUTO\nSTATUS: LOCK READY",
                    color = JarvisCyanDim,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 11.sp
                )
            }
        }

        // 5. Bottom Live Scan & Query Bar
        Surface(
            color = JarvisDarkBg.copy(alpha = 0.92f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .border(1.dp, JarvisBorderCyan)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Quick Suggestion Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPrompts = listOf("Identify object", "Read text/code", "Explain circuit/part", "Translate visible text")
                    quickPrompts.forEach { prompt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(JarvisDarkSurfaceVariant)
                                .border(1.dp, JarvisBorderCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { queryText = prompt }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = prompt,
                                color = JarvisCyanBright,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Query text field + Trigger Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = {
                            Text("Ask Jarvis about live target...", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_camera_query_input"),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyanBright,
                            unfocusedBorderColor = JarvisBorderCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Scan & Analyze Button
                    Button(
                        onClick = {
                            if (isCapturing) return@Button
                            val capture = imageCapture ?: return@Button
                            isCapturing = true

                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        try {
                                            val buffer = imageProxy.planes[0].buffer
                                            val bytes = ByteArray(buffer.remaining())
                                            buffer.get(bytes)
                                            val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            
                                            // Handle rotation
                                            val rotation = imageProxy.imageInfo.rotationDegrees
                                            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                            val finalBitmap = Bitmap.createBitmap(
                                                originalBitmap,
                                                0,
                                                0,
                                                originalBitmap.width,
                                                originalBitmap.height,
                                                matrix,
                                                true
                                            )
                                            
                                            imageProxy.close()
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                                onAnalyzeFrame(queryText, finalBitmap)
                                            }
                                        } catch (e: Exception) {
                                            Log.e("LiveCameraVisionHud", "Error converting frame", e)
                                            imageProxy.close()
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturing = false
                                            }
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("LiveCameraVisionHud", "Capture error: ${exception.message}", exception)
                                        ContextCompat.getMainExecutor(context).execute {
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCapturing) JarvisOrange else JarvisCyanBright
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("live_camera_scan_button")
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan & Ask",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
