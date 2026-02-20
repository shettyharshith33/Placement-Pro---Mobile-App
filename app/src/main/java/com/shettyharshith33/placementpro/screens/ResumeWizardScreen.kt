package com.shettyharshith33.placementpro.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.shettyharshith33.placementpro.models.User

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
    val navyBlue = Color(0xFF1C375B)

    // ---------------- STATE ----------------
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var cgpa by remember { mutableStateOf("") }
    var backlogs by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var skillInput by remember { mutableStateOf("") }
    var skillsList by remember { mutableStateOf(listOf<String>()) }
    var projectsList by remember { mutableStateOf(listOf<com.shettyharshith33.placementpro.models.Project>()) }
    var projectTitle by remember { mutableStateOf("") }
    var projectDesc by remember { mutableStateOf("") }

    var resumeUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // 🔥 FETCH EXISTING DATA IF ANY
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)
                user?.let {
                    name = it.name
                    phone = it.phone
                    branch = it.branch
                    cgpa = it.cgpa.toString()
                    backlogs = it.backlogs.toString()
                    rollNumber = it.rollNumber
                    skillsList = it.skills
                    projectsList = it.projects
                }
            }
        }
    }

    val tfColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        cursorColor = navyBlue,
        focusedBorderColor = navyBlue,
        unfocusedBorderColor = Color.LightGray,
        focusedLabelColor = navyBlue
    )

    val resumePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> resumeUri = uri }

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
                "projects" to projectsList,
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
                    Toast.makeText(context, "Profile Updated ✅", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        if (resumeUri != null) {
            val fileName = "${safeFileName(name)}_resume.pdf"
            val ref = storage.reference.child("resumes/$uid/$fileName")
            ref.putFile(resumeUri!!)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { uri -> saveToFirestore(uri.toString(), fileName) }
                .addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Upload Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            saveToFirestore("", "")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Professional Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = navyBlue,
                fontWeight = FontWeight.Bold
            )
            Text("Let's refine your dashboard experience.", color = Color.Gray, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // SECTION 1: PERSONAL INFO
            WizardSectionHeader("Personal Information", Icons.Default.Person)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Full Name") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        phone,
                        { if (it.length <= 10) phone = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Mobile Number") },
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: ACADEMIC INFO
            WizardSectionHeader("Academic Records", Icons.Default.Info)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(rollNumber, { rollNumber = it }, label = { Text("Roll Number") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(branch, { branch = it }, label = { Text("Branch (e.g., CSE)") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(cgpa, { cgpa = it }, label = { Text("CGPA") }, colors = tfColors, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(backlogs, { backlogs = it }, label = { Text("Backlogs") }, colors = tfColors, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: SKILLS
            WizardSectionHeader("Skill Set", Icons.Default.Star)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            skillInput,
                            { skillInput = it },
                            label = { Text("Type skill...") },
                            colors = tfColors,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            val skill = skillInput.trim()
                            if (skill.isNotEmpty() && !skillsList.contains(skill)) {
                                skillsList = skillsList + skill
                                skillInput = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = navyBlue)
                        }
                    }
                    FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        skillsList.forEach { skill ->
                            InputChip(
                                selected = true,
                                onClick = { skillsList = skillsList - skill },
                                label = { Text(skill) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: PROJECTS
            WizardSectionHeader("Key Projects", Icons.Default.List)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(projectTitle, { projectTitle = it }, label = { Text("Project Title") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(projectDesc, { projectDesc = it }, label = { Text("Short Description") }, colors = tfColors, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (projectTitle.isNotBlank()) {
                                projectsList = projectsList + com.shettyharshith33.placementpro.models.Project(projectTitle, projectDesc)
                                projectTitle = ""
                                projectDesc = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue.copy(alpha = 0.1f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = navyBlue)
                        Text("Add Project", color = navyBlue)
                    }

                    projectsList.forEach { proj ->
                        ListItem(
                            headlineContent = { Text(proj.title, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(proj.description) },
                            trailingContent = {
                                IconButton(onClick = { projectsList = projectsList - proj }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 4: ASSETS
            WizardSectionHeader("Professional Assets", Icons.Default.AccountBox)
            Button(
                onClick = { resumePicker.launch("application/pdf") },
                colors = ButtonDefaults.buttonColors(containerColor = if(resumeUri != null) Color(0xFF2E7D32) else navyBlue),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(if(resumeUri != null) Icons.Default.CheckCircle else Icons.Default.Build, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(if (resumeUri == null) "Choose Resume PDF" else "Resume Attached ✓")
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    val uid = auth.currentUser?.uid ?: return@Button
                    if (name.isBlank() || phone.length < 10 || branch.isBlank()) {
                        Toast.makeText(context, "Please verify all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> uploadAndSave(uid, token) }.addOnFailureListener { uploadAndSave(uid, "") }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("FINISH SETUP", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun WizardSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1C375B), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1C375B), fontSize = 16.sp)
    }
}