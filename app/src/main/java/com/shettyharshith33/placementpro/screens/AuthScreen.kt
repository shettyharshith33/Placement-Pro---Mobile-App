package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*

@Composable
fun AuthScreen(
    role: String,
    onRegisterClick: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val navyBlue = Color(0xFF1C375B)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Join as ${role.uppercase()}", style = MaterialTheme.typography.headlineMedium, color = navyBlue, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("College Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Set Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onRegisterClick(email, password) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
        ) {
            Text("CREATE ACCOUNT", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}