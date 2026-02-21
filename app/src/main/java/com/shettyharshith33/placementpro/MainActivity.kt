package com.shettyharshith33.placementpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shettyharshith33.placementpro.screens.PlacementProApp
import com.shettyharshith33.placementpro.ui.theme.PlacementProTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Notification Permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize Notification Channels for real-time alerts
        com.shettyharshith33.placementpro.utils.NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()
        setContent {
            PlacementProTheme {
                // Call the main App logic that handles all screens
                PlacementProApp()
            }
        }
    }
}