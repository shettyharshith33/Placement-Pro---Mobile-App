package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Message(val text: String, val isBot: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementBotScreen(onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(Message("Hello! I'm PlacementBot. How can I help you today?", true)) }
    val navyBlue = Color(0xFF1C375B)

    fun handleSend() {
        if (inputText.isBlank()) return
        messages.add(Message(inputText, false))
        
        val response = when {
            inputText.contains("cutoff", ignoreCase = true) -> "Most companies require a CGPA of 7.0 or above with no active backlogs."
            inputText.contains("resume", ignoreCase = true) -> "You can generate your branded resume from the 'Profile' tab in your dashboard!"
            inputText.contains("interview", ignoreCase = true) -> "Check your 'My Applications' tab. If you are scheduled, the date will appear there."
            else -> "I'm here to help with placements. Try asking about 'cutoff', 'resume', or 'interviews'."
        }
        
        messages.add(Message(response, true))
        inputText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PlacementBot", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyBlue)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            Surface(shadowElevation = 8.dp) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about placements...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { handleSend() }, colors = IconButtonDefaults.iconButtonColors(containerColor = navyBlue)) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: Message) {
    val alignment = if (msg.isBot) Alignment.Start else Alignment.End
    val color = if (msg.isBot) Color(0xFFE0E0E0) else Color(0xFF1C375B)
    val textColor = if (msg.isBot) Color.Black else Color.White

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = color,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(msg.text, modifier = Modifier.padding(12.dp), color = textColor, fontSize = 14.sp)
        }
    }
}
