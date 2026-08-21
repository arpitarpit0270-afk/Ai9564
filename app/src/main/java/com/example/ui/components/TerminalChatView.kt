package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.JarvisDeviceFileManager
import com.example.ui.ChatMessage
import com.example.ui.MessageSender
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDim
import com.example.ui.theme.JarvisDarkSurface
import com.example.ui.theme.JarvisDarkSurfaceVariant
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisStarkGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalChatView(
    messages: List<ChatMessage>,
    onSpeakMessage: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Quick prompt suggestion chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val suggestions = listOf(
                "Create Python script",
                "Create HTML file",
                "WhatsApp hello",
                "Open Instagram",
                "Play Lo-Fi on YouTube",
                "Flashlight ON",
                "Battery Diagnostics"
            )
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisCyan.copy(alpha = 0.12f))
                        .border(1.dp, JarvisCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chip_$suggestion")
                ) {
                    Text(
                        text = suggestion,
                        color = JarvisCyanBright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(message = message, onSpeak = { onSpeakMessage(message.text) })
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onSpeak: () -> Unit
) {
    val context = LocalContext.current
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM

    val bubbleBg = when {
        isUser -> JarvisDarkSurfaceVariant
        isSystem -> JarvisDarkSurface
        else -> JarvisDarkSurface
    }

    val borderColor = when {
        isUser -> JarvisElectricBlue.copy(alpha = 0.5f)
        isSystem -> JarvisStarkGold.copy(alpha = 0.4f)
        else -> JarvisCyan.copy(alpha = 0.6f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser && !isSystem) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(JarvisCyan.copy(alpha = 0.2f))
                    .border(1.dp, JarvisCyan, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            modifier = Modifier
                .fillMaxWidth(if (isSystem) 1f else 0.86f)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header line with Sender & Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (message.sender) {
                            MessageSender.USER -> "OPERATOR"
                            MessageSender.JARVIS -> if (message.isGeminiPowered) "J.A.R.V.I.S. // ${message.providerBadge ?: "AI"}" else "J.A.R.V.I.S. // CORE"
                            MessageSender.SYSTEM -> "SYSTEM NOTICE"
                        },
                        color = when (message.sender) {
                            MessageSender.USER -> JarvisElectricBlue
                            MessageSender.JARVIS -> JarvisCyanBright
                            MessageSender.SYSTEM -> JarvisStarkGold
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.timestamp,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!isUser && !isSystem) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speak replay",
                                    tint = JarvisCyanDim,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Attached Vision Image Thumbnail
                if (message.attachedBitmap != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, JarvisElectricBlue, RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = message.attachedBitmap.asImageBitmap(),
                            contentDescription = "Analyzed image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Attached File Pill
                if (message.attachedFile != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(JarvisElectricBlue.copy(alpha = 0.15f))
                            .border(1.dp, JarvisElectricBlue.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = JarvisCyanBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.attachedFile.name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Size: ${message.attachedFile.sizeFormatted} • ${message.attachedFile.mimeType}",
                                    color = TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message Text
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Action execution pill if an action took place
                if (message.actionExecuted != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(JarvisGreen.copy(alpha = 0.12f))
                            .border(1.dp, JarvisGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = JarvisGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.actionExecuted,
                                color = JarvisGreen,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Device Created File Card with Open & Share Actions
                if (message.createdFile != null && message.createdFile.success) {
                    val file = message.createdFile
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = JarvisDarkSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, JarvisStarkGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = JarvisStarkGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Device File Generated: ${file.fileName}",
                                    color = JarvisStarkGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Path: ${file.filePath} (${file.fileSizeKB} KB)",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (file.uri != null || file.file != null) {
                                    OutlinedButton(
                                        onClick = {
                                            JarvisDeviceFileManager.openFile(context, file.file, file.uri, file.mimeType)
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisCyanBright)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            JarvisDeviceFileManager.shareFile(context, file.file, file.uri, file.mimeType)
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan.copy(alpha = 0.25f), contentColor = JarvisCyanBright)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(JarvisElectricBlue.copy(alpha = 0.2f))
                    .border(1.dp, JarvisElectricBlue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = JarvisElectricBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
