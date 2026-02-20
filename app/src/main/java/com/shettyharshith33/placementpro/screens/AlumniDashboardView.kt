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
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.Referral

@Composable
fun AlumniDashboardView() {

    val db = FirebaseFirestore.getInstance()
    var referrals by remember { mutableStateOf<List<Referral>>(emptyList()) }
    val navyBlue = Color(0xFF1C375B)

    LaunchedEffect(Unit) {
        db.collection(FirestoreCollections.REFERRALS)
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    referrals = snapshot.toObjects(Referral::class.java)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "Alumni Connect",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = navyBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(referrals) { referral ->
                ReferralCard(referral)
            }
        }
    }
}