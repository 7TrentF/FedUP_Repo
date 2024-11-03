package com.example.fedup_foodwasteapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpirationCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: IngredientRepository
    private val apiService = RetrofitClient.apiService
    private val channelId = "ingredient_expiration_channel"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val appPreferences = AppPreferences.getInstance(context)

    init {
        val ingredientDao = AppDatabase.getDatabase(context).ingredientDao()
        repository = IngredientRepository(ingredientDao, apiService)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
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
    }

    override suspend fun doWork(): Result {
        // First check if notifications are enabled
        if (!appPreferences.areNotificationsEnabled()) {
            Log.d("ExpirationCheckWorker", "Notifications are disabled, skipping check")
            // Cancel any existing notifications
            notificationManager.cancelAll()
            return Result.success()
        }

        return try {
            Log.d("ExpirationCheckWorker", "Starting work")

            // Fetch ingredients
            val ingredients = fetchIngredientsFromFirebase()
            Log.d("ExpirationCheckWorker", "Fetched ${ingredients.size} ingredients")

            // Process expiration dates
            val (aboutToExpire, expired) = processIngredients(ingredients)

            // Handle notifications
            handleNotifications(aboutToExpire, expired)

            // Send data to server for FCM
            sendExpirationDataToServer(aboutToExpire, expired)

            Result.success()
        } catch (e: Exception) {
            Log.e("ExpirationCheckWorker", "Error during work execution", e)
            Result.failure()
        }
    }

    private fun processIngredients(ingredients: List<Ingredient>): Pair<List<Ingredient>, List<Ingredient>> {
        // Double-check notifications are still enabled
        if (!appPreferences.areNotificationsEnabled()) {
            return Pair(emptyList(), emptyList())
        }

        // Get notification timing preference
        val notificationDays = appPreferences.getNotificationDays()
        Log.d("ExpirationCheckWorker", "Using notification days: $notificationDays")

        val aboutToExpire = ingredients.filter {
            val daysUntilExpiry = daysUntilExpiration(it.expirationDate)
            daysUntilExpiry in 0..notificationDays
        }

        val expired = ingredients.filter {
            daysUntilExpiration(it.expirationDate) < 0
        }

        Log.d("ExpirationCheckWorker", "Found ${aboutToExpire.size} about to expire and ${expired.size} expired ingredients")
        return Pair(aboutToExpire, expired)
    }

    private fun handleNotifications(aboutToExpire: List<Ingredient>, expired: List<Ingredient>) {
        // Double-check notifications are still enabled
        if (!appPreferences.areNotificationsEnabled()) {
            notificationManager.cancelAll()
            return
        }

        if (aboutToExpire.isNotEmpty()) {
            showNotification(
                aboutToExpire,
                "Ingredients About to Expire",
                "${aboutToExpire.size} ingredients will expire soon"
            )
        }

        if (expired.isNotEmpty()) {
            showNotification(
                expired,
                "Expired Ingredients",
                "${expired.size} ingredients have expired"
            )
        }
    }

    private suspend fun sendExpirationDataToServer(aboutToExpire: List<Ingredient>, expired: List<Ingredient>) {
        // Only send data if notifications are enabled
        if (!appPreferences.areNotificationsEnabled()) {
            return
        }

        try {
            val auth = FirebaseAuth.getInstance()
            val idToken = auth.currentUser?.getIdToken(true)?.await()?.token ?: return

            val fcmToken = withContext(Dispatchers.IO) {
                FirebaseMessaging.getInstance().token.await()
            }

            val notificationData = mapOf(
                "aboutToExpireCount" to aboutToExpire.size.toString(),
                "expiredCount" to expired.size.toString(),
                "aboutToExpireItems" to aboutToExpire.joinToString { it.productName },
                "expiredItems" to expired.joinToString { it.productName },
                "notificationDays" to appPreferences.getNotificationDays().toString()
            )

            repository.sendExpirationData(
                token = idToken,
                fcmToken = fcmToken,
                notificationData = notificationData
            )

            Log.d("ExpirationCheckWorker", "Successfully sent data to server")
        } catch (e: Exception) {
            Log.e("ExpirationCheckWorker", "Error sending data to server", e)
        }
    }

    private fun daysUntilExpiration(expirationDate: String): Int {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE
            val expiryDate = LocalDate.parse(expirationDate, formatter)
            val today = LocalDate.now()
            today.until(expiryDate).days
        } catch (e: Exception) {
            Log.e("ExpirationCheckWorker", "Error parsing date: $expirationDate", e)
            Int.MAX_VALUE
        }
    }

    private fun showNotification(ingredients: List<Ingredient>, title: String, message: String) {
        // Final check before showing notification
        if (!appPreferences.areNotificationsEnabled()) {
            return
        }

        try {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    ingredients.joinToString("\n") { "• ${it.productName}" }
                ))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val notificationId = if (title.contains("Expired")) 2 else 1
            notificationManager.notify(notificationId, notification)

            Log.d("ExpirationCheckWorker", "Successfully showed notification: $title")
        } catch (e: Exception) {
            Log.e("ExpirationCheckWorker", "Error showing notification", e)
        }
    }

    private suspend fun fetchIngredientsFromFirebase(): List<Ingredient> {
        return try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser
            if (user != null) {
                val token = withContext(Dispatchers.IO) {
                    user.getIdToken(true).await().token
                }

                if (token != null) {
                    val ingredients = repository.fetchIngredientsFromApi(token)
                    ingredients ?: emptyList()
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("ExpirationCheckWorker", "Error fetching ingredients", e)
            emptyList()
        }
    }
}