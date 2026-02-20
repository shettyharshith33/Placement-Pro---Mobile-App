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
    val isActive: Boolean = true,
    // Add student specific fields here directly for easier querying in the "Criteria Engine"
    val cgpa: Double = 0.0,
    val backlogs: Int = 0,
    val branch: String = "",
    val phone: String = "",
    val rollNumber: String = "",
    val skills: List<String> = emptyList(),
    val resumeUrl: String = "",
    val resumeFileName: String = ""
)


data class Project(
    val title: String = "",
    val description: String = "",
    val techStack: List<String> = emptyList(),
    val githubLink: String = ""
)

data class Certification(val title: String = "", val issuer: String = "", val year: Int = 0)
data class Internship(val company: String = "", val role: String = "", val duration: String = "")


data class CompanyDrive(
    val companyId: String = "",
    val companyName: String = "",
    val roleOffered: String = "",
    val location: String = "",
    val description: String = "",
    // Use packageLPA to avoid reserved keyword 'package'
    val packageLPA: Double = 0.0,
    val isActive: Boolean = true,
    val minCGPA: Double = 0.0,
    val maxBacklogs: Int = 0,
    val batchYear: Int = 2026,
    val deadline: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val createdBy: String = "",
    val rounds: List<String> = emptyList(),
    val allowedBranches: List<String> = emptyList()
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

object ApplicationStatus {
    const val APPLIED = "Applied"
    const val APTITUDE = "Aptitude"
    const val CLEARED = "Cleared"
    const val INTERVIEW = "Interview Scheduled"
    const val SELECTED = "Selected"
    const val REJECTED = "Rejected"
}

data class Application(
    val applicationId: String = "",
    val driveId: String = "",
    val studentId: String = "",
    val companyName: String = "",
    val roleOffered: String = "",
    val status: String = ApplicationStatus.APPLIED,
    val appliedAt: Timestamp? = null
)




data class MentorSlot(
    val slotId: String = "",
    val alumniId: String = "",
    val alumniName: String = "",
    val availableTime: String = "",
    val isBooked: Boolean = false,
    val bookedBy: String = "",
    val createdAt: Timestamp? = null
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

    object CreateDrive : Screen("create_drive")

    object PlacementBot : Screen("placement_bot")

    object MarketIntelligence : Screen("market_intelligence")

    object InterviewScheduler : Screen("interview_scheduler")
}