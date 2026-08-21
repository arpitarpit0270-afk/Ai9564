package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ActionResult
import com.example.data.AiEngineType
import com.example.data.AiNatureType
import com.example.data.AssistantConfig
import com.example.data.CharacterPreset
import com.example.data.DeviceTelemetry
import com.example.data.GeminiJarvisService
import com.example.data.JarvisCommand
import com.example.data.JarvisResponse
import com.example.data.JarvisSpeechManager
import com.example.data.MultiApiConfig
import com.example.data.PhoneController
import com.example.service.JarvisAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class JarvisReactorState {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    EXECUTING
}

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString() + (0..999).random(),
    val sender: MessageSender,
    val text: String,
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val actionExecuted: String? = null,
    val isGeminiPowered: Boolean = false,
    val providerBadge: String? = null,
    val attachedFile: com.example.data.UploadedFileInfo? = null,
    val attachedBitmap: android.graphics.Bitmap? = null,
    val createdFile: com.example.data.CreatedDeviceInfo? = null
)

enum class MessageSender {
    USER,
    JARVIS,
    SYSTEM
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    val speechManager = JarvisSpeechManager(application)

    private val prefs = application.getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)

    private val _reactorState = MutableStateFlow(JarvisReactorState.STANDBY)
    val reactorState: StateFlow<JarvisReactorState> = _reactorState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _telemetry = MutableStateFlow<DeviceTelemetry?>(null)
    val telemetry: StateFlow<DeviceTelemetry?> = _telemetry.asStateFlow()

    private val _isAccessibilityActive = MutableStateFlow(false)
    val isAccessibilityActive: StateFlow<Boolean> = _isAccessibilityActive.asStateFlow()

    private val _isFloatingOverlayActive = MutableStateFlow(false)
    val isFloatingOverlayActive: StateFlow<Boolean> = _isFloatingOverlayActive.asStateFlow()

    // File Analysis & Vision Attachment States
    private val _attachedFile = MutableStateFlow<com.example.data.UploadedFileInfo?>(null)
    val attachedFile: StateFlow<com.example.data.UploadedFileInfo?> = _attachedFile.asStateFlow()

    private val _isVisionModeActive = MutableStateFlow(false)
    val isVisionModeActive: StateFlow<Boolean> = _isVisionModeActive.asStateFlow()

    private val _isLiveCameraHudActive = MutableStateFlow(false)
    val isLiveCameraHudActive: StateFlow<Boolean> = _isLiveCameraHudActive.asStateFlow()

    private val _capturedVisionBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val capturedVisionBitmap: StateFlow<android.graphics.Bitmap?> = _capturedVisionBitmap.asStateFlow()

    // Multi-API Configuration State
    private val _apiConfig = MutableStateFlow(
        MultiApiConfig(
            selectedEngine = try {
                AiEngineType.valueOf(prefs.getString("selected_ai_engine", AiEngineType.AUTO_HYBRID.name) ?: AiEngineType.AUTO_HYBRID.name)
            } catch (_: Exception) {
                AiEngineType.AUTO_HYBRID
            },
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            geminiModel = prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
            openAiApiKey = prefs.getString("openai_api_key", "") ?: "",
            openAiModel = prefs.getString("openai_model", "gpt-4o-mini") ?: "gpt-4o-mini",
            groqApiKey = prefs.getString("groq_api_key", "") ?: "",
            groqModel = prefs.getString("groq_model", "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile",
            deepSeekApiKey = prefs.getString("deepseek_api_key", "") ?: "",
            deepSeekModel = prefs.getString("deepseek_model", "deepseek-chat") ?: "deepseek-chat",
            claudeApiKey = prefs.getString("claude_api_key", "") ?: "",
            claudeModel = prefs.getString("claude_model", "claude-3-5-sonnet-20241022") ?: "claude-3-5-sonnet-20241022",
            openRouterApiKey = prefs.getString("openrouter_api_key", "") ?: "",
            openRouterModel = prefs.getString("openrouter_model", "google/gemini-2.0-flash-exp:free") ?: "google/gemini-2.0-flash-exp:free",
            customApiBaseUrl = prefs.getString("custom_api_base_url", "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            customModelName = prefs.getString("custom_model_name", "gpt-4o-mini") ?: "gpt-4o-mini"
        )
    )
    val apiConfig: StateFlow<MultiApiConfig> = _apiConfig.asStateFlow()

    // Assistant Identity & Persona State
    private val _assistantConfig = MutableStateFlow(
        AssistantConfig(
            name = prefs.getString("assistant_name", "J.A.R.V.I.S.") ?: "J.A.R.V.I.S.",
            userTitle = prefs.getString("user_title", "sir") ?: "sir",
            preset = try {
                CharacterPreset.valueOf(prefs.getString("character_preset", CharacterPreset.JARVIS.name) ?: CharacterPreset.JARVIS.name)
            } catch (_: Exception) {
                CharacterPreset.JARVIS
            },
            nature = try {
                AiNatureType.valueOf(prefs.getString("nature_type", AiNatureType.LOYAL_BUTLER.name) ?: AiNatureType.LOYAL_BUTLER.name)
            } catch (_: Exception) {
                AiNatureType.LOYAL_BUTLER
            },
            customNaturePrompt = prefs.getString("custom_nature_prompt", "") ?: "",
            wakeWord = prefs.getString("wake_word", "Jarvis") ?: "Jarvis",
            isWakeWordEnabled = prefs.getBoolean("wake_word_enabled", true),
            isContinuousMode = prefs.getBoolean("continuous_mode_enabled", true),
            isBargeInEnabled = prefs.getBoolean("barge_in_enabled", true)
        )
    )
    val assistantConfig: StateFlow<AssistantConfig> = _assistantConfig.asStateFlow()

    private val _isSecurityLockEnabled = MutableStateFlow(prefs.getBoolean("security_lock_enabled", false))
    val isSecurityLockEnabled: StateFlow<Boolean> = _isSecurityLockEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(!prefs.getBoolean("security_lock_enabled", false))
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _securityPin = MutableStateFlow(prefs.getString("security_pin", "3000") ?: "3000")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isFlashlightActive = MutableStateFlow(false)
    val isFlashlightActive: StateFlow<Boolean> = _isFlashlightActive.asStateFlow()

    private val _statusBannerText = MutableStateFlow("J.A.R.V.I.S. Mark VII // Online")
    val statusBannerText: StateFlow<String> = _statusBannerText.asStateFlow()

    // 0: AI Core, 1: Character & Voices, 2: Screen & Typer, 3: API Settings, 4: Apps Hub, 5: Hardware & Diagnostics
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    init {
        refreshTelemetry()
        checkAccessibilityStatus()
        checkOverlayStatus()

        // Apply saved character voice on start
        speechManager.applyCharacterPreset(_assistantConfig.value.preset)
        speechManager.setContinuousMode(_assistantConfig.value.isContinuousMode)

        val welcomeMsg = "Good day, ${_assistantConfig.value.userTitle}. ${_assistantConfig.value.name} is online. Full phone accessibility controls, screen auto-typing, continuous hands-free dialogue, and multi-API routing are active. How may I assist you?"
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.JARVIS,
                text = welcomeMsg,
                isGeminiPowered = false,
                providerBadge = "Core Online"
            )
        )

        viewModelScope.launch {
            delay(1000)
            if (_isAppUnlocked.value) {
                speechManager.speak(welcomeMsg)
            }
        }

        // Periodic telemetry and service status checker
        viewModelScope.launch {
            while (true) {
                refreshTelemetry()
                checkAccessibilityStatus()
                checkOverlayStatus()
                delay(8000)
            }
        }
    }

    fun checkAccessibilityStatus() {
        _isAccessibilityActive.value = PhoneController.isAccessibilityEnabled(getApplication())
    }

    fun checkOverlayStatus() {
        _isFloatingOverlayActive.value = PhoneController.isFloatingOverlayRunning()
    }

    fun unlockApp() {
        _isAppUnlocked.value = true
        speechManager.speak("Clearance verified. Systems unsealed, ${_assistantConfig.value.userTitle}.")
    }

    fun lockApp() {
        if (_isSecurityLockEnabled.value) {
            _isAppUnlocked.value = false
            speechManager.stopSpeaking()
            speechManager.stopListening()
        }
    }

    fun setSecurityLockEnabled(enabled: Boolean) {
        _isSecurityLockEnabled.value = enabled
        prefs.edit().putBoolean("security_lock_enabled", enabled).apply()
        if (!enabled) {
            _isAppUnlocked.value = true
        }
    }

    fun setSecurityPin(pin: String) {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            _securityPin.value = pin
            prefs.edit().putString("security_pin", pin).apply()
        }
    }

    // ==========================================
    // MULTI-API CONFIGURATION METHODS
    // ==========================================

    fun setAiEngine(engine: AiEngineType) {
        _apiConfig.value = _apiConfig.value.copy(selectedEngine = engine)
        prefs.edit().putString("selected_ai_engine", engine.name).apply()
        _statusBannerText.value = "AI Core // ${engine.displayName} Active"
    }

    fun updateGeminiConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(geminiApiKey = key.trim(), geminiModel = model)
        prefs.edit().putString("gemini_api_key", key.trim()).putString("gemini_model", model).apply()
    }

    fun updateOpenAiConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(openAiApiKey = key.trim(), openAiModel = model)
        prefs.edit().putString("openai_api_key", key.trim()).putString("openai_model", model).apply()
    }

    fun updateGroqConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(groqApiKey = key.trim(), groqModel = model)
        prefs.edit().putString("groq_api_key", key.trim()).putString("groq_model", model).apply()
    }

    fun updateDeepSeekConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(deepSeekApiKey = key.trim(), deepSeekModel = model)
        prefs.edit().putString("deepseek_api_key", key.trim()).putString("deepseek_model", model).apply()
    }

    fun updateClaudeConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(claudeApiKey = key.trim(), claudeModel = model)
        prefs.edit().putString("claude_api_key", key.trim()).putString("claude_model", model).apply()
    }

    fun updateOpenRouterConfig(key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(openRouterApiKey = key.trim(), openRouterModel = model)
        prefs.edit().putString("openrouter_api_key", key.trim()).putString("openrouter_model", model).apply()
    }

    fun updateCustomEndpointConfig(baseUrl: String, key: String, model: String) {
        _apiConfig.value = _apiConfig.value.copy(customApiBaseUrl = baseUrl.trim(), customApiKey = key.trim(), customModelName = model.trim())
        prefs.edit()
            .putString("custom_api_base_url", baseUrl.trim())
            .putString("custom_api_key", key.trim())
            .putString("custom_model_name", model.trim())
            .apply()
    }

    // ==========================================
    // ASSISTANT CHARACTER & NATURE CUSTOMIZATION
    // ==========================================

    fun selectCharacterPreset(preset: CharacterPreset) {
        val newConfig = _assistantConfig.value.copy(
            name = if (preset != CharacterPreset.CUSTOM) preset.assistantName else _assistantConfig.value.name,
            userTitle = if (preset != CharacterPreset.CUSTOM) preset.defaultTitle else _assistantConfig.value.userTitle,
            preset = preset
        )
        _assistantConfig.value = newConfig
        prefs.edit()
            .putString("assistant_name", newConfig.name)
            .putString("user_title", newConfig.userTitle)
            .putString("character_preset", preset.name)
            .apply()

        speechManager.applyCharacterPreset(preset)
        speechManager.previewVoice("${preset.assistantName} vocal matrix online. At your service, ${newConfig.userTitle}.")
    }

    fun updateAssistantIdentity(name: String, userTitle: String) {
        val cleanName = name.trim().ifBlank { "J.A.R.V.I.S." }
        val cleanTitle = userTitle.trim().ifBlank { "sir" }
        _assistantConfig.value = _assistantConfig.value.copy(name = cleanName, userTitle = cleanTitle)
        prefs.edit()
            .putString("assistant_name", cleanName)
            .putString("user_title", cleanTitle)
            .apply()
    }

    fun setAiNature(nature: AiNatureType, customPrompt: String = "") {
        _assistantConfig.value = _assistantConfig.value.copy(nature = nature, customNaturePrompt = customPrompt)
        prefs.edit()
            .putString("nature_type", nature.name)
            .putString("custom_nature_prompt", customPrompt)
            .apply()
    }

    fun setWakeWord(word: String, enabled: Boolean) {
        val cleanWord = word.trim().ifBlank { "Jarvis" }
        _assistantConfig.value = _assistantConfig.value.copy(wakeWord = cleanWord, isWakeWordEnabled = enabled)
        prefs.edit()
            .putString("wake_word", cleanWord)
            .putBoolean("wake_word_enabled", enabled)
            .apply()
    }

    fun toggleContinuousMode(enabled: Boolean) {
        _assistantConfig.value = _assistantConfig.value.copy(isContinuousMode = enabled)
        speechManager.setContinuousMode(enabled)
        prefs.edit().putBoolean("continuous_mode_enabled", enabled).apply()
        _statusBannerText.value = if (enabled) "Continuous Dialogue Loop: Active" else "Single Command Mode Active"
    }

    fun toggleBargeIn(enabled: Boolean) {
        _assistantConfig.value = _assistantConfig.value.copy(isBargeInEnabled = enabled)
        prefs.edit().putBoolean("barge_in_enabled", enabled).apply()
    }

    fun toggleFloatingOverlay() {
        val res = PhoneController.toggleFloatingOverlay(getApplication())
        checkOverlayStatus()
        speechManager.speak(res.message)
    }

    fun setTab(index: Int) {
        _activeTab.value = index
        checkAccessibilityStatus()
        checkOverlayStatus()
    }

    fun refreshTelemetry() {
        val tel = PhoneController.getDeviceTelemetry(getApplication())
        _telemetry.value = tel
    }

    // ==========================================
    // VOICE CONVERSATION & CONTINUOUS LOOP
    // ==========================================

    fun toggleVoiceListening() {
        // BARGE-IN: If Jarvis is currently speaking, tapping mic or reactor immediately interrupts and listens
        if (speechManager.isSpeaking.value) {
            speechManager.stopSpeaking()
            startListeningInternal()
            return
        }

        if (speechManager.isListening.value) {
            speechManager.stopListening()
            _reactorState.value = JarvisReactorState.STANDBY
            _statusBannerText.value = "${_assistantConfig.value.name} // Standby"
        } else {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        speechManager.stopSpeaking()
        _reactorState.value = JarvisReactorState.LISTENING
        _statusBannerText.value = "Auditory Sensors Active // Listening..."

        speechManager.startListening(
            onResult = { recognizedText ->
                _reactorState.value = JarvisReactorState.STANDBY
                processInput(recognizedText)
            },
            onError = { error ->
                _reactorState.value = JarvisReactorState.STANDBY
                _statusBannerText.value = "Mic: $error"
            }
        )
    }

    fun selectFileForAnalysis(uri: android.net.Uri) {
        viewModelScope.launch {
            val fileInfo = com.example.data.JarvisDeviceFileManager.readFileContent(getApplication(), uri)
            _attachedFile.value = fileInfo
            if (fileInfo != null) {
                _statusBannerText.value = "File attached: ${fileInfo.name} (${fileInfo.sizeFormatted})"
                speechManager.speak("File ${fileInfo.name} loaded into sensory buffer, ${_assistantConfig.value.userTitle}. Ask me anything about it.")
            } else {
                _statusBannerText.value = "Failed to load attached file"
            }
        }
    }

    fun clearAttachedFile() {
        _attachedFile.value = null
        _statusBannerText.value = "${_assistantConfig.value.name} // Standby"
    }

    fun setVisionModeActive(active: Boolean) {
        _isVisionModeActive.value = active
    }

    fun openLiveCameraHud() {
        _isLiveCameraHudActive.value = true
        speechManager.speak("Live Optical Vision HUD engaged. Point camera at target and tap Scan.")
    }

    fun closeLiveCameraHud() {
        _isLiveCameraHudActive.value = false
    }

    fun analyzeCurrentLiveScreen(customPrompt: String? = null) {
        val cleanPrompt = customPrompt?.trim()?.ifBlank { "What is on my screen right now? Summarize and explain all visible details." }
            ?: "What is on my screen right now? Summarize and explain all visible details."

        // Barge-in
        speechManager.stopSpeaking()
        speechManager.stopListening()

        val service = JarvisAccessibilityService.instance
        if (service == null) {
            val note = "Accessibility service is inactive. Please enable '${_assistantConfig.value.name} Automation' in Android Settings to read live screen."
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = note,
                actionExecuted = "Accessibility Disabled",
                isGeminiPowered = false,
                providerBadge = "Screen Sensor"
            )
            _messages.value = _messages.value + jarvisMsg
            speechManager.speak(note)
            return
        }

        val screenDump = service.dumpCurrentScreenContent()
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = "📱 [Live Screen Scan]: $cleanPrompt"
        )
        _messages.value = _messages.value + userMsg
        _reactorState.value = JarvisReactorState.THINKING
        _statusBannerText.value = "Reading Live Screen Buffer..."

        viewModelScope.launch {
            val promptWithContext = """
You are ${_assistantConfig.value.name}. The user asked: "$cleanPrompt"

Here is the live real-time accessibility hierarchy and text dumped directly from the user's active device screen:
--- ACTIVE SCREEN CONTENT DUMP ---
$screenDump
--- END OF SCREEN CONTENT ---

Please analyze the above screen content, identify the active app/page, explain the key information or error or form clearly, and answer the user's question directly.
""".trimIndent()

            val history = _messages.value
                .filter { it.sender != MessageSender.SYSTEM }
                .takeLast(4)
                .map { (if (it.sender == MessageSender.USER) "User: " else "${_assistantConfig.value.name}: ") to it.text }

            val response = GeminiJarvisService.processUserMessage(
                userMessage = promptWithContext,
                apiConfig = _apiConfig.value,
                assistantConfig = _assistantConfig.value,
                conversationHistory = history
            )

            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = response.replyText,
                actionExecuted = "Live Screen Analyzed (${screenDump.take(30)}...)",
                isGeminiPowered = response.isFromGemini,
                providerBadge = "Screen Vision Core"
            )
            _messages.value = _messages.value + jarvisMsg

            _statusBannerText.value = "${_assistantConfig.value.name} // Responding"
            _reactorState.value = JarvisReactorState.SPEAKING
            speechManager.speak(response.replyText) {
                _reactorState.value = JarvisReactorState.STANDBY
                _statusBannerText.value = "${_assistantConfig.value.name} // Standby"
            }
        }
    }

    fun setCapturedVisionBitmap(bitmap: android.graphics.Bitmap?) {
        _capturedVisionBitmap.value = bitmap
    }

    fun sendVisionAnalysis(prompt: String, bitmap: android.graphics.Bitmap) {
        val cleanPrompt = prompt.trim().ifBlank { "What do you see in this image? Explain in detail." }
        _isVisionModeActive.value = false
        _capturedVisionBitmap.value = null

        // Barge-in
        speechManager.stopSpeaking()
        speechManager.stopListening()

        val base64 = com.example.data.JarvisDeviceFileManager.bitmapToBase64(bitmap)

        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = cleanPrompt,
            attachedBitmap = bitmap
        )
        _messages.value = _messages.value + userMsg

        _reactorState.value = JarvisReactorState.THINKING
        _statusBannerText.value = "Visual Analysis in progress..."

        viewModelScope.launch {
            val history = _messages.value
                .filter { it.sender != MessageSender.SYSTEM }
                .takeLast(6)
                .map { (if (it.sender == MessageSender.USER) "User: " else "${_assistantConfig.value.name}: ") to it.text }

            val response: JarvisResponse = GeminiJarvisService.processUserMessage(
                userMessage = cleanPrompt,
                apiConfig = _apiConfig.value,
                assistantConfig = _assistantConfig.value,
                conversationHistory = history,
                attachedImageBase64 = base64,
                attachedImageMimeType = "image/jpeg"
            )

            var actionDescription: String? = null
            var createdFileInfo: com.example.data.CreatedDeviceInfo? = null

            if (response.command is JarvisCommand.CreateDeviceFile) {
                val cmd = response.command as JarvisCommand.CreateDeviceFile
                val fileRes = com.example.data.JarvisDeviceFileManager.createDeviceFile(getApplication(), cmd.fileName, cmd.content)
                createdFileInfo = fileRes
                actionDescription = if (fileRes.success) "File saved to ${fileRes.filePath}" else "File error: ${fileRes.errorMessage}"
            } else if (response.command !is JarvisCommand.None) {
                _reactorState.value = JarvisReactorState.EXECUTING
                val execResult = executeJarvisCommand(response.command)
                actionDescription = execResult.message
                delay(200)
            }

            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = response.replyText,
                actionExecuted = actionDescription,
                isGeminiPowered = response.isFromGemini,
                providerBadge = response.providerUsed,
                createdFile = createdFileInfo
            )
            _messages.value = _messages.value + jarvisMsg

            _statusBannerText.value = "${_assistantConfig.value.name} // Responding"
            _reactorState.value = JarvisReactorState.SPEAKING

            speechManager.speak(response.replyText) {
                _reactorState.value = JarvisReactorState.STANDBY
                _statusBannerText.value = "${_assistantConfig.value.name} // Standby"
                if (_assistantConfig.value.isContinuousMode && _isAppUnlocked.value) {
                    viewModelScope.launch {
                        delay(400)
                        if (!speechManager.isSpeaking.value && !speechManager.isListening.value) {
                            startListeningInternal()
                        }
                    }
                }
            }
        }
    }

    fun createDeviceFileDirectly(fileName: String, content: String) {
        viewModelScope.launch {
            val fileRes = com.example.data.JarvisDeviceFileManager.createDeviceFile(getApplication(), fileName, content)
            val userMsg = ChatMessage(
                sender = MessageSender.USER,
                text = "Create file \"$fileName\" on my device with code."
            )
            val replyText = if (fileRes.success) {
                "File \"$fileName\" created successfully in ${fileRes.folderType} storage (${fileRes.fileSizeKB} KB), ${_assistantConfig.value.userTitle}."
            } else {
                "Failed to create file \"$fileName\": ${fileRes.errorMessage}, ${_assistantConfig.value.userTitle}."
            }
            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = replyText,
                actionExecuted = if (fileRes.success) "Saved: ${fileRes.filePath}" else fileRes.errorMessage,
                isGeminiPowered = false,
                providerBadge = "Device File Engine",
                createdFile = fileRes
            )
            _messages.value = _messages.value + userMsg + jarvisMsg
            speechManager.speak(replyText)
        }
    }

    fun processInput(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        // BARGE-IN: stop TTS immediately
        speechManager.stopSpeaking()
        speechManager.stopListening()

        val fileAttached = _attachedFile.value
        _attachedFile.value = null // consume

        // Append user message
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = cleanText,
            attachedFile = fileAttached
        )
        _messages.value = _messages.value + userMsg

        _reactorState.value = JarvisReactorState.THINKING
        _statusBannerText.value = "Neural Processing // ${_apiConfig.value.selectedEngine.displayName}"

        viewModelScope.launch {
            val history = _messages.value
                .filter { it.sender != MessageSender.SYSTEM }
                .takeLast(6)
                .map { (if (it.sender == MessageSender.USER) "User: " else "${_assistantConfig.value.name}: ") to it.text }

            val response: JarvisResponse = GeminiJarvisService.processUserMessage(
                userMessage = cleanText,
                apiConfig = _apiConfig.value,
                assistantConfig = _assistantConfig.value,
                conversationHistory = history,
                attachedFile = fileAttached
            )

            var actionDescription: String? = null
            var createdFileInfo: com.example.data.CreatedDeviceInfo? = null

            if (response.command is JarvisCommand.CreateDeviceFile) {
                val cmd = response.command as JarvisCommand.CreateDeviceFile
                val fileRes = com.example.data.JarvisDeviceFileManager.createDeviceFile(getApplication(), cmd.fileName, cmd.content)
                createdFileInfo = fileRes
                actionDescription = if (fileRes.success) "File saved to ${fileRes.filePath}" else "File error: ${fileRes.errorMessage}"
            } else if (response.command !is JarvisCommand.None) {
                _reactorState.value = JarvisReactorState.EXECUTING
                val execResult = executeJarvisCommand(response.command)
                actionDescription = execResult.message
                delay(200)
            }

            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = response.replyText,
                actionExecuted = actionDescription,
                isGeminiPowered = response.isFromGemini,
                providerBadge = response.providerUsed,
                createdFile = createdFileInfo
            )
            _messages.value = _messages.value + jarvisMsg

            _statusBannerText.value = "${_assistantConfig.value.name} // Responding"
            _reactorState.value = JarvisReactorState.SPEAKING

            speechManager.speak(response.replyText) {
                _reactorState.value = JarvisReactorState.STANDBY
                _statusBannerText.value = "${_assistantConfig.value.name} // Standby"

                // CONTINUOUS CONVERSATION LOOP: Auto re-listen if enabled!
                if (_assistantConfig.value.isContinuousMode && _isAppUnlocked.value) {
                    viewModelScope.launch {
                        delay(400) // gentle breathing buffer
                        if (!speechManager.isSpeaking.value && !speechManager.isListening.value) {
                            startListeningInternal()
                        }
                    }
                }
            }
        }
    }

    fun executeDirectCommand(command: JarvisCommand, promptNote: String? = null) {
        viewModelScope.launch {
            _reactorState.value = JarvisReactorState.EXECUTING
            val result = executeJarvisCommand(command)
            val ackText = promptNote ?: result.message

            val jarvisMsg = ChatMessage(
                sender = MessageSender.JARVIS,
                text = ackText,
                actionExecuted = result.message,
                isGeminiPowered = false,
                providerBadge = "Direct Control"
            )
            _messages.value = _messages.value + jarvisMsg
            _reactorState.value = JarvisReactorState.SPEAKING
            speechManager.speak(ackText) {
                _reactorState.value = JarvisReactorState.STANDBY
            }
        }
    }

    fun typeTextAnywhere(text: String) {
        executeDirectCommand(
            JarvisCommand.AccessibilityTypeText(text),
            "Injecting \"$text\" into target input field, ${_assistantConfig.value.userTitle}."
        )
    }

    fun clickOnScreenText(text: String) {
        executeDirectCommand(
            JarvisCommand.AccessibilityClick(text),
            "Clicking on \"$text\" on screen, ${_assistantConfig.value.userTitle}."
        )
    }

    fun triggerGlobalAction(action: JarvisAccessibilityService.GlobalActionType, label: String) {
        val cmd = when (action) {
            JarvisAccessibilityService.GlobalActionType.HOME -> JarvisCommand.GlobalHome
            JarvisAccessibilityService.GlobalActionType.BACK -> JarvisCommand.GlobalBack
            JarvisAccessibilityService.GlobalActionType.RECENTS -> JarvisCommand.GlobalRecents
            JarvisAccessibilityService.GlobalActionType.NOTIFICATIONS -> JarvisCommand.GlobalNotifications
            JarvisAccessibilityService.GlobalActionType.QUICK_SETTINGS -> JarvisCommand.GlobalQuickSettings
            JarvisAccessibilityService.GlobalActionType.LOCK_SCREEN -> JarvisCommand.GlobalLockScreen
            JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT -> JarvisCommand.GlobalScreenshot
            JarvisAccessibilityService.GlobalActionType.POWER_DIALOG -> JarvisCommand.None
        }
        executeDirectCommand(cmd, "$label executed, ${_assistantConfig.value.userTitle}.")
    }

    fun openAccessibilitySettings() {
        executeDirectCommand(
            JarvisCommand.OpenAccessibilitySettings,
            "Opening Accessibility settings to calibrate automation clearance, ${_assistantConfig.value.userTitle}."
        )
    }

    private fun executeJarvisCommand(command: JarvisCommand): ActionResult {
        val context = getApplication<Application>()
        val res = when (command) {
            // Accessibility Actions
            is JarvisCommand.AccessibilityTypeText -> PhoneController.typeTextOnScreen(context, command.text)
            is JarvisCommand.AccessibilityClick -> PhoneController.clickOnScreenByText(context, command.targetText)
            is JarvisCommand.GlobalHome -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.HOME, "Home")
            is JarvisCommand.GlobalBack -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.BACK, "Back")
            is JarvisCommand.GlobalRecents -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.RECENTS, "Recent Apps")
            is JarvisCommand.GlobalNotifications -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.NOTIFICATIONS, "Notifications")
            is JarvisCommand.GlobalQuickSettings -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.QUICK_SETTINGS, "Quick Settings")
            is JarvisCommand.GlobalLockScreen -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.LOCK_SCREEN, "Lock Screen")
            is JarvisCommand.GlobalScreenshot -> PhoneController.performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT, "Screenshot")
            is JarvisCommand.OpenAccessibilitySettings -> PhoneController.openAccessibilitySettings(context)

            // Vision & Live Screen Perception
            is JarvisCommand.AnalyzeScreen -> {
                analyzeCurrentLiveScreen(command.prompt)
                ActionResult(true, "Screen analysis initiated.")
            }
            is JarvisCommand.OpenLiveVisionScanner -> {
                _isLiveCameraHudActive.value = true
                ActionResult(true, "Live Camera Vision HUD launched.")
            }

            // App & Comms
            is JarvisCommand.OpenWhatsApp -> PhoneController.openWhatsApp(context)
            is JarvisCommand.SendWhatsAppMsg -> PhoneController.sendWhatsAppMessage(context, command.phone, command.message)
            is JarvisCommand.OpenWhatsAppStatus -> PhoneController.openWhatsAppStatus(context)

            is JarvisCommand.OpenTelegram -> PhoneController.openTelegram(context)
            is JarvisCommand.SendTelegramMsg -> PhoneController.sendTelegramMessage(context, command.username)

            is JarvisCommand.OpenSpotify -> PhoneController.openSpotify(context)
            is JarvisCommand.PlaySpotify -> PhoneController.searchSpotify(context, command.query)

            is JarvisCommand.OpenTwitter -> PhoneController.openTwitter(context)
            is JarvisCommand.OpenNetflix -> PhoneController.openNetflix(context)
            is JarvisCommand.OpenWebUrl -> PhoneController.openBrowserUrl(context, command.url)

            is JarvisCommand.OpenInstagram -> PhoneController.openInstagram(context)
            is JarvisCommand.OpenInstagramProfile -> PhoneController.openInstagramProfile(context, command.username)
            is JarvisCommand.OpenInstagramReels -> PhoneController.openInstagramReels(context)
            is JarvisCommand.OpenInstagramDirect -> PhoneController.openInstagramDirect(context)
            is JarvisCommand.OpenInstagramStoryCamera -> PhoneController.openInstagramStoryCamera(context)

            is JarvisCommand.OpenYouTube -> PhoneController.openYouTube(context)
            is JarvisCommand.SearchYouTube -> PhoneController.searchYouTube(context, command.query)
            is JarvisCommand.PlayYouTube -> PhoneController.playYouTubeVideo(context, command.query)
            is JarvisCommand.OpenYouTubeTrending -> PhoneController.openYouTubeTrending(context)

            is JarvisCommand.Flashlight -> {
                val r = PhoneController.toggleFlashlight(context, command.state)
                _isFlashlightActive.value = PhoneController.isFlashlightOn()
                r
            }
            is JarvisCommand.OpenCamera -> PhoneController.openCamera(context, false)
            is JarvisCommand.RecordVideo -> PhoneController.openCamera(context, true)
            is JarvisCommand.CallPhone -> PhoneController.makePhoneCall(context, command.number)
            is JarvisCommand.SendSms -> PhoneController.sendSms(context, command.number, command.message)
            is JarvisCommand.OpenContacts -> PhoneController.openContacts(context)
            is JarvisCommand.SetAlarm -> PhoneController.setAlarm(context, command.hour, command.minute, command.label)
            is JarvisCommand.SetTimer -> PhoneController.setTimer(context, command.seconds, command.label)
            is JarvisCommand.OpenSettings -> PhoneController.openSettings(context, command.type)
            is JarvisCommand.OpenMaps -> PhoneController.openMaps(context, command.destination)
            is JarvisCommand.SearchGoogle -> PhoneController.searchGoogle(context, command.query)
            is JarvisCommand.OpenCalculator -> PhoneController.openCalculator(context)
            is JarvisCommand.BatteryStatus -> {
                val b = PhoneController.getBatteryTelemetry(context)
                ActionResult(true, "Battery is at ${b.percentage}%, Charging: ${b.isCharging}, Temp: ${b.temperatureCelsius}°C")
            }
            is JarvisCommand.DeviceTelemetryReport -> {
                refreshTelemetry()
                val t = _telemetry.value
                val report = if (t != null) {
                    "System Report: ${t.deviceModel}, ${t.androidVersion}. Battery: ${t.battery.percentage}%. RAM: ${t.memoryUsedMB}MB / ${t.memoryMaxMB}MB. Storage: ${t.storageAvailableGB}GB free."
                } else {
                    "All telemetry sensors operating within nominal parameters."
                }
                ActionResult(true, report)
            }
            is JarvisCommand.CreateDeviceFile -> {
                val fileRes = com.example.data.JarvisDeviceFileManager.createDeviceFile(context, command.fileName, command.content)
                if (fileRes.success) {
                    ActionResult(true, "File \"${command.fileName}\" written to device storage (${fileRes.filePath}).")
                } else {
                    ActionResult(false, "Failed to create file \"${command.fileName}\": ${fileRes.errorMessage}")
                }
            }
            is JarvisCommand.None -> ActionResult(true, "No device action required.")
        }
        checkAccessibilityStatus()
        return res
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "Terminal logs purged. ${_assistantConfig.value.name} memory buffer refreshed."
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
