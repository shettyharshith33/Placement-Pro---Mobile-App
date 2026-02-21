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
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalUriHandler
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.*
import java.text.SimpleDateFormat
import java.util.*

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
    var myApplications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var studentProfile by remember { mutableStateOf<User?>(null) }
    var mentorshipSlots by remember { mutableStateOf<List<MentorSlot>>(emptyList()) }
    var myInterviews by remember { mutableStateOf<List<Interview>>(emptyList()) }
    var jobReferrals by remember { mutableStateOf<List<Referral>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Drives, 1: Applied, 2: Interviews, 3: Referrals, 4: Mentorship, 5: Profile

    // 🔥 1. FETCH STUDENT PROFILE (Real-time)
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection(FirestoreCollections.USERS).document(uid)
            .addSnapshotListener { snapshot, _ ->
                studentProfile = snapshot?.toObject(User::class.java)
            }
    }

    val context = LocalContext.current

    // 🔥 2. LISTEN TO MY APPLICATIONS (Always sync for button states)
    LaunchedEffect(studentProfile) {
        val uid = studentProfile?.uid ?: return@LaunchedEffect
        
        // Listen to applications
        db.collection(FirestoreCollections.APPLICATIONS)
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    myApplications = snapshot.toObjects(Application::class.java)
                }
            }
    }

    // 🔥 2.1 LISTEN TO REAL-TIME NOTIFICATIONS (System Alerts)
    val shownNotifications = remember { mutableStateOf(mutableSetOf<String>()) }
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        // 1-minute buffer to ensure we catch notifications sent just before/at app start
        val listenerStartTime = com.google.firebase.Timestamp(java.util.Date(System.currentTimeMillis() - 60000))
        android.util.Log.d("NotifSync", "Listener started for UID: $uid at $listenerStartTime")
        
        db.collection(FirestoreCollections.NOTIFICATIONS)
            .whereEqualTo("userId", uid)
            .whereGreaterThan("timestamp", listenerStartTime)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NotifSync", "Firestore Error", error)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val notifObject = change.document.toObject(Notification::class.java)
                        android.util.Log.d("NotifSync", "NOTIF RECEIVED: ${notifObject.title}")
                        
                        if (notifObject.notificationId.isNotBlank() && !shownNotifications.value.contains(notifObject.notificationId)) {
                            shownNotifications.value.add(notifObject.notificationId)
                            
                            com.shettyharshith33.placementpro.utils.NotificationHelper.showLocalNotification(
                                context,
                                notifObject.title,
                                notifObject.message
                            )
                        }
                    }
                }
            }
    }

    // 🔥 3. LISTEN TO OTHER DATA BASED ON TAB
    LaunchedEffect(studentProfile, selectedTab) {
        if (studentProfile == null) return@LaunchedEffect
        val profile = studentProfile!!
        val uid = profile.uid

        // 🔥 LISTEN TO MY INTERVIEWS (Always)
        db.collection(FirestoreCollections.INTERVIEWS)
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    myInterviews = snapshot.toObjects(Interview::class.java)
                }
            }

        when(selectedTab) {
            0 -> {
                db.collection(FirestoreCollections.COMPANIES)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val allDrives = snapshot.toObjects(CompanyDrive::class.java)
                            availableDrives = allDrives.filter { drive ->
                                // Local filtering for robustness
                                drive.isActiveCheckedCust && 
                                profile.finalCgpa >= drive.minCGPA && 
                                profile.finalBacklogs <= drive.maxBacklogs
                            }.sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }
            1 -> {
                isLoading = false
            }
            2 -> {
                // Interviews are already synced globally in the block above
                isLoading = false
            }
            3 -> {
                db.collection(FirestoreCollections.REFERRALS)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val allReferrals = snapshot.toObjects(Referral::class.java)
                            jobReferrals = allReferrals.sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                        }
                    }
            }
            4 -> {
                db.collection("mentorship_slots")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            mentorshipSlots = snapshot.toObjects(MentorSlot::class.java)
                                .sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }
            5 -> {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = { /* Implemented in PlacementProApp */ }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = navyBlue,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = navyBlue
                    )
                }) {
                val tabs = listOf("Drives", "Applied", "Interviews", "Referrals", "Mentorship", "Profile")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 14.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
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

                            DrivesListContent(availableDrives, studentProfile, myApplications)
                        }
                        1 -> ApplicationsListContent(myApplications)
                        2 -> MyInterviewsContent(myInterviews)
                        3 -> StudentReferralSection(jobReferrals, studentProfile) 
                        4 -> MentorshipSectionStudent(mentorshipSlots, studentProfile?.uid ?: "")
                        5 -> StudentProfileView(studentProfile, onLogout, onNavigateToResumeWizard)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentProfileView(user: User?, onLogout: () -> Unit, onEditResume: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    if (user == null) return

    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var cgpa by remember { mutableStateOf(user.finalCgpa.toString()) }
    var isEditing by remember { mutableStateOf(false) }

    val navyBlue = Color(0xFF1C375B)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        // (Existing Profile UI)
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
                    val cgpaVal = cgpa.toDoubleOrNull() ?: user.finalCgpa
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
                    ProfileItemRow("CGPA", user.finalCgpa.toString())
                    ProfileItemRow("Backlogs", user.finalBacklogs.toString())
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
                Text("RESUME WIZARD (UPDATE PDF)", color = Color.White)
            }
            

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)), // Red Logout
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SECURE LOGOUT")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DrivesListContent(drives: List<CompanyDrive>, student: User?, myApplications: List<Application>) {
    if (student == null) return
    if (drives.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No matching jobs for your criteria yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(drives) { drive ->
                val existingApp = myApplications.find { it.driveId == drive.finalId }
                DriveCard(drive, student, existingApp)
            }
        }
    }
}

@Composable
fun DriveCard(
    drive: CompanyDrive,
    student: User,
    existingApplication: Application? = null
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isApplying by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showJDDialog by remember { mutableStateOf(false) }
    val navyBlue = Color(0xFF1C375B)
    val isApplied = existingApplication != null

    if (showJDDialog) {
        AlertDialog(
            onDismissRequest = { showJDDialog = false },
            title = { Column {
                Text(drive.companyName, fontWeight = FontWeight.Bold)
                Text(drive.roleOffered, fontSize = 14.sp, color = Color.Gray)
            } },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (drive.finalWebsite.isNotBlank()) {
                        val uriHandler = LocalUriHandler.current
                        Text(
                            text = "Website: ${drive.finalWebsite}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp).clickable {
                                if (drive.finalWebsite.isNotEmpty()) {
                                    uriHandler.openUri(drive.finalWebsite)
                                }
                            }
                        )
                    }
                    Text("Location/Mode: ${drive.workMode}", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Job Description:", fontWeight = FontWeight.Bold)
                    Text(drive.jobDescription.ifBlank { drive.description.ifBlank { "No description provided." } })
                    
                    if (drive.deadline != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Deadline: ${drive.deadline}", color = Color.Red, fontWeight = FontWeight.Bold)
                    }

                    if (drive.jdFileUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(drive.finalJdUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open PDF. Ensure a PDF viewer is installed.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / View PDF JD")
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showJDDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = navyBlue)) { Text("Close") } }
        )
    }

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
                        val appId = "${student.uid}_${drive.finalId}"
                        val app = Application(
                            applicationId = appId,
                            driveId = drive.finalId,
                            studentId = student.uid,
                            studentName = student.name,
                            studentEmail = student.email,
                            studentResumeUrl = student.resumeUrl,
                            studentCgpa = student.cgpa,
                            companyName = drive.finalName,
                            roleOffered = drive.finalRole,
                            status = ApplicationStatus.APPLIED,
                            appliedAt = Timestamp.now()
                        )
                        db.collection(FirestoreCollections.APPLICATIONS).document(appId).set(app)
                            .addOnSuccessListener {
                                isApplying = false
                                Toast.makeText(context, "Successfully Applied!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                isApplying = false
                                Toast.makeText(context, "Apply failed: ${it.message}", Toast.LENGTH_LONG).show()
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
                    Text(drive.finalName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = navyBlue)
                    Text(drive.finalRole, color = Color.Gray)
                }
                Text("${drive.packageDouble} LPA", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(drive.description, maxLines = 2, fontSize = 14.sp)
            TextButton(onClick = { showJDDialog = true }, contentPadding = PaddingValues(0.dp)) {
                Text("View Details & JD", color = navyBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            
            if (isApplied) {
                Spacer(modifier = Modifier.height(4.dp))
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
                Text("Criteria: CGPA ≥ ${drive.minCGPA}", fontSize = 12.sp, color = Color.Gray)
                
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
fun ApplicationsListContent(apps: List<Application>) {
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
fun ApplicationCardItem(app: Application) {
    val statusColor = when(app.status) {
        ApplicationStatus.SELECTED -> Color(0xFF2E7D32)
        ApplicationStatus.REJECTED -> Color.Red
        ApplicationStatus.INTERVIEW -> Color(0xFF2E7D32)
        ApplicationStatus.HR, ApplicationStatus.TECHNICAL, ApplicationStatus.SHORTLISTED, ApplicationStatus.APTITUDE -> Color(0xFFE65100)
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
fun MentorshipSectionStudent(slots: List<MentorSlot>, studentId: String) {
    val navyBlue = Color(0xFF1C375B)
    val myBookedSlots = slots.filter { it.bookedByList.contains(studentId) }
    val availableSlots = slots.filter { !it.bookedByList.contains(studentId) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (myBookedSlots.isNotEmpty()) {
            Text("Your Joined Mentorships", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            myBookedSlots.forEach { slot ->
                YourMentorshipCard(slot)
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text("Available Mentorship Slots", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (availableSlots.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No more available slots.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                items(availableSlots) { slot ->
                    StudentSlotCard(slot, studentId)
                }
            }
        }
    }
}

@Composable
fun YourMentorshipCard(slot: MentorSlot) {
    val navyBlue = Color(0xFF1C375B)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(slot.topic, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = navyBlue)
            Text("Mentor: ${slot.alumniName}", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(slot.alumniEmail, fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(slot.alumniPhone, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = navyBlue)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${slot.availableDate} | ${slot.startTime} - ${slot.endTime}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Contact mentor for meeting details ✉️", fontSize = 11.sp, color = Color(0xFF1976D2), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun StudentSlotCard(slot: MentorSlot, studentId: String) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val navyBlue = Color(0xFF1C375B)
    val remainingSlots = slot.maxSlots - slot.bookedByList.size
    val isFull = remainingSlots <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isFull) Color(0xFFF5F5F5) else Color.White),
        elevation = CardDefaults.cardElevation(if (isFull) 0.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isFull) Color.LightGray.copy(alpha=0.5f) else Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(slot.topic, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isFull) Color.Gray else navyBlue)
                    Text("By ${slot.alumniName}", fontSize = 14.sp, color = Color.Gray)
                }
                if (isFull) {
                    Text("FULL", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Button(
                        onClick = {
                            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                            val studentName = auth.currentUser?.displayName ?: "Student"
                            
                            val newList = slot.bookedByList.toMutableList()
                            val newNames = slot.bookedByNames.toMutableList()
                            newList.add(studentId)
                            newNames.add(studentName)
                            
                            val fullyBooked = newList.size >= slot.maxSlots
                            
                            db.collection("mentorship_slots").document(slot.slotId)
                                .update(
                                    "bookedByList", newList, 
                                    "bookedByNames", newNames,
                                    "isBooked", fullyBooked
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Successfully joined session! 🚀", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Join ($remainingSlots left)", fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isFull) Color.LightGray else Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("${slot.availableDate} | ${slot.startTime} - ${slot.endTime}", fontSize = 14.sp, color = if (isFull) Color.LightGray else Color.Gray)
            }
        }
    }
}

@Composable
fun ProfileItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.Bold, color = Color(0xFF1C375B), fontSize = 13.sp)
    }
}

@Composable
fun MyInterviewsContent(interviews: List<com.shettyharshith33.placementpro.models.Interview>) {
    val navyBlue = Color(0xFF1C375B)
    if (interviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text("No interviews scheduled yet.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Upcoming Interviews", fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(interviews) { interview ->
                StudentInterviewCard(interview)
            }
        }
    }
}

@Composable
fun StudentInterviewCard(interview: Interview) {
    val navyBlue = Color(0xFF1C375B)
    val sdf = SimpleDateFormat("EEE, MMM dd | hh:mm a", Locale.getDefault())
    val dateStr = interview.slotTimeTimestamp?.let { sdf.format(it.toDate()) } ?: "TBD"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(interview.companyName, fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 16.sp)
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                    Text(interview.status, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Round: ${interview.round}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Time: $dateStr", fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Venue: ${interview.venue}", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StudentReferralSection(referrals: List<Referral>, studentProfile: User?) {
    if (referrals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No alumni referrals posted yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(referrals) { referral ->
                StudentReferralCard(referral, studentProfile)
            }
        }
    }
}

@Composable
fun StudentReferralCard(referral: Referral, studentProfile: User?) {
    val navyBlue = Color(0xFF1C375B)
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var isRequesting by remember { mutableStateOf(false) }
    var requestStatus by remember { mutableStateOf<String?>(null) } // To track if already requested

    // Check if already requested
    LaunchedEffect(referral.referralId, studentProfile?.uid) {
        if (studentProfile != null) {
            db.collection(FirestoreCollections.REFERRAL_REQUESTS)
                .whereEqualTo("referralId", referral.referralId)
                .whereEqualTo("studentId", studentProfile.uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.isEmpty) {
                        requestStatus = snapshot.documents[0].getString("status")
                    }
                }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    referral.companyName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = navyBlue
                )
                Surface(
                    color = (if (referral.finalIsActive) Color(0xFF2E7D32) else Color.Red).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (referral.finalIsActive) "Hiring" else "Filled",
                        color = if (referral.finalIsActive) Color(0xFF2E7D32) else Color.Red,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("Role: ${referral.role}", fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                referral.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = navyBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("By ${referral.alumniName}", fontSize = 12.sp, color = navyBlue)
                    }
                }

                if (requestStatus != null) {
                    Surface(
                        color = navyBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = requestStatus!!,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = navyBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else if (!referral.finalIsActive) {
                    Text(
                        "Sorry! No more referrals",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            if (studentProfile == null || studentProfile.resumeUrl.isBlank()) {
                                Toast.makeText(context, "Please complete your profile and upload resume first!", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isRequesting = true
                            val reqId = "req_${UUID.randomUUID().toString().take(6)}"
                            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                            isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                            val nowIso = isoFormat.format(Date())

                            val request = ReferralRequest(
                                requestId = reqId,
                                referralId = referral.referralId,
                                alumniId = referral.alumniId,
                                studentId = studentProfile.uid,
                                studentName = studentProfile.name,
                                studentResumeUrl = studentProfile.resumeUrl,
                                studentCgpa = studentProfile.cgpa,
                                companyName = referral.companyName,
                                role = referral.role,
                                status = "Pending",
                                requestedAt = nowIso
                            )
                            db.collection(FirestoreCollections.REFERRAL_REQUESTS).document(reqId).set(request)
                                .addOnSuccessListener {
                                    isRequesting = false
                                    requestStatus = "Pending"
                                    Toast.makeText(context, "Referral Requested! 🚀", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    isRequesting = false
                                    Toast.makeText(context, "Request failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isRequesting,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        if (isRequesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Request Referral", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
