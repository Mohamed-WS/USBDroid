package com.usbdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.usbdroid.ui.theme.*
import com.usbdroid.viewmodel.AIMessage
import com.usbdroid.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AIScreen(viewModel: MainViewModel) {
    val messages by viewModel.aiMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Surface(color = Surface.copy(alpha = 0.5f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(SecondaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = SecondaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "AI Hardware Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackground
                    )
                    Text(
                        text = "Powered by Claude",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryPurple
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.clearAIChat() }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear chat", tint = OnSurfaceVariant)
                }
            }
        }

        Divider(color = Divider, thickness = 1.dp)

        // Chat Messages
        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AIWelcomeScreen()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    when (message) {
                        is AIMessage.User -> UserMessageBubble(message.content)
                        is AIMessage.Assistant -> AssistantMessageBubble(message.content)
                        is AIMessage.Error -> ErrorMessageBubble(message.message)
                        is AIMessage.Thinking -> ThinkingIndicator()
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            LaunchedEffect(messages.size) {
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size)
                }
            }
        }

        // Input Area
        Surface(color = Surface, tonalElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Ask about your USB device...",
                            color = OnSurfaceVariant.copy(alpha = 0.4f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryPurple,
                        unfocusedBorderColor = Border,
                        focusedTextColor = OnBackground,
                        unfocusedTextColor = OnBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendAIMessage(inputText)
                            inputText = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SecondaryPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun AIWelcomeScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(SecondaryPurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Psychology,
                contentDescription = null,
                tint = SecondaryPurple,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Hardware AI Assistant",
            style = MaterialTheme.typography.titleLarge,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "I can help you identify devices, troubleshoot issues,\nand guide you through firmware flashing and debugging.",
            color = OnSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick prompt chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "What is this device?" to Icons.Filled.Search,
                "How do I flash ESP32?" to Icons.Filled.Memory,
                "Serial port settings?" to Icons.Filled.Usb,
                "Pinout reference" to Icons.Filled.PinDrop
            ).forEach { (label, icon) ->
                SuggestionChip(label, icon)
            }
        }
    }
}

@Composable
private fun SuggestionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = SurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = SecondaryPurple, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = OnBackground, fontSize = 12.sp)
        }
    }
}

@Composable
private fun UserMessageBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = SecondaryPurple.copy(alpha = 0.2f)
            ),
            shape = RoundedCornerShape(16.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(4.dp)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                color = OnBackground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AssistantMessageBubble(content: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SecondaryPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = SecondaryPurple,
                modifier = Modifier.size(16.dp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Surface
            ),
            shape = RoundedCornerShape(16.dp).copy(bottomStart = androidx.compose.foundation.shape.CornerSize(4.dp)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = content,
                color = OnBackground,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun ErrorMessageBubble(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = ErrorRed.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(message, color = ErrorRed, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(SecondaryPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = SecondaryPurple,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("Analyzing...", color = SecondaryPurple, style = MaterialTheme.typography.bodySmall)

        // Animated dots
        val infiniteTransition = rememberInfiniteTransition(label = "dots")
        val dotAlpha by infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        Row {
            repeat(3) { i ->
                val anim by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = i * 150, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot_$i"
                )
                Text(
                    ".",
                    color = SecondaryPurple.copy(alpha = anim),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
