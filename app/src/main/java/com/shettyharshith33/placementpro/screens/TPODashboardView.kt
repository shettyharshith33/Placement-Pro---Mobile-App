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
fun TPODashboardView() {

    val db = FirebaseFirestore.getInstance()
    var companies by remember { mutableStateOf<List<CompanyDrive>>(emptyList()) }
    val navyBlue = Color(0xFF1C375B)

    // 🔥 Listen to companies (Drives)
    LaunchedEffect(Unit) {
        db.collection(FirestoreCollections.COMPANIES)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    companies = snapshot.toObjects(CompanyDrive::class.java)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "TPO Control Center",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = navyBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Active Placement Drives",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(companies) { company ->
                TPODriveCard(company)
            }
        }
    }
}

@Composable
fun TPODriveCard(company: CompanyDrive) {

    val navyBlue = Color(0xFF1C375B)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                company.companyName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = navyBlue
            )

            Text("Role: ${company.roleOffered}")
            Text("Min CGPA: ${company.minCGPA}")
            Text("Max Backlogs: ${company.maxBacklogs}")

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    // 🔥 NEXT STEP: Criteria Engine button
                },
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue)
            ) {
                Text("View Eligible Students")
            }
        }
    }
}