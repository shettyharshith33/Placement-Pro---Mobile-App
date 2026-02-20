package com.shettyharshith33.placementpro.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*

@Composable
fun VerificationWaitScreen(onCheck: () -> Unit) {
    val navyBlue = Color(0xFF1C375B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = navyBlue
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            color = navyBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A verification link has been sent to your college email address. Please click it to activate your account.",
            textAlign = TextAlign.Center,
            color = Color.Black, // Explicitly black for visibility
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onCheck,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = navyBlue,
                contentColor = Color.White
            )
        ) {
            Text("I HAVE VERIFIED", fontWeight = FontWeight.Bold)
        }
    }
}