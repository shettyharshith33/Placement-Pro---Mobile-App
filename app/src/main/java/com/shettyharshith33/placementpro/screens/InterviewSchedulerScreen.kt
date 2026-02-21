package com.shettyharshith33.placementpro.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSchedulerScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var interviews by remember { mutableStateOf<List<Interview>>(emptyList()) }
    // Cache for student details to avoid 0.0 CGPA issue
    var studentDetails by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    
    var selectedTab by remember { mutableStateOf(0) } // 0: Pending, 1: Scheduled
    val navyBlue = Color(0xFF1C375B)
    val context = LocalContext.current

    var selectedAppForSchedule by remember { mutableStateOf<Application?>(null) }

    // Listen to Data
    LaunchedEffect(Unit) {
        // 1. Listen to relevant applications
        db.collection(FirestoreCollections.USERS).whereEqualTo("role", UserRole.STUDENT)
            .addSnapshotListener { userSnap, _ ->
                if (userSnap != null) {
                    val users = userSnap.toObjects(User::class.java).associateBy { it.uid }
                    studentDetails = users
                }
            }

        db.collection(FirestoreCollections.APPLICATIONS)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val allApps = snapshot.toObjects(Application::class.java)
                    applications = allApps.filter { app ->
                        app.status == ApplicationStatus.SHORTLISTED ||
                        app.status == ApplicationStatus.TECHNICAL ||
                        app.status == ApplicationStatus.HR ||
                        app.status == ApplicationStatus.APTITUDE
                    }
                }
            }
        
        db.collection(FirestoreCollections.INTERVIEWS)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val allInterviews = snapshot.toObjects(Interview::class.java)
                    interviews = allInterviews.sortedByDescending { it.slotTimeTimestamp?.seconds ?: 0L }
                }
            }
    }

    if (selectedAppForSchedule != null) {
        InterviewSchedulerDialog(
            app = selectedAppForSchedule!!,
            onDismiss = { selectedAppForSchedule = null },
            onSchedule = { interview ->
                val batch = db.batch()
                val interviewRef = db.collection(FirestoreCollections.INTERVIEWS).document(interview.interviewId)
                val appRef = db.collection(FirestoreCollections.APPLICATIONS).document(selectedAppForSchedule!!.applicationId)

                // Enforce roleOffered sync
                val finalInterview = interview.copy(roleOffered = selectedAppForSchedule!!.roleOffered)

                batch.set(interviewRef, finalInterview)
                batch.update(appRef, "status", ApplicationStatus.INTERVIEW)

                // 🔔 Notify Student
                val notifId = UUID.randomUUID().toString()
                val notif = Notification(
                    notificationId = notifId,
                    userId = selectedAppForSchedule!!.studentId,
                    title = "Interview Scheduled: ${selectedAppForSchedule!!.companyName}",
                    message = "Your interview for ${finalInterview.round} (${finalInterview.roleOffered}) is scheduled on ${SimpleDateFormat("MMM dd | hh:mm a", Locale.getDefault()).format(finalInterview.slotTimeTimestamp!!.toDate())} at ${finalInterview.venue}",
                    timestamp = Timestamp.now(),
                    type = "interview_scheduled"
                )
                batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), notif)

                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, "Interview Scheduled for ${selectedAppForSchedule!!.studentName}", Toast.LENGTH_SHORT).show()
                    selectedAppForSchedule = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interview Scheduler Console", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyBlue)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = navyBlue) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("To Be Scheduled (${applications.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Upcoming Interviews (${interviews.size})") })
            }

            if (selectedTab == 0) {
                if (applications.isEmpty()) {
                    EmptyState("No candidates waiting for scheduling.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(applications) { app ->
                            val profile = studentDetails[app.studentId]
                            CandidateCard(app, profile) { selectedAppForSchedule = app }
                        }
                    }
                }
            } else {
                if (interviews.isEmpty()) {
                    EmptyState("No upcoming interviews found.")
                } else {
                    // 🔥 Create a lookup for roles based on driveId from applications
                    val driveRoleMap = applications.associate { it.driveId to it.roleOffered }
                    
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(interviews) { interview ->
                            // Dynamically fix the "Role: N/A" if the interview object is missing it
                            val displayRole = if (interview.roleOffered.isBlank()) {
                                driveRoleMap[interview.companyId] ?: "Technical Role"
                            } else interview.roleOffered
                            
                            InterviewCard(interview.copy(roleOffered = displayRole))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterviewSchedulerDialog(
    app: Application,
    onDismiss: () -> Unit,
    onSchedule: (Interview) -> Unit
) {
    val navyBlue = Color(0xFF1C375B)
    val context = LocalContext.current
    
    // Smart default for round
    val defaultRound = when(app.status) {
        ApplicationStatus.TECHNICAL -> "Technical Interview"
        ApplicationStatus.HR -> "HR Interview"
        ApplicationStatus.APTITUDE -> "Aptitude Test"
        else -> "Technical Interview"
    }
    
    var round by remember { mutableStateOf(defaultRound) }
    var venue by remember { mutableStateOf("Seminar Hall 1") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var dateString by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("") }

    val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDate.set(Calendar.YEAR, year)
            selectedDate.set(Calendar.MONTH, month)
            selectedDate.set(Calendar.DAY_OF_MONTH, day)
            dateString = sdfDate.format(selectedDate.time)
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    val timePicker = TimePickerDialog(
        context,
        { _, hour, minute ->
            selectedDate.set(Calendar.HOUR_OF_DAY, hour)
            selectedDate.set(Calendar.MINUTE, minute)
            timeString = sdfTime.format(selectedDate.time)
        },
        selectedDate.get(Calendar.HOUR_OF_DAY),
        selectedDate.get(Calendar.MINUTE),
        false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Interview: ${app.studentName}", fontWeight = FontWeight.Bold, color = navyBlue) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Role: ${app.roleOffered} at ${app.companyName}", fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(round, { round = it }, label = { Text("Interview Round (e.g. Technical/HR)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(venue, { venue = it }, label = { Text("Venue / Online Link") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = {},
                        label = { Text("Date") },
                        modifier = Modifier.weight(1f).clickable { datePicker.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray)
                    )
                    OutlinedTextField(
                        value = timeString,
                        onValueChange = {},
                        label = { Text("Time") },
                        modifier = Modifier.weight(1f).clickable { timePicker.show() },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dateString.isBlank() || timeString.isBlank()) {
                        Toast.makeText(context, "Please select date and time", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val interview = Interview(
                        interviewId = "int_${UUID.randomUUID().toString().take(6)}",
                        companyId = app.driveId,
                        companyName = app.companyName,
                        roleOffered = app.roleOffered,
                        round = round,
                        slotTime = Timestamp(selectedDate.time),
                        studentId = app.studentId,
                        studentName = app.studentName,
                        venue = venue
                    )
                    onSchedule(interview)
                },
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
            ) { Text("Confirm Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CandidateCard(app: Application, profile: User?, onAssign: () -> Unit) {
    val navyBlue = Color(0xFF1C375B)
    
    // 🔥 Prioritize data from User profile, fallback to Application document
    val displayCgpa = profile?.finalCgpa ?: app.finalCgpa
    val displayBranch = profile?.branch ?: "CSE" // Default to CSE if not found
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile?.name ?: app.studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = navyBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.companyName, color = navyBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(" • ", color = Color.Gray)
                    Text(app.roleOffered, color = Color.Gray, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = navyBlue.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                        Text("CGPA: $displayCgpa", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 12.sp)
                    }
                    Surface(color = Color.Gray.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp)) {
                        Text(displayBranch, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }
            Button(
                onClick = onAssign,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InterviewCard(interview: Interview) {
    val navyBlue = Color(0xFF1C375B)
    val sdf = SimpleDateFormat("EEE, MMM dd | hh:mm a", Locale.getDefault())
    val dateStr = interview.slotTimeTimestamp?.let { sdf.format(it.toDate()) } ?: "TBD"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(interview.companyName, fontWeight = FontWeight.Bold, color = navyBlue, fontSize = 16.sp)
                    Text("Role: ${interview.roleOffered.ifBlank { "N/A" }}", fontSize = 12.sp, color = Color.Gray)
                }
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                    Text(interview.status, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            InfoRow(Icons.Default.Person, "Student: ${interview.studentName}")
            InfoRow(Icons.Default.DateRange, "Timing: $dateStr")
            InfoRow(Icons.Default.LocationOn, "Venue: ${interview.venue}")
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = navyBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("ROUND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = navyBlue)
                        Text(interview.round, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = Color.DarkGray)
    }
}

/*private fun Modifier.size(dp: androidx.compose.ui.unit.Dp) = this.then(Modifier.size(dp))*/

@Composable
fun EmptyState(msg: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = Color.Gray)
    }
}
