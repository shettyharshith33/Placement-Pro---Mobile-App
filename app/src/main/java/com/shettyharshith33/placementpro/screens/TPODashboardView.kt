package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TPODashboardView(
    onNavigateToCreateDrive: (String?) -> Unit, onNavigateToScheduler: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val navyBlue = Color(0xFF1C375B)

    var companies by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var studentDetails by remember { mutableStateOf<Map<String, User>>(emptyMap()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Drives, 1: Applications
    var isLoading by remember { mutableStateOf(true) }
    var selectedAppForSchedule by remember { mutableStateOf<Application?>(null) }
    val context = LocalContext.current

    // 🔥 Listen to data
    LaunchedEffect(selectedTab) {
        isLoading = true
        
        // Listen for student details to avoid 0.0 CGPA bugs
        db.collection(FirestoreCollections.USERS).whereEqualTo("role", UserRole.STUDENT)
            .addSnapshotListener { userSnap, _ ->
                if (userSnap != null) {
                    val users = userSnap.toObjects(User::class.java).associateBy { it.uid }
                    studentDetails = users
                }
            }

        when (selectedTab) {
            0 -> {
                db.collection(FirestoreCollections.COMPANIES).addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        companies = snapshot.toObjects(CompanyDrive::class.java)
                            .sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                        isLoading = false
                    }
                }
            }
            1 -> {
                db.collection(FirestoreCollections.APPLICATIONS)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            applications = snapshot.toObjects(Application::class.java)
                                .sortedByDescending { it.appliedAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }
        }
    }

    Scaffold(
        containerColor = Color.White, 
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToCreateDrive(null) },
                    containerColor = navyBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create Drive") })
            }
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = navyBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = navyBlue
                    )
                }) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = {
                    Text("Drives", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = {
                    Text("Applicants", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = navyBlue)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when (selectedTab) {
                        0 -> {
                            TPOControlHeader(onNavigateToScheduler)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(companies) { company ->
                                    TPODriveCard(company, onEdit = { onNavigateToCreateDrive(company.finalId) })
                                }
                            }
                        }
                        1 -> TPOApplicationsContent(applications, studentDetails) { selectedAppForSchedule = it }
                    }
                }
            }
        }
    }

    if (selectedAppForSchedule != null) {
        TPOScheduleInterviewDialog(
            app = selectedAppForSchedule!!,
            onDismiss = { selectedAppForSchedule = null },
            onSchedule = { interview ->
                val batch = db.batch()
                val interviewRef = db.collection(FirestoreCollections.INTERVIEWS).document(interview.interviewId)
                val appRef = db.collection(FirestoreCollections.APPLICATIONS).document(selectedAppForSchedule!!.applicationId)

                batch.set(interviewRef, interview)
                batch.update(appRef, "status", ApplicationStatus.INTERVIEW)

                // 🔔 Notify Student
                val notifId = UUID.randomUUID().toString()
                val notif = Notification(
                    notificationId = notifId,
                    userId = selectedAppForSchedule!!.studentId,
                    title = "Interview Scheduled: ${selectedAppForSchedule!!.companyName}",
                    message = "Your interview for ${interview.round} is scheduled on ${SimpleDateFormat("MMM dd | hh:mm a", Locale.getDefault()).format(interview.slotTimeTimestamp!!.toDate())}",
                    timestamp = Timestamp.now(),
                    type = "interview_scheduled"
                )
                batch.set(db.collection(FirestoreCollections.NOTIFICATIONS).document(notifId), notif)

                batch.commit().addOnSuccessListener {
                    Toast.makeText(context, "Interview Scheduled Successfully", Toast.LENGTH_SHORT).show()
                    selectedAppForSchedule = null
                }
            }
        )
    }
}

@Composable
fun TPOControlHeader(onNavigateToScheduler: () -> Unit) {
    val navyBlue = Color(0xFF1C375B)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = navyBlue.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Pipeline Management", fontWeight = FontWeight.Bold, color = navyBlue)
            Text("Track candidates from aptitude to final selection.", fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Kudos your CGPA matches these companies")
        }
    }
}

@Composable
fun TPODriveCard(company: CompanyDrive, onEdit: () -> Unit) {
    val navyBlue = Color(0xFF1C375B)
    val db = FirebaseFirestore.getInstance()
    var eligibleCount by remember { mutableStateOf(0) }
    var showStats by remember { mutableStateOf(false) }
    var driveApplications by remember { mutableStateOf<List<Application>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(company.minCGPA, company.maxBacklogs) {
        db.collection(FirestoreCollections.USERS).whereEqualTo("role", UserRole.STUDENT)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val students = snapshot.toObjects(User::class.java)
                    val count = students.count { student ->
                        student.finalCgpa >= company.minCGPA && student.finalBacklogs <= company.maxBacklogs
                    }
                    eligibleCount = count
                }
            }
        
        db.collection(FirestoreCollections.APPLICATIONS)
            .whereEqualTo("driveId", company.finalId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    driveApplications = snapshot.toObjects(Application::class.java)
                }
            }
    }

    if (showStats) {
        AlertDialog(
            onDismissRequest = { showStats = false },
            title = { Text("${company.finalName} - Statistics", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val total = driveApplications.size
                    val selected = driveApplications.count { it.status == ApplicationStatus.SELECTED }
                    val rejected = driveApplications.count { it.status == ApplicationStatus.REJECTED }
                    Text("Total Applications: $total", fontWeight = FontWeight.SemiBold)
                    HorizontalDivider(thickness = 1.dp)
                    Text("Selected: $selected", color = Color(0xFF2E7D32))
                    Text("Rejected: $rejected", color = Color.Red)
                    Text("In Pipeline: ${total - selected - rejected}")
                }
            },
            confirmButton = { TextButton(onClick = { showStats = false }) { Text("Close") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(company.finalName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = navyBlue)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp), tint = navyBlue)
                }
            }
            Text(company.finalRole, fontWeight = FontWeight.Medium, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Package: ${company.packageDouble} LPA", fontWeight = FontWeight.SemiBold, color = navyBlue)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Criteria: CGPA \u2265 ${company.minCGPA}", fontSize = 12.sp)
                Surface(color = navyBlue.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
                    Text("$eligibleCount Eligible", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = navyBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showStats = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Stats", fontSize = 12.sp)
                }
                Button(
                    onClick = { Toast.makeText(context, "Notifications sent to $eligibleCount students!", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1.3f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = eligibleCount > 0
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Notify All", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun TPOApplicationsContent(apps: List<Application>, studentDetails: Map<String, User>, onSchedule: (Application) -> Unit) {
    val groupedApps = apps.groupBy { it.companyName }
    val navyBlue = Color(0xFF1C375B)

    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No applications yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            groupedApps.forEach { (companyName, companyApps) ->
                item {
                    var isExpanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = if (isExpanded) navyBlue else navyBlue.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(companyName, fontWeight = FontWeight.Bold, color = if (isExpanded) Color.White else navyBlue, fontSize = 18.sp)
                                    Text("${companyApps.size} Applicants", fontSize = 12.sp, color = if (isExpanded) Color.White.copy(alpha = 0.7f) else Color.Gray)
                                }
                                Icon(imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = if (isExpanded) Color.White else navyBlue)
                            }
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                companyApps.forEach { app ->
                                    ApplicationTpoCard(app, studentDetails[app.studentId], onSchedule)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationTpoCard(app: Application, profile: User?, onSchedule: (Application) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val navyBlue = Color(0xFF1C375B)
    val context = LocalContext.current
    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
    
    val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val appliedDate = app.appliedAtTimestamp?.let { sdf.format(it.toDate()) } ?: "N/A"
    val displayCgpa = profile?.finalCgpa ?: app.finalCgpa
    val displayBranch = profile?.branch ?: "CSE"

    var showStatusMenu by remember { mutableStateOf(false) }
    var statusToConfirm by remember { mutableStateOf<String?>(null) }

    if (statusToConfirm != null) {
        AlertDialog(
            onDismissRequest = { statusToConfirm = null },
            title = { Text("Update Status") },
            text = { Text("Change ${app.studentName}'s status to '$statusToConfirm'?") },
            confirmButton = {
                Button(onClick = {
                    val newStatus = statusToConfirm!!
                    db.collection(FirestoreCollections.APPLICATIONS).document(app.applicationId).update("status", newStatus)
                    statusToConfirm = null
                    Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show()
                }, colors = ButtonDefaults.buttonColors(containerColor = navyBlue)) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { statusToConfirm = null }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(app.companyName, fontWeight = FontWeight.ExtraBold, color = navyBlue, fontSize = 16.sp)
                    Text(app.roleOffered, color = Color.Gray, fontSize = 14.sp)
                }
                Surface(
                    onClick = { showStatusMenu = true },
                    color = if (app.status == ApplicationStatus.SELECTED) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(app.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = navyBlue)
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        listOf(ApplicationStatus.APPLIED, ApplicationStatus.SHORTLISTED, ApplicationStatus.TECHNICAL, ApplicationStatus.HR, ApplicationStatus.SELECTED, ApplicationStatus.REJECTED).forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = { showStatusMenu = false; statusToConfirm = s })
                        }
                    }
                }
            }
            Text("Candidate: ${app.studentName}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text("CGPA: $displayCgpa • Branch: $displayBranch", fontSize = 13.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    if (app.studentResumeUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.studentResumeUrl)))
                }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Resume", fontSize = 11.sp) }
                
                Button(onClick = { onSchedule(app) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), shape = RoundedCornerShape(8.dp)) {
                    Text("Schedule", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun TPOScheduleInterviewDialog(app: Application, onDismiss: () -> Unit, onSchedule: (Interview) -> Unit) {
    val navyBlue = Color(0xFF1C375B)
    val context = LocalContext.current
    var round by remember { mutableStateOf("Technical Interview") }
    var venue by remember { mutableStateOf("Placement Office") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var dateString by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("") }
    val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val datePicker = DatePickerDialog(context, { _, y, m, d ->
        selectedDate.set(y, m, d); dateString = sdfDate.format(selectedDate.time)
    }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH))

    val timePicker = TimePickerDialog(context, { _, h, min ->
        selectedDate.set(Calendar.HOUR_OF_DAY, h); selectedDate.set(Calendar.MINUTE, min); timeString = sdfTime.format(selectedDate.time)
    }, selectedDate.get(Calendar.HOUR_OF_DAY), selectedDate.get(Calendar.MINUTE), false)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule: ${app.studentName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(round, { round = it }, label = { Text("Round") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(venue, { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { datePicker.show() }, modifier = Modifier.weight(1f)) { Text(if(dateString.isEmpty()) "Date" else dateString) }
                    OutlinedButton(onClick = { timePicker.show() }, modifier = Modifier.weight(1f)) { Text(if(timeString.isEmpty()) "Time" else timeString) }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (dateString.isEmpty() || timeString.isEmpty()) return@Button
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
            }, colors = ButtonDefaults.buttonColors(containerColor = navyBlue)) { Text("Confirm") }
        }
    )
}
