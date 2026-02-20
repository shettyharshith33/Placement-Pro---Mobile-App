package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.CompanyDrive
import com.shettyharshith33.placementpro.models.FirestoreCollections
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDriveScreen(
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val navyBlue = Color(0xFF1C375B)

    // Form State
    var companyName by remember { mutableStateOf("") }
    var roleOffered by remember { mutableStateOf("") }
    var minCGPA by remember { mutableStateOf("7.0") }
    var maxBacklogs by remember { mutableStateOf("0") }
    var packageLPA by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Criteria Engine Results
    var eligibleCount by remember { mutableStateOf(0) }
    var isCalculating by remember { mutableStateOf(false) }

    // Logic for Criteria Engine
    LaunchedEffect(minCGPA, maxBacklogs) {
        isCalculating = true
        val cgpaVal = minCGPA.toDoubleOrNull() ?: 0.0
        val backlogsVal = maxBacklogs.toIntOrNull() ?: 0

        db.collection(FirestoreCollections.USERS)
            .whereEqualTo("role", "student")
            .whereGreaterThanOrEqualTo("cgpa", cgpaVal)
            .get()
            .addOnSuccessListener { snapshot ->
                // Basic filtering because Firestore has limits on multiple range queries
                val filtered = snapshot.documents.filter { doc ->
                    val studentBacklogs = doc.getLong("backlogs")?.toInt() ?: 0
                    studentBacklogs <= backlogsVal
                }
                eligibleCount = filtered.size
                isCalculating = false
            }
            .addOnFailureListener {
                isCalculating = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Drive", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Job Details", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = roleOffered, onValueChange = { roleOffered = it }, label = { Text("Role (e.g. SDE-1)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = packageLPA, onValueChange = { packageLPA = it }, label = { Text("Package (LPA)") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Drive Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            Text("The Criteria Engine", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 18.sp)
            Text("Set constraints to see eligible candidates instantly.", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = minCGPA,
                    onValueChange = { minCGPA = it },
                    label = { Text("Min CGPA") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedTextField(
                    value = maxBacklogs,
                    onValueChange = { maxBacklogs = it },
                    label = { Text("Max Backlogs") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real-time counter card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = navyBlue.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current Selection", fontWeight = FontWeight.Bold, color = navyBlue)
                        Text(
                            if (isCalculating) "Calculating..." else "$eligibleCount Students Eligible",
                            color = if (eligibleCount > 0) Color(0xFF2E7D32) else Color.Red,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    if (eligibleCount > 0) {
                        Button(
                            onClick = { 
                                // 🔥 Logic: Create notification entries for eligible students
                                val cgpaVal = minCGPA.toDoubleOrNull() ?: 0.0
                                val backlogsVal = maxBacklogs.toIntOrNull() ?: 0
                                
                                db.collection("users")
                                    .whereEqualTo("role", "student")
                                    .whereGreaterThanOrEqualTo("cgpa", cgpaVal)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        snapshot.documents.forEach { doc ->
                                            val studentBacklogs = doc.getLong("backlogs")?.toInt() ?: 0
                                            if (studentBacklogs <= backlogsVal) {
                                                val studentId = doc.getString("uid") ?: return@forEach
                                                val notifId = UUID.randomUUID().toString()
                                                val notif = hashMapOf(
                                                    "id" to notifId,
                                                    "targetUserId" to studentId,
                                                    "title" to "New Drive: $companyName",
                                                    "message" to "You are eligible for the $roleOffered role at $companyName. Apply now!",
                                                    "timestamp" to Timestamp.now(),
                                                    "isRead" to false
                                                )
                                                db.collection("notifications").document(notifId).set(notif)
                                            }
                                        }
                                        Toast.makeText(context, "Notifications sent to $eligibleCount students!", Toast.LENGTH_SHORT).show()
                                    }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Notify All")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (companyName.isNotBlank() && roleOffered.isNotBlank()) {
                        val driveId = UUID.randomUUID().toString()
                        val drive = CompanyDrive(
                            companyId = driveId,
                            companyName = companyName,
                            roleOffered = roleOffered,
                            description = description,
                            packageLPA = packageLPA.toDoubleOrNull() ?: 0.0,
                            minCGPA = minCGPA.toDoubleOrNull() ?: 0.0,
                            maxBacklogs = maxBacklogs.toIntOrNull() ?: 0,
                            isActive = true,
                            createdAt = Timestamp.now()
                        )

                        db.collection(FirestoreCollections.COMPANIES).document(driveId).set(drive)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Drive Created Successfully!", Toast.LENGTH_LONG).show()
                                onBack()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
            ) {
                Text("POST PLACEMENT DRIVE", fontWeight = FontWeight.Bold)
            }
        }
    }
}
