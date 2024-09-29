package com.example.fedup_foodwasteapp

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        scheduleExpirationCheck(applicationContext)
    }

    fun scheduleExpirationCheck(context: Context) {
        val expirationCheckRequest = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ExpirationCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            expirationCheckRequest
        )
    }
}
