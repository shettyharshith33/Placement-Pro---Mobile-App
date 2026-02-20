package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

import com.shettyharshith33.placementpro.models.FirestoreCollections

@Composable
fun ResumeWizardScreen(onComplete: () -> Unit) {

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val navyBlue = Color(0xFF1C375B)

    // 🔵 UI state
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Complete Your Profile",
            style = MaterialTheme.typography.headlineMedium,
            color = navyBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ NAME
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ PHONE (10 digit validation)
        OutlinedTextField(
            value = phone,
            onValueChange = { if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() } },
            label = { Text("Mobile Number (10 digits)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ ROLL NUMBER
        OutlinedTextField(
            value = rollNumber,
            onValueChange = { rollNumber = it },
            label = { Text("Roll Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ BRANCH
        OutlinedTextField(
            value = branch,
            onValueChange = { branch = it },
            label = { Text("Branch (e.g., CSE)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ CGPA
        OutlinedTextField(
            value = cgpa,
            onValueChange = { cgpa = it },
            label = { Text("Current CGPA") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ BACKLOGS
        OutlinedTextField(
            value = backlogs,
            onValueChange = { backlogs = it },
            label = { Text("Active Backlogs") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ SKILLS
        OutlinedTextField(
            value = skills,
            onValueChange = { skills = it },
            label = { Text("Skills (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {

                val uid = auth.currentUser?.uid ?: return@Button

                // 🚨 VALIDATION
                if (name.isBlank() ||
                    phone.length != 10 ||
                    branch.isBlank()
                ) {
                    Toast.makeText(
                        context,
                        "Enter valid details",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                isLoading = true

                // 🔥 GET FCM TOKEN
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->

                        val batch = db.batch()

                        // ================= STUDENT UPDATE =================
                        val studentUpdates = hashMapOf<String, Any>(
                            "branch" to branch.uppercase().trim(),
                            "cgpa" to (cgpa.toDoubleOrNull() ?: 0.0),
                            "backlogs" to (backlogs.toIntOrNull() ?: 0),
                            "skills" to skills.split(",").map { it.trim() },
                            "placed" to false,
                            "resumeUrl" to "",
                            "updatedAt" to Timestamp.now()
                        )

                        val studentRef = db.collection(
                            FirestoreCollections.STUDENTS
                        ).document(uid)

                        batch.update(studentRef, studentUpdates)

                        // ================= USER UPDATE =================
                        val userUpdates = hashMapOf<String, Any>(
                            "name" to name.trim(),
                            "phone" to phone,
                            "rollNumber" to rollNumber,
                            "profileCompleted" to true,
                            "fcmToken" to token,
                            "updatedAt" to Timestamp.now()
                        )

                        val userRef = db.collection(
                            FirestoreCollections.USERS
                        ).document(uid)

                        batch.update(userRef, userUpdates)

                        // ================= COMMIT =================
                        batch.commit()
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Profile Updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onComplete()
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    it.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("SAVE & CONTINUE", fontWeight = FontWeight.Bold)
            }
        }
    }
}