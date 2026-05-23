package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiApi
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAgentScreen(viewModel: MainViewModel) {
    val chatLogs by viewModel.chatLogs.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val streamingText by viewModel.activeStreamingResponse.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    var activeView by remember { mutableStateOf("chat") } // "chat" or "settings"
    var currentInput by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val modelsList = listOf(
        Pair("Gemini 2.0", "#4285F4"),
        Pair("GPT-4o", "#10A37F"),
        Pair("Claude", "#CC785C"),
        Pair("Groq ⚡", "#F55036")
    )
    var selectedModelBadge by remember { mutableStateOf("Gemini 2.0") }

    val suggestedPrompts = listOf(
        "এই মাসে কত খরচ?",
        "আজকের tasks?",
        "Weekly summary",
        "Saving tips"
    )

    // Ensure list scrolls down when message arrives
    LaunchedEffect(chatLogs.size, streamingText) {
        if (chatLogs.isNotEmpty()) {
            listState.animateScrollToItem(chatLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // AI Topbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeView == "chat") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("😈 ", fontSize = 20.sp)
                    Text(
                        text = "DEVIL ",
                        color = TextWhite,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "GPT",
                        color = DevilRed,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trash style clear logs
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat log history",
                            tint = TextGrey
                        )
                    }

                    // Settings Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface2)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .clickable { activeView = "settings" },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Agent Configuration",
                            tint = DevilRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { activeView = "chat" }) {
                        Text("←", color = DevilRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Agent Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }
            }
        }

        // Display contents
        if (activeView == "chat") {
            // Screen 4.A — Chat Interface
            
            // Models badge row visual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                modelsList.forEach { (m, hex) ->
                    val isSelected = selectedModelBadge == m
                    val clr = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DevilRedDim else DarkSurface2)
                            .border(
                                1.dp,
                                if (isSelected) DevilRed else BorderColor,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedModelBadge = m }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(clr)
                            )
                            Text(
                                text = m,
                                color = if (isSelected) TextWhite else TextGrey,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message scrolling area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (chatLogs.isEmpty() && streamingText == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("😈", fontSize = 56.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "হ্যালো Hridoy! আমি DEVIL GPT। তোমার expense, tasks, notes — সব কিছুতে access আছে আমার। কী জানতে চাও?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Suggested prompt pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            suggestedPrompts.forEach { p ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSurface2)
                                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.sendChatMessage(p)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(p, color = TextGrey, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 20.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(chatLogs) { log ->
                            val isUser = log.sender == "user"
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(DarkSurface2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("😈", fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                                )
                                            )
                                            .background(if (isUser) DevilRed else DarkSurface)
                                            .border(
                                                1.dp,
                                                if (isUser) Color.Transparent else BorderColor,
                                                RoundedCornerShape(16.dp)
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = log.text,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                if (isUser) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(DevilRedDim),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("👤", fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        // Real-time Streaming message
                        if (streamingText != null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(DarkSurface2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("😈", fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                                            .background(DarkSurface)
                                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = streamingText!!,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Typing bounce dots pulse when waiting response
                        if (isAiGenerating && streamingText == null) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(DarkSurface2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("😈", fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DarkSurface)
                                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        PulseCircle()
                                        PulseCircle()
                                        PulseCircle()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                // Attach icon simulation (Describe Image P1)
                IconButton(
                    onClick = {
                        viewModel.sendChatMessage("Describe this receipt image...", overrideUserAndDoNotSaveInput = true)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface2)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = "Attach Receipt Image", tint = TextWhite)
                }

                // Input bar
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    placeholder = { Text("কিছু জিজ্ঞেস করো...", color = TextDarkGrey, fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface2,
                        unfocusedContainerColor = DarkSurface2,
                        focusedBorderColor = BorderColor,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )

                // Voice mic simulation (Speech context P1)
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        currentInput = "Add ৳500 for lunch this afternoon"
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface2)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Simulate Voice input", tint = DevilRed)
                }

                // Send button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DevilRed)
                        .clickable {
                            if (currentInput.isNotBlank()) {
                                viewModel.sendChatMessage(currentInput)
                                currentInput = ""
                                keyboardController?.hide()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("➤", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            }
        } else {
            // Screen 4.E — Agent Settings Screen Scrollable view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 90.dp)
            ) {
                // Settings Section 1: AI Config
                Text(
                    text = "🤖 AI CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Gemini API Key Input
                        var maskKey by remember { mutableStateOf(true) }
                        OutlinedTextField(
                            value = settings.geminiApiKey,
                            onValueChange = { viewModel.saveSettings(settings.copy(geminiApiKey = it)) },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("AIzaSy...") },
                            visualTransformation = if (maskKey) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { maskKey = !maskKey }) {
                                    Icon(
                                        imageVector = if (maskKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Mask text"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Info Alert regarding key extraction security
                        Text(
                            text = "Security Warning: Keys stored in BuildConfig are meant for local testing. Avoid sharing untrusted APK outputs.",
                            color = AppleYellow,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        // Stream responses toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Stream Responses", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Faster dynamic typewriter words", color = TextGrey, fontSize = 10.sp)
                            }
                            Switch(
                                checked = settings.streamingEnabled,
                                onCheckedChange = { viewModel.saveSettings(settings.copy(streamingEnabled = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DevilRed
                                )
                            )
                        }

                        // Max tokens slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Max Output Tokens", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${settings.maxTokens}", color = DevilRed, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            }
                            Slider(
                                value = settings.maxTokens.toFloat(),
                                onValueChange = { viewModel.saveSettings(settings.copy(maxTokens = it.toInt())) },
                                valueRange = 256f..4096f,
                                colors = SliderDefaults.colors(
                                    thumbColor = DevilRed,
                                    activeTrackColor = DevilRed,
                                    inactiveTrackColor = DarkSurface3
                                )
                            )
                        }
                    }
                }

                // Settings Section 2: Memory Context
                Text(
                    text = "🧠 MEMORY CONFIG",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Write facts here about yourself (e.g. your targets, visual preferences or coffee tastes). The AI agent injects this immediately in system memory.",
                            color = TextGrey,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        OutlinedTextField(
                            value = settings.userMemory,
                            onValueChange = { viewModel.saveSettings(settings.copy(userMemory = it)) },
                            placeholder = { Text("e.g. I live in Dhaka. My standard daily snack is North End coffee.") },
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.saveSettings(settings.copy(userMemory = "")) },
                            colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear Memory Context", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Settings Section 3: System Prompt
                Text(
                    text = "📝 SYSTEM PROMPT EDITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = settings.customSystemPrompt,
                            onValueChange = { viewModel.saveSettings(settings.copy(customSystemPrompt = it)) },
                            label = { Text("System Instructions") },
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { viewModel.saveSettings(settings.copy(customSystemPrompt = "You are DEVIL GPT — a ruthlessly efficient, witty personal AI assistant.")) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface2),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RESET TO DEFAULT", color = TextWhite)
                        }
                    }
                }

                // Settings Section 4: Budget & Finance Config
                Text(
                    text = "💸 FINANCE TARGETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = settings.monthlyBudget.toString(),
                            onValueChange = { val value = it.toDoubleOrNull() ?: 10000.0; viewModel.saveSettings(settings.copy(monthlyBudget = value)) },
                            label = { Text("Monthly Budget Limit") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = settings.currencySymbol,
                            onValueChange = { viewModel.saveSettings(settings.copy(currencySymbol = it)) },
                            label = { Text("Currency Symbol") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Settings Section 5: App Personalization (Light/Dark Switch, Username)
                Text(
                    text = "🎨 PERSONALIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = settings.userName,
                            onValueChange = { viewModel.saveSettings(settings.copy(userName = it)) },
                            label = { Text("User Display Name") },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DevilRed,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulseCircle() {
    var state by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val size by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Size"
    )

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(DevilRed)
    )
}
