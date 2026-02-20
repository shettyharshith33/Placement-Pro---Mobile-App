package com.shettyharshith33.placementpro.screens


import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.classes.Screen

@Composable
fun PlacementProApp() {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Manage current screen state
    var currentScreen by remember { mutableStateOf<Screen>(Screen.RoleSelection) }
    var selectedRole by remember { mutableStateOf("") }

    Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.RoleSelection -> {
                RoleSelectionScreen { role ->
                    selectedRole = role
                    currentScreen = Screen.Login(role)
                }
            }
            is Screen.Login -> {
                LoginScreen(
                    role = screen.role,
                    onLoginSuccess = { currentScreen = Screen.Dashboard },
                    onNavigateToRegister = { currentScreen = Screen.Register(screen.role) }
                )
            }
            is Screen.Register -> {
                // RegisterScreen now handles its own Firebase logic to avoid "Too many arguments" error
                RegisterScreen(
                    role = screen.role,
                    onRegisterSuccess = {
                        currentScreen = Screen.VerificationWait
                    }
                )
            }
            is Screen.VerificationWait -> {
                VerificationWaitScreen {
                    // Force reload to pick up the verification status from Firebase
                    auth.currentUser?.reload()?.addOnCompleteListener {
                        if (auth.currentUser?.isEmailVerified == true) {
                            // Update Firestore once verified
                            db.collection("users").document(auth.currentUser?.uid!!)
                                .update("isVerified", true)
                                .addOnSuccessListener {
                                    currentScreen = Screen.Dashboard
                                }
                        } else {
                            Toast.makeText(context, "Verify email first!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            is Screen.Dashboard -> {
                // Placeholder for your Dashboard
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Welcome to the Dashboard!", color = Color(0xFF1C375B))
                    Button(onClick = {
                        auth.signOut()
                        currentScreen = Screen.RoleSelection
                    }) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}