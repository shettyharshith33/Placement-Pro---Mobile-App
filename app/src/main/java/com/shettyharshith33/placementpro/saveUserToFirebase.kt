package com.shettyharshith33.placementpro

import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

private fun saveUserToFirestore(uid: String, email: String, role: String) {
    // Ensure you use Firebase.firestore (lowercase 'f') for the KTX extension
    val db = Firebase.firestore

    val user = hashMapOf(
        "email" to email,
        "role" to role, // "student", "admin", or "alumni"
        "createdAt" to FieldValue.serverTimestamp()
    )

    db.collection("users").document(uid).set(user)
        .addOnSuccessListener {
            // Navigate to Dashboard
        }
        .addOnFailureListener { e ->
            // Handle error (e.g., log it)
        }
}