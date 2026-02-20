package com.shettyharshith33.placementpro.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.classes.UserRole

@Composable
fun LoginScreen(
    role: String,
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var secretCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Login as ${role.uppercase()}")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        // 🔴 TPO extra field
        if (role == UserRole.TPO) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = secretCode,
                onValueChange = { secretCode = it },
                label = { Text("Institution Secret Code") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {

                // 🟥 TPO SECURE LOGIN
                if (role == UserRole.TPO) {

                    db.collection("tpo_access")
                        .document("primary")
                        .get()
                        .addOnSuccessListener { doc ->

                            val correctCode = doc.getString("secretCode")
                            val allowedEmails =
                                doc.get("allowedEmails") as? List<*>

                            if (secretCode != correctCode ||
                                allowedEmails?.contains(email) != true
                            ) {
                                Toast.makeText(
                                    context,
                                    "Unauthorized TPO",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@addOnSuccessListener
                            }

                            auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener {
                                    onLoginSuccess(role)
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Login failed",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                } else {
                    // 🟩 STUDENT / ALUMNI LOGIN
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            onLoginSuccess(role)
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Login failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("LOGIN")
        }

        // ✅ Google only for student & alumni
        if (role != UserRole.TPO) {

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(
                        context,
                        "Google Sign-In next step",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in with Google")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onNavigateToRegister) {
                Text("New User? Register Here")
            }
        }
    }
}