package com.shettyharshith33.placementpro.classes


sealed class Screen {
    object RoleSelection : Screen()
    data class Login(val role: String) : Screen()
    data class Register(val role: String) : Screen()
    object VerificationWait : Screen()
    data class Dashboard(val role: String) : Screen()
}