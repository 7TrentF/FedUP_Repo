package com.FedUpGroup.fedup_foodwasteapp

// MyFirebaseMessagingService.kt
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessaging"
    private val channelId = "ingredient_expiration_channel"

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // You can store this token in SharedPreferences if needed
        saveTokenToPreferences(token)
    }

    private fun saveTokenToPreferences(token: String) {
        val sharedPreferences = getSharedPreferences("FCM_PREF", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("fcm_token", token)
            apply()
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload
        remoteMessage.data.isNotEmpty().let {
            handleNotification(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            it.body?.let { body ->
                showNotification(
                    it.title ?: "Ingredient Alert",
                    body,
                    remoteMessage.data["type"] ?: "general",
                    remoteMessage.data["items"]?.split(",") ?: emptyList()
                )
            }
        }
    }

    private fun handleNotification(data: Map<String, String>) {
        val title = data["title"] ?: "Ingredient Alert"
        val message = data["message"] ?: ""
        val type = data["type"] ?: "general"
        val items = data["items"]?.split(",") ?: emptyList()

        showNotification(title, message, type, items)
    }

    private fun showNotification(
        title: String,
        message: String,
        type: String,
        items: List<String>
    ) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ingredient Expiration Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for ingredient expiration alerts"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent for notification click
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure to create this icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Add item list if available
        if (items.isNotEmpty()) {
            val bigText = NotificationCompat.BigTextStyle()
                .bigText(items.joinToString("\n") { "• $it" })
            notificationBuilder.setStyle(bigText)
        }

        // Show notification
        val notificationId = when (type) {
            "expired" -> 2
            "about_to_expire" -> 1
            else -> 0
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}