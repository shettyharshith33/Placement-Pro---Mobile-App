/*
package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.CompanyDrive
import com.shettyharshith33.placementpro.models.FirestoreCollections

@Composable
fun StudentDashboard(onLogout: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var availableDrives by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    val navyBlue = Color(0xFF1C375B)

    // Fetch live company drives from Firestore
    LaunchedEffect(Unit) {
        db.collection(FirestoreCollections.COMPANIES)
            .whereEqualTo("isActive", true) // Only show open drives
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    availableDrives = snapshot.toObjects(CompanyDrive::class.java)
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Live Drives", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = navyBlue)
            TextButton(onClick = onLogout) { Text("Logout", color = Color.Red) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(availableDrives) { drive ->
                DriveCard(drive)
            }
        }
    }
}

@Composable
fun DriveCard(drive: CompanyDrive) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(drive.companyName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1C375B))
            Text("Role: ${drive.roleOffered}", style = MaterialTheme.typography.bodyMedium)
            Text("Package: ${drive.packageLPA} LPA", fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            Text("Eligibility: CGPA ≥ ${drive.minCGPA} | Backlogs ≤ ${drive.maxBacklogs}", style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = { */
/* Apply Logic here *//*
 },
                modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C375B))
            ) {
                Text("Apply Now")
            }
        }
    }
}*/




package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.CompanyDrive
import com.shettyharshith33.placementpro.models.FirestoreCollections

@Composable
fun StudentDashboard(
    onLogout: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var availableDrives by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    val navyBlue = Color(0xFF1C375B)

    // 🔥 LIVE DRIVES LISTENER
    LaunchedEffect(Unit) {
        db.collection(FirestoreCollections.COMPANIES)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    availableDrives =
                        snapshot.toObjects(CompanyDrive::class.java)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "Live Placement Drives",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = navyBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(availableDrives) { drive ->
                DriveCard(drive)
            }
        }
    }
}







@Composable
fun DriveCard(drive: CompanyDrive) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                drive.companyName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C375B)
            )

            Text("Role: ${drive.roleOffered}")
            Text(
                "Package: ${drive.packageLPA} LPA",
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Eligibility: CGPA ≥ ${drive.minCGPA} | Backlogs ≤ ${drive.maxBacklogs}",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = {
                    // TODO: Apply logic next step
                },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C375B)
                )
            ) {
                Text("Apply Now")
            }
        }
    }
}