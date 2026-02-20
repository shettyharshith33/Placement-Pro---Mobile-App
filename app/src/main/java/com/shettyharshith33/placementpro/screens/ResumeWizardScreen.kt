/*
package com.shettyharshith33.placementpro.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.shettyharshith33.placementpro.models.FirestoreCollections

private val PrimaryBlue = Color(0xFF4DA3FF)

// 🔥 VERY IMPORTANT — safe filename generator
private fun safeFileName(name: String): String {
    return name
        .trim()
        .lowercase()
        .replace("[^a-z0-9]".toRegex(), "_")
}

@Composable
fun ResumeWizardScreen(onComplete: () -> Unit) {

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // 🔵 UI state
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 📄 File picker
    val resumePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        resumeUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

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
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() } },
                label = { Text("Mobile Number (10 digits)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(rollNumber, { rollNumber = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(branch, { branch = it }, label = { Text("Branch (e.g., CSE)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(cgpa, { cgpa = it }, label = { Text("Current CGPA") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(backlogs, { backlogs = it }, label = { Text("Active Backlogs") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            // ✅ SKILLS → ARRAY
            OutlinedTextField(
                value = skills,
                onValueChange = { skills = it },
                label = { Text("Skills (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 📄 Upload Resume Button
            Button(
                onClick = { resumePicker.launch("application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (resumeUri == null) "Upload Resume (PDF)"
                    else "Resume Selected ✓",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {

                    val uid = auth.currentUser?.uid ?: return@Button

                    if (name.isBlank() || phone.length != 10 || branch.isBlank()) {
                        Toast.makeText(context, "Enter valid details", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->

                            fun saveData(resumeUrl: String, fileName: String) {

                                val skillsList = skills
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val studentUpdates = hashMapOf(
                                    "branch" to branch.uppercase().trim(),
                                    "cgpa" to (cgpa.toDoubleOrNull() ?: 0.0),
                                    "backlogs" to (backlogs.toIntOrNull() ?: 0),
                                    "skills" to skillsList,
                                    "placed" to false,
                                    "resumeUrl" to resumeUrl,
                                    "resumeFileName" to fileName,
                                    "updatedAt" to Timestamp.now()
                                )

                                val userUpdates = hashMapOf(
                                    "name" to name.trim(),
                                    "phone" to phone,
                                    "rollNumber" to rollNumber,
                                    "profileCompleted" to true,
                                    "fcmToken" to token,
                                    "updatedAt" to Timestamp.now()
                                )

                                db.collection(FirestoreCollections.STUDENTS)
                                    .document(uid)
                                    .set(studentUpdates, SetOptions.merge())

                                db.collection(FirestoreCollections.USERS)
                                    .document(uid)
                                    .set(userUpdates, SetOptions.merge())
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                        onComplete()
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                    }
                            }

                            // 🔥 Upload resume if selected
                            if (resumeUri != null) {

                                val safeName = safeFileName(name)
                                val fileName = "${safeName}.pdf"

                                val ref = storage.reference
                                    .child("resumes/$uid/$fileName")

                                ref.putFile(resumeUri!!)
                                    .continueWithTask { ref.downloadUrl }
                                    .addOnSuccessListener { uri ->
                                        saveData(uri.toString(), fileName)
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Resume upload failed", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                saveData("", "")
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SAVE & CONTINUE", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🔥 Full screen loader
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }
}*/






package com.shettyharshith33.placementpro.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.shettyharshith33.placementpro.models.FirestoreCollections

private val PrimaryBlue = Color(0xFF4DA3FF)

/* 🔥 safe filename */
private fun safeFileName(name: String): String {
    return name.trim().lowercase()
        .replace("[^a-z0-9]".toRegex(), "_")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResumeWizardScreen(onComplete: () -> Unit) {

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    // ---------------- UI STATE ----------------
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }

    // ⭐ NEW SKILLS SYSTEM
    var skillInput by remember { mutableStateOf("") }
    var skillsList by remember { mutableStateOf(listOf<String>()) }

    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // ---------- TextField colors (FIX WHITE TEXT BUG)
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = PrimaryBlue,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = PrimaryBlue
    )

    // ---------- File picker
    val resumePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> resumeUri = uri }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

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
                color = PrimaryBlue,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() }
                },
                label = { Text("Mobile Number (10 digits)") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = rollNumber,
                onValueChange = { rollNumber = it },
                label = { Text("Roll Number") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = branch,
                onValueChange = { branch = it },
                label = { Text("Branch (e.g., CSE)") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cgpa,
                onValueChange = { cgpa = it },
                label = { Text("Current CGPA") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = backlogs,
                onValueChange = { backlogs = it },
                label = { Text("Active Backlogs") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            // =====================================================
            // 🔥 NEW SKILLS INPUT (CHIP SYSTEM)
            // =====================================================

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Skills",
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    value = skillInput,
                    onValueChange = { skillInput = it },
                    label = { Text("Add skill") },
                    colors = OutlinedTextFieldDefaults.colors().copy(focusedTextColor = Color.Black) ,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        val skill = skillInput.trim()
                        if (skill.isNotEmpty() && !skillsList.contains(skill)) {
                            skillsList = skillsList + skill
                            skillInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔥 Skill chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skillsList.forEach { skill ->
                    AssistChip(
                        onClick = {},
                        label = { Text(skill, color = Color.Black) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    skillsList = skillsList - skill
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    )
                }
            }

            // =====================================================

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { resumePicker.launch("application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (resumeUri == null) "Upload Resume (PDF)"
                    else "Resume Selected ✓",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {

                    val uid = auth.currentUser?.uid ?: return@Button

                    if (name.isBlank() || phone.length != 10 || branch.isBlank()) {
                        Toast.makeText(context, "Enter valid details", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->

                            fun saveData(resumeUrl: String, fileName: String) {

                                // 🔥 PRIMARY — USERS (your requirement)
                                val userUpdates = hashMapOf(
                                    "name" to name.trim(),
                                    "phone" to phone,
                                    "rollNumber" to rollNumber,
                                    "branch" to branch.uppercase().trim(),
                                    "cgpa" to (cgpa.toDoubleOrNull() ?: 0.0),
                                    "backlogs" to (backlogs.toIntOrNull() ?: 0),
                                    "skills" to skillsList,
                                    "resumeUrl" to resumeUrl,
                                    "resumeFileName" to fileName,
                                    "profileCompleted" to true,
                                    "fcmToken" to token,
                                    "updatedAt" to Timestamp.now()
                                )

                                db.collection(FirestoreCollections.USERS)
                                    .document(uid)
                                    .set(userUpdates, SetOptions.merge())
                                    .addOnSuccessListener {
                                        isLoading = false
                                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                                        onComplete()
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
                                    }
                            }

                            // 🔥 resume upload
                            if (resumeUri != null) {

                                val safeName = safeFileName(name)
                                val fileName = "$safeName.pdf"

                                val ref = storage.reference
                                    .child("resumes/$uid/$fileName")

                                ref.putFile(resumeUri!!)
                                    .continueWithTask { ref.downloadUrl }
                                    .addOnSuccessListener { uri ->
                                        saveData(uri.toString(), fileName)
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, "Resume upload failed", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                saveData("", "")
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SAVE & CONTINUE", fontWeight = FontWeight.Bold)
                }
            }
        }

        // 🔥 loader overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }
}