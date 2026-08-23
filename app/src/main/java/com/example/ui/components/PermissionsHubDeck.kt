package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.JarvisAccessibilityService
import com.example.service.JarvisFloatingOverlayService
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
import com.example.ui.theme.JarvisLaserRed
import com.example.ui.theme.JarvisOrange
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isGranted: Boolean,
    val isSpecial: Boolean = false,
    val onGrant: () -> Unit
)

@Composable
fun PermissionsHubDeck(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Launcher for standard runtime permissions
    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    // Helper functions to check permissions
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isAccessibilityEnabled(): Boolean {
        return JarvisAccessibilityService.isAccessibilitySettingsEnabled(context)
    }

    fun isOverlayGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }
    }

    fun isStorageManagerGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Build permissions list dynamically with live status
    val standardRuntimePermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
            list.add(Manifest.permission.READ_MEDIA_IMAGES)
            list.add(Manifest.permission.READ_MEDIA_VIDEO)
            list.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        list
    }

    // Individual permission items
    val micGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.RECORD_AUDIO) }
    val cameraGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.CAMERA) }
    val notifGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else true
    }
    val phoneGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.CALL_PHONE) }
    val contactsGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.READ_CONTACTS) }
    val smsGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.SEND_SMS) }
    val locationGranted = remember(refreshTrigger) { isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) }
    val storageGranted = remember(refreshTrigger) { isStorageManagerGranted() }
    val accessibilityGranted = remember(refreshTrigger) { isAccessibilityEnabled() }
    val overlayGranted = remember(refreshTrigger) { isOverlayGranted() }
    val batteryGranted = remember(refreshTrigger) { isBatteryOptimizationIgnored() }

    val allItems = listOf(
        // Core Sensory
        PermissionItem(
            id = "mic",
            title = "Microphone & Voice Input",
            description = "Required for live voice commands, wake-word recognition, and speech dialogue.",
            icon = Icons.Default.Mic,
            isGranted = micGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        ),
        PermissionItem(
            id = "camera",
            title = "Camera & Optical HUD",
            description = "Required for live camera vision scanner and multimodal AI analysis.",
            icon = Icons.Default.CameraAlt,
            isGranted = cameraGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        ),
        PermissionItem(
            id = "notifications",
            title = "Push & Background Notifications",
            description = "Keeps Jarvis running in background and posts execution reports.",
            icon = Icons.Default.Notifications,
            isGranted = notifGranted,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }
        ),
        PermissionItem(
            id = "location",
            title = "GPS & Precise Location",
            description = "Used for accurate navigation, local queries, and Google Maps directions.",
            icon = Icons.Default.LocationOn,
            isGranted = locationGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        ),
        // Phone Control
        PermissionItem(
            id = "phone",
            title = "Phone Calls & Dialing",
            description = "Allows Jarvis to initiate phone calls to specified numbers or contacts.",
            icon = Icons.Default.Call,
            isGranted = phoneGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.READ_PHONE_STATE
                    )
                )
            }
        ),
        PermissionItem(
            id = "contacts",
            title = "Contacts Book Access",
            description = "Allows Jarvis to search contact names when you say 'Call John' or 'Message Mom'.",
            icon = Icons.Default.Contacts,
            isGranted = contactsGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
            }
        ),
        PermissionItem(
            id = "sms",
            title = "SMS Messaging",
            description = "Allows Jarvis to send and read text messages via voice command.",
            icon = Icons.Default.Sms,
            isGranted = smsGranted,
            onGrant = {
                runtimePermissionsLauncher.launch(
                    arrayOf(
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.READ_SMS,
                        Manifest.permission.RECEIVE_SMS
                    )
                )
            }
        ),
        PermissionItem(
            id = "storage",
            title = "File Storage & Media",
            description = "Allows Jarvis to generate, save, read, and export code files on your phone.",
            icon = Icons.Default.Folder,
            isGranted = storageGranted,
            isSpecial = true,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        context.startActivity(intent)
                    }
                } else {
                    runtimePermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }
        ),
        // Special Superuser System Access
        PermissionItem(
            id = "accessibility",
            title = "🤖 Autonomous Accessibility Service",
            description = "CRITICAL: Enables auto-typing text in any app, tapping buttons, and gesture scrolling.",
            icon = Icons.Default.Accessibility,
            isGranted = accessibilityGranted,
            isSpecial = true,
            onGrant = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        ),
        PermissionItem(
            id = "overlay",
            title = "🪟 Floating Arc Reactor (Display Over Apps)",
            description = "CRITICAL: Allows the floating Stark HUD orb to appear on top of other applications.",
            icon = Icons.Default.Layers,
            isGranted = overlayGranted,
            isSpecial = true,
            onGrant = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        ),
        PermissionItem(
            id = "battery",
            title = "🔋 Ignore Battery Optimization (Background Mode)",
            description = "CRITICAL: Prevents Android OS from killing Jarvis in background when screen is off.",
            icon = Icons.Default.BatterySaver,
            isGranted = batteryGranted,
            isSpecial = true,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }
            }
        )
    )

    val grantedCount = allItems.count { it.isGranted }
    val totalCount = allItems.size
    val progress = grantedCount.toFloat() / totalCount.toFloat()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("permissions_hub_deck"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top System Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = JarvisDarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (grantedCount == totalCount) JarvisGreen else JarvisCyanBright)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (grantedCount == totalCount) JarvisGreen else JarvisCyanBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "STARK PROTOCOL // PERMISSIONS HUB",
                                    color = JarvisCyanBright,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (grantedCount == totalCount) "ALL PERMISSIONS GRANTED (FULL AUTONOMY)" else "PARTIAL ACCESS: $grantedCount OF $totalCount ACTIVE",
                                    color = if (grantedCount == totalCount) JarvisGreen else JarvisOrange,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        IconButton(
                            onClick = { refreshTrigger++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = JarvisCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (grantedCount == totalCount) JarvisGreen else JarvisCyanBright,
                        trackColor = JarvisDarkSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Master Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1-Click Request All
                        Button(
                            onClick = {
                                runtimePermissionsLauncher.launch(standardRuntimePermissions.toTypedArray())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Grant All (1-Tap)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Direct Android App Info Settings Button
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyan),
                            border = BorderStroke(1.dp, JarvisBorderCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("App Info Settings", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Section: Runtime Permissions
        item {
            Text(
                text = "⚡ STANDARD RUNTIME PERMISSIONS",
                color = JarvisCyanDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        // Standard permissions list
        items(allItems.filter { !it.isSpecial }.size) { idx ->
            val item = allItems.filter { !it.isSpecial }[idx]
            PermissionCardRow(item = item)
        }

        // Section: Special Superuser Permissions
        item {
            Text(
                text = "🛡️ SPECIAL SYSTEM & SUPERUSER ACCESS",
                color = JarvisCyanDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        items(allItems.filter { it.isSpecial }.size) { idx ->
            val item = allItems.filter { it.isSpecial }[idx]
            PermissionCardRow(item = item)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PermissionCardRow(item: PermissionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isGranted) JarvisDarkSurface else JarvisDarkSurfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            1.dp,
            if (item.isGranted) JarvisBorderGlow else JarvisBorderCyan.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (item.isGranted) JarvisGreen.copy(alpha = 0.15f) else JarvisCyan.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (item.isGranted) JarvisGreen else JarvisCyanBright,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.description,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (item.isGranted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(JarvisGreen.copy(alpha = 0.15f))
                        .border(1.dp, JarvisGreen, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = JarvisGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ACTIVE",
                        color = JarvisGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Button(
                    onClick = item.onGrant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isSpecial) JarvisElectricBlue else JarvisCyanBright
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
                ) {
                    Text(
                        text = if (item.isSpecial) "Configure" else "Grant",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
