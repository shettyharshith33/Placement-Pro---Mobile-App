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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.Referral
import com.shettyharshith33.placementpro.models.User
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumniDashboardView() {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var referrals by remember { mutableStateOf<List<Referral>>(emptyList()) }
    var mentorshipSlots by remember {
        mutableStateOf<List<com.shettyharshith33.placementpro.models.MentorSlot>>(
            emptyList()
        )
    }
    var alumniProfile by remember { mutableStateOf<User?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Job Board, 1: My Slots, 2: Profile
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
                    .whereEqualTo("isActive", true)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            referrals = snapshot.toObjects(Referral::class.java)
                                .sortedByDescending { it.createdAtTimestamp?.seconds ?: 0L }
                            isLoading = false
                        }
                    }
            }

            1 -> {
                db.collection("mentorship_slots")
                    .whereEqualTo("alumniId", auth.currentUser?.uid ?: "")
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            mentorshipSlots =
                                snapshot.toObjects(com.shettyharshith33.placementpro.models.MentorSlot::class.java)
                            isLoading = false
                        }
                    }
            }

            2 -> {
                isLoading = false
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
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
                            "My Slots",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    })
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Profile",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
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
                        1 -> MentorshipSectionAlumni(mentorshipSlots)
                        2 -> AlumniProfileView(alumniProfile)
                    }
                }
            }
        }
    }

    if (showSlotDialog) {
        AddSlotDialog(
            onDismiss = { showSlotDialog = false },
            onSave = { time ->
                val id = UUID.randomUUID().toString()
                val slot = com.shettyharshith33.placementpro.models.MentorSlot(
                    slotId = id,
                    alumniId = auth.currentUser?.uid ?: "",
                    alumniName = alumniProfile?.name ?: "Alumni",
                    availableTime = time,
                    createdAt = com.google.firebase.Timestamp.now()
                )
                db.collection("mentorship_slots").document(id).set(slot)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Slot added successfully", Toast.LENGTH_SHORT)
                            .show()
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
                val referral = Referral(
                    referralId = id,
                    alumniId = auth.currentUser?.uid ?: "",
                    alumniName = alumniProfile?.name ?: "Alumni",
                    companyName = company,
                    role = role,
                    description = desc,
                    createdAt = com.google.firebase.Timestamp.now(),
                    isActive = true
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
                    color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Hiring",
                        color = Color(0xFF2E7D32),
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
fun MentorshipSectionAlumni(slots: List<com.shettyharshith33.placementpro.models.MentorSlot>) {
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
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(slot.availableTime, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (slot.isBooked) "Booked by Student" else "Open for Booking",
                                color = if (slot.isBooked) Color.Red else Color(0xFF2E7D32),
                                fontSize = 14.sp
                            )
                        }
                        if (slot.isBooked) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        } else {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )
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

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
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



@Composable
fun AddSlotDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var time by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Offer Mentorship Slot") },
        text = {
            Column {
                Text(
                    "Set your available time for juniors.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    placeholder = { Text("e.g. Sunday 10:00 AM") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (time.isNotBlank()) onSave(time) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C375B))
            ) {
                Text("Create Slot")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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