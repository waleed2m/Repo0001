package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val skyState by viewModel.currentSkyState.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Scroll to the bottom when new messages are added or when generating starts
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing) // Edge-to-edge notch safety
    ) {
        // 1. The Dynamic Sky Background Canvas
        SkyCanvas(
            skyState = skyState,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Chat Layout Layer
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant App Bar / Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22FFFFFF))
                    .border(width = 0.5.dp, color = Color(0x33FFFFFF))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x66FFFFFF))
                                .border(width = 1.dp, color = Color(0x4DFFFFFF), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = "Sky Companion Symbol",
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Sky Companion",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontFamily = FontFamily.SansSerif
                            )
                            Text(
                                text = "ONLINE • RADIANT",
                                fontSize = 9.sp,
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.clearChatHistory() },
                        modifier = Modifier
                            .testTag("clear_history_button")
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .border(width = 0.5.dp, color = Color(0x22FFFFFF), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear History",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Scrollable Message List (Glassmorphic Scroll Area)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Centered Date Marker
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x33FFFFFF))
                                .border(width = 0.5.dp, color = Color(0x4DFFFFFF), shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Today • Sky Connection",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                letterSpacing = 1.2.sp
                            )
                        }
                    }
                }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }

                if (isGenerating) {
                    item {
                        TypingIndicatorBubble()
                    }
                }
            }

            // Quick Mood Shifters Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x11FFFFFF))
                    .border(width = 0.5.dp, color = Color(0x1FFFFFFF))
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = "Tap a mood to shift the sky atmosphere:",
                    fontSize = 11.sp,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SkyChip(
                            label = "🌅 Morning",
                            isActive = skyState == SkyState.MORNING,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.MORNING) }
                        )
                    }
                    item {
                        SkyChip(
                            label = "☀️ Noon",
                            isActive = skyState == SkyState.MIDDAY,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.MIDDAY) }
                        )
                    }
                    item {
                        SkyChip(
                            label = "🌇 Sunset",
                            isActive = skyState == SkyState.SUNSET,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.SUNSET) }
                        )
                    }
                    item {
                        SkyChip(
                            label = "🌃 Night",
                            isActive = skyState == SkyState.NIGHT,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.NIGHT) }
                        )
                    }
                    item {
                        SkyChip(
                            label = "⛈️ Storm",
                            isActive = skyState == SkyState.STORM,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.STORM) }
                        )
                    }
                    item {
                        SkyChip(
                            label = "🌌 Aurora",
                            isActive = skyState == SkyState.AURORA,
                            onClick = { viewModel.setSkyStateDirectly(SkyState.AURORA) }
                        )
                    }
                }
            }

            // Minimalist Input Area (Translucent Floating Box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x22FFFFFF))
                    .border(width = 0.5.dp, color = Color(0x33FFFFFF))
                    .navigationBarsPadding() // gesture bar padding safety
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xCCFFFFFF))
                        .border(width = 1.dp, color = Color(0x99FFFFFF), shape = RoundedCornerShape(28.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimal decorative accent prefix
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x11000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "Decoration",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                "Message Sky...",
                                color = Color(0xFF94A3B8),
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF4F46E5)
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendMessage(textInput)
                                    textInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        },
                        enabled = textInput.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("send_button")
                            .clip(CircleShape)
                            .background(
                                if (textInput.isNotBlank()) Color(0xFF4F46E5) else Color(0x1F000000)
                            ),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    // AI message: Glassmorphic white/60 backdrop with slate-900 text and thin border
    // User message: Deep Indigo background with white text and smooth shadow
    val bubbleColor = if (message.isUser) Color(0xFF4F46E5) else Color(0xCCFFFFFF)
    val textColor = if (message.isUser) Color.White else Color(0xFF0F172A)
    val borderColors = if (message.isUser) Color.Transparent else Color(0x66FFFFFF)
    
    // Beautiful large round corner architecture
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (message.isUser) "user_message_bubble" else "bot_message_bubble"),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bubbleColor)
                    .border(width = 0.5.dp, color = borderColors, shape = shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            
            // Subtle sky emoji status under bubble
            if (!message.isUser) {
                val emoji = when (message.skyState) {
                    "MORNING" -> "🌅"
                    "MIDDAY" -> "☀️"
                    "SUNSET" -> "🌇"
                    "NIGHT" -> "🌃"
                    "STORM" -> "⛈️"
                    "AURORA" -> "🌌"
                    else -> "✨"
                }
                Text(
                    text = "$emoji Sky Spirit",
                    fontSize = 10.sp,
                    color = Color(0xFF334155).copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("typing_indicator"),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp))
                .background(Color(0xCCFFFFFF))
                .border(
                    width = 0.5.dp, 
                    color = Color(0x66FFFFFF), 
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 4.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "The sky is shifting...",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic
                )
                
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = Color(0xFF4F46E5),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
fun SkyChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Styling values reflecting Clean Minimalism white/30 background with soft borders
    val chipBg = if (isActive) Color(0x80FFFFFF) else Color(0x33FFFFFF)
    val chipBorderColor = if (isActive) Color(0xFF4F46E5) else Color(0x66FFFFFF)
    val chipTextColor = if (isActive) Color(0xFF4F46E5) else Color(0xFF334155)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(chipBg)
            .border(width = 1.2.dp, color = chipBorderColor, shape = RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = chipTextColor,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
