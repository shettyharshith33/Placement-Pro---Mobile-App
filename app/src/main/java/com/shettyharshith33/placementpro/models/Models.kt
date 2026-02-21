package com.shettyharshith33.placementpro.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

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
    const val INTERVIEWS = "interviews"
    const val REFERRAL_REQUESTS = "referral_requests"
}

// 2. Base User Model (Found in 'users' collection)
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // student, tpo, alumni
    val profileCompleted: Boolean = false,
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val fcmToken: String = "",
    val isActive: Boolean = true,
    // Add student specific fields here directly for easier querying in the "Criteria Engine"
    val cgpa: Any? = 0.0,
    val backlogs: Any? = 0,
    val branch: String = "",
    val phone: String = "",
    val rollNumber: String = "",
    val skills: List<String> = emptyList(),
    val projects: List<Project> = emptyList(),
    val resumeUrl: String = "",
    val resumeFileName: String = "",
    val placed: Boolean = false
) {
    @get:com.google.firebase.firestore.Exclude
    val finalCgpa: Double
        get() = when (cgpa) {
            is Number -> cgpa.toDouble()
            is String -> cgpa.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    @get:com.google.firebase.firestore.Exclude
    val finalBacklogs: Int
        get() = when (backlogs) {
            is Number -> backlogs.toInt()
            is String -> backlogs.toIntOrNull() ?: 0
            else -> 0
        }

    @get:com.google.firebase.firestore.Exclude
    val createdAtTimestamp: Timestamp?
        get() = when (val c = createdAt) {
            is Timestamp -> c
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(c)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}


data class Project(
    val title: String = "",
    val description: String = "",
    val techStack: List<String> = emptyList(),
    val githubLink: String = ""
)

data class Certification(val title: String = "", val issuer: String = "", val year: Int = 0)
data class Internship(val company: String = "", val role: String = "", val duration: String = "")


@IgnoreExtraProperties
data class CompanyDrive(
    // 🔥 Core Fields (Primary)
    val companyId: String = "",
    val companyName: String = "",
    val roleOffered: String = "",
    val companyWebsite: String = "",
    val jobDescription: String = "",
    val jdFileUrl: String = "",

    // 🕒 Legacy & Dynamic Fields (Fallbacks for Myntra/External Sync)
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val website: String = "",
    val status: String = "active",
    val jdUrl: String = "",
    
    val location: String = "",
    val description: String = "",
    @get:PropertyName("package")
    @set:PropertyName("package")
    var packageLPA: Any = 0.0,
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Any = true,
    val minCGPA: Double = 0.0,
    val maxBacklogs: Int = 0,
    val batchYear: Int = 2026,
    val workMode: String = "", 
    val deadline: Any? = null,
    val createdAt: Any? = null,
    val createdBy: String = "",
    val rounds: List<String> = emptyList(),
    val allowedBranches: List<String> = emptyList()
) {
    // ✨ COMPLEMENTARY GETTERS FOR UI RESILIENCE
    @get:com.google.firebase.firestore.Exclude
    val finalId: String get() = companyId.ifBlank { id }

    @get:com.google.firebase.firestore.Exclude
    val finalName: String get() = companyName.ifBlank { name }

    @get:com.google.firebase.firestore.Exclude
    val finalRole: String get() = roleOffered.ifBlank { role }

    @get:com.google.firebase.firestore.Exclude
    val finalWebsite: String get() = companyWebsite.ifBlank { website }

    @get:com.google.firebase.firestore.Exclude
    val finalJdUrl: String get() = jdFileUrl.ifBlank { jdUrl }

    @get:com.google.firebase.firestore.Exclude
    val packageDouble: Double
        get() = when (val p = packageLPA) {
            is Number -> p.toDouble()
            is String -> p.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    @get:com.google.firebase.firestore.Exclude
    val isActiveCheckedCust: Boolean
        get() = when (val a = isActive) {
            is Boolean -> a
            is String -> a.toBoolean()
            else -> status == "active"
        }

    @get:com.google.firebase.firestore.Exclude
    val createdAtTimestamp: Timestamp?
        get() = when (val c = createdAt) {
            is Timestamp -> c
            is String -> {
                try {
                    // Try parsing ISO 8601 if it's a string
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(c)!!)
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
}




data class Referral(
    val referralId: String = "",
    val alumniId: String = "",
    val alumniName: String = "",
    val companyName: String = "",
    val role: String = "",
    val description: String = "",
    val deadline: Any? = null,
    val createdAt: Any? = null,
    @get:com.google.firebase.firestore.PropertyName("active")
    val activeState: Any? = null,
    @get:com.google.firebase.firestore.PropertyName("isActive")
    val isActiveState: Any? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val finalIsActive: Boolean
        get() {
            val a = when (activeState) {
                is Boolean -> activeState
                is String -> activeState.toBoolean()
                else -> true
            }
            val ia = when (isActiveState) {
                is Boolean -> isActiveState
                is String -> isActiveState.toBoolean()
                else -> true
            }
            return a && ia
        }

    @get:com.google.firebase.firestore.Exclude
    val createdAtTimestamp: Timestamp?
        get() = when (val c = createdAt) {
            is Timestamp -> c
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(c)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }

    @get:com.google.firebase.firestore.Exclude
    val deadlineTimestamp: Timestamp?
        get() = when (val d = deadline) {
            is Timestamp -> d
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(d)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}

object ApplicationStatus {
    const val APPLIED = "Applied"
    const val SHORTLISTED = "Shortlisted"
    const val APTITUDE = "Aptitude"
    const val TECHNICAL = "Technical"
    const val HR = "Hr"
    const val SELECTED = "Selected"
    const val REJECTED = "Rejected"
    const val INTERVIEW = "Interview Scheduled"
}

data class Application(
    val applicationId: String = "",
    val driveId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val studentResumeUrl: String = "",
    val studentCgpa: Any? = 0.0,
    val cgpa: Any? = 0.0, // Fallback
    val companyName: String = "",
    val roleOffered: String = "",
    val status: String = ApplicationStatus.APPLIED,
    val appliedAt: Any? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val finalCgpa: Double
        get() {
            val sc = when (studentCgpa) {
                is Number -> studentCgpa.toDouble()
                is String -> studentCgpa.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val c = when (cgpa) {
                is Number -> cgpa.toDouble()
                is String -> cgpa.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            return if (sc > 0.0) sc else c
        }

    @get:com.google.firebase.firestore.Exclude
    val appliedAtTimestamp: Timestamp?
        get() = when (val a = appliedAt) {
            is Timestamp -> a
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(a)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}


data class MentorSlot(
    val slotId: String = "",
    val alumniId: String = "",
    val alumniName: String = "",
    val alumniEmail: String = "",
    val alumniPhone: String = "",
    val topic: String = "",
    val availableDate: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val maxSlots: Int = 1,
    val bookedByList: List<String> = emptyList(),
    val bookedByNames: List<String> = emptyList(),
    val isBooked: Boolean = false, // Becomes true when bookedByList.size >= maxSlots
    val createdAt: Any? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val createdAtTimestamp: Timestamp?
        get() = when (val c = createdAt) {
            is Timestamp -> c
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(c)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}

data class Interview(
    val interviewId: String = "",
    val companyId: String = "",
    val companyName: String = "",
    val roleOffered: String = "",
    val round: String = "",
    val slotTime: Any? = null,
    val status: String = "Scheduled",
    val studentId: String = "",
    val studentName: String = "",
    val venue: String = ""
) {
    @get:com.google.firebase.firestore.Exclude
    val slotTimeTimestamp: Timestamp?
        get() = when (val s = slotTime) {
            is Timestamp -> s
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(s)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}

data class Notification(
    val notificationId: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Any? = null,
    val isRead: Boolean = false,
    val type: String = "info"
) {
    @get:com.google.firebase.firestore.Exclude
    val timestampVal: Timestamp?
        get() = when (val t = timestamp) {
            is Timestamp -> t
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(t)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}

data class ReferralRequest(
    val requestId: String = "",
    val referralId: String = "",
    val alumniId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val studentResumeUrl: String = "",
    val studentCgpa: Any? = 0.0,
    val cgpa: Any? = 0.0, // Fallback
    val companyName: String = "",
    val role: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val requestedAt: Any? = null
) {
    @get:com.google.firebase.firestore.Exclude
    val finalCgpa: Double
        get() {
            val sc = when (studentCgpa) {
                is Number -> studentCgpa.toDouble()
                is String -> studentCgpa.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            val c = when (cgpa) {
                is Number -> cgpa.toDouble()
                is String -> cgpa.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            return if (sc > 0.0) sc else c
        }

    @get:com.google.firebase.firestore.Exclude
    val requestedAtTimestamp: Timestamp?
        get() = when (val r = requestedAt) {
            is Timestamp -> r
            is String -> {
                try {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    Timestamp(sdf.parse(r)!!)
                } catch (e: Exception) { null }
            }
            else -> null
        }
}

// Navigation routes
sealed class Screen(val route: String) {

    object Splash : Screen("splash")

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

    object CreateDrive : Screen("create_drive?driveId={driveId}") {
        fun createRoute(driveId: String? = null) = if (driveId != null) "create_drive?driveId=$driveId" else "create_drive"
    }

    object PlacementBot : Screen("placement_bot")

    object MarketIntelligence : Screen("market_intelligence")

    object InterviewScheduler : Screen("interview_scheduler")
}