package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import android.content.Intent
import android.net.Uri
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
import com.shettyharshith33.placementpro.models.CompanyDrive
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TPODashboardView(
    onNavigateToCreateDrive: () -> Unit,
    onNavigateToScheduler: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val navyBlue = Color(0xFF1C375B)
    
    var companies by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    var applications by remember { mutableStateOf<List<com.shettyharshith33.placementpro.models.Application>>(emptyList()) }
    var tpoProfile by remember { mutableStateOf<User?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Drives, 1: Applications, 2: Profile
    var isLoading by remember { mutableStateOf(true) }

    // 🔥 Listen to data
    LaunchedEffect(selectedTab) {
        isLoading = true
        when(selectedTab) {
            0 -> {
                db.collection(FirestoreCollections.COMPANIES)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            companies = snapshot.toObjects(CompanyDrive::class.java).sortedByDescending { it.createdAt }
                            isLoading = false
                        }
                    }
            }
            1 -> {
                db.collection(FirestoreCollections.APPLICATIONS)
                    .addSnapshotListener { snapshot, _ ->
                        if (snapshot != null) {
                            applications = snapshot.toObjects(com.shettyharshith33.placementpro.models.Application::class.java).sortedByDescending { it.appliedAt }
                            isLoading = false
                        }
                    }
            }
            2 -> {
                val uid = auth.currentUser?.uid ?: return@LaunchedEffect
                db.collection(FirestoreCollections.USERS).document(uid)
                    .get().addOnSuccessListener { doc ->
                        tpoProfile = doc.toObject(User::class.java)
                        isLoading = false
                    }
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateDrive,
                    containerColor = navyBlue,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Create Drive") }
                )
            }
        }
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Drives", fontWeight = if(selectedTab==0) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Applicants", fontWeight = if(selectedTab==1) FontWeight.Bold else FontWeight.Normal) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Profile", fontWeight = if(selectedTab==2) FontWeight.Bold else FontWeight.Normal) })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = navyBlue)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    when(selectedTab) {
                        0 -> {
                            TPOControlHeader(onNavigateToScheduler)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(companies) { company ->
                                    TPODriveCard(company)
                                }
                            }
                        }
                        1 -> TPOApplicationsContent(applications)
                        2 -> TPOProfileView(tpoProfile)
                    }
                }
            }
        }
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
            Button(
                onClick = onNavigateToScheduler,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Scheduler Console")
            }
        }
    }
}

@Composable
fun TPODriveCard(company: CompanyDrive) {
    val navyBlue = Color(0xFF1C375B)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(company.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = navyBlue)
                Surface(color = if (company.isActive) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color.Red.copy(alpha=0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(if (company.isActive) "Active" else "Closed", color = if (company.isActive) Color(0xFF2E7D32) else Color.Red, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(company.roleOffered, fontWeight = FontWeight.Medium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Package: ${company.packageLPA} LPA", fontWeight = FontWeight.SemiBold, color = navyBlue)
                Text("Batch: ${company.batchYear}", fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(16.dp), Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Criteria: CGPA ≥ ${company.minCGPA} | Backlogs ≤ ${company.maxBacklogs}", fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { /* View Applications for this specific drive */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("View Detailed Statistics")
            }
        }
    }
}

@Composable
fun TPOProfileView(user: User?) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    if (user == null) return

    var name by remember { mutableStateOf(user.name) }
    var isEditing by remember { mutableStateOf(false) }
    val navyBlue = Color(0xFF1C375B)

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = navyBlue.copy(alpha = 0.1f)) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.padding(20.dp), tint = navyBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isEditing) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Admin Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    db.collection(FirestoreCollections.USERS).document(user.uid)
                        .update("name", name)
                        .addOnSuccessListener {
                            isEditing = false
                            Toast.makeText(context, "Admin Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
            ) {
                Text("SAVE")
            }
            TextButton(onClick = { isEditing = false }) { Text("Cancel") }
        } else {
            Text(user.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = navyBlue)
            Text(user.email, color = Color.Gray)
            Text("Placement Coordinator", fontSize = 14.sp, color = navyBlue, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(32.dp))
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileItemRow("Department", "Training & Placement Cell")
                    ProfileItemRow("Access Level", "Full Administrative Access")
                    ProfileItemRow("Last Login", "Today")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = { isEditing = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("EDIT ADMIN PROFILE")
            }
        }
    }
}

@Composable
fun TPOApplicationsContent(apps: List<com.shettyharshith33.placementpro.models.Application>) {
    if (apps.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No applications received yet.", color = Color.Gray)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(apps) { app ->
                ApplicationTpoCard(app)
            }
        }
    }
}

@Composable
fun ApplicationTpoCard(app: com.shettyharshith33.placementpro.models.Application) {
    val navyBlue = Color(0xFF1C375B)
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(app.companyName, fontWeight = FontWeight.ExtraBold, color = navyBlue, fontSize = 16.sp)
                Surface(color = Color(0xFF2E7D32).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(app.status, color = Color(0xFF2E7D32), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Candidate: ${app.studentName}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("CGPA: ${app.studentCgpa}", color = navyBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text("Role Offered: ${app.roleOffered}", fontSize = 13.sp)
            Text("Applied On: ${app.appliedAt?.toDate()?.toLocaleString() ?: "N/A"}", fontSize = 11.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { 
                    if (app.studentResumeUrl.isNotBlank()) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(app.studentResumeUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open resume", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No resume uploaded by student", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Student Resume", fontSize = 12.sp)
            }
        }
    }
}