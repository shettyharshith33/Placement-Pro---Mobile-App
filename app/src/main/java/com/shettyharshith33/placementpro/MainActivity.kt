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
        enableEdgeToEdge()
        setContent {
            PlacementProTheme {
                // Call the main App logic that handles all screens
                PlacementProApp()
            }
        }
    }
}