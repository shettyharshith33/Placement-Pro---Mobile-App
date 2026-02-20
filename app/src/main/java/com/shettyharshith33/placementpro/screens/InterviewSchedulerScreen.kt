package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.Application
import com.shettyharshith33.placementpro.models.ApplicationStatus
import com.shettyharshith33.placementpro.models.FirestoreCollections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterviewSchedulerScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var applications by remember { mutableStateOf<List<Application>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Pending, 1: Scheduled
    val navyBlue = Color(0xFF1C375B)

    LaunchedEffect(selectedTab) {
        val targetStatus = if (selectedTab == 0) ApplicationStatus.CLEARED else ApplicationStatus.INTERVIEW
        db.collection(FirestoreCollections.APPLICATIONS)
            .whereEqualTo("status", targetStatus)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    applications = snapshot.toObjects(Application::class.java)
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interview Scheduler", color = Color.White) },
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Cleared Apti") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Scheduled") })
            }

            if (applications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No candidates found.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(applications) { app ->
                        SchedulerCard(app, selectedTab == 0) {
                            // Update status to Interview Scheduled
                            db.collection(FirestoreCollections.APPLICATIONS).document(app.applicationId)
                                .update("status", ApplicationStatus.INTERVIEW)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchedulerCard(app: Application, isPending: Boolean, onSchedule: () -> Unit) {
    val navyBlue = Color(0xFF1C375B)
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.companyName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Candidate ID: ${app.studentId}", style = MaterialTheme.typography.bodySmall)
                Text("Role: ${app.roleOffered}", color = navyBlue)
            }
            
            if (isPending) {
                Button(onClick = onSchedule, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign Slot")
                }
            } else {
                Surface(color = Color(0xFF2E7D32).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Scheduled", color = Color(0xFF2E7D32), modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
