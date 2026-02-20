package com.shettyharshith33.placementpro.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shettyharshith33.placementpro.models.UserRole

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String) -> Unit
) {
    // Navy Blue color used in your Dashboard and Verification screens
    val NavyBlue = Color(0xFF1C375B)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Section
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = NavyBlue,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PlacementPro",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NavyBlue
            )

            Text(
                text = "Empowering your career journey",
                fontSize = 14.sp,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Interactive Role Cards
            RoleCard(
                title = "Student",
                description = "Build your profile, track drives, and get placed.",
                icon = Icons.Default.Face,
                onClick = { onRoleSelected(UserRole.STUDENT) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "Alumni",
                description = "Give back through referrals and mentorship.",
                icon = Icons.Default.AccountBox,
                onClick = { onRoleSelected(UserRole.ALUMNI) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            RoleCard(
                title = "TPO Admin",
                description = "Manage recruitment drives and student data.",
                icon = Icons.Default.Star,
                onClick = { onRoleSelected(UserRole.TPO) }
            )
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val NavyBlue = Color(0xFF1C375B)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container with soft background
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = NavyBlue.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NavyBlue,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
    }
}