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
            inputText.contains("cutoff", ignoreCase = true) || inputText.contains("criteria", ignoreCase = true) -> "Most companies require a CGPA of 7.0 or above with no active backlogs. Check individual drive details for specifics."
            inputText.contains("resume", ignoreCase = true) -> "You can refine your professional profile and attach your resume from the 'Resume Wizard' in your profile settings!"
            inputText.contains("interview", ignoreCase = true) || inputText.contains("timing", ignoreCase = true) -> "Interview timings are posted in the 'My Applications' section once a TPO assigns a slot. Keep checking there!"
            inputText.contains("venue", ignoreCase = true) || inputText.contains("location", ignoreCase = true) -> "Most interviews are currently happening in the 'MCA Seminar Hall' or virtually via Microsoft Teams. Verify the drive description if it's virtual."
            inputText.contains("refer", ignoreCase = true) || inputText.contains("alumni", ignoreCase = true) -> "Go to the 'Alumni Connect' section to see job referrals from our graduates or book a mentorship slot!"
            inputText.contains("hello", ignoreCase = true) || inputText.contains("hi", ignoreCase = true) -> "Hello! I'm your 24/7 Career Assistant. Ask me about cutoffs, venues, or resumes!"
            else -> "I'm specializing in placement queries. Try asking: 'What is the cutoff?', 'Where is the venue?', or 'How to get a referral?'"
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
