package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.CompanyDrive
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.User

@Composable
fun StudentDashboard(
    onLogout: () -> Unit,
    onNavigateToMarket: () -> Unit = {},
    onNavigateToResumeWizard: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val navyBlue = Color(0xFF1C375B)

    var availableDrives by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    var myApplications by remember { mutableStateOf<List<com.shettyharshith33.placementpro.models.Application>>(emptyList()) }
    var studentProfile by remember { mutableStateOf<User?>(null) }
    var mentorshipSlots by remember { mutableStateOf<List<com.shettyharshith33.placementpro.models.MentorSlot>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Drives, 1: Applied, 2: Mentorship, 3: Profile

    // 🔥 1. FETCH STUDENT PROFILE (Real-time)
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection(FirestoreCollections.USERS).document(uid)
            .addSnapshotListener { snapshot, _ ->
                studentProfile = snapshot?.toObject(User::class.java)
            }
    }

    // 🔥 2. LISTEN TO DATA BASED ON ELIGIBILITY
    LaunchedEffect(studentProfile, selectedTab) {
        if (studentProfile == null) return@LaunchedEffect
        val profile = studentProfile!!
        val uid = profile.uid

        when(selectedTab) {
            0 -> {
                db.collection(FirestoreCollections.COMPANIES)
                    .whereEqualTo("isActive", true)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val allDrives = snapshot.toObjects(CompanyDrive::class.java)
                            // 🧠 Filter locally for exact eligibility matching
                            availableDrives = allDrives.filter { drive ->
                                profile.cgpa >= drive.minCGPA && profile.backlogs <= drive.maxBacklogs
                            }.sortedByDescending { it.createdAt }
                            isLoading = false
                        }
                    }
            }
            1 -> {
                db.collection(FirestoreCollections.APPLICATIONS)
                    .whereEqualTo("studentId", uid)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            myApplications = snapshot.toObjects(com.shettyharshith33.placementpro.models.Application::class.java)
                            isLoading = false
                        }
                    }
            }
            2 -> {
                db.collection("mentorship_slots")
                    .whereEqualTo("isBooked", false)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            mentorshipSlots = snapshot.toObjects(com.shettyharshith33.placementpro.models.MentorSlot::class.java)
                            isLoading = false
                        }
                    }
            }
            3 -> {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = navyBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = navyBlue
                    )
                }
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; isLoading = true }, text = { Text("Jobs", fontWeight = if(selectedTab==0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; isLoading = true }, text = { Text("Applied", fontWeight = if(selectedTab==1) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2; isLoading = true }, text = { Text("Mentor", fontWeight = if(selectedTab==2) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3; isLoading = true }, text = { Text("Profile", fontWeight = if(selectedTab==3) FontWeight.Bold else FontWeight.Normal) })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = navyBlue)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when(selectedTab) {
                        0 -> {
                            // 🔥 KUDOS MESSAGE
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Kudos! You are eligible for the following companies based on your academic profile.",
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Button(
                                onClick = onNavigateToMarket,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check Market Match (Skill Analysis)")
                            }
                            DrivesListContent(availableDrives, studentProfile, myApplications)
                        }
                        1 -> ApplicationsListContent(myApplications)
                        2 -> MentorshipSectionStudent(mentorshipSlots, studentProfile?.uid ?: "")
                        3 -> StudentProfileView(studentProfile, onNavigateToResumeWizard)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileView(user: User?, onEditResume: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    if (user == null) return

    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var cgpa by remember { mutableStateOf(user.cgpa.toString()) }
    var isEditing by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF1C375B)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(90.dp),
            shape = CircleShape,
            color = navyBlue.copy(alpha = 0.1f)
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = navyBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (isEditing) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = cgpa, onValueChange = { cgpa = it }, label = { Text("Current CGPA") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val cgpaVal = cgpa.toDoubleOrNull() ?: user.cgpa
                    db.collection(FirestoreCollections.USERS).document(user.uid)
                        .update("name", name, "phone", phone, "cgpa", cgpaVal)
                        .addOnSuccessListener {
                            isEditing = false
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
            ) {
                Text("SAVE PROFILE")
            }
            TextButton(onClick = { isEditing = false }) { Text("Cancel") }
        } else {
            Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = navyBlue)
            Text(user.email, color = Color.Gray, fontSize = 14.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { isEditing = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray.copy(alpha=0.5f)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Personal Details", color = Color.Black, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItemRow("Roll Number", user.rollNumber)
                    ProfileItemRow("Branch", user.branch)
                    ProfileItemRow("CGPA", user.cgpa.toString())
                    ProfileItemRow("Backlogs", user.backlogs.toString())
                    ProfileItemRow("Phone", user.phone)
                    
                    if (user.skills.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Skills", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 12.sp)
                        Text(user.skills.joinToString(", "), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onEditResume,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Create, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RESUME WIZARD (UPDATE PDF)")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { com.shettyharshith33.placementpro.utils.ResumeGenerator.generateResumePdf(context, user) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("DOWNLOAD BRANDED PDF")
            }
        }
    }
}

@Composable
fun DrivesListContent(drives: List<CompanyDrive>, student: User?, myApplications: List<com.shettyharshith33.placementpro.models.Application>) {
    if (student == null) return
    if (drives.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No matching jobs for your criteria yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(drives) { drive ->
                val application = myApplications.find { it.driveId == drive.companyId }
                DriveCard(drive, student, application)
            }
        }
    }
}

@Composable
fun DriveCard(
    drive: CompanyDrive,
    student: User,
    existingApplication: com.shettyharshith33.placementpro.models.Application? = null
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isApplying by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val navyBlue = Color(0xFF1C375B)

    val isApplied = existingApplication != null

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Application") },
            text = { Text("Are you sure you want to apply for the ${drive.roleOffered} role at ${drive.companyName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        isApplying = true
                        val appId = "${student.uid}_${drive.companyId}"
                        val app = com.shettyharshith33.placementpro.models.Application(
                            applicationId = appId,
                            driveId = drive.companyId,
                            studentId = student.uid,
                            studentName = student.name,
                            studentResumeUrl = student.resumeUrl,
                            studentCgpa = student.cgpa,
                            companyName = drive.companyName,
                            roleOffered = drive.roleOffered,
                            appliedAt = Timestamp.now()
                        )
                        db.collection(FirestoreCollections.APPLICATIONS).document(appId).set(app)
                            .addOnSuccessListener {
                                isApplying = false
                                Toast.makeText(context, "Successfully Applied!", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
                ) {
                    Text("Yes, Apply", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(drive.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = navyBlue)
                    Text(drive.roleOffered, color = Color.Gray)
                }
                Text("${drive.packageLPA} LPA", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(drive.description, maxLines = 2, fontSize = 14.sp)
            
            if (isApplied) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Status: ${existingApplication?.status}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = navyBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Criteria: CGPA ≥ ${drive.minCGPA} | Backlogs ≤ ${drive.maxBacklogs}", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = {
                        if (isApplied) return@Button
                        
                        // Trigger Vibration
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(50)
                        }
                        showConfirmDialog = true
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApplied) Color.Gray else navyBlue
                    ),
                    enabled = !isApplying && !isApplied
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (isApplied) "Applied" else "Apply Now", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationsListContent(apps: List<com.shettyharshith33.placementpro.models.Application>) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Your applications will appear here.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app ->
                ApplicationCardItem(app)
            }
        }
    }
}

@Composable
fun ApplicationCardItem(app: com.shettyharshith33.placementpro.models.Application) {
    val statusColor = when(app.status) {
        "Selected" -> Color(0xFF2E7D32)
        "Rejected" -> Color.Red
        "Interview Scheduled" -> Color(0xFF1C375B)
        else -> Color.Gray
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.companyName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(app.roleOffered, color = Color.Gray)
            }
            Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(app.status, color = statusColor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MentorshipSectionStudent(slots: List<com.shettyharshith33.placementpro.models.MentorSlot>, studentId: String) {
    if (slots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No alumni slots available right now.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(slots) { slot ->
                StudentSlotCard(slot, studentId)
            }
        }
    }
}

@Composable
fun StudentSlotCard(slot: com.shettyharshith33.placementpro.models.MentorSlot, studentId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val navyBlue = Color(0xFF1C375B)

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(slot.alumniName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Available: ${slot.availableTime}", color = navyBlue, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    db.collection("mentorship_slots").document(slot.slotId)
                        .update("isBooked", true, "bookedBy", studentId)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Session Booked! Check your email for details.", Toast.LENGTH_LONG).show()
                        }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Book Slot")
            }
        }
    }
}