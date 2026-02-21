package com.shettyharshith33.placementpro.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.shettyharshith33.placementpro.models.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniDashboardView() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var referrals by remember { mutableStateOf<List<Referral>>(emptyList()) }
    var mentorshipSlots by remember {
        mutableStateOf<List<MentorSlot>>(
            emptyList()
        )
    }
    var alumniProfile by remember { mutableStateOf<User?>(null) }
    var referralRequests by remember { mutableStateOf<List<ReferralRequest>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Job Board, 1: Requests, 2: My Slots, 3: Profile
    var showPostDialog by remember { mutableStateOf(false) }
    var showSlotDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val navyBlue = Color(0xFF1C375B)

    // 🔥 FETCH PROFILE
    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection(FirestoreCollections.USERS).document(uid)
            .addSnapshotListener { snapshot, _ ->
                alumniProfile = snapshot?.toObject(User::class.java)
            }
    }

    LaunchedEffect(selectedTab) {
        isLoading = true
        when (selectedTab) {
            0 -> {
                db.collection(FirestoreCollections.REFERRALS)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val allReferrals = snapshot.toObjects(Referral::class.java)
                            referrals = allReferrals.sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }

            1 -> {
                db.collection(FirestoreCollections.REFERRAL_REQUESTS)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            val currentUid = auth.currentUser?.uid ?: ""
                            val allRequests = snapshot.toObjects(ReferralRequest::class.java)
                            referralRequests = allRequests.filter { it.alumniId == currentUid }
                                .sortedByDescending { it.requestedAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }

            2 -> {
                db.collection("mentorship_slots")
                    .whereEqualTo("alumniId", auth.currentUser?.uid ?: "")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            mentorshipSlots =
                                snapshot.toObjects(MentorSlot::class.java)
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
        containerColor = Color.White,
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 2) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) showPostDialog = true else showSlotDialog = true
                    },
                    containerColor = navyBlue,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Job Board",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Verify Requests",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    })
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "My Slots",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    })
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            "Profile",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = navyBlue)
                }
            } else {
                Column(modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                    when (selectedTab) {
                        0 -> ReferralSection(referrals)
                        1 -> VerifyReferralSection(referralRequests)
                        2 -> MentorshipSectionAlumni(mentorshipSlots)
                        3 -> AlumniProfileView(alumniProfile)
                    }
                }
            }
        }
    }

    if (showSlotDialog) {
        AddSlotDialog(
            onDismiss = { showSlotDialog = false },
            onSave = { topic, date, start, end, maxSlots ->
                val id = UUID.randomUUID().toString()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val nowIso = isoFormat.format(Date())

                val slot = MentorSlot(
                    slotId = id,
                    alumniId = auth.currentUser?.uid ?: "",
                    alumniName = alumniProfile?.name ?: "Alumni",
                    alumniEmail = alumniProfile?.email ?: "",
                    alumniPhone = alumniProfile?.phone ?: "",
                    topic = topic,
                    availableDate = date,
                    startTime = start,
                    endTime = end,
                    maxSlots = maxSlots,
                    bookedByList = emptyList(),
                    isBooked = false,
                    createdAt = nowIso
                )
                db.collection("mentorship_slots").document(id).set(slot)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Mentorship session posted with $maxSlots slots!", Toast.LENGTH_SHORT).show()
                    }
                showSlotDialog = false
            }
        )
    }

    if (showPostDialog) {
        PostReferralDialog(
            onDismiss = { showPostDialog = false },
            onPost = { company, role, desc ->
                val id = UUID.randomUUID().toString()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                isoFormat.timeZone = TimeZone.getTimeZone("UTC")
                val nowIso = isoFormat.format(Date())
                
                // Default deadline: 30 days from now
                val deadlineDate = Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
                val deadlineIso = isoFormat.format(deadlineDate)

                val referral = Referral(
                    referralId = id,
                    alumniId = auth.currentUser?.uid ?: "",
                    alumniName = alumniProfile?.name ?: "Alumni",
                    companyName = company,
                    role = role,
                    description = desc,
                    createdAt = nowIso,
                    deadline = deadlineIso,
                    isActiveState = true,
                    activeState = true
                )
                db.collection(FirestoreCollections.REFERRALS).document(id).set(referral)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Job posted successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                showPostDialog = false
            }
        )
    }
}

@Composable
fun ReferralSection(referrals: List<Referral>) {
    if (referrals.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No referrals posted yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(referrals) { referral ->
                ReferralCard(referral)
            }
        }
    }
}

@Composable
fun ReferralCard(referral: Referral) {
    val navyBlue = Color(0xFF1C375B)
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
                        if (referral.finalIsActive) "Hiring" else "Closed/Filled",
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = navyBlue
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Posted by ${referral.alumniName}", fontSize = 12.sp, color = navyBlue)
            }
        }
    }
}

@Composable
fun MentorshipSectionAlumni(slots: List<MentorSlot>) {
    val navyBlue = Color(0xFF1C375B)
    if (slots.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    Color.LightGray
                )
                Text("No availability set.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(slots) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        if (slot.isBooked) Color.Red.copy(alpha = 0.3f) else Color.LightGray
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(slot.topic, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = navyBlue)
                            Surface(
                                color = if (slot.isBooked) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${slot.bookedByList.size} / ${slot.maxSlots} Booked",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = if (slot.isBooked) Color(0xFFC62828) else Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${slot.availableDate} | ${slot.startTime} - ${slot.endTime}", fontSize = 14.sp, color = Color.Gray)
                        }
                        if (slot.bookedByNames.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Students: ${slot.bookedByNames.joinToString(", ")}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = navyBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlumniProfileView(user: User?) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    if (user == null) return

    var name by remember { mutableStateOf(user.name) }
    var phone by remember { mutableStateOf(user.phone) }
    var isEditing by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState()), 
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = Color(0xFF1C375B).copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(20.dp),
                tint = Color(0xFF1C375B)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    db.collection(FirestoreCollections.USERS).document(user.uid)
                        .update("name", name, "phone", phone)
                        .addOnSuccessListener {
                            isEditing = false
                            Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C375B))
            ) {
                Text("SAVE CHANGES")
            }
            TextButton(onClick = { isEditing = false }) {
                Text("Cancel", color = Color.Gray)
            }
        } else {
            Text(
                user.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C375B)
            )
            Text(user.email, color = Color.Gray)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItemRow("Role", user.role.uppercase())
                    ProfileItemRow(
                        "Phone",
                        if (user.phone.isBlank()) "Not provided" else user.phone
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { isEditing = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("EDIT PROFILE")
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSlotDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, Int) -> Unit) {
    var topic by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var maxSlots by remember { mutableStateOf("1") }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            date = String.format("%04d-%02d-%02d", year, month + 1, day)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun showTimePicker(onSelection: (String) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onSelection(String.format("%02d:%02d", hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Mentorship Session") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = topic, 
                    onValueChange = { topic = it }, 
                    label = { Text("Topic (e.g. Group Resume Check)") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Available Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = {},
                        label = { Text("Start Time") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker { start = it } }) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = {},
                        label = { Text("End Time") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = { showTimePicker { end = it } }) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = maxSlots, 
                    onValueChange = { if (it.all { char -> char.isDigit() }) maxSlots = it }, 
                    label = { Text("Number of Slots (Capacity)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val slotsInt = maxSlots.toIntOrNull() ?: 1
                    if (topic.isNotBlank() && date.isNotBlank() && start.isNotBlank()) {
                        onSave(topic, date, start, end, slotsInt) 
                    } else {
                        Toast.makeText(context, "Please complete all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C375B))
            ) {
                Text("Post Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun VerifyReferralSection(requests: List<ReferralRequest>) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No referral requests yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(requests) { request ->
                ReferralRequestCard(request)
            }
        }
    }
}

@Composable
fun ReferralRequestCard(request: ReferralRequest) {
    val navyBlue = Color(0xFF1C375B)
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(request.studentName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = navyBlue)
                    Text("Requested for: ${request.companyName}", fontSize = 14.sp, color = Color.Gray)
                }
                Surface(
                    color = when(request.status) {
                        "Approved" -> Color(0xFFE8F5E9)
                        "Rejected" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFF5F5F5)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        request.status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = when(request.status) {
                            "Approved" -> Color(0xFF2E7D32)
                            "Rejected" -> Color(0xFFC62828)
                            else -> Color.DarkGray
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("CGPA: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(request.finalCgpa.toString(), fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (request.studentResumeUrl.isNotBlank()) {
                         try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(request.studentResumeUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open resume link", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No resume link provided", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Student Resume")
            }
            
            if (request.status == "Pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            db.collection(FirestoreCollections.REFERRAL_REQUESTS).document(request.requestId)
                                .update("status", "Approved")
                            
                            // Close the parent referral so no one else can apply
                            if (request.referralId.isNotBlank()) {
                                db.collection(FirestoreCollections.REFERRALS).document(request.referralId)
                                    .update("isActiveState", false, "activeState", false)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Approve")
                    }
                    OutlinedButton(
                        onClick = {
                            db.collection(FirestoreCollections.REFERRAL_REQUESTS).document(request.requestId)
                                .update("status", "Rejected")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reject", color = Color.Red)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostReferralDialog(onDismiss: () -> Unit, onPost: (String, String, String) -> Unit) {
    var company by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post referral opportunity") },
        text = {
            Column {
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Role / Position") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Short Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (company.isNotBlank()) onPost(company, role, desc) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C375B))
            ) {
                Text("Post Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}