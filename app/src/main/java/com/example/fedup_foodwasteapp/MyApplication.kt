package com.example.fedup_foodwasteapp

import android.app.Application
import android.content.Context
import com.google.firebase.FirebaseApp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit


class MyApplication : Application() {
    lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Initialize the NetworkMonitor
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        scheduleExpirationCheck(applicationContext)
    }


    fun startNetworkMonitoring() {
        networkMonitor.startMonitoring()
    }

    fun stopNetworkMonitoring() {
        networkMonitor.stopMonitoring()
    }
    override fun onTerminate() {
        super.onTerminate()
        // Stop network monitoring
        networkMonitor.stopMonitoring()
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
