package com.shettyharshith33.placementpro.screens



import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shettyharshith33.placementpro.models.UserRole

@Composable
fun RoleSelectionScreen(
    onRoleSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text("Placement Pro")

        Spacer(modifier = Modifier.height(32.dp))

        listOf(
            UserRole.STUDENT,
            UserRole.ALUMNI,
            UserRole.TPO
        ).forEach { role ->

            Button(
                onClick = { onRoleSelected(role) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(role.uppercase())
            }
        }
    }
}