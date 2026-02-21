package com.shettyharshith33.placementpro.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

data class Message(val text: String, val isBot: Boolean)

private val NavyBlue = Color(0xFF1C375B)
private val LightNavy = Color(0xFF2A4B7C)
private val BotBubbleColor = Color(0xFFF3F4F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementBotScreen(onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(Message("Hello! I'm your AI Placement Assistant. How can I help you today?", true)) }
    var isTyping by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // 🔥 Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            val target = if (isTyping) messages.size else messages.size - 1
            if (target >= 0) {
                listState.animateScrollToItem(target)
            }
        }
    }

    // 🔥 Optimized Webhook Caller
    fun callN8NWebhook(userQuery: String) {
        scope.launch {
            isTyping = true
            val botResponse = withContext(Dispatchers.IO) {
                try {
                    val webhookUrl = "https://akki1908.app.n8n.cloud/webhook/8a78886b-07e7-47ea-8b44-ddb16a54d411"
                    val url = URL(webhookUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.readTimeout = 25000 
                    conn.connectTimeout = 20000
                    conn.doOutput = true

                    val jsonParam = JSONObject()
                    jsonParam.put("chatInput", userQuery)
                    jsonParam.put("sessionId", "student_user_${System.currentTimeMillis()}") 

                    conn.outputStream.use { it.write(jsonParam.toString().toByteArray()) }

                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }

                        try {
                            if (responseText.trim().startsWith("[")) {
                                val jsonArray = JSONArray(responseText)
                                val firstObj = jsonArray.getJSONObject(0)
                                when {
                                    firstObj.has("output") -> firstObj.getString("output")
                                    firstObj.has("response") -> firstObj.getString("response")
                                    firstObj.has("text") -> firstObj.getString("text")
                                    else -> firstObj.toString()
                                }
                            } else {
                                val jsonObj = JSONObject(responseText)
                                when {
                                    jsonObj.has("output") -> jsonObj.getString("output")
                                    jsonObj.has("response") -> jsonObj.getString("response")
                                    else -> responseText
                                }
                            }
                        } catch (e: Exception) {
                            responseText 
                        }
                    } else {
                        "Error: The AI brain is resting (Code ${conn.responseCode}). Try again in a moment!"
                    }
                } catch (e: Exception) {
                    "Connection failed. Make sure the placement server is online."
                }
            }
            messages.add(Message(botResponse, true))
            isTyping = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Placement Advisor AI", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Online & Powered by n8n", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = NavyBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(NavyBlue, Color.White), endY = 600f))
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                    if (isTyping) {
                        item { TypingIndicator() }
                    }
                }
            }

            Surface(
                shadowElevation = 12.dp,
                color = Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).navigationBarsPadding().imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about placements...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = NavyBlue,
                            cursorColor = NavyBlue
                        ),
                        shape = RoundedCornerShape(28.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText.trim()
                                messages.add(Message(query, false))
                                inputText = ""
                                callN8NWebhook(query)
                            }
                        },
                        containerColor = NavyBlue,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: Message) {
    val isBot = msg.isBot
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isBot) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Surface(
            color = if (isBot) BotBubbleColor else LightNavy,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isBot) 4.dp else 16.dp,
                bottomEnd = if (isBot) 16.dp else 4.dp
            ),
            tonalElevation = if (isBot) 1.dp else 4.dp
        ) {
            Text(
                text = msg.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = if (isBot) Color.Black else Color.White,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val alpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0f at index * 200
                        1f at (index * 200) + 400
                        0f at 1000
                    }
                ), label = "typing"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color.Gray.copy(alpha = alpha), CircleShape)
            )
        }
    }
}