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

private val PrimaryBlue = Color(0xFF1C375B) // Professional Navy from your screenshots

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

    // UI STATE
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var skillInput by remember { mutableStateOf("") }
    var skillsList by remember { mutableStateOf(listOf<String>()) }
    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = PrimaryBlue,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = PrimaryBlue
    )

    val resumePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> resumeUri = uri }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = phone, onValueChange = { if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() } }, label = { Text("Mobile Number") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it }, label = { Text("Roll Number") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch (e.g., CSE)") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = cgpa, onValueChange = { cgpa = it }, label = { Text("CGPA") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = backlogs, onValueChange = { backlogs = it }, label = { Text("Backlogs") }, colors = tfColors, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(24.dp))
            Text("Skills", fontWeight = FontWeight.SemiBold, color = PrimaryBlue, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = skillInput, onValueChange = { skillInput = it }, label = { Text("Add skill") }, colors = tfColors, modifier = Modifier.weight(1f), singleLine = true)
                IconButton(onClick = {
                    val skill = skillInput.trim()
                    if (skill.isNotEmpty() && !skillsList.contains(skill)) {
                        skillsList = skillsList + skill
                        skillInput = ""
                    }
                }) { Icon(Icons.Default.Add, contentDescription = "Add", tint = PrimaryBlue) }
            }
            FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                skillsList.forEach { skill ->
                    AssistChip(onClick = {}, label = { Text(skill) }, trailingIcon = {
                        IconButton(onClick = { skillsList = skillsList - skill }) { Icon(Icons.Default.Close, modifier = Modifier.size(16.dp), contentDescription = null) }
                    })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { resumePicker.launch("application/pdf") }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), modifier = Modifier.fillMaxWidth()) {
                Text(if (resumeUri == null) "Upload Resume (PDF)" else "Resume Selected ✓", color = Color.White)
            }

            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    val uid = auth.currentUser?.uid ?: return@Button
                    if (name.isBlank() || phone.length != 10 || branch.isBlank()) {
                        Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true

                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        fun saveData(resumeUrl: String, fileName: String) {
                            val userUpdates = hashMapOf(
                                "uid" to uid,
                                "name" to name.trim(),
                                "phone" to phone,
                                "rollNumber" to rollNumber.trim().uppercase(),
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

                            // FORCE PATH: Direct string "users" to match your Firestore exactly
                            db.collection("users").document(uid)
                                .set(userUpdates, SetOptions.merge())
                                .addOnSuccessListener {
                                    isLoading = false
                                    onComplete()
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Firestore Error: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                        }

                        if (resumeUri != null) {
                            val fileName = "${safeFileName(name)}_resume.pdf"
                            val ref = storage.reference.child("resumes/$uid/$fileName")
                            ref.putFile(resumeUri!!)
                                .continueWithTask { ref.downloadUrl }
                                .addOnSuccessListener { uri -> saveData(uri.toString(), fileName) }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Resume Upload Error", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            saveData("", "")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("SAVE & CONTINUE", fontWeight = FontWeight.Bold)
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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

private val PrimaryBlue = Color(0xFF1C375B)

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

    // ---------------- STATE ----------------
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var skillInput by remember { mutableStateOf("") }
    var skillsList by remember { mutableStateOf(listOf<String>()) }
    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // ---------------- TextField colors (fix white text bug)
    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = PrimaryBlue,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = PrimaryBlue
    )

    val resumePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> resumeUri = uri }

    // =========================================================
    // 🔥 MAIN SAVE FUNCTION (BULLETPROOF)
    // =========================================================
    fun uploadAndSave(uid: String, token: String) {

        fun saveToFirestore(resumeUrlFinal: String, fileName: String) {

            val userUpdates = hashMapOf(
                "uid" to uid,
                "name" to name.trim(),
                "phone" to phone,
                "rollNumber" to rollNumber.trim().uppercase(),
                "branch" to branch.uppercase().trim(),
                "cgpa" to (cgpa.toDoubleOrNull() ?: 0.0),
                "backlogs" to (backlogs.toIntOrNull() ?: 0),
                "skills" to skillsList,
                "resumeUrl" to resumeUrlFinal,
                "resumeFileName" to fileName,
                "profileCompleted" to true,
                "fcmToken" to token,
                "updatedAt" to Timestamp.now()
            )

            db.collection("users")
                .document(uid)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener {
                    isLoading = false
                    Toast.makeText(context, "Saved to users ✅", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Firestore FAILED: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        // ---------- resume upload ----------
        if (resumeUri != null) {

            val fileName = "${safeFileName(name)}_resume.pdf"
            val ref = storage.reference.child("resumes/$uid/$fileName")

            ref.putFile(resumeUri!!)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { uri ->
                    saveToFirestore(uri.toString(), fileName)
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Resume upload failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

        } else {
            saveToFirestore("", "")
        }
    }

    // =========================================================
    // UI
    // =========================================================
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

            OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                phone,
                {
                    if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() }
                },
                label = { Text("Mobile Number") },
                colors = tfColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(rollNumber, { rollNumber = it }, label = { Text("Roll Number") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(branch, { branch = it }, label = { Text("Branch (e.g., CSE)") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(cgpa, { cgpa = it }, label = { Text("CGPA") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(backlogs, { backlogs = it }, label = { Text("Backlogs") }, colors = tfColors, modifier = Modifier.fillMaxWidth())

            // ---------------- SKILLS ----------------
            Spacer(modifier = Modifier.height(24.dp))
            Text("Skills", fontWeight = FontWeight.SemiBold, color = PrimaryBlue, modifier = Modifier.fillMaxWidth())

            Row(verticalAlignment = Alignment.CenterVertically) {

                OutlinedTextField(
                    skillInput,
                    { skillInput = it },
                    label = { Text("Add skill") },
                    colors = tfColors,
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        val skill = skillInput.trim()
                        if (skill.isNotEmpty() && !skillsList.contains(skill)) {
                            skillsList = skillsList + skill
                            skillInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = PrimaryBlue)
                }
            }

            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skillsList.forEach { skill ->
                    AssistChip(
                        onClick = {},
                        label = { Text(skill, color = Color.Black) },
                        trailingIcon = {
                            IconButton(onClick = { skillsList = skillsList - skill }) {
                                Icon(Icons.Default.Close, modifier = Modifier.size(16.dp), contentDescription = null)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { resumePicker.launch("application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (resumeUri == null) "Upload Resume (PDF)" else "Resume Selected ✓",
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ================= SAVE BUTTON =================
            Button(
                onClick = {

                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (name.isBlank() || phone.length != 10 || branch.isBlank()) {
                        Toast.makeText(context, "Fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { token ->
                            uploadAndSave(uid, token)
                        }
                        .addOnFailureListener {
                            uploadAndSave(uid, "")
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
    }
}