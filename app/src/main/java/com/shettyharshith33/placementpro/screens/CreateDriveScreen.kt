package com.shettyharshith33.placementpro.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.shettyharshith33.placementpro.models.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateDriveScreen(
    onBack: () -> Unit,
    driveId: String? = null // For Edit Mode
) {
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current
    val navyBlue = Color(0xFF1C375B)
    val branches = listOf("CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "MCA", "MBA", "M.Tech")

    // Form State
    var companyName by remember { mutableStateOf("") }
    var roleOffered by remember { mutableStateOf("") }
    var minCGPA by remember { mutableStateOf("7.0") }
    var maxBacklogs by remember { mutableStateOf("0") }
    var packageLPA by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var jobDescription by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var batchYear by remember { mutableStateOf("2026") }
    var deadline by remember { mutableStateOf("") }
    var selectedBranches by remember { mutableStateOf(setOf<String>()) }
    
    // File State
    var jdUri by remember { mutableStateOf<Uri?>(null) }
    var currentJdUrl by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var eligibleCount by remember { mutableStateOf(0) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = navyBlue,
        focusedLabelColor = navyBlue,
        cursorColor = navyBlue
    )

    // 🔥 DATE PICKER
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            deadline = "$day/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // 🔥 FILE PICKER
    val jdPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> jdUri = uri }

    // 🔥 FETCH EXISTING DATA (If in Edit Mode)
    LaunchedEffect(driveId) {
        if (driveId != null) {
            isLoading = true
            db.collection(FirestoreCollections.COMPANIES).document(driveId).get()
                .addOnSuccessListener { doc ->
                    val drive = doc.toObject(CompanyDrive::class.java)
                    drive?.let {
                        companyName = it.companyName
                        roleOffered = it.roleOffered
                        minCGPA = it.minCGPA.toString()
                        maxBacklogs = it.maxBacklogs.toString()
                        packageLPA = it.packageDouble.toString()
                        description = it.description
                        jobDescription = it.jobDescription
                        location = it.workMode
                        website = it.companyWebsite
                        batchYear = it.batchYear.toString()
                        deadline = it.deadline?.toString() ?: ""
                        selectedBranches = it.allowedBranches.toSet()
                        currentJdUrl = it.jdFileUrl
                    }
                    isLoading = false
                }
        }
    }

    // 🔥 CRITERIA ENGINE LOGIC (Real-time)
    LaunchedEffect(minCGPA, maxBacklogs) {
        val cgpaVal = minCGPA.toDoubleOrNull() ?: 0.0
        val backlogsVal = maxBacklogs.toIntOrNull() ?: 0

        db.collection(FirestoreCollections.USERS)
            .whereEqualTo("role", UserRole.STUDENT)
            .get()
            .addOnSuccessListener { snapshot ->
                val students = snapshot.toObjects(User::class.java)
                eligibleCount = students.count { student ->
                    student.finalCgpa >= cgpaVal && student.finalBacklogs <= backlogsVal
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (driveId == null) "Post New Drive" else "Edit Drive", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyBlue)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = navyBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Company Info
                FormHeader("Company Information")
                OutlinedTextField(companyName, { companyName = it }, label = { Text("Company Name") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(website, { website = it }, label = { Text("Company Website (Optional)") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Section: Job Info
                FormHeader("Job Details")
                OutlinedTextField(roleOffered, { roleOffered = it }, label = { Text("Role Offered (e.g. SDE-1)") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(packageLPA, { packageLPA = it }, label = { Text("Package (LPA)") }, modifier = Modifier.weight(1f), colors = textFieldColors)
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(location, { location = it }, label = { Text("Location/Work Mode") }, modifier = Modifier.weight(1.5f), colors = textFieldColors)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Brief Overview") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, minLines = 2)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(jobDescription, { jobDescription = it }, label = { Text("Detailed Job Description (JD)") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, minLines = 4)

                Spacer(modifier = Modifier.height(16.dp))
                
                // JD File Upload Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = navyBlue.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Job Description PDF", fontWeight = FontWeight.Bold, color = navyBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { jdPicker.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (jdUri != null) "Change File" else "Upload JD / PDF")
                        }
                        if (jdUri != null) {
                            Text("Selected: ${jdUri?.lastPathSegment}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                        } else if (currentJdUrl.isNotBlank()) {
                            Text("Current JD attached ✅", fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section: Eligibility
                FormHeader("Eligibility Requirements")
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(minCGPA, { minCGPA = it }, label = { Text("Min CGPA") }, modifier = Modifier.weight(1f), colors = textFieldColors)
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(maxBacklogs, { maxBacklogs = it }, label = { Text("Max Backlogs") }, modifier = Modifier.weight(1f), colors = textFieldColors)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(batchYear, { batchYear = it }, label = { Text("Target Batch Year") }, modifier = Modifier.weight(1f), colors = textFieldColors)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = deadline,
                            onValueChange = {},
                            label = { Text("Deadline") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = true,
                            colors = textFieldColors
                        )
                        // Invisible overlay to capture clicks
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { datePickerDialog.show() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Eligible Branches:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = navyBlue)
                FlowRow(modifier = Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    branches.forEach { branch ->
                        FilterChip(
                            selected = selectedBranches.contains(branch),
                            onClick = {
                                selectedBranches = if (selectedBranches.contains(branch)) selectedBranches - branch else selectedBranches + branch
                            },
                            label = { Text(branch) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Criteria Engine Real-time Result
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = navyBlue.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Criteria Engine Check (Real-time)", fontWeight = FontWeight.Bold, color = navyBlue)
                            Text("$eligibleCount Students meet these criteria", fontSize = 14.sp)
                        }
                        Icon(if (eligibleCount > 0) Icons.Default.CheckCircle else Icons.Default.Info, contentDescription = null, tint = if (eligibleCount > 0) Color(0xFF2E7D32) else Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (companyName.isBlank() || roleOffered.isBlank() || packageLPA.isBlank()) {
                            Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        isLoading = true
                        val id = driveId ?: UUID.randomUUID().toString()

                        fun sendNotificationsToEligibleStudents(driveId: String, companyName: String, cgpa: Double, backlogs: Int, branches: List<String>, isUpdate: Boolean) {
                            db.collection(FirestoreCollections.USERS)
                                .whereEqualTo("role", UserRole.STUDENT)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val students = snapshot.toObjects(User::class.java)
                                    val eligible = students.filter { student ->
                                        student.finalCgpa >= cgpa && 
                                        student.finalBacklogs <= backlogs && 
                                        (branches.isEmpty() || branches.contains(student.branch))
                                    }

                                    val batch = db.batch()
                                    eligible.forEach { student ->
                                        val notifId = UUID.randomUUID().toString()
                                        val notif = Notification(
                                            notificationId = notifId,
                                            userId = student.uid,
                                            title = if (isUpdate) "Drive Updated: $companyName" else "New Drive: $companyName",
                                            message = if (isUpdate) "Details for $companyName have been updated. Check them out!" else "You are eligible for $companyName! Apply now for the ${roleOffered} role.",
                                            timestamp = com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                            type = if (isUpdate) "drive_update" else "new_drive"
                                        )
                                        val ref = db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId)
                                        batch.set(ref, notif)
                                    }
                                    
                                    batch.commit().addOnSuccessListener {
                                        android.util.Log.d("DriveNotif", "Broadcast sent to ${eligible.size} students")
                                        Toast.makeText(context, "System Alerts Sent! Opening Email... 🚀", Toast.LENGTH_SHORT).show()

                                        // 🔥 3. OPEN GMAIL INTENT FOR BROADCAST
                                        val emails = eligible.map { it.email }.filter { it.isNotBlank() }
                                        if (emails.isNotEmpty()) {
                                            val subject = if (isUpdate) "Drive Updated: $companyName" else "New Drive: $companyName"
                                            val body = "Hello Students,\n\n${if (isUpdate) "Details for $companyName have been updated." else "You are eligible for the vacancy at $companyName for the role of $roleOffered."}\n\nPlease check the PlacementPro app for more details and to apply.\n\nBest regards,\nPlacement Cell"
                                            
                                            val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:${emails.joinToString(",")}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                                            }
                                            context.startActivity(Intent.createChooser(selectorIntent, "Email Broadcast"))
                                        }
                                    }.addOnFailureListener {
                                        android.util.Log.e("DriveNotif", "Broadcast failed", it)
                                    }
                                }
                        }

                        fun saveToFirestore(finalJdUrl: String) {
                            val isoFormat = SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                                Locale.getDefault()
                            )
                            val auth = FirebaseAuth.getInstance()
                            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                            val nowIso = isoFormat.format(Date())

                            val driveMap = hashMapOf(
                                // 🌟 Primary Fields
                                "companyId" to id,
                                "companyName" to companyName.trim(),
                                "companyWebsite" to website.trim(),
                                "roleOffered" to roleOffered.trim(),
                                "jobDescription" to jobDescription.trim(),
                                "jdFileUrl" to finalJdUrl,
                                "workMode" to location.trim(),
                                "isActive" to true,

                                // 🕒 Legacy/External Sync Fields (The "Perfect Schema")
                                "id" to id,
                                "name" to companyName.trim(),
                                "role" to roleOffered.trim(),
                                "website" to website.trim(),
                                "status" to "active",
                                "description" to description.trim(),
                                "jdUrl" to finalJdUrl,
                                "updatedAt" to nowIso,

                                // 📊 Common Fields
                                "package" to (packageLPA.toDoubleOrNull() ?: 0.0),
                                "minCGPA" to (minCGPA.toDoubleOrNull() ?: 0.0),
                                "maxBacklogs" to (maxBacklogs.toIntOrNull() ?: 0),
                                "batchYear" to (batchYear.toIntOrNull() ?: 2026),
                                "deadline" to deadline.trim(),
                                "allowedBranches" to selectedBranches.toList(),
                                "createdAt" to (if (driveId == null) Timestamp.now() else null),
                                "createdBy" to auth.currentUser?.uid
                            ).filterValues { it != null }

                            db.collection(FirestoreCollections.COMPANIES).document(id).set(driveMap, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener {
                                    sendNotificationsToEligibleStudents(
                                        id, 
                                        companyName, 
                                        minCGPA.toDoubleOrNull() ?: 0.0, 
                                        maxBacklogs.toIntOrNull() ?: 0, 
                                        selectedBranches.toList(),
                                        isUpdate = driveId != null
                                    )
                                    isLoading = false
                                    Toast.makeText(context, if(driveId == null) "Drive Posted! 🚀" else "Drive Updated! ✅", Toast.LENGTH_LONG).show()
                                    onBack()
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }

                        if (jdUri != null) {
                            val fileRef = storage.reference.child("jds/$id.pdf")
                            fileRef.putFile(jdUri!!).addOnSuccessListener {
                                fileRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                    saveToFirestore(downloadUrl.toString())
                                }
                            }.addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "File upload failed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            saveToFirestore(currentJdUrl)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (driveId == null) "SUBMIT PLACEMENT DRIVE" else "UPDATE DRIVE DETAILS", fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun FormHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C375B),
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

