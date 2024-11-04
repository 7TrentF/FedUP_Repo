package com.FedUpGroup.fedup_foodwasteapp

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.google.firebase.FirebaseApp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MyApplication : Application() {
    //private lateinit var syncManager: SyncManager
    private lateinit var repository: IngredientRepository

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "my_database"
        ).build()

        val ingredientDao = database.ingredientDao()
        val apiService = RetrofitClient.apiService

        repository = IngredientRepository(ingredientDao, apiService)

        scheduleExpirationCheck(applicationContext)
    }
    override fun onTerminate() {
        super.onTerminate()
    }

    private fun scheduleExpirationCheck(context: Context) {
        val expirationCheckRequest = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ExpirationCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            expirationCheckRequest
        )
    }
}
