package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.GeminiJarvisService
import com.example.data.JarvisCommand
import com.example.data.JarvisSpeechManager
import com.example.data.PhoneController

class JarvisFloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var speechManager: JarvisSpeechManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var isExpanded = false

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
                "J.A.R.V.I.S. Floating HUD",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Autonomous Floating Screen Controller"
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
            .setContentTitle("J.A.R.V.I.S. Floating Arc Reactor")
            .setContentText("Screen auto-typing & phone automation active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(7771, notification)
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
            x = 40
            y = 300
        }

        // Programmatic lightweight Sci-Fi Floating HUD container
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xEE0B1426.toInt())
            setPadding(12, 12, 12, 12)
        }

        // 1. Arc Reactor Head / Drag handle
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val reactorIcon = TextView(this).apply {
            text = "⚡"
            textSize = 24f
            setPadding(8, 8, 8, 8)
            setTextColor(0xFF00F0FF.toInt())
        }

        val statusText = TextView(this).apply {
            text = "J.A.R.V.I.S."
            textSize = 12f
            setTextColor(0xFF00F0FF.toInt())
            setPadding(4, 0, 8, 0)
        }

        headerRow.addView(reactorIcon)
        headerRow.addView(statusText)
        container.addView(headerRow)

        // 2. Expandable Action Strip
        val actionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.VISIBLE
            setPadding(0, 8, 0, 0)
        }

        fun createPillButton(text: String, onClick: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                textSize = 10f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(0xFF132F4C.toInt())
                setPadding(10, 4, 10, 4)
                setOnClickListener { onClick() }
            }
        }

        val micBtn = createPillButton("🎤 Speak") {
            statusText.text = "Listening..."
            speechManager?.startListening(
                onResult = { recognized ->
                    statusText.text = "⚡ Executing..."
                    val res = GeminiJarvisService.parseLocalCommand(recognized)
                    PhoneController.typeTextOnScreen(this, recognized)
                    speechManager?.speak(res.replyText) {
                        mainHandler.post { statusText.text = "J.A.R.V.I.S." }
                    }
                },
                onError = {
                    mainHandler.post { statusText.text = "J.A.R.V.I.S." }
                }
            )
        }

        val typeBtn = createPillButton("✍️ Type") {
            val phrases = listOf("Main raste mein hoon.", "Yes, proceed.", "Happy Birthday!", "Thanks, received.")
            val randomPhrase = phrases.random()
            val res = PhoneController.typeTextOnScreen(this, randomPhrase)
            Toast.makeText(this, res.message, Toast.LENGTH_SHORT).show()
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

        val closeBtn = createPillButton("✕") {
            stopSelf()
        }

        actionStrip.addView(micBtn)
        actionStrip.addView(typeBtn)
        actionStrip.addView(homeBtn)
        actionStrip.addView(backBtn)
        actionStrip.addView(screenBtn)
        actionStrip.addView(closeBtn)
        container.addView(actionStrip)

        // Drag listener on Header Row
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
                        // Toggle action strip
                        actionStrip.visibility = if (actionStrip.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        windowManager?.addView(container, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
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
