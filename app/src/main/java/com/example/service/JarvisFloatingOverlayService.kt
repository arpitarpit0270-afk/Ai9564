package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AiEngineType
import com.example.data.AiNatureType
import com.example.data.AssistantConfig
import com.example.data.CharacterPreset
import com.example.data.GeminiJarvisService
import com.example.data.JarvisCommand
import com.example.data.JarvisSpeechManager
import com.example.data.MultiApiConfig
import com.example.data.PhoneController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisFloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var speechManager: JarvisSpeechManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isExpanded = true

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        speechManager = JarvisSpeechManager(this)
        startForegroundNotification()
        setupFloatingView()
    }

    private fun startForegroundNotification() {
        val channelId = "jarvis_floating_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "J.A.R.V.I.S. Floating Arc Reactor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Autonomous Floating Screen Controller & Background Assistant"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val appIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("J.A.R.V.I.S. Background Engine Active")
            .setContentText("Autonomous floating HUD & instant voice responder online")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(7771, notification)
    }

    private fun getStoredApiConfig(): MultiApiConfig {
        val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        return MultiApiConfig(
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
    }

    private fun getStoredAssistantConfig(): AssistantConfig {
        val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        return AssistantConfig(
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
    }

    private fun setupFloatingView() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        }

        // Main HUD container with dark sci-fi border background
        val containerBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16).toFloat()
            setColor(0xF00A111E.toInt())
            setStroke(dpToPx(1), 0xFF00F0FF.toInt())
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = containerBg
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        // 1. Header Drag & Status Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(4))
        }

        val reactorIcon = TextView(this).apply {
            text = "⚡"
            textSize = 20f
            setTextColor(0xFF00F0FF.toInt())
            setPadding(dpToPx(2), dpToPx(2), dpToPx(6), dpToPx(2))
        }

        val statusText = TextView(this).apply {
            text = "JARVIS // ONLINE"
            textSize = 11f
            setTextColor(0xFF00F0FF.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, dpToPx(8), 0)
        }

        val minMaxBtn = TextView(this).apply {
            text = "◀▶"
            textSize = 10f
            setTextColor(0xFF00A3FF.toInt())
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
        }

        headerRow.addView(reactorIcon)
        headerRow.addView(statusText)
        headerRow.addView(minMaxBtn)
        container.addView(headerRow)

        // 2. Expandable Action Strip
        val actionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
            setPadding(0, dpToPx(4), 0, 0)
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, 0)
        }

        fun createPillButton(text: String, bgColor: Int = 0xFF102A45.toInt(), onClick: () -> Unit): Button {
            val btnBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(bgColor)
                setStroke(dpToPx(1), 0xFF00F0FF.toInt())
            }
            return Button(this).apply {
                this.text = text
                textSize = 10f
                setTextColor(Color.WHITE)
                background = btnBg
                val padH = dpToPx(8)
                val padV = dpToPx(4)
                setPadding(padH, padV, padH, padV)
                minHeight = 0
                minimumHeight = 0
                minWidth = 0
                minimumWidth = 0
                setOnClickListener { onClick() }
            }
        }

        // Voice Command execution with ultra-low latency
        fun processFloatingSpeechInput(query: String) {
            statusText.text = "🤖 Thinking..."
            serviceScope.launch {
                val apiConfig = getStoredApiConfig()
                val assistantConfig = getStoredAssistantConfig()

                // Fast local check first (<20ms response)
                val localRes = GeminiJarvisService.parseLocalCommand(query, assistantConfig)
                if (localRes.command != JarvisCommand.None) {
                    statusText.text = "⚡ Executing..."
                    PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, localRes.command)
                    statusText.text = "🔊 Responding..."
                    speechManager?.speak(localRes.replyText) {
                        mainHandler.post { statusText.text = "JARVIS // IDLE" }
                    }
                    return@launch
                }

                // Full AI Brain query with user's configured model
                val response = withContext(Dispatchers.IO) {
                    GeminiJarvisService.processUserMessage(
                        userMessage = query,
                        apiConfig = apiConfig,
                        assistantConfig = assistantConfig,
                        conversationHistory = emptyList()
                    )
                }

                statusText.text = "⚡ Executing..."
                // Execute any phone action or screen automation
                if (response.command != JarvisCommand.None) {
                    PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, response.command)
                }

                // Speak voice answer without delay
                statusText.text = "🔊 Responding..."
                speechManager?.speak(response.replyText) {
                    mainHandler.post { statusText.text = "JARVIS // ONLINE" }
                }
            }
        }

        val micBtn = createPillButton("🎙️ Voice", 0xFF00527C.toInt()) {
            speechManager?.stopSpeaking()
            statusText.text = "🎙️ Listening..."
            speechManager?.startListening(
                onResult = { recognized ->
                    processFloatingSpeechInput(recognized)
                },
                onError = {
                    mainHandler.post {
                        statusText.text = "JARVIS // ONLINE"
                    }
                }
            )
        }

        val typeBtn = createPillButton("✍️ Type") {
            val phrases = listOf(
                "Main raste mein hoon.",
                "Yes, confirmed.",
                "Thanks, received.",
                "Call me later.",
                "Send details please."
            )
            val selected = phrases.random()
            val res = PhoneController.typeTextOnScreen(this, selected)
            Toast.makeText(this, res.message, Toast.LENGTH_SHORT).show()
        }

        val scrollDownBtn = createPillButton("⬇️ Scroll") {
            PhoneController.performScroll(this, "down")
        }

        val scrollUpBtn = createPillButton("⬆️ Scroll") {
            PhoneController.performScroll(this, "up")
        }

        val homeBtn = createPillButton("🏠") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.HOME, "Home")
        }

        val backBtn = createPillButton("◀") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.BACK, "Back")
        }

        val screenBtn = createPillButton("📸") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT, "Screenshot")
        }

        val openAppBtn = createPillButton("📱 App") {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        }

        val closeBtn = createPillButton("✕", 0xFF661010.toInt()) {
            stopSelf()
        }

        row1.addView(micBtn)
        row1.addView(typeBtn)
        row1.addView(scrollDownBtn)
        row1.addView(scrollUpBtn)
        row1.addView(homeBtn)

        row2.addView(backBtn)
        row2.addView(screenBtn)
        row2.addView(openAppBtn)
        row2.addView(closeBtn)

        actionStrip.addView(row1)
        actionStrip.addView(row2)
        container.addView(actionStrip)

        // Drag & minimize gestures
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        headerRow.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isMoving = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        isExpanded = !isExpanded
                        actionStrip.visibility = if (isExpanded) View.VISIBLE else View.GONE
                        minMaxBtn.text = if (isExpanded) "◀▶" else "▲"
                    }
                    true
                }
                else -> false
            }
        }

        minMaxBtn.setOnClickListener {
            isExpanded = !isExpanded
            actionStrip.visibility = if (isExpanded) View.VISIBLE else View.GONE
            minMaxBtn.text = if (isExpanded) "◀▶" else "▲"
        }

        floatingView = container
        windowManager?.addView(container, params)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        if (floatingView != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
        speechManager?.destroy()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false

        fun isFloatingOverlayRunning(): Boolean = isRunning
    }
}
