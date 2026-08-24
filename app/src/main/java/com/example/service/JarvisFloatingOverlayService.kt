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
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AiEngineType
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
    private var wakeWordEngine: com.example.data.JarvisWakeWordEngine? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isExpanded = true
    private var touchSlop = 0

    companion object {
        var isRunning = false
            private set

        fun isFloatingOverlayRunning(): Boolean = isRunning
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        speechManager = JarvisSpeechManager(this)
        wakeWordEngine = com.example.data.JarvisWakeWordEngine.getInstance(this)
        startForegroundNotification()
        setupFloatingView()
        setupBackgroundWakeWord()
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
            .setContentTitle("J.A.R.V.I.S. Floating Assistant Active")
            .setContentText("Tap overlay for English/Hindi conversation & phone automation")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(7771, notification)
    }

    private fun getStoredApiConfig(): MultiApiConfig {
        val prefs = getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)
        return MultiApiConfig(
            selectedEngine = try {
                AiEngineType.valueOf(prefs.getString("selected_ai_engine", AiEngineType.GEMINI_FREE.name) ?: AiEngineType.GEMINI_FREE.name)
            } catch (_: Exception) {
                AiEngineType.GEMINI_FREE
            },
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            geminiModel = prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        )
    }

    private fun getStoredAssistantConfig(): AssistantConfig {
        val prefs = getSharedPreferences("jarvis_ai_prefs", Context.MODE_PRIVATE)
        return AssistantConfig(
            name = prefs.getString("assistant_name", "J.A.R.V.I.S.") ?: "J.A.R.V.I.S.",
            userTitle = prefs.getString("user_title", "sir") ?: "sir",
            preset = try {
                CharacterPreset.valueOf(prefs.getString("character_preset", CharacterPreset.JARVIS.name) ?: CharacterPreset.JARVIS.name)
            } catch (_: Exception) {
                CharacterPreset.JARVIS
            },
            wakeWord = prefs.getString("wake_word", "Jarvis") ?: "Jarvis",
            isWakeWordEnabled = prefs.getBoolean("wake_word_enabled", true),
            isContinuousMode = prefs.getBoolean("continuous_mode_enabled", true)
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 200
        }

        // Root container holds both the Mini Badge and the Expanded HUD Deck
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // ============================================================
        // 1. MINI FLOATING ARC REACTOR BADGE (When Collapsed)
        // ============================================================
        val miniBadgeBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0xF0071322.toInt())
            setStroke(dpToPx(2), 0xFF00F0FF.toInt())
        }

        val miniBadge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = miniBadgeBg
            val pad = dpToPx(12)
            setPadding(pad, pad, pad, pad)
            visibility = View.GONE
        }

        val miniIcon = TextView(this).apply {
            text = "⚡"
            textSize = 22f
            setTextColor(0xFF00F0FF.toInt())
        }
        miniBadge.addView(miniIcon)

        // ============================================================
        // 2. EXPANDED HOLOGRAPHIC HUD DECK (When Expanded)
        // ============================================================
        val hudBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(16).toFloat()
            setColor(0xF2060F1E.toInt())
            setStroke(dpToPx(1), 0xFF00F0FF.toInt())
        }

        val hudDeck = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = hudBg
            val pad = dpToPx(10)
            setPadding(pad, pad, pad, pad)
            visibility = View.VISIBLE
        }

        // Header Row (Draggable title bar + Status)
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(6))
        }

        val reactorIcon = TextView(this).apply {
            text = "⚡"
            textSize = 18f
            setTextColor(0xFF00F0FF.toInt())
            setPadding(0, 0, dpToPx(6), 0)
        }

        val statusText = TextView(this).apply {
            tag = "status_text"
            text = "JARVIS AI // ONLINE"
            textSize = 11f
            setTextColor(0xFF00F0FF.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(0, 0, dpToPx(8), 0)
        }

        val minimizeBtn = TextView(this).apply {
            text = " ➖ "
            textSize = 12f
            setTextColor(0xFF80D8FF.toInt())
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
        }

        val closeBtnTop = TextView(this).apply {
            text = " ✕ "
            textSize = 12f
            setTextColor(0xFFFF5252.toInt())
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2))
        }

        headerRow.addView(reactorIcon)
        headerRow.addView(statusText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        headerRow.addView(minimizeBtn)
        headerRow.addView(closeBtnTop)
        hudDeck.addView(headerRow)

        // Response Dialogue Card (Shows Gemini Text Answer On Screen)
        val responseCardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(10).toFloat()
            setColor(0xFF0A182E.toInt())
            setStroke(dpToPx(1), 0x6600F0FF.toInt())
        }

        val responseScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(280), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(8)
            }
            background = responseCardBg
            val p = dpToPx(8)
            setPadding(p, p, p, p)
        }

        val responseText = TextView(this).apply {
            tag = "response_text"
            text = "Ask me anything in English or Hindi, or tap Voice to speak!"
            textSize = 11f
            setTextColor(0xFFE0F7FA.toInt())
            setLineSpacing(2f, 1.15f)
        }
        responseScrollView.addView(responseText)
        hudDeck.addView(responseScrollView)

        // Button Helper
        fun createPillButton(label: String, bgColor: Int = 0xFF102845.toInt(), strokeColor: Int = 0xFF00F0FF.toInt(), onClick: () -> Unit): Button {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(bgColor)
                setStroke(dpToPx(1), strokeColor)
            }
            return Button(this).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                background = bg
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

        // AI Processor for Speech & Text Commands
        fun executeUserQuery(query: String) {
            statusText.text = "🧠 AI Thinking..."
            responseText.text = "Q: \"$query\"\n\nThinking..."
            
            serviceScope.launch {
                val apiConfig = getStoredApiConfig()
                val assistantConfig = getStoredAssistantConfig()

                // Fast local check first
                val localRes = GeminiJarvisService.parseLocalCommand(query, assistantConfig)
                if (localRes.command != JarvisCommand.None) {
                    statusText.text = "⚡ Executing..."
                    PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, localRes.command)
                    statusText.text = "🔊 Speaking..."
                    responseText.text = "Q: \"$query\"\n\n${localRes.replyText}"
                    speechManager?.speak(localRes.replyText) {
                        mainHandler.post { statusText.text = "JARVIS AI // ONLINE" }
                    }
                    return@launch
                }

                // Call Gemini API (Free tier model)
                val response = withContext(Dispatchers.IO) {
                    GeminiJarvisService.processUserMessage(
                        userMessage = query,
                        apiConfig = apiConfig,
                        assistantConfig = assistantConfig,
                        conversationHistory = emptyList()
                    )
                }

                statusText.text = "⚡ Executing..."
                if (response.command != JarvisCommand.None) {
                    PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, response.command)
                }

                statusText.text = "🔊 Speaking..."
                responseText.text = "Q: \"$query\"\n\n${response.replyText}"
                speechManager?.speak(response.replyText) {
                    mainHandler.post { statusText.text = "JARVIS AI // ONLINE" }
                }
            }
        }

        // Action Buttons Row 1 (Voice Mic, Type, Scroll Down, Scroll Up)
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(4))
        }

        val micBtn = createPillButton("🎙️ Voice", 0xFF006994.toInt(), 0xFF00E5FF.toInt()) {
            speechManager?.stopSpeaking()
            statusText.text = "🎙️ Listening..."
            responseText.text = "Listening... Speak in English or Hindi now."
            speechManager?.startListening(
                onResult = { query ->
                    executeUserQuery(query)
                },
                onError = { err ->
                    mainHandler.post {
                        statusText.text = "JARVIS AI // ONLINE"
                        responseText.text = "Voice error: $err. Tap 🎙️ to try again."
                    }
                }
            )
        }

        val typeBtn = createPillButton("✍️ Type") {
            val phrases = listOf(
                "Hello! How can I assist you today?",
                "Main raste mein hoon, thodi der mein baat karte hain.",
                "Thank you, received.",
                "Yes, confirmed."
            )
            val res = PhoneController.typeTextOnScreen(this, phrases.random())
            Toast.makeText(this, res.message, Toast.LENGTH_SHORT).show()
        }

        val scrollDownBtn = createPillButton("⬇️ Scroll") {
            PhoneController.performScroll(this, "down")
        }

        val scrollUpBtn = createPillButton("⬆️ Scroll") {
            PhoneController.performScroll(this, "up")
        }

        row1.addView(micBtn)
        row1.addView(typeBtn)
        row1.addView(scrollDownBtn)
        row1.addView(scrollUpBtn)
        hudDeck.addView(row1)

        // Action Buttons Row 2 (Home, Back, Screenshot, Torch, App)
        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val homeBtn = createPillButton("🏠 Home") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.HOME, "Home")
        }

        val backBtn = createPillButton("◀ Back") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.BACK, "Back")
        }

        val screenshotBtn = createPillButton("📸 Shot") {
            PhoneController.performGlobalAction(this, JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT, "Screenshot")
        }

        val openAppBtn = createPillButton("📱 App", 0xFF0D47A1.toInt()) {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
            }
        }

        row2.addView(homeBtn)
        row2.addView(backBtn)
        row2.addView(screenshotBtn)
        row2.addView(openAppBtn)
        hudDeck.addView(row2)

        rootLayout.addView(miniBadge)
        rootLayout.addView(hudDeck)

        // ============================================================
        // 3. SEAMLESS DRAGGING & MINIMIZE/EXPAND LOGIC
        // ============================================================
        fun toggleExpand(expand: Boolean) {
            isExpanded = expand
            miniBadge.visibility = if (isExpanded) View.GONE else View.VISIBLE
            hudDeck.visibility = if (isExpanded) View.VISIBLE else View.GONE
            try {
                windowManager?.updateViewLayout(rootLayout, params)
            } catch (_: Exception) {}
        }

        minimizeBtn.setOnClickListener {
            toggleExpand(false)
        }

        closeBtnTop.setOnClickListener {
            stopSelf()
        }

        // Dragging handler on Header & Mini Badge
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        var downTime = 0L

        val dragTouchListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    downTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(rootLayout, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val duration = System.currentTimeMillis() - downTime
                    if (!isDragging && duration < 350) {
                        // Click detected!
                        if (!isExpanded) {
                            toggleExpand(true)
                        }
                    }
                    true
                }
                else -> false
            }
        }

        headerRow.setOnTouchListener(dragTouchListener)
        miniBadge.setOnTouchListener(dragTouchListener)

        floatingView = rootLayout
        windowManager?.addView(rootLayout, params)
    }

    private fun setupBackgroundWakeWord() {
        val assistantConfig = getStoredAssistantConfig()
        wakeWordEngine?.setSelectedKeyword(assistantConfig.wakeWord)
        wakeWordEngine?.setWakeWordEnabled(assistantConfig.isWakeWordEnabled)
        wakeWordEngine?.setWakeListener { keyword ->
            mainHandler.post {
                speechManager?.stopSpeaking()
                val statusText = floatingView?.findViewWithTag<TextView>("status_text")
                val responseText = floatingView?.findViewWithTag<TextView>("response_text")
                statusText?.text = "⚡ Wake ($keyword)..."
                responseText?.text = "Wake word detected ($keyword). Listening..."

                speechManager?.startListening(
                    onResult = { query ->
                        serviceScope.launch {
                            statusText?.text = "🧠 AI Thinking..."
                            responseText?.text = "Q: \"$query\"\n\nProcessing..."
                            val apiConfig = getStoredApiConfig()
                            val config = getStoredAssistantConfig()

                            val localRes = GeminiJarvisService.parseLocalCommand(query, config)
                            if (localRes.command != JarvisCommand.None) {
                                PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, localRes.command)
                                responseText?.text = "Q: \"$query\"\n\n${localRes.replyText}"
                                speechManager?.speak(localRes.replyText)
                                statusText?.text = "JARVIS AI // ONLINE"
                                return@launch
                            }

                            val response = withContext(Dispatchers.IO) {
                                GeminiJarvisService.processUserMessage(
                                    userMessage = query,
                                    apiConfig = apiConfig,
                                    assistantConfig = config,
                                    conversationHistory = emptyList()
                                )
                            }
                            if (response.command != JarvisCommand.None) {
                                PhoneController.executeJarvisCommand(this@JarvisFloatingOverlayService, response.command)
                            }
                            responseText?.text = "Q: \"$query\"\n\n${response.replyText}"
                            speechManager?.speak(response.replyText)
                            statusText?.text = "JARVIS AI // ONLINE"
                        }
                    },
                    onError = {
                        statusText?.text = "JARVIS AI // ONLINE"
                    }
                )
            }
        }
        wakeWordEngine?.startListening()
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
        wakeWordEngine?.stopListening()
        serviceScope.cancel()
        if (floatingView != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (_: Exception) {}
            floatingView = null
        }
        speechManager?.destroy()
    }
}
