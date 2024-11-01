package com.example.fedup_foodwasteapp
import android.Manifest
import android.app.NotificationChannel
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.Worker
import java.util.*
import java.util.concurrent.TimeUnit
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewModelScope

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExpirationCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository: IngredientRepository
    private val apiService = RetrofitClient.apiService

    init {
        // Initialize Room database and get DAO
        val ingredientDao = AppDatabase.getDatabase(context).ingredientDao() // Corrected method name

        // Initialize the repository with the required dependencies
        repository = IngredientRepository(ingredientDao, apiService)
    }

    override suspend fun doWork(): Result {
        Log.d("ExpirationCheckWorker", "Worker is running")

        val ingredients = fetchIngredientsFromFirebase()
        Log.d("ExpirationCheckWorker", "Fetched ingredients: $ingredients")

        val aboutToExpire = ingredients.filter { isAboutToExpire(it.expirationDate) }
        Log.d("ExpirationCheckWorker", "Expiring ingredients: $aboutToExpire")

        if (aboutToExpire.isNotEmpty()) {
            showNotification(aboutToExpire)
            Log.d("ExpirationCheckWorker", "Notification shown")
        } else {
            Log.d("ExpirationCheckWorker", "No ingredients about to expire")
        }

        return Result.success()
    }

    private fun isAboutToExpire(expirationDate: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE
            val expiryDate = LocalDate.parse(expirationDate, formatter)
            val today = LocalDate.now()
            val daysUntilExpiry = today.until(expiryDate).days
            daysUntilExpiry in 0..3
        } catch (e: Exception) {
            false // Return false if date parsing fails
        }
    }

    private fun showNotification(expiringIngredients: List<Ingredient>) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "expiry_notifications"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Ingredient Expiration Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Ingredients About to Expire")
            .setContentText("You have ingredients expiring soon!")
            .setStyle(NotificationCompat.BigTextStyle().bigText(expiringIngredients.joinToString { it.productName }))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1, notification)
    }


    private suspend fun fetchIngredientsFromFirebase(): List<Ingredient> {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user != null) {
            // Get ID token to use in the API call
            val token = withContext(Dispatchers.IO) {
                user.getIdToken(true).await()
            }.token

            // Use your API repository to fetch the ingredients using the token
            if (token != null) {
                val ingredients = repository.fetchIngredientsFromApi(token)
                return ingredients ?: emptyList()
            }
        }
        return emptyList()
    }
}


