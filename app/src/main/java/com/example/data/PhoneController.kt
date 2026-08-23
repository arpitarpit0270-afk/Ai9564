package com.example.data

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisFloatingOverlayService
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PhoneController {

    private var isTorchOn: Boolean = false

    // ==========================================
    // FLOATING OVERLAY ARC REACTOR WIDGET
    // ==========================================

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlayPermissionSettings(context: Context): ActionResult {
        return try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening overlay permission settings, sir.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open overlay settings: ${e.localizedMessage}")
        }
    }

    fun toggleFloatingOverlay(context: Context): ActionResult {
        if (!canDrawOverlays(context)) {
            openOverlayPermissionSettings(context)
            return ActionResult(
                success = false,
                message = "Overlay permission required. Please allow 'Display over other apps' in settings."
            )
        }

        return if (JarvisFloatingOverlayService.isFloatingOverlayRunning()) {
            val stopIntent = Intent(context, JarvisFloatingOverlayService::class.java)
            context.stopService(stopIntent)
            ActionResult(success = true, message = "Floating Arc Reactor deactivated, sir.")
        } else {
            val startIntent = Intent(context, JarvisFloatingOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
            ActionResult(success = true, message = "Floating Arc Reactor active on screen, sir.")
        }
    }

    fun isFloatingOverlayRunning(): Boolean = JarvisFloatingOverlayService.isFloatingOverlayRunning()

    // ==========================================
    // ACCESSIBILITY & UNIVERSAL ON-SCREEN CONTROLS
    // ==========================================

    fun isAccessibilityEnabled(context: Context): Boolean {
        return JarvisAccessibilityService.isAccessibilitySettingsEnabled(context)
    }

    fun openAccessibilitySettings(context: Context): ActionResult {
        return try {
            JarvisAccessibilityService.openAccessibilitySettings(context)
            ActionResult(success = true, message = "Opening Accessibility configuration matrix, sir.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open Accessibility settings: ${e.localizedMessage}")
        }
    }

    fun typeTextOnScreen(context: Context, text: String): ActionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val typed = service.typeTextIntoFocusedField(text)
            if (typed) {
                ActionResult(success = true, message = "Typed text successfully: \"$text\"")
            } else {
                ActionResult(success = false, message = "No active input field focused on screen to write \"$text\".")
            }
        } else {
            // Service not active - prompt user to enable accessibility
            openAccessibilitySettings(context)
            ActionResult(
                success = false,
                message = "Accessibility permission required for universal screen typing. Please enable J.A.R.V.I.S. in Accessibility settings, sir."
            )
        }
    }

    fun clickOnScreenByText(context: Context, targetText: String): ActionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val clicked = service.clickNodeByText(targetText)
            if (clicked) {
                ActionResult(success = true, message = "Clicked \"$targetText\" on screen.")
            } else {
                ActionResult(success = false, message = "Could not locate interactive element with label \"$targetText\".")
            }
        } else {
            openAccessibilitySettings(context)
            ActionResult(
                success = false,
                message = "Accessibility permission required for UI clicks. Please grant permission in settings."
            )
        }
    }

    fun clickAtCoordinates(context: Context, x: Float, y: Float): ActionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val clicked = service.clickAtCoordinates(x, y)
            if (clicked) {
                ActionResult(success = true, message = "Tapped at coordinates ($x, $y) on screen.")
            } else {
                ActionResult(success = false, message = "Could not execute tap at ($x, $y).")
            }
        } else {
            openAccessibilitySettings(context)
            ActionResult(
                success = false,
                message = "Accessibility permission required for coordinate clicking. Please grant permission in settings."
            )
        }
    }

    fun performScroll(context: Context, direction: String): ActionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val displayMetrics = context.resources.displayMetrics
            val width = displayMetrics.widthPixels.toFloat()
            val height = displayMetrics.heightPixels.toFloat()
            val midX = width / 2
            val midY = height / 2

            val success = when (direction.lowercase()) {
                "down", "scroll down" -> service.performSwipe(midX, midY + 400f, midX, midY - 400f)
                "up", "scroll up" -> service.performSwipe(midX, midY - 400f, midX, midY + 400f)
                "left", "scroll left" -> service.performSwipe(midX + 300f, midY, midX - 300f, midY)
                "right", "scroll right" -> service.performSwipe(midX - 300f, midY, midX + 300f, midY)
                else -> service.performSwipe(midX, midY + 400f, midX, midY - 400f)
            }
            if (success) {
                ActionResult(success = true, message = "Scrolled $direction on screen, sir.")
            } else {
                ActionResult(success = false, message = "Could not perform scroll gesture.")
            }
        } else {
            openAccessibilitySettings(context)
            ActionResult(
                success = false,
                message = "Accessibility permission required for scrolling."
            )
        }
    }

    fun performGlobalAction(context: Context, action: JarvisAccessibilityService.GlobalActionType, actionLabel: String): ActionResult {
        val service = JarvisAccessibilityService.instance
        return if (service != null) {
            val executed = service.triggerGlobalAction(action)
            if (executed) {
                ActionResult(success = true, message = "$actionLabel executed successfully, sir.")
            } else {
                ActionResult(success = false, message = "Could not execute $actionLabel on this device version.")
            }
        } else {
            openAccessibilitySettings(context)
            ActionResult(
                success = false,
                message = "Accessibility permission needed for $actionLabel. Please enable J.A.R.V.I.S. in Accessibility Settings."
            )
        }
    }

    // ==========================================
    // WHATSAPP & COMMUNICATIONS
    // ==========================================

    fun openWhatsApp(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening WhatsApp, sir.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open WhatsApp: ${e.localizedMessage}")
        }
    }

    fun sendWhatsAppMessage(context: Context, phoneNumber: String?, message: String): ActionResult {
        return try {
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
            val encodedMsg = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            val uri = if (cleanPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            }
            val dest = if (cleanPhone.isNotEmpty()) "to $cleanPhone" else ""
            ActionResult(success = true, message = "Composing WhatsApp message $dest with content: \"$message\"")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to send WhatsApp message: ${e.localizedMessage}")
        }
    }

    fun openWhatsAppStatus(context: Context): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening WhatsApp Status.")
        } catch (e: Exception) {
            openWhatsApp(context)
        }
    }

    fun openInstagram(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Accessing Instagram mainframe.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch Instagram: ${e.localizedMessage}")
        }
    }

    fun openInstagramProfile(context: Context, username: String): ActionResult {
        val cleanUser = username.trim().removePrefix("@")
        return try {
            val appUri = Uri.parse("http://instagram.com/_u/$cleanUser")
            val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$cleanUser")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Navigating to Instagram profile @$cleanUser.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Unable to open profile @$cleanUser: ${e.localizedMessage}")
        }
    }

    fun openInstagramDirect(context: Context): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/direct/inbox/")).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/direct/inbox/")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Opening Instagram Direct Messages.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to open Instagram DMs: ${e.localizedMessage}")
        }
    }

    fun openInstagramReels(context: Context): ActionResult {
        return try {
            val uri = Uri.parse("https://www.instagram.com/reels/")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.instagram.android")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Launching Instagram Reels feed.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to open Reels: ${e.localizedMessage}")
        }
    }

    fun openInstagramStoryCamera(context: Context): ActionResult {
        return try {
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult(success = true, message = "Opening Instagram Story Camera.")
            } else {
                openInstagram(context)
            }
        } catch (e: Exception) {
            openInstagram(context)
        }
    }

    fun openYouTube(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Initiating YouTube uplink.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch YouTube: ${e.localizedMessage}")
        }
    }

    fun searchYouTube(context: Context, query: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val appUri = Uri.parse("vnd.youtube://results?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, appUri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webUri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Searching YouTube for \"$query\", sir.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "YouTube search error: ${e.localizedMessage}")
        }
    }

    fun playYouTubeVideo(context: Context, queryOrId: String): ActionResult {
        return try {
            if (queryOrId.length == 11 && !queryOrId.contains(" ")) {
                val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$queryOrId")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (appIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(appIntent)
                    return ActionResult(success = true, message = "Playing YouTube video ID: $queryOrId")
                }
            }
            searchYouTube(context, queryOrId)
        } catch (e: Exception) {
            searchYouTube(context, queryOrId)
        }
    }

    fun openYouTubeTrending(context: Context): ActionResult {
        return try {
            val uri = Uri.parse("https://www.youtube.com/feed/trending")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.youtube")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Displaying YouTube Trending stream.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to open trending: ${e.localizedMessage}")
        }
    }

    fun toggleFlashlight(context: Context, state: Boolean? = null): ActionResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager == null) {
                return ActionResult(success = false, message = "Camera hardware unavailable.")
            }
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: "0"

            val targetState = state ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            val statusStr = if (isTorchOn) "ACTIVATED" else "DEACTIVATED"
            ActionResult(success = true, message = "Flashlight photon emitter $statusStr, sir.")
        } catch (e: CameraAccessException) {
            ActionResult(success = false, message = "Flashlight access error: ${e.localizedMessage}")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Torch operation failed: ${e.localizedMessage}")
        }
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun makePhoneCall(context: Context, phoneNumber: String): ActionResult {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9+*#]"), "")
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening communications dialer for $cleanPhone.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Dialer error: ${e.localizedMessage}")
        }
    }

    fun sendSms(context: Context, phoneNumber: String?, message: String): ActionResult {
        return try {
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: ""
            val uri = Uri.parse("smsto:$cleanPhone")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening SMS transmitter for $cleanPhone.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "SMS failure: ${e.localizedMessage}")
        }
    }

    fun openContacts(context: Context): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Displaying contact directory.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not open contacts: ${e.localizedMessage}")
        }
    }

    fun openCamera(context: Context, video: Boolean = false): ActionResult {
        return try {
            val action = if (video) MediaStore.ACTION_VIDEO_CAPTURE else MediaStore.ACTION_IMAGE_CAPTURE
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mode = if (video) "Video Recorder" else "Optical Camera"
            ActionResult(success = true, message = "Launching $mode interface.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Camera launch failure: ${e.localizedMessage}")
        }
    }

    fun setAlarm(context: Context, hour: Int, minute: Int, message: String = "Jarvis Alarm"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            ActionResult(success = true, message = "Alarm scheduled for $timeStr with tag \"$message\".")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to set alarm: ${e.localizedMessage}")
        }
    }

    fun setTimer(context: Context, seconds: Int, message: String = "Jarvis Timer"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val secs = seconds % 60
            val durStr = if (mins > 0) "$mins min $secs sec" else "$secs seconds"
            ActionResult(success = true, message = "Chronometer timer initiated for $durStr.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Timer error: ${e.localizedMessage}")
        }
    }

    fun openSettings(context: Context, type: String = "main"): ActionResult {
        return try {
            val action = when (type.lowercase()) {
                "wifi", "wi-fi", "internet" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
                "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
                "battery", "power" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                "apps", "applications" -> Settings.ACTION_APPLICATION_SETTINGS
                "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                "storage" -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                "accessibility", "a11y" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening system configuration: $type.")
        } catch (e: Exception) {
            try {
                val genericIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
                ActionResult(success = true, message = "Opening main System Settings.")
            } catch (ex: Exception) {
                ActionResult(success = false, message = "Settings inaccessible: ${ex.localizedMessage}")
            }
        }
    }

    fun openMaps(context: Context, destination: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())
            val navUri = Uri.parse("google.navigation:q=$encoded")
            val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
                val fallback = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
            ActionResult(success = true, message = "Plotting course to $destination via tactical GPS.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Navigation error: ${e.localizedMessage}")
        }
    }

    fun searchGoogle(context: Context, query: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val uri = Uri.parse("https://www.google.com/search?q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Querying global neural network for \"$query\".")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Search failed: ${e.localizedMessage}")
        }
    }

    fun openEmail(context: Context, to: String? = null, subject: String? = null, body: String? = null): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${to ?: ""}")
                if (!subject.isNullOrEmpty()) putExtra(Intent.EXTRA_SUBJECT, subject)
                if (!body.isNullOrEmpty()) putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Preparing digital mail transmission.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Mail client error: ${e.localizedMessage}")
        }
    }

    fun openCalculator(context: Context): ActionResult {
        return try {
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_APP_CALCULATOR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening computational calculator matrix.")
        } catch (e: Exception) {
            searchGoogle(context, "calculator")
        }
    }

    fun openClock(context: Context): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Accessing chronological clock system.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Clock launch error: ${e.localizedMessage}")
        }
    }

    fun openTelegram(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                ?: context.packageManager.getLaunchIntentForPackage("org.thunderdog.challegram")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Accessing Telegram secure comms link.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch Telegram: ${e.localizedMessage}")
        }
    }

    fun sendTelegramMessage(context: Context, username: String?): ActionResult {
        return try {
            val cleanUser = username?.trim()?.removePrefix("@") ?: ""
            val uri = if (cleanUser.isNotEmpty()) Uri.parse("https://t.me/$cleanUser") else Uri.parse("https://t.me/")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val target = if (cleanUser.isNotEmpty()) "@$cleanUser" else "contacts"
            ActionResult(success = true, message = "Opening Telegram chat with $target.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Telegram communication error: ${e.localizedMessage}")
        }
    }

    fun openSpotify(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Initializing Spotify sonic array.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch Spotify: ${e.localizedMessage}")
        }
    }

    fun searchSpotify(context: Context, query: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            ActionResult(success = true, message = "Searching Spotify for \"$query\", sir.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Spotify search failure: ${e.localizedMessage}")
        }
    }

    fun openTwitter(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.twitter.android")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening X (Twitter) global feed.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch X: ${e.localizedMessage}")
        }
    }

    fun openNetflix(context: Context): ActionResult {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("com.netflix.mediaclient")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.netflix.com/"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening Netflix entertainment array.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Could not launch Netflix: ${e.localizedMessage}")
        }
    }

    fun openBrowserUrl(context: Context, url: String): ActionResult {
        return try {
            val validUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Navigating to $validUrl.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Failed to open URL: ${e.localizedMessage}")
        }
    }

    fun openCalendar(context: Context): ActionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(success = true, message = "Opening calendar agenda.")
        } catch (e: Exception) {
            ActionResult(success = false, message = "Calendar inaccessible: ${e.localizedMessage}")
        }
    }

    fun getBatteryTelemetry(context: Context): BatteryInfo {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()).toInt() else 85
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val temperature = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0f
        return BatteryInfo(percentage = batteryPct, isCharging = isCharging, temperatureCelsius = temperature)
    }

    fun getDeviceTelemetry(context: Context): DeviceTelemetry {
        val battery = getBatteryTelemetry(context)
        val runtime = Runtime.getRuntime()
        val usedMemMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemMB = runtime.maxMemory() / (1024 * 1024)

        var storageAvailGB = 0.0
        var storageTotalGB = 0.0
        try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong
            storageAvailGB = (availableBlocks * blockSize) / (1024.0 * 1024 * 1024)
            storageTotalGB = (totalBlocks * blockSize) / (1024.0 * 1024 * 1024)
        } catch (_: Exception) {}

        return DeviceTelemetry(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            battery = battery,
            memoryUsedMB = usedMemMB,
            memoryMaxMB = maxMemMB,
            storageAvailableGB = String.format("%.1f", storageAvailGB),
            storageTotalGB = String.format("%.1f", storageTotalGB)
        )
    }

    fun executeJarvisCommand(context: Context, command: JarvisCommand): ActionResult {
        return when (command) {
            // Accessibility Actions
            is JarvisCommand.AccessibilityTypeText -> typeTextOnScreen(context, command.text)
            is JarvisCommand.AccessibilityClick -> clickOnScreenByText(context, command.targetText)
            is JarvisCommand.AccessibilityClickCoords -> clickAtCoordinates(context, command.x, command.y)
            is JarvisCommand.AccessibilityScroll -> performScroll(context, command.direction)
            is JarvisCommand.GlobalHome -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.HOME, "Home")
            is JarvisCommand.GlobalBack -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.BACK, "Back")
            is JarvisCommand.GlobalRecents -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.RECENTS, "Recent Apps")
            is JarvisCommand.GlobalNotifications -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.NOTIFICATIONS, "Notifications")
            is JarvisCommand.GlobalQuickSettings -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.QUICK_SETTINGS, "Quick Settings")
            is JarvisCommand.GlobalLockScreen -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.LOCK_SCREEN, "Lock Screen")
            is JarvisCommand.GlobalScreenshot -> performGlobalAction(context, JarvisAccessibilityService.GlobalActionType.TAKE_SCREENSHOT, "Screenshot")
            is JarvisCommand.OpenAccessibilitySettings -> openAccessibilitySettings(context)

            // App & Comms
            is JarvisCommand.OpenWhatsApp -> openWhatsApp(context)
            is JarvisCommand.SendWhatsAppMsg -> sendWhatsAppMessage(context, command.phone, command.message)
            is JarvisCommand.OpenWhatsAppStatus -> openWhatsAppStatus(context)
            is JarvisCommand.OpenTelegram -> openTelegram(context)
            is JarvisCommand.SendTelegramMsg -> sendTelegramMessage(context, command.username)
            is JarvisCommand.OpenSpotify -> openSpotify(context)
            is JarvisCommand.PlaySpotify -> searchSpotify(context, command.query)
            is JarvisCommand.OpenTwitter -> openTwitter(context)
            is JarvisCommand.OpenInstagram -> openInstagram(context)
            is JarvisCommand.OpenInstagramReels -> openInstagramReels(context)
            is JarvisCommand.OpenInstagramDirect -> openInstagramDirect(context)
            is JarvisCommand.OpenInstagramProfile -> openInstagramProfile(context, command.username)
            is JarvisCommand.OpenInstagramStoryCamera -> openInstagramStoryCamera(context)
            is JarvisCommand.OpenNetflix -> openNetflix(context)
            is JarvisCommand.OpenWebUrl -> openBrowserUrl(context, command.url)
            is JarvisCommand.OpenYouTube -> openYouTube(context)
            is JarvisCommand.SearchYouTube -> searchYouTube(context, command.query)
            is JarvisCommand.PlayYouTube -> playYouTubeVideo(context, command.query)
            is JarvisCommand.OpenYouTubeTrending -> openYouTubeTrending(context)

            // Hardware & Utility
            is JarvisCommand.Flashlight -> toggleFlashlight(context, command.state)
            is JarvisCommand.OpenCamera -> openCamera(context, false)
            is JarvisCommand.RecordVideo -> openCamera(context, true)
            is JarvisCommand.CallPhone -> makePhoneCall(context, command.number)
            is JarvisCommand.SendSms -> sendSms(context, command.number, command.message)
            is JarvisCommand.OpenContacts -> openContacts(context)
            is JarvisCommand.SetAlarm -> setAlarm(context, command.hour, command.minute, command.label)
            is JarvisCommand.SetTimer -> setTimer(context, command.seconds, command.label)
            is JarvisCommand.OpenSettings -> openSettings(context, command.type)
            is JarvisCommand.OpenMaps -> openMaps(context, command.destination)
            is JarvisCommand.SearchGoogle -> searchGoogle(context, command.query)
            is JarvisCommand.OpenCalculator -> openCalculator(context)
            is JarvisCommand.CreateDeviceFile -> {
                val fileRes = JarvisDeviceFileManager.createDeviceFile(context, command.fileName, command.content)
                if (fileRes.success) {
                    ActionResult(true, "File \"${command.fileName}\" created at ${fileRes.filePath}.")
                } else {
                    ActionResult(false, "Failed to create file: ${fileRes.errorMessage}")
                }
            }
            is JarvisCommand.BatteryStatus -> {
                val battery = getBatteryTelemetry(context)
                ActionResult(true, "Battery level is at ${battery.percentage}%, ${if (battery.isCharging) "charging" else "discharging"}.")
            }
            is JarvisCommand.DeviceTelemetryReport -> {
                val telem = getDeviceTelemetry(context)
                ActionResult(true, "Device ${telem.deviceModel}, RAM: ${telem.memoryUsedMB}MB / ${telem.memoryMaxMB}MB.")
            }
            else -> ActionResult(true, "Command acknowledged.")
        }
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String
)

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val temperatureCelsius: Float
)

data class DeviceTelemetry(
    val deviceModel: String,
    val androidVersion: String,
    val battery: BatteryInfo,
    val memoryUsedMB: Long,
    val memoryMaxMB: Long,
    val storageAvailableGB: String,
    val storageTotalGB: String
)
