package com.example.fedup_foodwasteapp

import android.app.Application

class FedUpFoodWaste : Application() {
    lateinit var syncManager: SyncManager
    // Expose the database instance as a public property
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Any other initializations can be done here
    }
}