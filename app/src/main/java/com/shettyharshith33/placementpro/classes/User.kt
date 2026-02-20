package com.shettyharshith33.placementpro.classes

import com.google.firebase.Timestamp

// 1. COLLECTION PATH CONSTANTS
// These match the collection names in your Firestore database
object FirestoreCollections {
    const val USERS             = "users"
    const val STUDENTS          = "students"
    const val COMPANIES         = "companies"
    const val APPLICATIONS      = "applications"
    const val SETTINGS          = "settings"
    const val ANALYTICS         = "analytics"
}

// 2. USER ROLE CONSTANTS
object UserRole {
    const val STUDENT = "student"
    const val TPO     = "tpo"
    const val ALUMNI   = "alumni"
}

// 3. CORE USER MODEL
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val profileCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val isActive: Boolean = true
)