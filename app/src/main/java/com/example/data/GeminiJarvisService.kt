package com.example.data

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class JarvisCommand {
    // Accessibility Universal Typing & Clicking
    data class AccessibilityTypeText(val text: String) : JarvisCommand()
    data class AccessibilityClick(val targetText: String) : JarvisCommand()
    object GlobalHome : JarvisCommand()
    object GlobalBack : JarvisCommand()
    object GlobalRecents : JarvisCommand()
    object GlobalNotifications : JarvisCommand()
    object GlobalQuickSettings : JarvisCommand()
    object GlobalLockScreen : JarvisCommand()
    object GlobalScreenshot : JarvisCommand()
    object OpenAccessibilitySettings : JarvisCommand()

    // Apps & Comms
    object OpenWhatsApp : JarvisCommand()
    data class SendWhatsAppMsg(val phone: String?, val message: String) : JarvisCommand()
    object OpenWhatsAppStatus : JarvisCommand()

    object OpenTelegram : JarvisCommand()
    data class SendTelegramMsg(val username: String?, val message: String) : JarvisCommand()

    object OpenSpotify : JarvisCommand()
    data class PlaySpotify(val query: String) : JarvisCommand()

    object OpenTwitter : JarvisCommand()
    object OpenNetflix : JarvisCommand()
    data class OpenWebUrl(val url: String) : JarvisCommand()

    object OpenInstagram : JarvisCommand()
    data class OpenInstagramProfile(val username: String) : JarvisCommand()
    object OpenInstagramReels : JarvisCommand()
    object OpenInstagramDirect : JarvisCommand()
    object OpenInstagramStoryCamera : JarvisCommand()

    object OpenYouTube : JarvisCommand()
    data class SearchYouTube(val query: String) : JarvisCommand()
    data class PlayYouTube(val query: String) : JarvisCommand()
    object OpenYouTubeTrending : JarvisCommand()

    // Vision & Screen Perception
    data class AnalyzeScreen(val prompt: String = "Explain what is on my screen right now") : JarvisCommand()
    object OpenLiveVisionScanner : JarvisCommand()

    // Hardware & System
    data class Flashlight(val state: Boolean?) : JarvisCommand()
    object OpenCamera : JarvisCommand()
    object RecordVideo : JarvisCommand()
    data class CallPhone(val number: String) : JarvisCommand()
    data class SendSms(val number: String?, val message: String) : JarvisCommand()
    object OpenContacts : JarvisCommand()
    data class SetAlarm(val hour: Int, val minute: Int, val label: String) : JarvisCommand()
    data class SetTimer(val seconds: Int, val label: String) : JarvisCommand()
    data class OpenSettings(val type: String) : JarvisCommand()
    data class OpenMaps(val destination: String) : JarvisCommand()
    data class SearchGoogle(val query: String) : JarvisCommand()
    object OpenCalculator : JarvisCommand()
    object BatteryStatus : JarvisCommand()
    object DeviceTelemetryReport : JarvisCommand()
    data class CreateDeviceFile(val fileName: String, val content: String) : JarvisCommand()
    object None : JarvisCommand()
}

data class JarvisResponse(
    val replyText: String,
    val command: JarvisCommand,
    val isFromGemini: Boolean,
    val providerUsed: String = "Neural Core",
    val createdFileInfo: CreatedDeviceInfo? = null
)

object GeminiJarvisService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun buildSystemPrompt(assistantConfig: AssistantConfig): String {
        val name = assistantConfig.name.ifBlank { "J.A.R.V.I.S." }
        val title = assistantConfig.userTitle.ifBlank { "sir" }
        val naturePrompt = if (assistantConfig.nature == AiNatureType.CUSTOM_PROMPT && assistantConfig.customNaturePrompt.isNotBlank()) {
            assistantConfig.customNaturePrompt
        } else {
            assistantConfig.nature.promptInstruction
        }

        return """
You are $name, an autonomous AI assistant with full Android phone control, universal screen auto-typing, and device automation.
Address the user as '$title'.
Assistant Personality & Tone: $naturePrompt

Language & Intelligence:
You understand English, Hindi, and Hinglish fluently (e.g., "type karo", "likh do", "home screen pe jao", "back jao", "notification kholo", "screenshot lo", "phone lock karo", "whatsapp pe message bhejo", "torch jalao", "gaana chalao", "kya haal hai").

CRITICAL PHONE AUTOMATION & AUTO-TYPING INSTRUCTION:
Whenever the user requests an action (typing, clicking, navigating, opening apps, hardware control), you MUST prepend an ACTION tag at the very beginning of your response in the exact format:
[ACTION:COMMAND_TYPE:PARAMETERS]

List of Valid ACTION Tags:
- [ACTION:TYPE_TEXT:TEXT_TO_TYPE] (For typing into whatever app or input box is active on screen)
- [ACTION:CLICK_VIEW:TARGET_BUTTON_OR_TEXT] (For clicking buttons/links on screen like 'Send', 'Search', 'Submit', 'Login')
- [ACTION:GLOBAL_HOME:] (Press Home)
- [ACTION:GLOBAL_BACK:] (Press Back)
- [ACTION:GLOBAL_RECENTS:] (Open Recent Apps Overview)
- [ACTION:GLOBAL_NOTIFICATIONS:] (Pull down Notification Shade)
- [ACTION:GLOBAL_QUICK_SETTINGS:] (Open Quick Settings Panel)
- [ACTION:GLOBAL_LOCK:] (Lock the device screen)
- [ACTION:GLOBAL_SCREENSHOT:] (Capture a screenshot)
- [ACTION:ACCESSIBILITY_SETTINGS:] (Open Accessibility Settings)

- [ACTION:WHATSAPP_OPEN:]
- [ACTION:WHATSAPP_MSG:PHONE_NUMBER|MESSAGE_TEXT]
- [ACTION:WHATSAPP_STATUS:]
- [ACTION:TELEGRAM_OPEN:]
- [ACTION:TELEGRAM_MSG:USERNAME|MESSAGE_TEXT]
- [ACTION:SPOTIFY_OPEN:]
- [ACTION:SPOTIFY_PLAY:SONG_OR_ARTIST]
- [ACTION:TWITTER_OPEN:]
- [ACTION:NETFLIX_OPEN:]
- [ACTION:WEB_OPEN:URL]
- [ACTION:INSTAGRAM_OPEN:]
- [ACTION:INSTAGRAM_PROFILE:USERNAME]
- [ACTION:INSTAGRAM_REELS:]
- [ACTION:INSTAGRAM_DM:]
- [ACTION:INSTAGRAM_STORY:]
- [ACTION:YOUTUBE_OPEN:]
- [ACTION:YOUTUBE_SEARCH:QUERY]
- [ACTION:YOUTUBE_PLAY:SONG_OR_VIDEO_NAME]
- [ACTION:YOUTUBE_TRENDING:]
- [ACTION:FLASHLIGHT_ON:]
- [ACTION:FLASHLIGHT_OFF:]
- [ACTION:FLASHLIGHT_TOGGLE:]
- [ACTION:CALL:PHONE_NUMBER]
- [ACTION:SMS:PHONE_NUMBER|MESSAGE_TEXT]
- [ACTION:CAMERA:]
- [ACTION:VIDEO_RECORD:]
- [ACTION:ALARM:HOUR|MINUTE|LABEL]
- [ACTION:TIMER:SECONDS|LABEL]
- [ACTION:SETTINGS:TYPE] (wifi, bluetooth, sound, display, battery, apps, accessibility, main)
- [ACTION:MAPS:DESTINATION_NAME]
- [ACTION:GOOGLE_SEARCH:QUERY]
- [ACTION:CALCULATOR:]
- [ACTION:BATTERY:]
- [ACTION:TELEMETRY:]
- [ACTION:CREATE_FILE:FILE_NAME|FILE_CONTENT] (Creates a file directly on the user's device and writes the code/content into it)
- [ACTION:NONE:]

If the user asks to create a file or generate code for a file, use the [ACTION:CREATE_FILE:fileName|code_content] tag so it is saved directly to their phone.
After the action tag, provide your natural, engaging, and in-character response. Keep it concise (1-2 sentences) for immediate zero-delay TTS speech playback.
""".trimIndent()
    }

    suspend fun processUserMessage(
        userMessage: String,
        apiConfig: MultiApiConfig,
        assistantConfig: AssistantConfig = AssistantConfig(),
        conversationHistory: List<Pair<String, String>> = emptyList(),
        attachedImageBase64: String? = null,
        attachedImageMimeType: String = "image/jpeg",
        attachedFile: UploadedFileInfo? = null
    ): JarvisResponse = withContext(Dispatchers.IO) {
        val cleanMessage = stripWakeWord(userMessage, assistantConfig.wakeWord)
        val systemPrompt = buildSystemPrompt(assistantConfig)

        // If file content is attached, enrich prompt
        val effectiveMessage = when {
            attachedFile != null && attachedFile.textContent != null ->
                "$cleanMessage\n\n[ATTACHED FILE: ${attachedFile.name}]\n```\n${attachedFile.textContent}\n```"
            else -> cleanMessage
        }

        val effectiveImageBase64 = attachedImageBase64 ?: attachedFile?.base64Data
        val effectiveImageMime = if (attachedFile?.base64Data != null) attachedFile.mimeType else attachedImageMimeType

        // 1. If Local Offline mode is explicitly chosen
        if (apiConfig.selectedEngine == AiEngineType.LOCAL_OFFLINE) {
            return@withContext parseLocalCommand(effectiveMessage, assistantConfig)
        }

        // 2. Route based on selected engine
        when (apiConfig.selectedEngine) {
            AiEngineType.GEMINI_FREE -> {
                val key = resolveGeminiKey(apiConfig.geminiApiKey)
                if (key.isNotEmpty()) {
                    val res = callGeminiApi(apiConfig.geminiModel, effectiveMessage, key, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Gemini ${apiConfig.geminiModel}", assistantConfig)
                }
            }
            AiEngineType.OPENAI_GPT -> {
                val key = apiConfig.openAiApiKey.trim()
                if (key.isNotEmpty()) {
                    val res = callOpenAiCompatibleApi("https://api.openai.com/v1/chat/completions", apiConfig.openAiModel, effectiveMessage, key, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "OpenAI ${apiConfig.openAiModel}", assistantConfig)
                }
            }
            AiEngineType.GROQ -> {
                val key = apiConfig.groqApiKey.trim()
                if (key.isNotEmpty()) {
                    val res = callOpenAiCompatibleApi("https://api.groq.com/openai/v1/chat/completions", apiConfig.groqModel, effectiveMessage, key, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Groq ${apiConfig.groqModel}", assistantConfig)
                }
            }
            AiEngineType.DEEPSEEK -> {
                val key = apiConfig.deepSeekApiKey.trim()
                if (key.isNotEmpty()) {
                    val res = callOpenAiCompatibleApi("https://api.deepseek.com/chat/completions", apiConfig.deepSeekModel, effectiveMessage, key, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "DeepSeek ${apiConfig.deepSeekModel}", assistantConfig)
                }
            }
            AiEngineType.CLAUDE -> {
                val key = apiConfig.claudeApiKey.trim()
                if (key.isNotEmpty()) {
                    val res = callClaudeApi(apiConfig.claudeModel, effectiveMessage, key, systemPrompt, conversationHistory)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Claude ${apiConfig.claudeModel}", assistantConfig)
                }
            }
            AiEngineType.OPENROUTER -> {
                val key = apiConfig.openRouterApiKey.trim()
                if (key.isNotEmpty()) {
                    val res = callOpenAiCompatibleApi("https://openrouter.ai/api/v1/chat/completions", apiConfig.openRouterModel, effectiveMessage, key, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "OpenRouter ${apiConfig.openRouterModel}", assistantConfig)
                }
            }
            AiEngineType.CUSTOM_ENDPOINT -> {
                val baseUrl = apiConfig.customApiBaseUrl.trim().removeSuffix("/")
                val endpoint = if (baseUrl.endsWith("/chat/completions")) baseUrl else "$baseUrl/chat/completions"
                val res = callOpenAiCompatibleApi(endpoint, apiConfig.customModelName, effectiveMessage, apiConfig.customApiKey.trim(), systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Custom (${apiConfig.customModelName})", assistantConfig)
            }
            AiEngineType.AUTO_HYBRID -> {
                // Auto-Hybrid: If image attached, prioritize Gemini Vision or OpenAI
                if (effectiveImageBase64 != null) {
                    val geminiKey = resolveGeminiKey(apiConfig.geminiApiKey)
                    if (geminiKey.isNotEmpty()) {
                        val res = callGeminiApi("gemini-2.5-flash", effectiveMessage, geminiKey, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                            ?: callGeminiApi("gemini-2.0-flash", effectiveMessage, geminiKey, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                        if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Gemini Vision", assistantConfig)
                    }
                    if (apiConfig.openAiApiKey.isNotBlank()) {
                        val res = callOpenAiCompatibleApi("https://api.openai.com/v1/chat/completions", apiConfig.openAiModel, effectiveMessage, apiConfig.openAiApiKey.trim(), systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                        if (res != null) return@withContext parseAiOutput(res, cleanMessage, "OpenAI Vision", assistantConfig)
                    }
                }

                if (apiConfig.groqApiKey.isNotBlank()) {
                    val res = callOpenAiCompatibleApi("https://api.groq.com/openai/v1/chat/completions", apiConfig.groqModel, effectiveMessage, apiConfig.groqApiKey.trim(), systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Groq Lightning", assistantConfig)
                }

                val geminiKey = resolveGeminiKey(apiConfig.geminiApiKey)
                if (geminiKey.isNotEmpty()) {
                    for (model in listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-1.5-flash")) {
                        val res = callGeminiApi(model, effectiveMessage, geminiKey, systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                        if (res != null) return@withContext parseAiOutput(res, cleanMessage, "Gemini ($model)", assistantConfig)
                    }
                }

                if (apiConfig.openAiApiKey.isNotBlank()) {
                    val res = callOpenAiCompatibleApi("https://api.openai.com/v1/chat/completions", apiConfig.openAiModel, effectiveMessage, apiConfig.openAiApiKey.trim(), systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "OpenAI", assistantConfig)
                }

                if (apiConfig.deepSeekApiKey.isNotBlank()) {
                    val res = callOpenAiCompatibleApi("https://api.deepseek.com/chat/completions", apiConfig.deepSeekModel, effectiveMessage, apiConfig.deepSeekApiKey.trim(), systemPrompt, conversationHistory, effectiveImageBase64, effectiveImageMime)
                    if (res != null) return@withContext parseAiOutput(res, cleanMessage, "DeepSeek", assistantConfig)
                }
            }
            else -> {}
        }

        // Fallback: Ultra-fast Zero-Latency Local Intelligence Engine
        return@withContext parseLocalCommand(cleanMessage, assistantConfig)
    }

    private fun resolveGeminiKey(customKey: String): String {
        return when {
            customKey.isNotBlank() -> customKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }
    }

    private fun stripWakeWord(text: String, wakeWord: String): String {
        val trimmed = text.trim()
        if (wakeWord.isBlank()) return trimmed
        val regex = Regex("""(?i)^(hey\s+)?${Regex.escape(wakeWord)}[,!\s]*""")
        return trimmed.replace(regex, "").trim().ifBlank { trimmed }
    }

    private fun callOpenAiCompatibleApi(
        endpointUrl: String,
        modelName: String,
        userMessage: String,
        apiKey: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        imageBase64: String? = null,
        imageMimeType: String = "image/jpeg"
    ): String? {
        try {
            val rootJson = JSONObject()
            rootJson.put("model", modelName)

            val messages = JSONArray()
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })

            conversationHistory.takeLast(4).forEach { (user, ai) ->
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", user)
                })
                messages.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", ai)
                })
            }

            if (imageBase64 != null) {
                val contentArray = JSONArray()
                contentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", userMessage)
                })
                contentArray.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:$imageMimeType;base64,$imageBase64")
                    })
                })
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            } else {
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userMessage)
                })
            }

            rootJson.put("messages", messages)
            rootJson.put("temperature", 0.6)
            rootJson.put("max_tokens", 450)

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val requestBuilder = Request.Builder()
                .url(endpointUrl)
                .post(requestBody)

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val responseString = response.body?.string() ?: return null

            if (!response.isSuccessful) return null

            val jsonObj = JSONObject(responseString)
            val choices = jsonObj.optJSONArray("choices") ?: return null
            if (choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val msg = first.optJSONObject("message")
                return msg?.optString("content")
            }
        } catch (_: Exception) {}
        return null
    }

    private fun callClaudeApi(
        modelName: String,
        userMessage: String,
        apiKey: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>
    ): String? {
        try {
            val rootJson = JSONObject()
            rootJson.put("model", modelName)
            rootJson.put("system", systemPrompt)
            rootJson.put("max_tokens", 450)

            val messages = JSONArray()
            conversationHistory.takeLast(4).forEach { (user, ai) ->
                messages.put(JSONObject().apply {
                    put("role", "user")
                    put("content", user)
                })
                messages.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", ai)
                })
            }
            messages.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
            rootJson.put("messages", messages)

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: return null

            if (!response.isSuccessful) return null

            val jsonObj = JSONObject(responseString)
            val contentArr = jsonObj.optJSONArray("content") ?: return null
            if (contentArr.length() > 0) {
                return contentArr.getJSONObject(0).optString("text")
            }
        } catch (_: Exception) {}
        return null
    }

    private fun callGeminiApi(
        modelName: String,
        userMessage: String,
        apiKey: String,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        imageBase64: String? = null,
        imageMimeType: String = "image/jpeg"
    ): String? {
        try {
            val rootJson = JSONObject()

            // System Instruction
            val systemInstruction = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemPrompt))
                })
            }
            rootJson.put("systemInstruction", systemInstruction)

            // Contents array
            val contentsArray = JSONArray()

            conversationHistory.takeLast(4).forEach { (user, ai) ->
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", user))
                    })
                })
                contentsArray.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", ai))
                    })
                })
            }

            val userParts = JSONArray()
            if (imageBase64 != null) {
                userParts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", imageMimeType)
                        put("data", imageBase64)
                    })
                })
            }
            userParts.put(JSONObject().apply {
                put("text", userMessage)
            })

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", userParts)
            })
            rootJson.put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.6)
                put("topP", 0.9)
                put("maxOutputTokens", 500)
            }
            rootJson.put("generationConfig", genConfig)

            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: return null

            if (!response.isSuccessful) return null

            val jsonObj = JSONObject(responseString)
            val candidates = jsonObj.optJSONArray("candidates") ?: return null
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content") ?: return null
                val parts = content.optJSONArray("parts") ?: return null
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text")
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun parseAiOutput(rawText: String, originalPrompt: String, provider: String, assistantConfig: AssistantConfig): JarvisResponse {
        val actionRegex = Regex("""\[ACTION:([A-Z_]+):?(.*?)\]""", RegexOption.IGNORE_CASE)
        val match = actionRegex.find(rawText)

        if (match != null) {
            val actionType = match.groupValues[1].uppercase()
            val params = match.groupValues[2].trim()
            val cleanReply = rawText.replace(match.value, "").trim()

            val command = mapActionToCommand(actionType, params, originalPrompt)
            return JarvisResponse(
                replyText = if (cleanReply.isNotBlank()) cleanReply else "Executing requested action, ${assistantConfig.userTitle}.",
                command = command,
                isFromGemini = true,
                providerUsed = provider
            )
        }

        val fallbackLocal = parseLocalCommand(originalPrompt, assistantConfig)
        return if (fallbackLocal.command !is JarvisCommand.None) {
            JarvisResponse(replyText = rawText, command = fallbackLocal.command, isFromGemini = true, providerUsed = provider)
        } else {
            JarvisResponse(replyText = rawText, command = JarvisCommand.None, isFromGemini = true, providerUsed = provider)
        }
    }

    private fun mapActionToCommand(actionType: String, params: String, prompt: String): JarvisCommand {
        val parts = params.split("|")
        return when (actionType) {
            "ANALYZE_SCREEN" -> JarvisCommand.AnalyzeScreen(params.ifBlank { prompt })
            "LIVE_CAMERA", "LIVE_VISION" -> JarvisCommand.OpenLiveVisionScanner
            "CREATE_FILE" -> {
                val fileName = parts.getOrNull(0)?.trim()?.ifBlank { "jarvis_code.py" } ?: "jarvis_code.py"
                val firstPipe = params.indexOf('|')
                val content = if (firstPipe != -1 && firstPipe + 1 < params.length) {
                    params.substring(firstPipe + 1).trim()
                } else {
                    prompt
                }
                JarvisCommand.CreateDeviceFile(fileName, content)
            }
            "TYPE_TEXT" -> JarvisCommand.AccessibilityTypeText(params.ifBlank { prompt })
            "CLICK_VIEW" -> JarvisCommand.AccessibilityClick(params.ifBlank { "Send" })
            "GLOBAL_HOME" -> JarvisCommand.GlobalHome
            "GLOBAL_BACK" -> JarvisCommand.GlobalBack
            "GLOBAL_RECENTS" -> JarvisCommand.GlobalRecents
            "GLOBAL_NOTIFICATIONS" -> JarvisCommand.GlobalNotifications
            "GLOBAL_QUICK_SETTINGS" -> JarvisCommand.GlobalQuickSettings
            "GLOBAL_LOCK" -> JarvisCommand.GlobalLockScreen
            "GLOBAL_SCREENSHOT" -> JarvisCommand.GlobalScreenshot
            "ACCESSIBILITY_SETTINGS" -> JarvisCommand.OpenAccessibilitySettings

            "WHATSAPP_OPEN" -> JarvisCommand.OpenWhatsApp
            "WHATSAPP_MSG" -> {
                val phone = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val msg = parts.getOrNull(1) ?: prompt
                JarvisCommand.SendWhatsAppMsg(phone, msg)
            }
            "WHATSAPP_STATUS" -> JarvisCommand.OpenWhatsAppStatus
            "TELEGRAM_OPEN" -> JarvisCommand.OpenTelegram
            "TELEGRAM_MSG" -> {
                val user = parts.getOrNull(0)?.takeIf { it.isNotBlank() }
                val msg = parts.getOrNull(1) ?: prompt
                JarvisCommand.SendTelegramMsg(user, msg)
            }
            "SPOTIFY_OPEN" -> JarvisCommand.OpenSpotify
            "SPOTIFY_PLAY" -> JarvisCommand.PlaySpotify(params.ifBlank { prompt })
            "TWITTER_OPEN" -> JarvisCommand.OpenTwitter
            "NETFLIX_OPEN" -> JarvisCommand.OpenNetflix
            "WEB_OPEN" -> JarvisCommand.OpenWebUrl(params.ifBlank { "https://google.com" })
            "INSTAGRAM_OPEN" -> JarvisCommand.OpenInstagram
            "INSTAGRAM_PROFILE" -> {
                val user = parts.getOrNull(0)?.trim()?.removePrefix("@") ?: ""
                if (user.isNotEmpty()) JarvisCommand.OpenInstagramProfile(user) else JarvisCommand.OpenInstagram
            }
            "INSTAGRAM_REELS" -> JarvisCommand.OpenInstagramReels
            "INSTAGRAM_DM" -> JarvisCommand.OpenInstagramDirect
            "INSTAGRAM_STORY" -> JarvisCommand.OpenInstagramStoryCamera
            "YOUTUBE_OPEN" -> JarvisCommand.OpenYouTube
            "YOUTUBE_SEARCH" -> JarvisCommand.SearchYouTube(params.ifBlank { prompt })
            "YOUTUBE_PLAY" -> JarvisCommand.PlayYouTube(params.ifBlank { prompt })
            "YOUTUBE_TRENDING" -> JarvisCommand.OpenYouTubeTrending
            "FLASHLIGHT_ON" -> JarvisCommand.Flashlight(true)
            "FLASHLIGHT_OFF" -> JarvisCommand.Flashlight(false)
            "FLASHLIGHT_TOGGLE" -> JarvisCommand.Flashlight(null)
            "CALL" -> JarvisCommand.CallPhone(params.filter { it.isDigit() || it == '+' })
            "SMS" -> {
                val phone = parts.getOrNull(0)
                val msg = parts.getOrNull(1) ?: prompt
                JarvisCommand.SendSms(phone, msg)
            }
            "CAMERA" -> JarvisCommand.OpenCamera
            "VIDEO_RECORD" -> JarvisCommand.RecordVideo
            "ALARM" -> {
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 7
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val label = parts.getOrNull(2) ?: "Jarvis Alarm"
                JarvisCommand.SetAlarm(h, m, label)
            }
            "TIMER" -> {
                val sec = parts.getOrNull(0)?.toIntOrNull() ?: 300
                val label = parts.getOrNull(1) ?: "Jarvis Timer"
                JarvisCommand.SetTimer(sec, label)
            }
            "SETTINGS" -> JarvisCommand.OpenSettings(params.ifBlank { "main" })
            "MAPS" -> JarvisCommand.OpenMaps(params.ifBlank { prompt })
            "GOOGLE_SEARCH" -> JarvisCommand.SearchGoogle(params.ifBlank { prompt })
            "CALCULATOR" -> JarvisCommand.OpenCalculator
            "BATTERY" -> JarvisCommand.BatteryStatus
            "TELEMETRY" -> JarvisCommand.DeviceTelemetryReport
            "CONTACTS" -> JarvisCommand.OpenContacts
            else -> JarvisCommand.None
        }
    }

    /**
     * Ultra-fast Zero-Latency Local Heuristic Intelligence Engine
     */
    fun parseLocalCommand(text: String, assistantConfig: AssistantConfig = AssistantConfig()): JarvisResponse {
        val lower = text.lowercase().trim()
        val title = assistantConfig.userTitle.ifBlank { "sir" }
        val name = assistantConfig.name.ifBlank { "J.A.R.V.I.S." }

        return when {
            // Live Screen Perception & Analysis ("screen dekho", "live screen", "read screen", "screen pe kya hai")
            lower.contains("screen dekho") || lower.contains("screen padho") || lower.contains("screen par kya") || lower.contains("screen pe kya") || lower.contains("read screen") || lower.contains("read my screen") || lower.contains("analyze screen") || lower.contains("screen summary") || lower == "screen" -> {
                JarvisResponse("Analyzing active screen buffer and reading visible content, $title.", JarvisCommand.AnalyzeScreen("Summarize and explain everything visible on screen right now"), false, "Screen Perception Core")
            }

            // Live Camera HUD Scanner & Real-time Vision ("camera dekho", "live camera", "vision", "scan target")
            lower.contains("live camera") || lower.contains("live vision") || lower.contains("camera hud") || lower.contains("scan karo") || lower.contains("optical scan") || lower == "vision" || lower == "scan" || lower == "live" -> {
                JarvisResponse("Engaging real-time Live Optical Vision HUD and viewfinder, $title.", JarvisCommand.OpenLiveVisionScanner, false, "Optical Vision Core")
            }

            // Local Autonomous File Creator
            lower.contains("create file") || lower.contains("file banao") || lower.contains("file create karo") || lower.contains("save file") || lower.contains("code save karo") || lower.contains("save as file") -> {
                val nameMatch = Regex("""(?i)(file\s+named?|file\s+called?|file\s+naam\s+ki|file)\s+([a-zA-Z0-9_\-.]+)""").find(text)
                val fileName = nameMatch?.groupValues?.getOrNull(2) ?: "jarvis_script.py"
                val defaultCode = """# Created by $name AI Assistant
# Timestamp: ${System.currentTimeMillis()}

def main():
    print("Execution initialized by $name.")

if __name__ == "__main__":
    main()
""".trimIndent()
                JarvisResponse("Creating file \"$fileName\" directly on your device storage, $title.", JarvisCommand.CreateDeviceFile(fileName, defaultCode), false, "Instant Local Engine")
            }

            // Auto Typing on Screen
            lower.startsWith("type ") || lower.startsWith("type karo ") || lower.startsWith("likho ") || lower.startsWith("likh do ") || lower.contains("screen pe type") || lower.contains("yahan type") || lower.contains("vahan type") -> {
                val cleanText = text.replace(Regex("""(?i)^(type karo|type|likh do|likho|screen pe type karo|yahan type karo|vahan type karo|likh)\s*"""), "").trim().trim('"', '\'')
                val targetText = cleanText.ifBlank { "Hello from $name" }
                JarvisResponse("Injecting text \"$targetText\" into active screen input field, $title.", JarvisCommand.AccessibilityTypeText(targetText), false, "Instant Local Engine")
            }

            // Click on Screen Button
            lower.startsWith("click ") || lower.startsWith("press ") || lower.contains("click karo") || lower.contains("button dabao") || lower.contains("daba do") -> {
                val target = text.replace(Regex("""(?i)(click karo|click on|click|press|button|dabao|daba do|pe|par)"""), "").trim()
                JarvisResponse("Executing on-screen tap on \"${target.ifBlank { "element" }}\", $title.", JarvisCommand.AccessibilityClick(target.ifBlank { "Send" }), false, "Instant Local Engine")
            }

            // Global System Actions
            lower.contains("home screen") || lower == "home" || lower == "go home" || lower.contains("home jao") || lower.contains("home pe jao") -> {
                JarvisResponse("Returning to device home screen, $title.", JarvisCommand.GlobalHome, false, "Instant Local Engine")
            }
            lower.contains("back jao") || lower == "go back" || lower == "back" || lower.contains("peeche jao") || lower.contains("press back") -> {
                JarvisResponse("Navigating back, $title.", JarvisCommand.GlobalBack, false, "Instant Local Engine")
            }
            lower.contains("recent apps") || lower.contains("recents") || lower.contains("app switch") || lower.contains("recent dikhao") || lower.contains("open recents") -> {
                JarvisResponse("Displaying recent application overview, $title.", JarvisCommand.GlobalRecents, false, "Instant Local Engine")
            }
            lower.contains("notification") || lower.contains("notifications") || lower.contains("notification panel") || lower.contains("notification kholo") -> {
                JarvisResponse("Expanding notification shade, $title.", JarvisCommand.GlobalNotifications, false, "Instant Local Engine")
            }
            lower.contains("quick settings") || lower.contains("control center") -> {
                JarvisResponse("Expanding quick settings panel, $title.", JarvisCommand.GlobalQuickSettings, false, "Instant Local Engine")
            }
            lower.contains("screenshot") || lower.contains("screen shot") || lower.contains("screenshot lo") || lower.contains("screenshot kheecho") -> {
                JarvisResponse("Capturing device screenshot, $title.", JarvisCommand.GlobalScreenshot, false, "Instant Local Engine")
            }
            lower.contains("lock screen") || lower.contains("screen lock") || lower.contains("phone lock") || lower.contains("lock karo") || lower == "lock phone" -> {
                JarvisResponse("Locking device interface securely, $title.", JarvisCommand.GlobalLockScreen, false, "Instant Local Engine")
            }
            lower.contains("accessibility") || lower.contains("permission") || lower.contains("accessibility settings") -> {
                JarvisResponse("Opening Accessibility settings portal, $title.", JarvisCommand.OpenAccessibilitySettings, false, "Instant Local Engine")
            }

            // Telegram
            lower.contains("telegram") -> {
                if (lower.contains("send") || lower.contains("message") || lower.contains("msg") || lower.contains("text") || lower.contains("bhejo") || lower.contains("karo")) {
                    val user = text.replace(Regex("""(?i)(send|message|msg|text|to|on|via|telegram|bhejo|pe|ko|karo|@)"""), "").trim()
                    JarvisResponse("Establishing Telegram comms channel, $title.", JarvisCommand.SendTelegramMsg(user.ifBlank { null }, "Hello from $name AI"), false, "Instant Local Engine")
                } else {
                    JarvisResponse("Opening Telegram interface, $title.", JarvisCommand.OpenTelegram, false, "Instant Local Engine")
                }
            }

            // Spotify
            lower.contains("spotify") -> {
                if (lower.contains("play") || lower.contains("search") || lower.contains("song") || lower.contains("track") || lower.contains("chalao") || lower.contains("bajao")) {
                    val query = text.replace(Regex("""(?i)(play|search|find|on|spotify|track|song|music|gaana|gana|chalao|bajao|sunao)"""), "").trim()
                    JarvisResponse("Streaming \"${query.ifBlank { "Favorites" }}\" on Spotify, $title.", JarvisCommand.PlaySpotify(query.ifBlank { "Top Hits" }), false, "Instant Local Engine")
                } else {
                    JarvisResponse("Opening Spotify sonic interface, $title.", JarvisCommand.OpenSpotify, false, "Instant Local Engine")
                }
            }

            // Twitter / X
            lower.contains("twitter") || lower.contains("x app") || lower == "open x" || lower == "x kholo" -> {
                JarvisResponse("Accessing X global feed, $title.", JarvisCommand.OpenTwitter, false, "Instant Local Engine")
            }

            // Netflix
            lower.contains("netflix") -> {
                JarvisResponse("Opening Netflix media stream, $title.", JarvisCommand.OpenNetflix, false, "Instant Local Engine")
            }

            // WhatsApp
            lower.contains("whatsapp") || lower.contains("watsapp") -> {
                if (lower.contains("send") || lower.contains("message") || lower.contains("msg") || lower.contains("text") || lower.contains("bhejo") || lower.contains("karo")) {
                    val phoneRegex = Regex("""(\+?[0-9]{8,15})""")
                    val phoneMatch = phoneRegex.find(text)
                    val phone = phoneMatch?.value
                    var msg = text.replace(Regex("""(?i)(send|message|msg|text|to|on|via|whatsapp|watsapp|bhejo|pe|ko|karo)"""), "").trim()
                    if (phone != null) msg = msg.replace(phone, "").trim()
                    if (msg.isBlank()) msg = "Hello from $name AI"
                    JarvisResponse("Right away, $title. Preparing WhatsApp transmission.", JarvisCommand.SendWhatsAppMsg(phone, msg), false, "Instant Local Engine")
                } else if (lower.contains("status")) {
                    JarvisResponse("Opening WhatsApp Status matrix, $title.", JarvisCommand.OpenWhatsAppStatus, false, "Instant Local Engine")
                } else {
                    JarvisResponse("Opening WhatsApp application now, $title.", JarvisCommand.OpenWhatsApp, false, "Instant Local Engine")
                }
            }

            // Instagram
            lower.contains("instagram") || lower.contains("insta") -> {
                if (lower.contains("reel") || lower.contains("reels")) {
                    JarvisResponse("Accessing Instagram Reels feed, $title.", JarvisCommand.OpenInstagramReels, false, "Instant Local Engine")
                } else if (lower.contains("dm") || lower.contains("message") || lower.contains("chat") || lower.contains("inbox")) {
                    JarvisResponse("Opening Instagram Direct Messages, $title.", JarvisCommand.OpenInstagramDirect, false, "Instant Local Engine")
                } else if (lower.contains("story") || lower.contains("camera")) {
                    JarvisResponse("Opening Instagram story capture mode, $title.", JarvisCommand.OpenInstagramStoryCamera, false, "Instant Local Engine")
                } else if (lower.contains("profile") || lower.contains("user") || lower.contains("of") || lower.contains("search") || lower.contains("dekho")) {
                    val username = text.replace(Regex("""(?i)(open|view|show|search|profile|user|of|on|instagram|insta|@|kholo|dekho)"""), "").trim()
                    if (username.isNotEmpty()) {
                        JarvisResponse("Locating Instagram profile @$username, $title.", JarvisCommand.OpenInstagramProfile(username), false, "Instant Local Engine")
                    } else {
                        JarvisResponse("Opening Instagram, $title.", JarvisCommand.OpenInstagram, false, "Instant Local Engine")
                    }
                } else {
                    JarvisResponse("Opening Instagram interface, $title.", JarvisCommand.OpenInstagram, false, "Instant Local Engine")
                }
            }

            // YouTube
            lower.contains("youtube") || lower.contains("yt") || lower.startsWith("play ") || lower.contains("gaana") || lower.contains("gana") || lower.contains("song") || lower.contains("bajao") || lower.contains("sunao") -> {
                if (lower.contains("trend") || lower.contains("trending")) {
                    JarvisResponse("Opening YouTube Trending catalog, $title.", JarvisCommand.OpenYouTubeTrending, false, "Instant Local Engine")
                } else if (lower.startsWith("play ") || lower.contains("play") || lower.contains("search") || lower.contains("song") || lower.contains("video") || lower.contains("gaana") || lower.contains("gana") || lower.contains("bajao") || lower.contains("chalao") || lower.contains("sunao")) {
                    val query = text.replace(Regex("""(?i)(play|search|find|for|on|youtube|yt|video|song|track|gaana|gana|bajao|chalao|sunao|kholo)"""), "").trim()
                    val finalQuery = query.ifBlank { "Top trending music" }
                    JarvisResponse("Playing \"$finalQuery\" on YouTube, $title.", JarvisCommand.PlayYouTube(finalQuery), false, "Instant Local Engine")
                } else {
                    JarvisResponse("Opening YouTube mainframe, $title.", JarvisCommand.OpenYouTube, false, "Instant Local Engine")
                }
            }

            // Flashlight / Torch
            lower.contains("flashlight") || lower.contains("torch") || lower.contains("light") -> {
                if (lower.contains("on") || lower.contains("enable") || lower.contains("activate") || lower.contains("start") || lower.contains("jalao") || lower.contains("chalu") || lower.contains("lagao")) {
                    JarvisResponse("Flashlight emitter activated, $title.", JarvisCommand.Flashlight(true), false, "Instant Local Engine")
                } else if (lower.contains("off") || lower.contains("disable") || lower.contains("stop") || lower.contains("deactivate") || lower.contains("band") || lower.contains("bujhao")) {
                    JarvisResponse("Flashlight emitter deactivated, $title.", JarvisCommand.Flashlight(false), false, "Instant Local Engine")
                } else {
                    JarvisResponse("Toggling flashlight state, $title.", JarvisCommand.Flashlight(null), false, "Instant Local Engine")
                }
            }

            // Camera / Video
            lower.contains("camera") || lower.contains("photo") || lower.contains("picture") || lower.contains("selfie") || lower.contains("khicho") -> {
                if (lower.contains("video") || lower.contains("record") || lower.contains("banao")) {
                    JarvisResponse("Opening video recorder, $title.", JarvisCommand.RecordVideo, false, "Instant Local Engine")
                } else {
                    JarvisResponse("Engaging optical sensors, $title.", JarvisCommand.OpenCamera, false, "Instant Local Engine")
                }
            }
            lower.contains("record video") || lower.contains("video record") -> JarvisResponse("Opening video recorder, $title.", JarvisCommand.RecordVideo, false, "Instant Local Engine")

            // Phone Calls & Contacts
            lower.startsWith("call") || lower.contains("dial") || lower.contains("phone call") || lower.contains("call lagao") || lower.contains("phone karo") || lower.contains("call karo") -> {
                val num = text.replace(Regex("[^0-9+*#]"), "")
                if (num.isNotEmpty()) {
                    JarvisResponse("Establishing voice link with $num, $title.", JarvisCommand.CallPhone(num), false, "Instant Local Engine")
                } else {
                    JarvisResponse("Opening telecom dialer, $title.", JarvisCommand.CallPhone(""), false, "Instant Local Engine")
                }
            }
            lower.contains("contacts") || lower.contains("contact list") || lower.contains("address book") -> {
                JarvisResponse("Accessing contact database, $title.", JarvisCommand.OpenContacts, false, "Instant Local Engine")
            }

            // SMS
            lower.startsWith("sms") || lower.contains("send sms") || lower.contains("send text") || lower.contains("sms bhejo") -> {
                val phone = Regex("""(\+?[0-9]{8,15})""").find(text)?.value
                val msg = text.replace(Regex("""(?i)(send|sms|text|to|saying|that|bhejo|karo)"""), "").replace(phone ?: "", "").trim()
                JarvisResponse("Preparing SMS transmission, $title.", JarvisCommand.SendSms(phone, msg.ifBlank { "Hello" }), false, "Instant Local Engine")
            }

            // Alarm & Timer
            lower.contains("alarm") -> {
                val timeMatch = Regex("""(\d{1,2})[:.]?(\d{2})?""").find(text)
                val hour = timeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
                val min = timeMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                val isPm = lower.contains("pm") && hour < 12
                val adjustedHour = if (isPm) hour + 12 else hour
                JarvisResponse("Scheduling alarm for ${String.format("%02d:%02d", adjustedHour, min)}, $title.", JarvisCommand.SetAlarm(adjustedHour, min, "$name Alarm"), false, "Instant Local Engine")
            }
            lower.contains("timer") -> {
                val minMatch = Regex("""(\d+)\s*(min|minute)""").find(lower)
                val secMatch = Regex("""(\d+)\s*(sec|second)""").find(lower)
                val minutes = minMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val seconds = secMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                val totalSec = if (minutes > 0 || seconds > 0) (minutes * 60) + seconds else 300
                JarvisResponse("Timer set for ${totalSec / 60}m ${totalSec % 60}s, $title.", JarvisCommand.SetTimer(totalSec, "$name Timer"), false, "Instant Local Engine")
            }

            // Settings & Wi-Fi & Bluetooth
            lower.contains("wifi") || lower.contains("wi-fi") -> JarvisResponse("Opening Wi-Fi configuration matrix, $title.", JarvisCommand.OpenSettings("wifi"), false, "Instant Local Engine")
            lower.contains("bluetooth") || lower.contains("bt") -> JarvisResponse("Opening Bluetooth diagnostics, $title.", JarvisCommand.OpenSettings("bluetooth"), false, "Instant Local Engine")
            lower.contains("sound") || lower.contains("volume") || lower.contains("audio") || lower.contains("awaaz") -> JarvisResponse("Opening Sound & Volume levels, $title.", JarvisCommand.OpenSettings("sound"), false, "Instant Local Engine")
            lower.contains("display") || lower.contains("brightness") -> JarvisResponse("Opening Display parameters, $title.", JarvisCommand.OpenSettings("display"), false, "Instant Local Engine")
            lower.contains("settings") || lower.contains("setting") -> JarvisResponse("Accessing device control console, $title.", JarvisCommand.OpenSettings("main"), false, "Instant Local Engine")

            // Maps & Navigation
            lower.contains("navigate") || lower.contains("directions") || lower.contains("maps") || lower.contains("map") || lower.contains("route to") || lower.contains("rasta") -> {
                val destination = text.replace(Regex("""(?i)(navigate|directions|maps|map|to|route|take me to|rasta|dikhao|kholo)"""), "").trim()
                JarvisResponse("Plotting course to ${destination.ifBlank { "destination" }}, $title.", JarvisCommand.OpenMaps(destination.ifBlank { "Nearest fuel" }), false, "Instant Local Engine")
            }

            // Google Search & Calculator
            lower.contains("calculate") || lower.contains("calculator") || lower.contains("math") || lower.contains("hisaab") -> {
                JarvisResponse("Engaging computational matrix, $title.", JarvisCommand.OpenCalculator, false, "Instant Local Engine")
            }
            lower.startsWith("search ") || lower.startsWith("google ") || lower.contains("who is ") || lower.contains("what is ") || lower.contains("kya hai") || lower.contains("kaun hai") -> {
                val query = text.replace(Regex("""(?i)(search|google|for|online|kya hai|kaun hai)"""), "").trim()
                JarvisResponse("Querying global data stream for \"$query\", $title.", JarvisCommand.SearchGoogle(query), false, "Instant Local Engine")
            }

            // Battery & Telemetry
            lower.contains("battery") || lower.contains("power level") || lower.contains("charge") || lower.contains("charging") -> {
                JarvisResponse("Diagnostics: Power cells operating at optimal parameters, $title.", JarvisCommand.BatteryStatus, false, "Instant Local Engine")
            }
            lower.contains("system status") || lower.contains("telemetry") || lower.contains("diagnostics") || lower.contains("status") -> {
                JarvisResponse("All $name subsystems operational and ready for your command, $title.", JarvisCommand.DeviceTelemetryReport, false, "Instant Local Engine")
            }

            // Greetings
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") || lower.contains("namaste") || lower.contains("kaisa hai") || lower.contains("kaise ho") -> {
                JarvisResponse("Good day, $title. $name is online, full accessibility controls enabled, and awaiting your command.", JarvisCommand.None, false, "Instant Local Engine")
            }
            lower.contains("who are you") || lower.contains("what can you do") || lower.contains("tum kaun ho") || lower.contains("kya kar sakte ho") -> {
                JarvisResponse("I am $name, your autonomous personal AI assistant. With full Accessibility permissions, I can type into any app on your screen, click buttons, press Home/Back/Recents, take screenshots, lock your device, and execute any phone command with zero delay.", JarvisCommand.None, false, "Instant Local Engine")
            }
            lower.contains("thank you") || lower.contains("thanks") || lower.contains("shukriya") || lower.contains("dhanyawad") -> {
                JarvisResponse("Always at your service, $title.", JarvisCommand.None, false, "Instant Local Engine")
            }

            else -> {
                JarvisResponse(
                    replyText = "Command acknowledged, $title. I am processing \"$text\". How may I assist you further?",
                    command = JarvisCommand.None,
                    isFromGemini = false,
                    providerUsed = "Instant Local Engine"
                )
            }
        }
    }
}
