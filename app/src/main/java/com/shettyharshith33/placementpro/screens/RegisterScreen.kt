/*
package com.shettyharshith33.placementpro.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.FirestoreCollections

@Composable
fun RegisterScreen(
    role: String,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val navyBlue = Color(0xFF1C375B)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedBorderColor = navyBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = navyBlue
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = navyBlue,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = navyBlue
            )

            Text(
                text = "Registering as ${role.replaceFirstChar { it.uppercase() }}",
                fontSize = 16.sp,
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
                label = { Text("College Email") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                    if (name.isBlank() || email.isBlank() || password.length < 6) {
                        Toast.makeText(context, "Please fill all fields (Min 6 chars for password)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnSuccessListener { result ->
                            val uid = result.user!!.uid
                            val userMap = hashMapOf(
                                "uid" to uid,
                                "name" to name.trim(),
                                "email" to email.trim(),
                                "role" to role.lowercase(),
                                "profileCompleted" to false,
                                "createdAt" to com.google.firebase.Timestamp.now()
                            )

                            // Save to 'users' collection
                            db.collection(FirestoreCollections.USERS).document(uid)
                                .set(userMap)
                                .addOnSuccessListener {
                                    result.user!!.sendEmailVerification()
                                    isLoading = false
                                    onRegisterSuccess()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = navyBlue),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("SIGN UP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.shettyharshith33.placementpro.models.FirestoreCollections

private val PrimaryBlue = Color(0xFF4DA3FF)

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
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedBorderColor = PrimaryBlue,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = PrimaryBlue,
        cursorColor = PrimaryBlue
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

                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Create Account",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryBlue
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
                    label = { Text("College Email") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        if (name.isBlank() || email.isBlank() || password.length < 6) {
                            Toast.makeText(
                                context,
                                "Please fill all fields (Min 6 chars password)",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true

                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val uid = result.user!!.uid

                                val userMap = hashMapOf(
                                    "uid" to uid,
                                    "name" to name.trim(),
                                    "email" to email.trim(),
                                    "role" to role.lowercase(),
                                    "profileCompleted" to false,
                                    "createdAt" to com.google.firebase.Timestamp.now()
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
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
                CircularProgressIndicator(color = PrimaryBlue)
            }
        }
    }
}