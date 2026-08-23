package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiEngineType
import com.example.data.AiNatureType
import com.example.data.AssistantConfig
import com.example.data.CharacterPreset
import com.example.data.DeviceTelemetry
import com.example.data.JarvisCommand
import com.example.data.JarvisSpeechManager
import com.example.data.JarvisVoiceProfile
import com.example.data.MultiApiConfig
import com.example.service.JarvisAccessibilityService
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.InstagramRed
import com.example.ui.theme.JarvisBorderCyan
import com.example.ui.theme.JarvisBorderGlow
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisDarkSurfaceVariant
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.YouTubeRed

private val TelegramBlue = Color(0xFF229ED9)

// ==============================================================================
// 1. CHARACTER, VOICES, NATURE & WAKE-WORD DECK
// ==============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CharacterVoiceDeck(
    assistantConfig: AssistantConfig,
    speechManager: JarvisSpeechManager,
    onSelectPreset: (CharacterPreset) -> Unit,
    onUpdateIdentity: (String, String) -> Unit,
    onSetNature: (AiNatureType, String) -> Unit,
    onSetWakeWord: (String, Boolean) -> Unit,
    onToggleContinuousMode: (Boolean) -> Unit,
    onToggleBargeIn: (Boolean) -> Unit
) {
    val availableVoices by speechManager.availableVoices.collectAsState()
    val selectedVoiceId by speechManager.currentVoiceId.collectAsState()
    val isSpeaking by speechManager.isSpeaking.collectAsState()

    var nameInput by remember(assistantConfig.name) { mutableStateOf(assistantConfig.name) }
    var titleInput by remember(assistantConfig.userTitle) { mutableStateOf(assistantConfig.userTitle) }
    var wakeWordInput by remember(assistantConfig.wakeWord) { mutableStateOf(assistantConfig.wakeWord) }
    var customPromptInput by remember(assistantConfig.customNaturePrompt) { mutableStateOf(assistantConfig.customNaturePrompt) }

    var speechPitch by remember { mutableStateOf(speechManager.speechPitch) }
    var speechRate by remember { mutableStateOf(speechManager.speechRate) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Matrix",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "VOCAL & CHARACTER MATRIX",
                                color = JarvisCyanBright,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Customize assistant name, character voice, nature & wake-word",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 1: Assistant Identity & User Title
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ASSISTANT IDENTITY & SALUTATION",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                onUpdateIdentity(it, titleInput)
                            },
                            label = { Text("Assistant Name", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f).testTag("assistant_name_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = {
                                titleInput = it
                                onUpdateIdentity(nameInput, it)
                            },
                            label = { Text("Calls you (Title)", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("user_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active: \"$nameInput\" addressing you as \"$titleInput\"",
                        color = JarvisCyanDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Section 2: Character Voice Presets (JARVIS, FRIDAY, EDITH, ULTRON, KAREN, DOST, CUSTOM)
        item {
            Text(
                text = "CHARACTER PROFILES & VOCAL PRESETS",
                color = JarvisCyanBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(CharacterPreset.values()) { preset ->
            val isSelected = assistantConfig.preset == preset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPreset(preset) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) JarvisDarkSurfaceVariant else JarvisDarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) JarvisCyanBright else JarvisBorderGlow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.displayName,
                                color = if (isSelected) JarvisCyanBright else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) JarvisCyanBright.copy(alpha = 0.2f) else JarvisBorderGlow.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = preset.subtitle,
                                    color = if (isSelected) JarvisCyanBright else TextSecondary,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.description,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Pitch: ${preset.defaultPitch}x • Speed: ${preset.defaultSpeed}x • Salutation: \"${preset.defaultTitle}\"",
                            color = JarvisCyanDim,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = {
                            speechManager.applyCharacterPreset(preset)
                            speechManager.previewVoice("${preset.assistantName} vocal matrix operational. Ready for orders, ${preset.defaultTitle}.")
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .background(JarvisCyanBright.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Test Voice",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Section 3: AI Nature / Personality Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AI NATURE & BEHAVIOR MODE",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AiNatureType.values().forEach { nature ->
                        val isSelected = assistantConfig.nature == nature
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSetNature(nature, customPromptInput) }
                                .background(
                                    if (isSelected) JarvisCyanBright.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = nature.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nature.title,
                                    color = if (isSelected) JarvisCyanBright else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = nature.description,
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = JarvisCyanBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (assistantConfig.nature == AiNatureType.CUSTOM_PROMPT) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customPromptInput,
                            onValueChange = {
                                customPromptInput = it
                                onSetNature(AiNatureType.CUSTOM_PROMPT, it)
                            },
                            label = { Text("Custom Persona Instructions Prompt", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("custom_nature_prompt"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            minLines = 2
                        )
                    }
                }
            }
        }

        // Section 4: Continuous Dialogue Loop & Barge-in Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CONVERSATION PROTOCOLS",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Continuous Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Dialogue Loop (Lagatar Baat)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Click mic once -> AI speaks and automatically re-opens listening for continuous talk.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = assistantConfig.isContinuousMode,
                            onCheckedChange = { onToggleContinuousMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyanBright,
                                checkedTrackColor = JarvisCyanDim
                            ),
                            modifier = Modifier.testTag("continuous_mode_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Barge-In Speech Interruption Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Speech Interruption (Barge-in)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "If Jarvis is speaking and you speak or tap mic, it stops speaking immediately and listens to you.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = assistantConfig.isBargeInEnabled,
                            onCheckedChange = { onToggleBargeIn(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyanBright,
                                checkedTrackColor = JarvisCyanDim
                            ),
                            modifier = Modifier.testTag("barge_in_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Wake Word Configuration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Wake Word Detection",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Say \"${wakeWordInput}\" to trigger direct command execution.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = assistantConfig.isWakeWordEnabled,
                            onCheckedChange = { onSetWakeWord(wakeWordInput, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyanBright,
                                checkedTrackColor = JarvisCyanDim
                            ),
                            modifier = Modifier.testTag("wake_word_switch")
                        )
                    }

                    if (assistantConfig.isWakeWordEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Jarvis", "Friday", "Edith", "Dost", "Alex").forEach { presetWord ->
                                Button(
                                    onClick = {
                                        wakeWordInput = presetWord
                                        onSetWakeWord(presetWord, true)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (wakeWordInput.equals(presetWord, ignoreCase = true)) JarvisCyanBright else JarvisDarkSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(
                                        text = presetWord,
                                        fontSize = 10.sp,
                                        color = if (wakeWordInput.equals(presetWord, ignoreCase = true)) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Fine Audio Pitch & Speed Tuning
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "AUDIO FREQUENCY & SPEECH RATE TUNING",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Pitch: ${String.format("%.2f", speechPitch)}x",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            speechPitch = it
                            speechManager.speechPitch = it
                        },
                        valueRange = 0.5f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyanBright,
                            activeTrackColor = JarvisCyanBright,
                            inactiveTrackColor = JarvisBorderGlow
                        )
                    )

                    Text(
                        text = "Speed Rate: ${String.format("%.2f", speechRate)}x",
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            speechRate = it
                            speechManager.speechRate = it
                        },
                        valueRange = 0.6f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = JarvisCyanBright,
                            activeTrackColor = JarvisCyanBright,
                            inactiveTrackColor = JarvisBorderGlow
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            speechManager.previewVoice("Audio frequency calibrated to ${String.format("%.2f", speechPitch)} pitch and ${String.format("%.2f", speechRate)} velocity, sir.")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp))
                    ) {
                        Icon(imageVector = Icons.Default.VolumeUp, contentDescription = null, tint = JarvisCyanBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Voice Audio Output", color = JarvisCyanBright, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 2. MULTI-API CONFIGURATION DECK (IN-APP API KEY MANAGER)
// ==============================================================================

@Composable
fun ApiConfigurationDeck(
    apiConfig: MultiApiConfig,
    onSelectEngine: (AiEngineType) -> Unit,
    onUpdateGemini: (String, String) -> Unit,
    onUpdateOpenAi: (String, String) -> Unit,
    onUpdateGroq: (String, String) -> Unit,
    onUpdateDeepSeek: (String, String) -> Unit,
    onUpdateClaude: (String, String) -> Unit,
    onUpdateOpenRouter: (String, String) -> Unit,
    onUpdateCustomEndpoint: (String, String, String) -> Unit
) {
    var geminiKey by remember(apiConfig.geminiApiKey) { mutableStateOf(apiConfig.geminiApiKey) }
    var geminiModel by remember(apiConfig.geminiModel) { mutableStateOf(apiConfig.geminiModel) }

    var openAiKey by remember(apiConfig.openAiApiKey) { mutableStateOf(apiConfig.openAiApiKey) }
    var openAiModel by remember(apiConfig.openAiModel) { mutableStateOf(apiConfig.openAiModel) }

    var groqKey by remember(apiConfig.groqApiKey) { mutableStateOf(apiConfig.groqApiKey) }
    var groqModel by remember(apiConfig.groqModel) { mutableStateOf(apiConfig.groqModel) }

    var deepSeekKey by remember(apiConfig.deepSeekApiKey) { mutableStateOf(apiConfig.deepSeekApiKey) }
    var deepSeekModel by remember(apiConfig.deepSeekModel) { mutableStateOf(apiConfig.deepSeekModel) }

    var claudeKey by remember(apiConfig.claudeApiKey) { mutableStateOf(apiConfig.claudeApiKey) }
    var claudeModel by remember(apiConfig.claudeModel) { mutableStateOf(apiConfig.claudeModel) }

    var openRouterKey by remember(apiConfig.openRouterApiKey) { mutableStateOf(apiConfig.openRouterApiKey) }
    var openRouterModel by remember(apiConfig.openRouterModel) { mutableStateOf(apiConfig.openRouterModel) }

    var customBaseUrl by remember(apiConfig.customApiBaseUrl) { mutableStateOf(apiConfig.customApiBaseUrl) }
    var customKey by remember(apiConfig.customApiKey) { mutableStateOf(apiConfig.customApiKey) }
    var customModel by remember(apiConfig.customModelName) { mutableStateOf(apiConfig.customModelName) }

    var savedNotification by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "API Hub",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "MULTI-API CONFIGURATION HUB",
                                color = JarvisCyanBright,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Add, edit & switch between Gemini, Groq, OpenAI, Claude & DeepSeek APIs",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Engine Switcher
        item {
            Text(
                text = "ACTIVE AI BRAIN SELECTOR",
                color = JarvisStarkGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(AiEngineType.values()) { engine ->
            val isSelected = apiConfig.selectedEngine == engine
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectEngine(engine) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) JarvisDarkSurfaceVariant else JarvisDarkSurface
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) JarvisCyanBright else JarvisBorderGlow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = engine.badge,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = engine.displayName,
                            color = if (isSelected) JarvisCyanBright else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = engine.description,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // API Key Section 1: Google Gemini Free API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "GOOGLE GEMINI API",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Ultra-low latency Google AI Studio free tier models",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = geminiKey,
                        onValueChange = {
                            geminiKey = it
                            onUpdateGemini(it, geminiModel)
                        },
                        label = { Text("Gemini API Key (AIzaSy...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("gemini_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyanBright,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.5-pro").forEach { model ->
                            Button(
                                onClick = {
                                    geminiModel = model
                                    onUpdateGemini(geminiKey, model)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (geminiModel == model) JarvisCyanBright else JarvisDarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = model.replace("gemini-", ""),
                                    fontSize = 9.sp,
                                    color = if (geminiModel == model) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // API Key Section 2: Groq Lightning LPU
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "GROQ LIGHTNING LPU API",
                        color = JarvisOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Sub-second inference speed for instant voice responses",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = {
                            groqKey = it
                            onUpdateGroq(it, groqModel)
                        },
                        label = { Text("Groq API Key (gsk_...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("groq_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisOrange,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768").forEach { model ->
                            Button(
                                onClick = {
                                    groqModel = model
                                    onUpdateGroq(groqKey, model)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (groqModel == model) JarvisOrange else JarvisDarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = model.take(14),
                                    fontSize = 9.sp,
                                    color = if (groqModel == model) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // API Key Section 3: OpenAI Official API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "OPENAI GPT MATRIX API",
                        color = JarvisGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "GPT-4o, GPT-4o-mini, o3-mini models",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = openAiKey,
                        onValueChange = {
                            openAiKey = it
                            onUpdateOpenAi(it, openAiModel)
                        },
                        label = { Text("OpenAI API Key (sk-...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("openai_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisGreen,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo").forEach { model ->
                            Button(
                                onClick = {
                                    openAiModel = model
                                    onUpdateOpenAi(openAiKey, model)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (openAiModel == model) JarvisGreen else JarvisDarkSurfaceVariant
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = model,
                                    fontSize = 9.sp,
                                    color = if (openAiModel == model) Color.Black else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // API Key Section 4: DeepSeek API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "DEEPSEEK AI API",
                        color = JarvisElectricBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DeepSeek Chat & DeepSeek Reasoner",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = deepSeekKey,
                        onValueChange = {
                            deepSeekKey = it
                            onUpdateDeepSeek(it, deepSeekModel)
                        },
                        label = { Text("DeepSeek API Key (sk-...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("deepseek_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisElectricBlue,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // API Key Section 5: Anthropic Claude API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ANTHROPIC CLAUDE API",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Claude 3.5 Sonnet & Claude 3 Haiku",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = claudeKey,
                        onValueChange = {
                            claudeKey = it
                            onUpdateClaude(it, claudeModel)
                        },
                        label = { Text("Claude API Key (sk-ant-...)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("claude_key_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisStarkGold,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                }
            }
        }

        // API Key Section 6: OpenRouter & Custom OpenAI-Compatible Base URL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CUSTOM OPENAI-COMPATIBLE ENDPOINT",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Connect any self-hosted proxy, Ollama, LM-Studio or custom server",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customBaseUrl,
                        onValueChange = {
                            customBaseUrl = it
                            onUpdateCustomEndpoint(it, customKey, customModel)
                        },
                        label = { Text("Base URL (e.g. https://api.openai.com/v1)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisCyanBright,
                            unfocusedBorderColor = JarvisBorderGlow,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customKey,
                            onValueChange = {
                                customKey = it
                                onUpdateCustomEndpoint(customBaseUrl, it, customModel)
                            },
                            label = { Text("API Key", fontSize = 11.sp) },
                            modifier = Modifier.weight(1.2f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customModel,
                            onValueChange = {
                                customModel = it
                                onUpdateCustomEndpoint(customBaseUrl, customKey, it)
                            },
                            label = { Text("Model Name", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 3. SCREEN AUTOMATION, AUTO-TYPER & FLOATING OVERLAY DECK
// ==============================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenAutomationDeck(
    isAccessibilityActive: Boolean,
    isFloatingOverlayActive: Boolean,
    onTypeText: (String) -> Unit,
    onClickText: (String) -> Unit,
    onClickCoords: (Float, Float) -> Unit = { _, _ -> },
    onScrollScreen: (String) -> Unit = {},
    onGlobalAction: (JarvisAccessibilityService.GlobalActionType, String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onToggleFloatingOverlay: () -> Unit
) {
    var textToType by remember { mutableStateOf("") }
    var elementToClick by remember { mutableStateOf("") }
    var coordX by remember { mutableStateOf("540") }
    var coordY by remember { mutableStateOf("1200") }

    val quickPhrases = listOf(
        "Main raste mein hoon.",
        "Yes, acknowledged.",
        "Please check this.",
        "Happy Birthday!",
        "Thanks a lot!",
        "Will call you later."
    )

    val commonClickTargets = listOf(
        "Send", "Search", "Submit", "Like", "Post", "Done", "Login", "Next"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Clearance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityActive) JarvisDarkSurfaceVariant else Color(0xFF261214)
                ),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAccessibilityActive) JarvisCyanBright else JarvisRed
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = "Accessibility Status",
                                tint = if (isAccessibilityActive) JarvisCyanBright else JarvisRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isAccessibilityActive) "ACCESSIBILITY CLEARANCE ACTIVE" else "ACCESSIBILITY PERMISSION REQUIRED",
                                    color = if (isAccessibilityActive) JarvisCyanBright else JarvisRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isAccessibilityActive) "Universal screen auto-typing & tap control enabled" else "Grant permission in Android settings to type anywhere",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (!isAccessibilityActive) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisRed),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("grant_accessibility_button")
                            ) {
                                Text("Grant", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Floating Arc Reactor Overlay Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FLOATING ARC REACTOR OVERLAY",
                                    color = JarvisCyanBright,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(if (isFloatingOverlayActive) JarvisGreen.copy(alpha = 0.2f) else JarvisRed.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isFloatingOverlayActive) "ACTIVE" else "OFF",
                                        color = if (isFloatingOverlayActive) JarvisGreen else JarvisRed,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Floating bubble over WhatsApp, Instagram, Browser & any app for 1-tap voice & typing.",
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }

                        Switch(
                            checked = isFloatingOverlayActive,
                            onCheckedChange = { onToggleFloatingOverlay() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisCyanBright,
                                checkedTrackColor = JarvisCyanDim
                            ),
                            modifier = Modifier.testTag("floating_overlay_switch")
                        )
                    }
                }
            }
        }

        // Section 1: Universal Auto-Typer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = JarvisCyanBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNIVERSAL SCREEN AUTO-TYPER",
                            color = JarvisCyanBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Types directly into whatever text box is active on screen in any app",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textToType,
                            onValueChange = { textToType = it },
                            placeholder = { Text("Enter text to type anywhere...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).testTag("autotyper_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (textToType.isNotBlank()) {
                                    onTypeText(textToType)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("inject_text_button")
                        ) {
                            Text("Type", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Phrases (Tap to Auto-Type):",
                        color = JarvisCyanDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPhrases.forEach { phrase ->
                            Box(
                                modifier = Modifier
                                    .background(JarvisDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp))
                                    .clickable { onTypeText(phrase) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = phrase, color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Screen Element Clicker
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = JarvisStarkGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ON-SCREEN ELEMENT CLICKER",
                            color = JarvisStarkGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Taps any button, tab, or link by its label text",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = elementToClick,
                            onValueChange = { elementToClick = it },
                            placeholder = { Text("Button text (e.g. Send, Submit)...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).testTag("click_target_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisStarkGold,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (elementToClick.isNotBlank()) {
                                    onClickText(elementToClick)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisStarkGold),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tap", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonClickTargets.forEach { target ->
                            Box(
                                modifier = Modifier
                                    .background(JarvisDarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, JarvisBorderGlow, RoundedCornerShape(8.dp))
                                    .clickable { onClickText(target) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "Tap \"$target\"", color = JarvisStarkGold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section 2.5: Coordinate Tap & Screen Scrolling
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = JarvisCyanBright)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRECISE COORDINATE TAP & SCROLL",
                            color = JarvisCyanBright,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Tap any (X, Y) pixel on screen or perform directional swipes",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = coordX,
                            onValueChange = { coordX = it },
                            label = { Text("X (px)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = coordY,
                            onValueChange = { coordY = it },
                            label = { Text("Y (px)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = JarvisCyanBright,
                                unfocusedBorderColor = JarvisBorderGlow,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val x = coordX.toFloatOrNull() ?: 540f
                                val y = coordY.toFloatOrNull() ?: 1200f
                                onClickCoords(x, y)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tap", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Swipe / Scroll Screen:",
                        color = JarvisCyanDim,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("down" to "⬇️ Down", "up" to "⬆️ Up", "left" to "⬅️ Left", "right" to "➡️ Right").forEach { (dir, label) ->
                            Button(
                                onClick = { onScrollScreen(dir) },
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                                modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(6.dp)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(label, fontSize = 9.sp, color = TextPrimary, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Global System Navigation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "GLOBAL SYSTEM GESTURES",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.HOME, "Home Screen") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Home", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.BACK, "Back") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.RECENTS, "Recent Apps") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = JarvisCyanBright, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recents", color = TextPrimary, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.NOTIFICATIONS, "Notifications") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderGlow, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = JarvisStarkGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Notifs", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT, "Screenshot") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderGlow, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📸 Screenshot", color = TextPrimary, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onGlobalAction(JarvisAccessibilityService.GlobalActionType.LOCK_SCREEN, "Lock Screen") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = JarvisRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lock", color = JarvisRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 4. APPS CONTROL HUB
// ==============================================================================

@Composable
fun AppsControlHub(
    onCommand: (JarvisCommand, String) -> Unit
) {
    var whatsAppPhone by remember { mutableStateOf("") }
    var whatsAppMsg by remember { mutableStateOf("") }

    var telegramUser by remember { mutableStateOf("") }
    var spotifyQuery by remember { mutableStateOf("") }
    var youtubeQuery by remember { mutableStateOf("") }
    var instagramUser by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTONOMOUS APPS CONTROL MATRIX",
                        color = JarvisCyanBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Control WhatsApp, Telegram, Spotify, Instagram, YouTube & more directly",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // WhatsApp Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = WhatsAppGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("WHATSAPP INTEGRATION", color = WhatsAppGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onCommand(JarvisCommand.OpenWhatsApp, "Opening WhatsApp application.") },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open App", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onCommand(JarvisCommand.OpenWhatsAppStatus, "Opening WhatsApp status.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).border(1.dp, WhatsAppGreen, RoundedCornerShape(8.dp))
                        ) {
                            Text("Status", color = WhatsAppGreen, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Spotify Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = JarvisGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPOTIFY SONIC STREAM", color = JarvisGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = spotifyQuery,
                            onValueChange = { spotifyQuery = it },
                            placeholder = { Text("Song, artist or playlist...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCommand(JarvisCommand.PlaySpotify(spotifyQuery.ifBlank { "Top Hits" }), "Streaming on Spotify.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Play", color = Color.Black)
                        }
                    }
                }
            }
        }

        // YouTube Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YouTubeRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = YouTubeRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YOUTUBE MAINFRAME", color = YouTubeRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = youtubeQuery,
                            onValueChange = { youtubeQuery = it },
                            placeholder = { Text("Video or music search...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCommand(JarvisCommand.PlayYouTube(youtubeQuery.ifBlank { "Top trending" }), "Streaming YouTube.") },
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Watch", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 5. HARDWARE TOOLS DECK
// ==============================================================================

@Composable
fun HardwareToolsDeck(
    isFlashlightOn: Boolean,
    onCommand: (JarvisCommand, String) -> Unit
) {
    var callNumber by remember { mutableStateOf("") }
    var alarmHour by remember { mutableStateOf("7") }
    var alarmMin by remember { mutableStateOf("00") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HARDWARE & SENSORY CONTROLS",
                        color = JarvisCyanBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Torch, Camera, Video, Telephony, Alarms & Settings",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Flashlight & Camera Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).clickable {
                        onCommand(JarvisCommand.Flashlight(!isFlashlightOn), "Toggling photon emitter.")
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlashlightOn) JarvisStarkGold.copy(alpha = 0.2f) else JarvisDarkSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFlashlightOn) JarvisStarkGold else JarvisBorderGlow)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashlightOn) JarvisStarkGold else TextSecondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isFlashlightOn) "TORCH ON" else "TORCH OFF",
                            color = if (isFlashlightOn) JarvisStarkGold else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f).clickable {
                        onCommand(JarvisCommand.OpenCamera, "Opening optical camera.")
                    },
                    colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = JarvisCyanBright,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CAMERA",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Device File & Code Creator Card
        item {
            var fileNameInput by remember { mutableStateOf("script.py") }
            var fileContentInput by remember { mutableStateOf("# Autonomous script created by Jarvis\nprint(\"Hello, World!\")\n") }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisStarkGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = JarvisStarkGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LOCAL FILE & CODE GENERATOR",
                            color = JarvisStarkGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Generates and saves code files directly into your device storage (Documents / Downloads).",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fileNameInput,
                        onValueChange = { fileNameInput = it },
                        label = { Text("File Name (e.g. index.html, main.py, app.kt)", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisStarkGold,
                            unfocusedBorderColor = JarvisBorderGlow
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fileContentInput,
                        onValueChange = { fileContentInput = it },
                        label = { Text("File / Code Content", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = JarvisStarkGold,
                            unfocusedBorderColor = JarvisBorderGlow
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (fileNameInput.isNotBlank()) {
                                onCommand(
                                    JarvisCommand.CreateDeviceFile(fileNameInput.trim(), fileContentInput),
                                    "Generating file \"${fileNameInput.trim()}\" directly onto device storage."
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisStarkGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create & Save File to Device", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Phone Dialer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = JarvisGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TELECOM VOICE LINK", color = JarvisGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = callNumber,
                            onValueChange = { callNumber = it },
                            placeholder = { Text("Phone number...", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCommand(JarvisCommand.CallPhone(callNumber), "Dialing number.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Call", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Quick Settings Launchers
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SYSTEM SETTINGS PORTALS",
                        color = JarvisCyanBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onCommand(JarvisCommand.OpenSettings("wifi"), "Opening Wi-Fi configuration.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Wi-Fi", fontSize = 10.sp, color = JarvisCyanBright)
                        }

                        Button(
                            onClick = { onCommand(JarvisCommand.OpenSettings("bluetooth"), "Opening Bluetooth diagnostics.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bluetooth", fontSize = 10.sp, color = JarvisCyanBright)
                        }

                        Button(
                            onClick = { onCommand(JarvisCommand.OpenSettings("sound"), "Opening Sound settings.") },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisDarkSurfaceVariant),
                            modifier = Modifier.weight(1f).border(1.dp, JarvisBorderCyan, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Sound", fontSize = 10.sp, color = JarvisCyanBright)
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 6. SYSTEM DIAGNOSTICS & SECURITY DECK
// ==============================================================================

@Composable
fun SystemDiagnosticsDeck(
    telemetry: DeviceTelemetry?,
    isSecurityLockEnabled: Boolean,
    onToggleSecurityLock: (Boolean) -> Unit,
    securityPin: String,
    onPinChange: (String) -> Unit
) {
    var pinInput by remember(securityPin) { mutableStateOf(securityPin) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SYSTEM TELEMETRY & STARK DEFENSE",
                        color = JarvisCyanBright,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Hardware diagnostics, memory metrics & biometric security clearance",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Telemetry Metrics Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "HARDWARE METRICS",
                        color = JarvisStarkGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (telemetry != null) {
                        Text("Device: ${telemetry.deviceModel}", color = TextPrimary, fontSize = 12.sp)
                        Text("OS: ${telemetry.androidVersion}", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Battery: ${telemetry.battery.percentage}% (Temp: ${telemetry.battery.temperatureCelsius}°C)", color = JarvisCyanBright, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { telemetry.battery.percentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = JarvisCyanBright,
                            trackColor = JarvisDarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("RAM Used: ${telemetry.memoryUsedMB} MB / ${telemetry.memoryMaxMB} MB", color = JarvisGreen, fontSize = 12.sp)
                        LinearProgressIndicator(
                            progress = { (telemetry.memoryUsedMB.toFloat() / telemetry.memoryMaxMB.coerceAtLeast(1L)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = JarvisGreen,
                            trackColor = JarvisDarkSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Storage: ${telemetry.storageAvailableGB} GB Available / ${telemetry.storageTotalGB} GB Total", color = JarvisElectricBlue, fontSize = 12.sp)
                    } else {
                        Text("Calibrating hardware sensors...", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Security Lock Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorderGlow)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "STARK LEVEL 7 SECURITY LOCK",
                        color = JarvisRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Biometric & PIN Seal", color = TextPrimary, fontSize = 13.sp)
                            Text("Requires face/fingerprint or 4-digit PIN upon app open.", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = isSecurityLockEnabled,
                            onCheckedChange = { onToggleSecurityLock(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = JarvisRed,
                                checkedTrackColor = JarvisRed.copy(alpha = 0.4f)
                            )
                        )
                    }

                    if (isSecurityLockEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                    pinInput = it
                                    if (it.length == 4) onPinChange(it)
                                }
                            },
                            label = { Text("4-Digit Security PIN", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
