package com.shettyharshith33.placementpro.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.classes.FirestoreCollections
import com.shettyharshith33.placementpro.classes.User

@Composable
fun RegisterScreen(
    role: String,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val navyBlue = Color(0xFF1C375B)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create ${role.uppercase()} Account",
            style = MaterialTheme.typography.headlineMedium,
            color = navyBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("Password", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.length < 6) {
                    Toast.makeText(context, "Enter valid details (min 6 chars)", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isLoading = true
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val uid = result.user!!.uid
                        val user = User(
                            uid = uid,
                            name = name.trim(),
                            email = email.trim(),
                            role = role,
                            createdAt = Timestamp.now()
                        )

                        db.collection(FirestoreCollections.USERS)
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener {
                                result.user!!.sendEmailVerification()
                                isLoading = false
                                onRegisterSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                    }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("CREATE ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}