package com.shettyharshith33.placementpro.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.shettyharshith33.placementpro.utils.NotificationHelper

class PlacementProMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle FCM messages when app is in foreground or background
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Placement Pro Update"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "You have a new notification."

        NotificationHelper.showLocalNotification(
            context = applicationContext,
            title = title,
            message = body
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // This is where you would send the token to your server
        Log.d("FCM", "New token generated: $token")
    }
}
