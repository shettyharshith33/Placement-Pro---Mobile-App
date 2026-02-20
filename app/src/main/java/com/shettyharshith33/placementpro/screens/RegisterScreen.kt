package com.shettyharshith33.placementpro.screens


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.StudentProfile
import com.shettyharshith33.placementpro.models.User
import com.shettyharshith33.placementpro.models.UserRole

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
// Inside RegisterScreen Button onClick:
                auth.createUserWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { result ->
                        val uid = result.user!!.uid

                        // 1. Create the Base User (for Auth & Web sync)
                        val userBase = User(
                            uid = uid,
                            name = name.trim(),
                            email = email.trim(),
                            role = UserRole.STUDENT, // or role passed from previous screen
                            createdAt = Timestamp.now(),
                            profileCompleted = false
                        )

                        // 2. Create the Empty Student Profile (To be filled in Resume Wizard)
                        val studentProfile = StudentProfile(
                            uid = uid,
                            batchYear = 2026 // Default from your schema
                        )

                        val batch = db.batch()
                        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
                        val studentRef = db.collection(FirestoreCollections.STUDENTS).document(uid)

                        batch.set(userRef, userBase)
                        batch.set(studentRef, studentProfile)

                        batch.commit().addOnSuccessListener {
                            result.user!!.sendEmailVerification()
                            onRegisterSuccess()
                        }
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