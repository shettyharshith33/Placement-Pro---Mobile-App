package com.shettyharshith33.placementpro.screens


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shettyharshith33.placementpro.models.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    role: String,
    onLogout: () -> Unit
) {
    val navyBlue = Color(0xFF1C375B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PlacementPro — ${role.replaceFirstChar { it.uppercase() }}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = navyBlue
                ),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (role) {

                // ================= STUDENT =================
                UserRole.STUDENT -> {
                    StudentDashboard(
                        onLogout = onLogout
                    )
                }

                // ================= TPO =================
                UserRole.TPO -> {
                    TPODashboardView()
                }

                // ================= ALUMNI =================
                UserRole.ALUMNI -> {
                    AlumniDashboardView()
                }

                // ================= FALLBACK =================
                else -> {
                    Text(
                        text = "Unknown role. Contact admin.",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}