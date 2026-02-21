package com.shettyharshith33.placementpro.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import com.shettyharshith33.placementpro.models.FirestoreCollections
import com.shettyharshith33.placementpro.models.Screen
import com.shettyharshith33.placementpro.models.UserRole

@Composable
fun PlacementProApp() {

    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {

        // ================= SPLASH =================
        composable(Screen.Splash.route) {
            SplashScreen { destination ->
                navController.navigate(destination) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // ================= ROLE SELECTION =================
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen { role ->
                navController.navigate(Screen.Login.createRoute(role))
            }
        }

        // ================= LOGIN =================
        composable(Screen.Login.route) { backStackEntry ->

            val role = backStackEntry.arguments?.getString("role") ?: ""

            LoginScreen(
                role = role,

                onLoginSuccess = {

                    val uid = auth.currentUser?.uid ?: return@LoginScreen

                    // 🔥 ALWAYS VERIFY ROLE FROM FIRESTORE
                    db.collection(FirestoreCollections.USERS)
                        .document(uid)
                        .get()
                        .addOnSuccessListener { doc ->

                            val actualRole =
                                doc.getString("role") ?: ""

                            val profileComplete =
                                doc.getBoolean("profileCompleted") ?: false

                            // 🧠 First-time student → Resume Wizard
                            if (actualRole == UserRole.STUDENT && !profileComplete) {

                                navController.navigate(Screen.ResumeWizard.route) {
                                    popUpTo(Screen.RoleSelection.route)
                                }

                            } else {

                                navController.navigate(
                                    Screen.Dashboard.createRoute(actualRole)
                                ) {
                                    popUpTo(Screen.RoleSelection.route)
                                }
                            }
                        }
                },

                onNavigateToRegister = {
                    navController.navigate(
                        Screen.Register.createRoute(role)
                    )
                }
            )
        }

        // ================= REGISTER =================
        composable(Screen.Register.route) { backStackEntry ->

            val role = backStackEntry.arguments?.getString("role") ?: ""

            RegisterScreen(
                role = role,
                onRegisterSuccess = {
                    navController.navigate(Screen.Verification.route)
                }
            )
        }

        // ================= EMAIL VERIFICATION =================
        composable(Screen.Verification.route) {

            VerificationWaitScreen {

                auth.currentUser?.reload()?.addOnSuccessListener {

                    if (auth.currentUser?.isEmailVerified == true) {

                        navController.navigate(Screen.RoleSelection.route) {
                            popUpTo(0)
                        }
                    }
                }
            }
        }

        // ================= RESUME WIZARD =================
        composable(Screen.ResumeWizard.route) {

            ResumeWizardScreen {

                navController.navigate(
                    Screen.Dashboard.createRoute(UserRole.STUDENT)
                ) {
                    popUpTo(0)
                }
            }
        }

        // ================= DASHBOARD =================
        composable(Screen.Dashboard.route) { backStackEntry ->

            val role = backStackEntry.arguments?.getString("role") ?: ""

            DashboardContent(
                role = role,
                onLogout = {
                    auth.signOut()
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(0)
                    }
                },
                onNavigateToCreateDrive = { driveId ->
                    navController.navigate(Screen.CreateDrive.createRoute(driveId))
                },
                onNavigateToBot = {
                    navController.navigate(Screen.PlacementBot.route)
                },
                onNavigateToMarket = {
                    navController.navigate(Screen.MarketIntelligence.route)
                }
            )
        }

        // ================= CREATE DRIVE (TPO) =================
        composable(Screen.CreateDrive.route) { backStackEntry ->
            val driveId = backStackEntry.arguments?.getString("driveId")
            CreateDriveScreen(
                onBack = { navController.popBackStack() },
                driveId = driveId
            )
        }

        // ================= PLACEMENT BOT =================
        composable(Screen.PlacementBot.route) {
            PlacementBotScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ================= MARKET INTELLIGENCE =================
        composable(Screen.MarketIntelligence.route) {
            MarketIntelligenceScreen(
                onBack = { navController.popBackStack() }
            )
        }

    }
}