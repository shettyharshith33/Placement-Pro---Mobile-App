package com.shettyharshith33.placementpro.models

import com.google.firebase.Timestamp

// 1. Roles & Collections Constants
object UserRole {
    const val STUDENT = "student"
    const val TPO = "tpo"
    const val ALUMNI = "alumni"
}

object FirestoreCollections {
    const val USERS = "users"
    const val STUDENTS = "students"
    const val COMPANIES = "companies"
    const val APPLICATIONS = "applications"
    const val SETTINGS = "settings"
    const val SKILL_MARKET = "skillMarketData"
    const val NOTIFICATIONS = "notifications"
    const val REFERRALS = "referrals"
}

// 2. Base User Model (Found in 'users' collection)
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // student, tpo, alumni
    val profileCompleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val fcmToken: String = "",
    val isActive: Boolean = true
)

// 3. Extended Student Model (Found in 'students' collection)
data class StudentProfile(
    val uid: String = "",
    val branch: String = "",
    val batchYear: Int = 2026,
    val cgpa: Double = 0.0,
    val backlogs: Int = 0,
    val phone: String = "",
    val skills: List<String> = emptyList(),
    val resumeUrl: String? = null,
    val profileScore: Int = 0,
    val placed: Boolean = false,
    val projects: List<Project> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val internships: List<Internship> = emptyList()
)

data class Project(
    val title: String = "",
    val description: String = "",
    val techStack: List<String> = emptyList(),
    val githubLink: String = ""
)

data class Certification(val title: String = "", val issuer: String = "", val year: Int = 0)
data class Internship(val company: String = "", val role: String = "", val duration: String = "")

// 4. Company/Job Drive Model
data class CompanyDrive(
    val companyId: String = "",
    val companyName: String = "",
    val roleOffered: String = "",
    val packageLPA: Double = 0.0,
    val minCGPA: Double = 0.0,
    val maxBacklogs: Int = 0,
    val allowedBranches: List<String> = emptyList(),
    val deadline: Timestamp? = null,
    val isActive: Boolean = true,
    val location: String = ""
)




data class Referral(
    val referralId: String = "",
    val alumniId: String = "",
    val alumniName: String = "",
    val companyName: String = "",
    val role: String = "",
    val description: String = "",
    val deadline: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val isActive: Boolean = true
)




// Navigation routes
sealed class Screen(val route: String) {

    object RoleSelection : Screen("role_selection")

    object Login : Screen("login/{role}") {
        fun createRoute(role: String) = "login/$role"
    }

    object Register : Screen("register/{role}") {
        fun createRoute(role: String) = "register/$role"
    }

    object Verification : Screen("verification")

    object ResumeWizard : Screen("resume_wizard")

    object Dashboard : Screen("dashboard/{role}") {
        fun createRoute(role: String) = "dashboard/$role"
    }
}