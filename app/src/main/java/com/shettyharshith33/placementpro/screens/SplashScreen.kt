package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.Screen
import com.shettyharshith33.placementpro.models.UserRole

@Composable
fun SplashScreen(onNavigate: (String) -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val navyBlue = Color(0xFF1C375B)

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onNavigate(Screen.RoleSelection.route)
        } else {
            db.collection(FirestoreCollections.USERS).document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    val role = doc.getString("role") ?: ""
                    val isComplete = doc.getBoolean("profileCompleted") ?: false
                    
                    if (role == UserRole.STUDENT && !isComplete) {
                        onNavigate(Screen.ResumeWizard.route)
                    } else if (role.isNotBlank()) {
                        onNavigate(Screen.Dashboard.createRoute(role))
                    } else {
                        onNavigate(Screen.RoleSelection.route)
                    }
                }
                .addOnFailureListener {
                    onNavigate(Screen.RoleSelection.route)
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = navyBlue
        )
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.BottomCenter).size(60.dp),
            color = navyBlue
        )
    }
}
