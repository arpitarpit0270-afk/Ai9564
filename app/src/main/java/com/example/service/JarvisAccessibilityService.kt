package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class JarvisAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Jarvis Accessibility Service Connected & Active.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track active window/package if necessary
    }

    override fun onInterrupt() {
        Log.w(TAG, "Jarvis Accessibility Service interrupted.")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    /**
     * Finds the currently focused or any editable text input field and writes text into it.
     */
    fun typeTextIntoFocusedField(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false

        // Strategy 1: Target currently focused input node
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && injectTextIntoNode(focusedNode, text)) {
            focusedNode.recycle()
            rootNode.recycle()
            return true
        }

        // Strategy 2: Target accessibility focused node
        val a11yFocusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
        if (a11yFocusedNode != null && injectTextIntoNode(a11yFocusedNode, text)) {
            a11yFocusedNode.recycle()
            rootNode.recycle()
            return true
        }

        // Strategy 3: Recursively find first editable node
        val editableNode = findFirstEditableNode(rootNode)
        if (editableNode != null) {
            val success = injectTextIntoNode(editableNode, text)
            editableNode.recycle()
            rootNode.recycle()
            return success
        }

        rootNode.recycle()
        return false
    }

    /**
     * Injects text into a specific node via ACTION_SET_TEXT or clipboard paste.
     */
    private fun injectTextIntoNode(node: AccessibilityNodeInfo, text: String): Boolean {
        try {
            // First ensure node is focused
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Attempt direct setText
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                return true
            }

            // Fallback: Copy to clipboard and perform paste action
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("Jarvis AutoType", text)
                clipboard.setPrimaryClip(clip)
                if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting text into node: ${e.localizedMessage}")
        }
        return false
    }

    /**
     * Recursively traverses window hierarchy to find any editable input element.
     */
    private fun findFirstEditableNode(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) {
                return found
            }
            child.recycle()
        }
        return null
    }

    /**
     * Finds a clickable node matching specified text or content description and performs click.
     */
    fun clickNodeByText(targetText: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val cleanTarget = targetText.trim()

        // 1. Try exact search via system method
        val matchedNodes = rootNode.findAccessibilityNodeInfosByText(cleanTarget)
        if (!matchedNodes.isNullOrEmpty()) {
            for (node in matchedNodes) {
                var current: AccessibilityNodeInfo? = node
                while (current != null) {
                    if (current.isClickable) {
                        val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        rootNode.recycle()
                        return clicked
                    }
                    current = current.parent
                }
            }
        }

        // 2. Deep recursive fuzzy search across all text, contentDescription, hintText & view IDs
        val fuzzyMatch = findNodeFuzzy(rootNode, cleanTarget)
        if (fuzzyMatch != null) {
            var current: AccessibilityNodeInfo? = fuzzyMatch
            while (current != null) {
                if (current.isClickable) {
                    val clicked = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    rootNode.recycle()
                    return clicked
                }
                current = current.parent
            }
            // If parent not clickable, try clicking node directly anyway
            val clicked = fuzzyMatch.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            fuzzyMatch.recycle()
            rootNode.recycle()
            return clicked
        }

        rootNode.recycle()
        return false
    }

    private fun findNodeFuzzy(node: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val lowerTarget = target.lowercase()

        val text = node.text?.toString()?.lowercase()
        val desc = node.contentDescription?.toString()?.lowercase()
        val viewId = node.viewIdResourceName?.lowercase()

        if (text?.contains(lowerTarget) == true || desc?.contains(lowerTarget) == true || viewId?.contains(lowerTarget) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeFuzzy(child, target)
            if (found != null) {
                return found
            }
            child.recycle()
        }
        return null
    }

    /**
     * Taps at specific screen coordinates (X, Y) using Accessibility Gestures.
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Performs a scroll/swipe gesture from (startX, startY) to (endX, endY).
     */
    fun performSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = android.graphics.Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(stroke)
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Executes global navigation and system control actions.
     */
    fun triggerGlobalAction(action: GlobalActionType): Boolean {
        val actionId = when (action) {
            GlobalActionType.BACK -> GLOBAL_ACTION_BACK
            GlobalActionType.HOME -> GLOBAL_ACTION_HOME
            GlobalActionType.RECENTS -> GLOBAL_ACTION_RECENTS
            GlobalActionType.NOTIFICATIONS -> GLOBAL_ACTION_NOTIFICATIONS
            GlobalActionType.QUICK_SETTINGS -> GLOBAL_ACTION_QUICK_SETTINGS
            GlobalActionType.POWER_DIALOG -> GLOBAL_ACTION_POWER_DIALOG
            GlobalActionType.LOCK_SCREEN -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    GLOBAL_ACTION_POWER_DIALOG
                }
            }
            GlobalActionType.TAKE_SCREENSHOT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    GLOBAL_ACTION_TAKE_SCREENSHOT
                } else {
                    return false
                }
            }
        }
        return performGlobalAction(actionId)
    }

    enum class GlobalActionType {
        BACK,
        HOME,
        RECENTS,
        NOTIFICATIONS,
        QUICK_SETTINGS,
        POWER_DIALOG,
        LOCK_SCREEN,
        TAKE_SCREENSHOT
    }

    /**
     * Extracts and summarizes all visible text, package info, and UI elements on the current active screen.
     */
    fun dumpCurrentScreenContent(): String {
        val rootNode = rootInActiveWindow ?: return "Screen buffer empty: Accessibility node tree is unavailable. Ensure Jarvis Accessibility Service is granted permission."
        val textList = mutableListOf<String>()
        val packageName = rootNode.packageName?.toString() ?: "Unknown App"
        
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null) return
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            if (!text.isNullOrBlank()) {
                textList.add(text)
            } else if (!desc.isNullOrBlank()) {
                textList.add("[$desc]")
            }
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                traverse(child)
                child?.recycle()
            }
        }

        try {
            traverse(rootNode)
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting screen content: ${e.localizedMessage}")
        }

        val combinedText = textList.distinct().joinToString("\n").take(3000)
        return if (combinedText.isNotBlank()) {
            "Active Application: $packageName\nVisible On-Screen Content:\n$combinedText"
        } else {
            "Active Application: $packageName\nScreen appears graphical or lacks text accessibility nodes."
        }
    }

    companion object {
        private const val TAG = "JarvisA11yService"

        @Volatile
        var instance: JarvisAccessibilityService? = null

        fun isRunning(): Boolean = instance != null

        /**
         * Checks if the Accessibility Service is enabled in system settings.
         */
        fun isAccessibilitySettingsEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${JarvisAccessibilityService::class.java.canonicalName}"
            val accessibilityEnabled = try {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED
                )
            } catch (e: Exception) {
                0
            }

            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                val splitter = TextUtils.SimpleStringSplitter(':')
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val service = splitter.next()
                    if (service.equals(expectedServiceName, ignoreCase = true) || service.contains(JarvisAccessibilityService::class.java.simpleName)) {
                        return true
                    }
                }
            }
            return isRunning()
        }

        /**
         * Opens Android Accessibility settings directly so user can grant permission in 1 tap.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings: ${e.localizedMessage}")
            }
        }
    }
}
