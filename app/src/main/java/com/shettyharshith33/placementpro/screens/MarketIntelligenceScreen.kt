package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.User

data class MarketTrend(val role: String, val essentialSkills: List<String>, val demand: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketIntelligenceScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var userProfile by remember { mutableStateOf<User?>(null) }
    val navyBlue = Color(0xFF1C375B)

    val marketTrends = listOf(
        MarketTrend("SDE - 1", listOf("Java", "Kotlin", "Spring Boot", "AWS"), "High"),
        MarketTrend("Data Analyst", listOf("Python", "SQL", "PowerBI", "Tableau"), "Medium"),
        MarketTrend("Cloud Engineer", listOf("Docker", "Kubernetes", "Azure", "Linux"), "Very High")
    )

    LaunchedEffect(Unit) {
        val uid = auth.currentUser?.uid ?: return@LaunchedEffect
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            userProfile = doc.toObject(User::class.java)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Market Intelligence", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = navyBlue)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Skill Gap Analysis", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = navyBlue)
                Text("Compare your profile against industry standards.", color = Color.Gray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(marketTrends) { trend ->
                SkillGapCard(trend, userProfile?.skills ?: emptyList())
            }
        }
    }
}

@Composable
fun SkillGapCard(trend: MarketTrend, userSkills: List<String>) {
    val navyBlue = Color(0xFF1C375B)
    val missingSkills = trend.essentialSkills.filter { it !in userSkills }
    val matchPercentage = ((trend.essentialSkills.size - missingSkills.size).toDouble() / trend.essentialSkills.size * 100).toInt()

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(trend.role, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Match: $matchPercentage%", color = if (matchPercentage > 70) Color(0xFF2E7D32) else Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
            }
            Text("Demand: ${trend.demand}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = matchPercentage / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = if (matchPercentage > 70) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (missingSkills.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("You have all essential skills for this role!", color = Color(0xFF2E7D32))
                }
            } else {
                Text("Missing Essentials:", fontWeight = FontWeight.SemiBold)
                missingSkills.forEach { skill ->
                    Row(modifier = Modifier.padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(skill, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* Open Learning Link */ },
                    colors = ButtonDefaults.buttonColors(containerColor = navyBlue.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = navyBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Learning Path", color = navyBlue, fontSize = 12.sp)
                }
            }
        }
    }
}
