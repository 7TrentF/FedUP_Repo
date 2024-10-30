package com.example.fedup_foodwasteapp

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
class SyncManager private constructor(
    private val context: Context,
    private val repository: IngredientRepository
) {
    private val syncService = SyncService(repository, context)
    private val connectivityManager = ConnectivityManager(syncService, context)

    fun startSync() {
        connectivityManager.startListening()
        if (connectivityManager.isNetworkAvailable()) {
            syncService.startSync()
        }
    }

    fun stopSync() {
        connectivityManager.stopListening()
    }

    companion object {
        @Volatile
        private var instance: SyncManager? = null

        fun getInstance(context: Context, repository: IngredientRepository): SyncManager {
            return instance ?: synchronized(this) {
                instance ?: SyncManager(context.applicationContext, repository).also {
                    instance = it
                }
            }
        }
    }
}