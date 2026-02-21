/*
package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.R
import com.shettyharshith33.placementpro.models.FirestoreCollections

private val NavyBlue = Color(0xFF1C375B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    role: String,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedBorderColor = NavyBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = NavyBlue,
        cursorColor = NavyBlue
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Surface(color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {



                // --- Updated Lottie Animation Configuration ---
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcomescreen))

                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever, // Keep looping
                    reverseOnRepeat = true, // 🔥 This makes it play backwards after finishing
                    restartOnPlay = false
                )

// The LottieAnimation call remains the same
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 16.dp)
                )


                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NavyBlue
                )

                Text(
                    text = "Registering as ${role.replaceFirstChar { it.uppercase() }}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                val branches = listOf("CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "MCA", "MBA", "M.Tech", "Other")
                var branchExpanded by remember { mutableStateOf(false) }
                var selectedBranch by remember { mutableStateOf("") }

                if (role.lowercase() == "student") {
                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = !branchExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBranch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Branch") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = branchExpanded,
                            onDismissRequest = { branchExpanded = false }
                        ) {
                            branches.forEach { branchName ->
                                DropdownMenuItem(
                                    text = { Text(branchName) },
                                    onClick = {
                                        selectedBranch = branchName
                                        branchExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Set Password") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.length < 6 || (role.lowercase() == "student" && selectedBranch.isBlank())) {
                            Toast.makeText(
                                context,
                                "Please fill all fields properly",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true

                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val uid = result.user!!.uid

                                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    val fcmToken = if (task.isSuccessful) task.result else ""

                                    val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                                    isoFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                    val nowIso = isoFormat.format(java.util.Date())

                                    val userMap = hashMapOf(
                                        "uid" to uid,
                                        "name" to name.trim(),
                                        "email" to email.trim(),
                                        "phone" to phone.trim(),
                                        "role" to role.lowercase(),
                                        "branch" to if (role.lowercase() == "student") selectedBranch else "CSE",
                                        "cgpa" to 0.0,
                                        "backlogs" to 0,
                                        "batchYear" to 2026,
                                        "rollNumber" to "",
                                        "profileCompleted" to (role.lowercase() == "alumni"),
                                        "createdAt" to nowIso,
                                        "updatedAt" to nowIso,
                                        "fcmToken" to fcmToken,
                                        "placed" to false
                                    )

                                    db.collection(FirestoreCollections.USERS)
                                        .document(uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            result.user!!.sendEmailVerification()
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "Database error",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Registration failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            "SIGN UP",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 🔥 FULL SCREEN LOADING OVERLAY (PRO FEATURE)
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }
    }
}*/





package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.R
import com.shettyharshith33.placementpro.models.FirestoreCollections

private val NavyBlue = Color(0xFF1C375B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    role: String,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // 🔥 Added state for password visibility
    var passwordVisible by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedBorderColor = NavyBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = NavyBlue,
        cursorColor = NavyBlue
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Surface(color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // --- Lottie Animation ---
                val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.welcomescreen))
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = LottieConstants.IterateForever,
                    reverseOnRepeat = true,
                    restartOnPlay = false
                )

                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(200.dp).padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Create Account", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = NavyBlue)
                Text(text = "Registering as ${role.replaceFirstChar { it.uppercase() }}", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(32.dp))

                // Standard Fields
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors, singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))

                // Branch Selector for Students
                val branches = listOf("CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "MCA", "MBA", "M.Tech", "Other")
                var branchExpanded by remember { mutableStateOf(false) }
                var selectedBranch by remember { mutableStateOf("") }

                if (role.lowercase() == "student") {
                    ExposedDropdownMenuBox(
                        expanded = branchExpanded,
                        onExpandedChange = { branchExpanded = !branchExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBranch,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Branch") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = branchExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = textFieldColors
                        )
                        ExposedDropdownMenu(expanded = branchExpanded, onDismissRequest = { branchExpanded = false }) {
                            branches.forEach { branchName ->
                                DropdownMenuItem(text = { Text(branchName) }, onClick = { selectedBranch = branchName; branchExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 🔥 Password Field with Eye Toggle
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Set Password") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true,
                    // Toggle transformation based on state
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.eye_open else R.drawable.eye_closed
                                ),
                                contentDescription = "Toggle password visibility",
                                modifier = Modifier.size(24.dp),
                                tint = if (passwordVisible) NavyBlue else Color.Gray
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sign Up Button
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.length < 6 || (role.lowercase() == "student" && selectedBranch.isBlank())) {
                            Toast.makeText(context, "Please fill all fields properly", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val uid = result.user!!.uid
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    val fcmToken = if (task.isSuccessful) task.result else ""

                                    // Use proper server timestamps for database consistency
                                    val userMap = hashMapOf(
                                        "uid" to uid,
                                        "name" to name.trim(),
                                        "email" to email.trim(),
                                        "phone" to phone.trim(),
                                        "role" to role.lowercase(),
                                        "branch" to if (role.lowercase() == "student") selectedBranch else "CSE",
                                        "cgpa" to 0.0,
                                        "backlogs" to 0,
                                        "batchYear" to 2026,
                                        "rollNumber" to "",
                                        "profileCompleted" to (role.lowercase() == "alumni"),
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "updatedAt" to com.google.firebase.Timestamp.now(),
                                        "fcmToken" to fcmToken,
                                        "placed" to false
                                    )

                                    db.collection(FirestoreCollections.USERS).document(uid).set(userMap)
                                        .addOnSuccessListener {
                                            result.user!!.sendEmailVerification()
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            Toast.makeText(context, "Database error", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener {
                                isLoading = false
                                Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyBlue),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SIGN UP", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Full Screen Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NavyBlue)
            }
        }
    }
}