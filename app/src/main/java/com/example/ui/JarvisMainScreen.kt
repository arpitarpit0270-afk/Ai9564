package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.UploadedFileInfo
import com.example.ui.components.ApiConfigurationDeck
import com.example.ui.components.AppsControlHub
import com.example.ui.components.ArcReactorCore
import com.example.ui.components.CharacterVoiceDeck
import com.example.ui.components.HardwareToolsDeck
import com.example.ui.components.LiveCameraVisionHud
import com.example.ui.components.ScreenAutomationDeck
import com.example.ui.components.StarkSecurityLockScreen
import com.example.ui.components.SystemDiagnosticsDeck
import com.example.ui.components.TerminalChatView
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisDarkSurfaceVariant
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()

    if (!isAppUnlocked) {
        StarkSecurityLockScreen(
            onUnlocked = { viewModel.unlockApp() },
            savedPin = securityPin
        )
        return
    }

    val reactorState by viewModel.reactorState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isListening by viewModel.speechManager.isListening.collectAsState()
    val isSpeaking by viewModel.speechManager.isSpeaking.collectAsState()
    val liveRmsLevel by viewModel.speechManager.liveRmsDb.collectAsState()
    val statusBanner by viewModel.statusBannerText.collectAsState()
    val isFlashlightOn by viewModel.isFlashlightActive.collectAsState()
    val isSecurityLockEnabled by viewModel.isSecurityLockEnabled.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityActive.collectAsState()
    val isFloatingOverlayActive by viewModel.isFloatingOverlayActive.collectAsState()
    val apiConfig by viewModel.apiConfig.collectAsState()
    val assistantConfig by viewModel.assistantConfig.collectAsState()
    val attachedFile by viewModel.attachedFile.collectAsState()
    val isVisionModeActive by viewModel.isVisionModeActive.collectAsState()
    val isLiveCameraHudActive by viewModel.isLiveCameraHudActive.collectAsState()
    val capturedVisionBitmap by viewModel.capturedVisionBitmap.collectAsState()

    var inputText by remember { mutableStateOf("") }

    // Live Camera Vision HUD Fullscreen Overlay
    if (isLiveCameraHudActive) {
        LiveCameraVisionHud(
            assistantName = assistantConfig.name,
            onClose = { viewModel.closeLiveCameraHud() },
            onAnalyzeFrame = { prompt, bitmap ->
                viewModel.closeLiveCameraHud()
                viewModel.sendVisionAnalysis(prompt, bitmap)
            }
        )
        return
    }

    // Camera Capture Launcher for Vision Mode
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setCapturedVisionBitmap(bitmap)
            viewModel.setVisionModeActive(true)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openLiveCameraHud()
        }
    }

    // Document / File Picker Launcher for Analysis
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectFileForAnalysis(uri)
        }
    }

    // Vision Analysis Prompt Dialog
    if (isVisionModeActive && capturedVisionBitmap != null) {
        JarvisVisionDialog(
            bitmap = capturedVisionBitmap!!,
            assistantName = assistantConfig.name,
            onDismiss = { viewModel.setVisionModeActive(false) },
            onAnalyze = { prompt ->
                viewModel.sendVisionAnalysis(prompt, capturedVisionBitmap!!)
            }
        )
    }

    Scaffold(
        containerColor = JarvisDarkBg,
        topBar = {
            JarvisTopHudBar(
                assistantName = assistantConfig.name,
                engineBadge = apiConfig.selectedEngine.badge,
                telemetry = telemetry,
                isLockActive = isSecurityLockEnabled,
                onSettingsClick = { viewModel.setTab(3) }, // Opens APIs tab
                onClearChat = { viewModel.clearChat() }
            )
        },
        bottomBar = {
            JarvisBottomInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSend = {
                    viewModel.processInput(inputText)
                    inputText = ""
                },
                isListening = isListening,
                isSpeaking = isSpeaking,
                isContinuous = assistantConfig.isContinuousMode,
                attachedFile = attachedFile,
                onClearAttachedFile = { viewModel.clearAttachedFile() },
                onMicClick = { viewModel.toggleVoiceListening() },
                onLiveCameraClick = {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        viewModel.openLiveCameraHud()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onScreenVisionClick = {
                    viewModel.analyzeCurrentLiveScreen()
                },
                onAttachFileClick = {
                    filePickerLauncher.launch("*/*")
                }
            )
        },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arc Reactor Central Visualizer
            ArcReactorCore(
                state = reactorState,
                audioRmsLevel = liveRmsLevel,
                onClick = { viewModel.toggleVoiceListening() },
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                sizeDp = 175.dp
            )

            // Live status marquee banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(JarvisDarkSurface)
                    .border(1.dp, JarvisBorderGlow, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusBanner,
                    color = JarvisCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Navigation Tabs (6 Modules)
            val tabs = listOf("⚡ Core", "🎭 Voice & Nature", "📱 Screen & Typer", "⚙️ APIs Hub", "🌐 Apps", "🛠 Tools")
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = JarvisDarkSurfaceVariant,
                contentColor = JarvisCyan,
                edgePadding = 8.dp,
                indicator = { tabPositions ->
                    if (activeTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = JarvisCyanBright,
                            height = 2.5.dp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, JarvisBorderGlow, RoundedCornerShape(8.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { viewModel.setTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeTab == index) JarvisCyanBright else TextSecondary
                            )
                        },
                        modifier = Modifier.testTag("tab_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tab Content Deck
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    0 -> TerminalChatView(
                        messages = messages,
                        onSpeakMessage = { viewModel.speechManager.speak(it) },
                        onSuggestionClick = { prompt -> viewModel.processInput(prompt) }
                    )
                    1 -> CharacterVoiceDeck(
                        assistantConfig = assistantConfig,
                        speechManager = viewModel.speechManager,
                        onSelectPreset = { viewModel.selectCharacterPreset(it) },
                        onUpdateIdentity = { name, title -> viewModel.updateAssistantIdentity(name, title) },
                        onSetNature = { nature, customPrompt -> viewModel.setAiNature(nature, customPrompt) },
                        onSetWakeWord = { word, enabled -> viewModel.setWakeWord(word, enabled) },
                        onToggleContinuousMode = { viewModel.toggleContinuousMode(it) },
                        onToggleBargeIn = { viewModel.toggleBargeIn(it) }
                    )
                    2 -> ScreenAutomationDeck(
                        isAccessibilityActive = isAccessibilityActive,
                        isFloatingOverlayActive = isFloatingOverlayActive,
                        onTypeText = { text -> viewModel.typeTextAnywhere(text) },
                        onClickText = { target -> viewModel.clickOnScreenText(target) },
                        onGlobalAction = { action, label -> viewModel.triggerGlobalAction(action, label) },
                        onOpenAccessibilitySettings = { viewModel.openAccessibilitySettings() },
                        onToggleFloatingOverlay = { viewModel.toggleFloatingOverlay() }
                    )
                    3 -> ApiConfigurationDeck(
                        apiConfig = apiConfig,
                        onSelectEngine = { viewModel.setAiEngine(it) },
                        onUpdateGemini = { key, model -> viewModel.updateGeminiConfig(key, model) },
                        onUpdateOpenAi = { key, model -> viewModel.updateOpenAiConfig(key, model) },
                        onUpdateGroq = { key, model -> viewModel.updateGroqConfig(key, model) },
                        onUpdateDeepSeek = { key, model -> viewModel.updateDeepSeekConfig(key, model) },
                        onUpdateClaude = { key, model -> viewModel.updateClaudeConfig(key, model) },
                        onUpdateOpenRouter = { key, model -> viewModel.updateOpenRouterConfig(key, model) },
                        onUpdateCustomEndpoint = { baseUrl, key, model -> viewModel.updateCustomEndpointConfig(baseUrl, key, model) }
                    )
                    4 -> AppsControlHub(
                        onCommand = { cmd, note -> viewModel.executeDirectCommand(cmd, note) }
                    )
                    5 -> HardwareToolsDeck(
                        isFlashlightOn = isFlashlightOn,
                        onCommand = { cmd, note -> viewModel.executeDirectCommand(cmd, note) }
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisVisionDialog(
    bitmap: Bitmap,
    assistantName: String,
    onDismiss: () -> Unit,
    onAnalyze: (String) -> Unit
) {
    var visionPrompt by remember { mutableStateOf("Explain what this is, identify details, and analyze this image.") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JarvisDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = JarvisCyanBright)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$assistantName // OPTICAL SENSORS",
                    color = JarvisCyanBright,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, JarvisCyan, RoundedCornerShape(10.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Captured camera visual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Inquire About Visual Target:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = visionPrompt,
                    onValueChange = { visionPrompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanBright,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAnalyze(visionPrompt) },
                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze Image", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = TextSecondary, fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun JarvisTopHudBar(
    assistantName: String,
    engineBadge: String,
    telemetry: com.example.data.DeviceTelemetry?,
    isLockActive: Boolean = false,
    onSettingsClick: () -> Unit,
    onClearChat: () -> Unit
) {
    val currentTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }
    val batteryPct = telemetry?.battery?.percentage ?: 92
    val isCharging = telemetry?.battery?.isCharging == true

    Surface(
        color = JarvisDarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Jarvis Identity Brand & Active Engine Badge
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
                            text = assistantName,
                            color = JarvisCyanBright,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(JarvisCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = engineBadge,
                                color = JarvisCyanBright,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = "AUTONOMOUS AGENT ACTIVE",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Right: Telemetry Chips & Navigation Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Battery telemetry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(JarvisDarkSurfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                        contentDescription = "Battery",
                        tint = if (batteryPct > 20) JarvisGreen else JarvisRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$batteryPct%",
                        color = TextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Time chip
                Text(
                    text = currentTime,
                    color = JarvisCyanDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Terminal",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(28.dp).testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "APIs Settings",
                        tint = JarvisCyanBright,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun JarvisBottomInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    isContinuous: Boolean,
    attachedFile: UploadedFileInfo? = null,
    onClearAttachedFile: () -> Unit = {},
    onMicClick: () -> Unit,
    onLiveCameraClick: () -> Unit,
    onScreenVisionClick: () -> Unit,
    onAttachFileClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else if (isSpeaking) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Surface(
        color = JarvisDarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, JarvisBorderGlow)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Floating preview banner for attached file if present
            if (attachedFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JarvisElectricBlue.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Attached: ${attachedFile.name} (${attachedFile.sizeFormatted})",
                            color = JarvisCyanBright,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onClearAttachedFile,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attached file",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Microphone / Interruption Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(if (isListening || isSpeaking) micPulse else 1f)
                        .clip(CircleShape)
                        .background(
                            when {
                                isListening -> JarvisCyanBright
                                isSpeaking -> JarvisOrange
                                isContinuous -> JarvisGreen.copy(alpha = 0.25f)
                                else -> JarvisDarkSurfaceVariant
                            }
                        )
                        .border(
                            1.5.dp,
                            when {
                                isListening -> JarvisCyanBright
                                isSpeaking -> JarvisOrange
                                isContinuous -> JarvisGreen
                                else -> JarvisBorderCyan
                            },
                            CircleShape
                        )
                        .clickable { onMicClick() }
                        .testTag("mic_toggle_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isSpeaking -> Icons.Default.VolumeUp
                            isListening -> Icons.Default.Mic
                            else -> Icons.Default.Mic
                        },
                        contentDescription = if (isSpeaking) "Barge In / Interrupt" else "Microphone",
                        tint = when {
                            isListening -> Color.Black
                            isSpeaking -> Color.Black
                            isContinuous -> JarvisGreen
                            else -> JarvisCyanBright
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Live Camera HUD Scanner Button
                IconButton(
                    onClick = onLiveCameraClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(JarvisDarkSurfaceVariant)
                        .border(1.dp, JarvisCyan.copy(alpha = 0.4f), CircleShape)
                        .testTag("camera_vision_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Live Camera HUD",
                        tint = JarvisCyanBright,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Live Screen Perception Button
                IconButton(
                    onClick = onScreenVisionClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(JarvisDarkSurfaceVariant)
                        .border(1.dp, JarvisStarkGold.copy(alpha = 0.5f), CircleShape)
                        .testTag("screen_vision_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Analyze Live Screen",
                        tint = JarvisStarkGold,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // File Attachment Button
                IconButton(
                    onClick = onAttachFileClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (attachedFile != null) JarvisElectricBlue.copy(alpha = 0.4f) else JarvisDarkSurfaceVariant)
                        .border(1.dp, if (attachedFile != null) JarvisElectricBlue else JarvisCyan.copy(alpha = 0.4f), CircleShape)
                        .testTag("attach_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach File for Analysis",
                        tint = if (attachedFile != null) JarvisCyanBright else TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Command / Dialogue Input Box
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            text = if (isListening) "Listening..." else if (isSpeaking) "Tap mic to interrupt..." else "Type command, code, or query...",
                            color = if (isListening) JarvisCyanBright else TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input_field"),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyanBright,
                        unfocusedBorderColor = JarvisBorderGlow,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSend()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) JarvisCyanBright else JarvisDarkSurfaceVariant)
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.Black else TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
